// wcore_dolphin.dll
//
// A Dolphin-libretro-only frontend for Working Consoles.
//
// The normal wcore_retro.dll shim is a software-frame frontend: it asks the core
// for a CPU picture and copies it into Java.  Dolphin's hardware renderer needs
// a real OpenGL context instead.  This shim creates a hidden WGL 3.3 context,
// shares it with Minecraft's context when possible, and renders Dolphin into an
// FBO.  The FBO's colour texture is either Minecraft's own GPU texture (passed
// in by the Java side) or a texture owned by this shim.
//
// No frame is read back for the normal in-world display path.  wr_copy_video is
// kept as a fallback for the Vulkan backend or if WGL sharing is unavailable.
//
// Build (x64):
//   call "...\vcvars64.bat"
//   cl /nologo /std:c++17 /O2 /EHsc /LD wcore_dolphin.cpp /link opengl32.lib user32.lib gdi32.lib /OUT:wcore_dolphin.dll

#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <windows.h>
#include <GL/gl.h>

#include "libretro.h"

#include <algorithm>
#include <atomic>
#include <cstdarg>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <map>
#include <mutex>
#include <string>
#include <vector>

#ifndef APIENTRY
#define APIENTRY
#endif

// ---------------------------------------------------------------------------
// WGL / OpenGL 3.x entry points that are not in opengl32.lib
// ---------------------------------------------------------------------------
#define WGL_CONTEXT_MAJOR_VERSION_ARB 0x2091
#define WGL_CONTEXT_MINOR_VERSION_ARB 0x2092
#define WGL_CONTEXT_PROFILE_MASK_ARB 0x9126
#define WGL_CONTEXT_CORE_PROFILE_BIT_ARB 0x00000001
#define WGL_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB 0x00000002

typedef HGLRC(APIENTRY* PFNWGLCREATECONTEXTATTRIBSARBPROC)(HDC, HGLRC, const int*);

#define GL_FRAMEBUFFER 0x8D40
#define GL_READ_FRAMEBUFFER 0x8CA8
#define GL_DRAW_FRAMEBUFFER 0x8CA9
#define GL_COLOR_ATTACHMENT0 0x8CE0
#define GL_FRAMEBUFFER_COMPLETE 0x8CD5
#define GL_RGBA8 0x8058
#define GL_CLAMP_TO_EDGE 0x812F
#define GL_PACK_ALIGNMENT 0x0D05
#define GL_UNPACK_ALIGNMENT 0x0CF5
#define GL_READ_BUFFER 0x0C02
#define GL_SCISSOR_TEST 0x0C11
#define GL_BLEND 0x0BE2
#define GL_DEPTH_TEST 0x0B71
#define GL_STENCIL_TEST 0x0B90
#define GL_CULL_FACE 0x0B44

typedef void(APIENTRY* PFNGLGENFRAMEBUFFERSPROC)(GLsizei, GLuint*);
typedef void(APIENTRY* PFNGLBINDFRAMEBUFFERPROC)(GLenum, GLuint);
typedef void(APIENTRY* PFNGLFRAMEBUFFERTEXTURE2DPROC)(GLenum, GLenum, GLenum, GLuint, GLint);
typedef GLenum(APIENTRY* PFNGLCHECKFRAMEBUFFERSTATUSPROC)(GLenum);
typedef void(APIENTRY* PFNGLDELETEFRAMEBUFFERSPROC)(GLsizei, const GLuint*);
typedef void(APIENTRY* PFNGLREADBUFFERPROC)(GLenum);
typedef void(APIENTRY* PFNWCBLITFRAMEBUFFERPROC)(GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLbitfield, GLenum);

static PFNGLGENFRAMEBUFFERSPROC p_glGenFramebuffers = nullptr;
static PFNGLBINDFRAMEBUFFERPROC p_glBindFramebuffer = nullptr;
static PFNGLFRAMEBUFFERTEXTURE2DPROC p_glFramebufferTexture2D = nullptr;
static PFNGLCHECKFRAMEBUFFERSTATUSPROC p_glCheckFramebufferStatus = nullptr;
static PFNGLDELETEFRAMEBUFFERSPROC p_glDeleteFramebuffers = nullptr;
static PFNGLREADBUFFERPROC p_glReadBuffer = nullptr;
static PFNWCBLITFRAMEBUFFERPROC p_glBlitFramebuffer = nullptr;

// ---------------------------------------------------------------------------
// Core symbols
// ---------------------------------------------------------------------------
static HMODULE g_core_module = nullptr;
static void (RETRO_CALLCONV *g_retro_init)(void) = nullptr;
static void (RETRO_CALLCONV *g_retro_deinit)(void) = nullptr;
static void (RETRO_CALLCONV *g_retro_get_system_info)(retro_system_info*) = nullptr;
static void (RETRO_CALLCONV *g_retro_get_system_av_info)(retro_system_av_info*) = nullptr;
static void (RETRO_CALLCONV *g_retro_set_environment)(retro_environment_t) = nullptr;
static void (RETRO_CALLCONV *g_retro_set_video_refresh)(retro_video_refresh_t) = nullptr;
static void (RETRO_CALLCONV *g_retro_set_audio_sample)(retro_audio_sample_t) = nullptr;
static void (RETRO_CALLCONV *g_retro_set_audio_sample_batch)(retro_audio_sample_batch_t) = nullptr;
static void (RETRO_CALLCONV *g_retro_set_input_poll)(retro_input_poll_t) = nullptr;
static void (RETRO_CALLCONV *g_retro_set_input_state)(retro_input_state_t) = nullptr;
static void (RETRO_CALLCONV *g_retro_set_controller_port_device)(unsigned, unsigned) = nullptr;
static bool (RETRO_CALLCONV *g_retro_load_game)(const retro_game_info*) = nullptr;
static void (RETRO_CALLCONV *g_retro_unload_game)(void) = nullptr;
static void (RETRO_CALLCONV *g_retro_run)(void) = nullptr;
static size_t (RETRO_CALLCONV *g_retro_serialize_size)(void) = nullptr;
static bool (RETRO_CALLCONV *g_retro_serialize)(void*, size_t) = nullptr;
static bool (RETRO_CALLCONV *g_retro_unserialize)(const void*, size_t) = nullptr;

static std::string g_system_dir;
static std::string g_save_dir;

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------
static HWND g_hwnd = nullptr;
static HDC g_hdc = nullptr;
static HGLRC g_gl = nullptr;
static HGLRC g_mc_gl = nullptr;
static HDC g_mc_dc = nullptr;
static bool g_shared_with_mc = false;
static bool g_context_ready = false;
static bool g_hw_set = false;
static bool g_direct = false;
static bool g_running = false;

static GLuint g_fbo = 0;
static GLuint g_tex = 0;
static bool g_owns_tex = false;

// Direct mode double-buffers: Dolphin draws into g_draw_tex, and only a finished
// frame is copied into g_tex (Minecraft's texture) through g_pub_fbo.  Drawing
// straight into Minecraft's texture let it copy half-drawn frames (flicker in
// heavy scenes, where a frame takes longest to draw).
static GLuint g_draw_tex = 0;
static GLuint g_pub_fbo = 0;
static std::atomic<bool> g_frame_pending{false};
static int g_tex_w = 640;
static int g_tex_h = 528;

// Java can hand us a RenderPearl/OpenGL texture before wr_create.  The texture
// belongs to Minecraft, so we only attach it to our FBO and never delete it.
static GLuint g_external_tex = 0;
static int g_external_w = 0;
static int g_external_h = 0;

static std::atomic<int> g_frame_w{640};
static std::atomic<int> g_frame_h{528};
static std::atomic<int> g_serial{0};
static std::atomic<int> g_video_cb_count{0};
static std::atomic<int> g_run_count{0};
static std::atomic<double> g_fps{60.0};
static std::atomic<int> g_sample_rate{48000};
static std::atomic<bool> g_quit{false};

static retro_hw_render_callback g_hw_render = {};
static retro_log_callback g_log_cb = {};
static FILE* g_log_file = nullptr;
static std::mutex g_log_mutex;

static std::mutex g_opt_mutex;
static std::map<std::string, std::string> g_options;
static std::map<std::string, std::string> g_option_defaults;

static std::mutex g_audio_mutex;
static std::vector<int16_t> g_audio;

struct PadState {
   std::atomic<int> keys{0};
   std::atomic<int> lx{0};
   std::atomic<int> ly{0};
   std::atomic<int> rx{0};
   std::atomic<int> ry{0};
};
static PadState g_pads[2];

static std::mutex g_run_mutex;

// ---------------------------------------------------------------------------
// Logging
// ---------------------------------------------------------------------------
static void log_line(const char* fmt, ...) {
   if (!g_log_file) return;
   std::lock_guard<std::mutex> lock(g_log_mutex);
   SYSTEMTIME st;
   GetLocalTime(&st);
   std::fprintf(g_log_file, "[%02d:%02d:%02d.%03d] ", st.wHour, st.wMinute, st.wSecond, st.wMilliseconds);
   va_list ap;
   va_start(ap, fmt);
   std::vfprintf(g_log_file, fmt, ap);
   va_end(ap);
   std::fputc('\n', g_log_file);
   std::fflush(g_log_file);
}

static void RETRO_CALLCONV core_log(enum retro_log_level level, const char* fmt, ...) {
   if (!g_log_file) return;
   const char* name = "info";
   switch (level) {
      case RETRO_LOG_DEBUG: name = "debug"; break;
      case RETRO_LOG_INFO: name = "info"; break;
      case RETRO_LOG_WARN: name = "warn"; break;
      case RETRO_LOG_ERROR: name = "error"; break;
      default: break;
   }
   std::lock_guard<std::mutex> lock(g_log_mutex);
   std::fprintf(g_log_file, "[core %s] ", name);
   va_list ap;
   va_start(ap, fmt);
   std::vfprintf(g_log_file, fmt, ap);
   va_end(ap);
   std::fputc('\n', g_log_file);
   std::fflush(g_log_file);
}

// ---------------------------------------------------------------------------
// WGL context
// ---------------------------------------------------------------------------
typedef HGLRC(APIENTRY* PFNWGLCREATECONTEXTATTRIBSARBPROC)(HDC, HGLRC, const int*);

static LRESULT CALLBACK wnd_proc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp) {
   return DefWindowProcW(hwnd, msg, wp, lp);
}

static bool ensure_window_class() {
   static bool done = false;
   if (done) return true;
   WNDCLASSEXW wc = {};
   wc.cbSize = sizeof(wc);
   wc.style = CS_OWNDC;
   wc.lpfnWndProc = wnd_proc;
   wc.hInstance = GetModuleHandleW(nullptr);
   wc.hCursor = LoadCursorW(nullptr, reinterpret_cast<LPCWSTR>(IDC_ARROW));
   wc.lpszClassName = L"WCoreDolphinGL";
   if (!RegisterClassExW(&wc) && GetLastError() != ERROR_CLASS_ALREADY_EXISTS) {
      return false;
   }
   done = true;
   return true;
}

static void* gl_proc(const char* name) {
   void* p = reinterpret_cast<void*>(wglGetProcAddress(name));
   if (p == nullptr || p == reinterpret_cast<void*>(1) || p == reinterpret_cast<void*>(2) ||
       p == reinterpret_cast<void*>(3) || p == reinterpret_cast<void*>(-1)) {
      static HMODULE opengl = LoadLibraryW(L"opengl32.dll");
      p = opengl ? reinterpret_cast<void*>(GetProcAddress(opengl, name)) : nullptr;
   }
   return p;
}

static bool load_gl_functions() {
   p_glGenFramebuffers = reinterpret_cast<PFNGLGENFRAMEBUFFERSPROC>(gl_proc("glGenFramebuffers"));
   p_glBindFramebuffer = reinterpret_cast<PFNGLBINDFRAMEBUFFERPROC>(gl_proc("glBindFramebuffer"));
   p_glFramebufferTexture2D = reinterpret_cast<PFNGLFRAMEBUFFERTEXTURE2DPROC>(gl_proc("glFramebufferTexture2D"));
   p_glCheckFramebufferStatus = reinterpret_cast<PFNGLCHECKFRAMEBUFFERSTATUSPROC>(gl_proc("glCheckFramebufferStatus"));
   p_glDeleteFramebuffers = reinterpret_cast<PFNGLDELETEFRAMEBUFFERSPROC>(gl_proc("glDeleteFramebuffers"));
   p_glReadBuffer = reinterpret_cast<PFNGLREADBUFFERPROC>(gl_proc("glReadBuffer"));
   p_glBlitFramebuffer = reinterpret_cast<PFNWCBLITFRAMEBUFFERPROC>(gl_proc("glBlitFramebuffer"));
   return p_glGenFramebuffers && p_glBindFramebuffer && p_glFramebufferTexture2D &&
          p_glCheckFramebufferStatus && p_glDeleteFramebuffers && p_glReadBuffer;
}

static int choose_pixel_format(HDC dc) {
   // Prefer the exact pixel format Minecraft's context uses.  wglShareLists
   // requires matching formats, and this is the only reliable way to get it.
   int format = 0;
   if (g_mc_dc) {
      format = GetPixelFormat(g_mc_dc);
      if (format) {
         PIXELFORMATDESCRIPTOR pfd = {};
         if (DescribePixelFormat(g_mc_dc, format, sizeof(pfd), &pfd) &&
             (pfd.dwFlags & PFD_SUPPORT_OPENGL) != 0) {
            if (SetPixelFormat(dc, format, &pfd)) {
               return format;
            }
         }
      }
   }

   PIXELFORMATDESCRIPTOR pfd = {};
   pfd.nSize = sizeof(pfd);
   pfd.nVersion = 1;
   pfd.dwFlags = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER;
   pfd.iPixelType = PFD_TYPE_RGBA;
   pfd.cColorBits = 32;
   pfd.cRedBits = 8;
   pfd.cGreenBits = 8;
   pfd.cBlueBits = 8;
   pfd.cAlphaBits = 8;
   pfd.cDepthBits = 24;
   pfd.cStencilBits = 8;
   pfd.iLayerType = PFD_MAIN_PLANE;
   format = ChoosePixelFormat(dc, &pfd);
   if (format && SetPixelFormat(dc, format, &pfd)) {
      return format;
   }
   return 0;
}

static bool create_context() {
   if (!ensure_window_class()) {
      log_line("could not register the hidden window class (%lu)", GetLastError());
      return false;
   }

   g_hwnd = CreateWindowExW(0, L"WCoreDolphinGL", L"wcore-dolphin", WS_POPUP,
                            0, 0, 1, 1, nullptr, nullptr, GetModuleHandleW(nullptr), nullptr);
   if (!g_hwnd) {
      log_line("could not create the hidden window (%lu)", GetLastError());
      return false;
   }
   g_hdc = GetDC(g_hwnd);
   if (!g_hdc) {
      log_line("could not get the hidden window DC");
      return false;
   }

   g_mc_gl = wglGetCurrentContext();
   g_mc_dc = wglGetCurrentDC();
   int format = choose_pixel_format(g_hdc);
   if (!format) {
      log_line("could not set a pixel format on the hidden window");
      return false;
   }
   log_line("pixel format %d (minecraft format %d)", format,
            g_mc_dc ? GetPixelFormat(g_mc_dc) : 0);

   // wglCreateContextAttribsARB can be queried while Minecraft's context is
   // current.  If there is no current context (for example a test harness),
   // create a temporary compatibility context just to get the entry point.
   HGLRC temp = nullptr;
   bool temp_current = false;
   if (!g_mc_gl) {
      temp = wglCreateContext(g_hdc);
      if (temp && wglMakeCurrent(g_hdc, temp)) {
         temp_current = true;
      }
   }

   auto create_attribs = reinterpret_cast<PFNWGLCREATECONTEXTATTRIBSARBPROC>(
      gl_proc("wglCreateContextAttribsARB"));

   if (create_attribs) {
      const int attribs[] = {
         WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
         WGL_CONTEXT_MINOR_VERSION_ARB, 3,
         WGL_CONTEXT_PROFILE_MASK_ARB, WGL_CONTEXT_CORE_PROFILE_BIT_ARB,
         0
      };
      g_gl = create_attribs(g_hdc, nullptr, attribs);
      if (!g_gl) {
         log_line("3.3 core context failed (%lu); trying compatibility context", GetLastError());
      }
   }
   if (!g_gl) {
      g_gl = wglCreateContext(g_hdc);
   }

   if (temp_current) {
      wglMakeCurrent(nullptr, nullptr);
   }
   if (temp) {
      wglDeleteContext(temp);
   }
   if (!g_gl) {
      log_line("could not create an OpenGL context (%lu)", GetLastError());
      return false;
   }

   // Share before making our context current and before creating any objects.
   if (g_mc_gl && g_mc_gl != g_gl) {
      if (wglShareLists(g_mc_gl, g_gl)) {
         g_shared_with_mc = true;
         log_line("shared the Dolphin GL context with Minecraft's context");
      } else {
         log_line("wglShareLists failed (%lu); will use the CPU fallback", GetLastError());
      }
   } else {
      log_line("no current Minecraft GL context; zero-copy texture sharing is unavailable");
   }

   if (!wglMakeCurrent(g_hdc, g_gl)) {
      log_line("could not make the Dolphin GL context current (%lu)", GetLastError());
      return false;
   }

   const char* version = reinterpret_cast<const char*>(glGetString(GL_VERSION));
   const char* renderer = reinterpret_cast<const char*>(glGetString(GL_RENDERER));
   log_line("OpenGL %s on %s", version ? version : "?", renderer ? renderer : "?");
   return true;
}

static bool create_fbo() {
   if (!load_gl_functions()) {
      log_line("required FBO entry points are missing");
      return false;
   }

   if (g_external_tex && g_shared_with_mc) {
      g_tex = g_external_tex;
      g_owns_tex = false;
      g_direct = true;
      g_tex_w = g_external_w > 0 ? g_external_w : 640;
      g_tex_h = g_external_h > 0 ? g_external_h : 528;
      log_line("using Minecraft's shared texture %u (%dx%d)", g_tex, g_tex_w, g_tex_h);
   } else {
      g_tex_w = g_external_w > 0 ? g_external_w : 640;
      g_tex_h = g_external_h > 0 ? g_external_h : 528;
      glGenTextures(1, &g_tex);
      glBindTexture(GL_TEXTURE_2D, g_tex);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, g_tex_w, g_tex_h, 0, GL_RGBA,
                   GL_UNSIGNED_BYTE, nullptr);
      g_owns_tex = true;
      g_direct = false;
      log_line("created an internal %dx%d screen texture %u", g_tex_w, g_tex_h, g_tex);
   }

   GLuint target = g_tex;
   if (g_direct && p_glBlitFramebuffer) {
      glGenTextures(1, &g_draw_tex);
      glBindTexture(GL_TEXTURE_2D, g_draw_tex);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
      std::vector<uint32_t> black((size_t)g_tex_w * g_tex_h, 0xFF000000u);   // a new texture holds garbage (grey bars)
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, g_tex_w, g_tex_h, 0, GL_RGBA, GL_UNSIGNED_BYTE, black.data());
      p_glGenFramebuffers(1, &g_pub_fbo);
      p_glBindFramebuffer(GL_FRAMEBUFFER, g_pub_fbo);
      p_glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, g_tex, 0);
      if (p_glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) {
         target = g_draw_tex;
         log_line("double-buffered: drawing into %u, publishing to %u", g_draw_tex, g_tex);
      } else {
         log_line("publish FBO incomplete; drawing straight into the shared texture");
         p_glDeleteFramebuffers(1, &g_pub_fbo);
         g_pub_fbo = 0;
         glDeleteTextures(1, &g_draw_tex);
         g_draw_tex = 0;
      }
   }

   p_glGenFramebuffers(1, &g_fbo);
   p_glBindFramebuffer(GL_FRAMEBUFFER, g_fbo);
   p_glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, target, 0);
   GLenum status = p_glCheckFramebufferStatus(GL_FRAMEBUFFER);
   p_glBindFramebuffer(GL_FRAMEBUFFER, 0);
   if (status != GL_FRAMEBUFFER_COMPLETE) {
      log_line("screen FBO is incomplete: 0x%04X", (unsigned)status);
      return false;
   }
   glViewport(0, 0, g_tex_w, g_tex_h);
   log_line("screen FBO %u is complete", g_fbo);
   return true;
}

static void destroy_fbo() {
   if (g_fbo && p_glDeleteFramebuffers) {
      p_glDeleteFramebuffers(1, &g_fbo);
      g_fbo = 0;
   }
   if (g_pub_fbo && p_glDeleteFramebuffers) {
      p_glDeleteFramebuffers(1, &g_pub_fbo);
      g_pub_fbo = 0;
   }
   if (g_draw_tex) {
      glDeleteTextures(1, &g_draw_tex);
      g_draw_tex = 0;
   }
   g_frame_pending.store(false);
   if (g_owns_tex && g_tex) {
      glDeleteTextures(1, &g_tex);
   }
   g_tex = 0;
   g_owns_tex = false;
   g_direct = false;
}

static void release_context() {
   if (wglGetCurrentContext() == g_gl) {
      wglMakeCurrent(nullptr, nullptr);
   }
   if (g_gl) {
      wglDeleteContext(g_gl);
      g_gl = nullptr;
   }
   if (g_hdc && g_hwnd) {
      ReleaseDC(g_hwnd, g_hdc);
      g_hdc = nullptr;
   }
   if (g_hwnd) {
      DestroyWindow(g_hwnd);
      g_hwnd = nullptr;
   }
}

// ---------------------------------------------------------------------------
// libretro callbacks
// ---------------------------------------------------------------------------
static uintptr_t RETRO_CALLCONV get_current_framebuffer(void) {
   static std::atomic<int> calls{0};
   int n = calls.fetch_add(1) + 1;
   if (n <= 10 || (n % 120) == 0) {
      log_line("get_current_framebuffer -> %u (call %d)", g_fbo, n);
   }
   return static_cast<uintptr_t>(g_fbo);
}

static retro_proc_address_t RETRO_CALLCONV get_proc_address(const char* sym) {
   return reinterpret_cast<retro_proc_address_t>(gl_proc(sym));
}

static std::string option_value(const char* key) {
   if (!key) return {};
   std::lock_guard<std::mutex> lock(g_opt_mutex);
   auto it = g_options.find(key);
   if (it != g_options.end()) return it->second;
   it = g_option_defaults.find(key);
   if (it != g_option_defaults.end()) return it->second;
   return {};
}

static void remember_legacy_variables(const retro_variable* vars) {
   if (!vars) return;
   std::lock_guard<std::mutex> lock(g_opt_mutex);
   for (const retro_variable* v = vars; v->key; ++v) {
      if (!v->key || !v->value) continue;
      std::string spec(v->value);
      std::string value;
      size_t semi = spec.find("; ");
      if (semi != std::string::npos) {
         size_t start = semi + 2;
         size_t bar = spec.find('|', start);
         value = spec.substr(start, bar == std::string::npos ? std::string::npos : bar - start);
      }
      if (!value.empty() && g_option_defaults.find(v->key) == g_option_defaults.end()) {
         g_option_defaults[v->key] = value;
      }
   }
}

static void remember_v2_options(const retro_core_options_v2* opts) {
   if (!opts || !opts->definitions) return;
   std::lock_guard<std::mutex> lock(g_opt_mutex);
   for (const retro_core_option_v2_definition* d = opts->definitions; d->key; ++d) {
      if (d->default_value && g_option_defaults.find(d->key) == g_option_defaults.end()) {
         g_option_defaults[d->key] = d->default_value;
      }
   }
}

static bool RETRO_CALLCONV environment(unsigned cmd, void* data) {
   switch (cmd) {
      case RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY:
         if (data) *static_cast<const char**>(data) = g_system_dir.c_str();
         return true;
      case RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY:
         if (data) *static_cast<const char**>(data) = g_save_dir.c_str();
         return true;
      case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
         return data && (*static_cast<const enum retro_pixel_format*>(data) == RETRO_PIXEL_FORMAT_XRGB8888 ||
                         *static_cast<const enum retro_pixel_format*>(data) == RETRO_PIXEL_FORMAT_RGB565);
      case RETRO_ENVIRONMENT_GET_CAN_DUPE:
         if (data) *static_cast<bool*>(data) = true;
         return true;
      case RETRO_ENVIRONMENT_GET_VARIABLE: {
         auto* var = static_cast<retro_variable*>(data);
         if (!var || !var->key) return false;
         static thread_local std::string value;
         value = option_value(var->key);
         if (value.empty()) {
            var->value = nullptr;
            return false;
         }
         var->value = value.c_str();
         return true;
      }
      case RETRO_ENVIRONMENT_GET_VARIABLE_UPDATE:
         if (data) *static_cast<bool*>(data) = false;
         return true;
      case RETRO_ENVIRONMENT_SET_VARIABLES:
         remember_legacy_variables(static_cast<const retro_variable*>(data));
         return true;
      case RETRO_ENVIRONMENT_SET_CORE_OPTIONS:
         remember_legacy_variables(static_cast<const retro_variable*>(data));
         return true;
      case RETRO_ENVIRONMENT_SET_CORE_OPTIONS_V2:
         remember_v2_options(static_cast<const retro_core_options_v2*>(data));
         return true;
      case RETRO_ENVIRONMENT_GET_CORE_OPTIONS_VERSION:
         if (data) *static_cast<unsigned*>(data) = 0;
         return true;
      case RETRO_ENVIRONMENT_SET_HW_RENDER: {
         auto* cb = static_cast<retro_hw_render_callback*>(data);
         if (!cb) return false;
         if (cb->context_type != RETRO_HW_CONTEXT_OPENGL_CORE &&
             cb->context_type != RETRO_HW_CONTEXT_OPENGL) {
            log_line("core requested unsupported hardware context %d", (int)cb->context_type);
            return false;
         }
         cb->get_current_framebuffer = get_current_framebuffer;
         cb->get_proc_address = get_proc_address;
         g_hw_render = *cb;
         g_hw_set = true;
         log_line("accepted hardware renderer (context type %d, version %u.%u)", (int)cb->context_type,
                  cb->version_major, cb->version_minor);
         return true;
      }
      case RETRO_ENVIRONMENT_GET_PREFERRED_HW_RENDER:
         if (data) *static_cast<enum retro_hw_context_type*>(data) = RETRO_HW_CONTEXT_OPENGL_CORE;
         return true;
      case RETRO_ENVIRONMENT_SET_HW_SHARED_CONTEXT:
         return true;
      case RETRO_ENVIRONMENT_GET_LOG_INTERFACE:
         if (data) {
            g_log_cb.log = core_log;
            *static_cast<retro_log_callback*>(data) = g_log_cb;
         }
         return true;
      case RETRO_ENVIRONMENT_SET_PERFORMANCE_LEVEL:
         return true;
      case RETRO_ENVIRONMENT_SET_CONTROLLER_INFO:
      case RETRO_ENVIRONMENT_SET_INPUT_DESCRIPTORS:
      case RETRO_ENVIRONMENT_SET_SUPPORT_NO_GAME:
      case RETRO_ENVIRONMENT_SET_GEOMETRY:
      case RETRO_ENVIRONMENT_SET_SYSTEM_AV_INFO:
         if (cmd == RETRO_ENVIRONMENT_SET_GEOMETRY && data) {
            auto* g = static_cast<const retro_game_geometry*>(data);
            if (g->base_width && g->base_height) {
               g_frame_w.store((int)g->base_width);
               g_frame_h.store((int)g->base_height);
            }
         }
         return true;
      case RETRO_ENVIRONMENT_GET_LANGUAGE:
         if (data) *static_cast<unsigned*>(data) = RETRO_LANGUAGE_ENGLISH;
         return true;
      case RETRO_ENVIRONMENT_GET_INPUT_BITMASKS:
         if (data) *static_cast<bool*>(data) = false;
         return true;
      case RETRO_ENVIRONMENT_GET_RUMBLE_INTERFACE:
      case RETRO_ENVIRONMENT_GET_SENSOR_INTERFACE:
      case RETRO_ENVIRONMENT_GET_CAMERA_INTERFACE:
      case RETRO_ENVIRONMENT_GET_LOCATION_INTERFACE:
      case RETRO_ENVIRONMENT_GET_VFS_INTERFACE:
         return false;
      case RETRO_ENVIRONMENT_SHUTDOWN:
         g_quit.store(true);
         return true;
      default:
         return false;
   }
}

static void RETRO_CALLCONV video_cb(const void* data, unsigned width, unsigned height, size_t pitch) {
   (void)pitch;
   int call = g_video_cb_count.fetch_add(1) + 1;
   if (data == RETRO_HW_FRAME_BUFFER_VALID) {
      if (width && height) {
         g_frame_w.store((int)width);
         g_frame_h.store((int)height);
      }
      // The frame is not finished on the GPU yet: wr_run_frame publishes it after glFinish.
      int serial = g_serial.load();
      if (g_pub_fbo) {
         g_frame_pending.store(true);
      } else {
         serial = g_serial.fetch_add(1, std::memory_order_release) + 1;
      }
      if (call <= 10 || (call % 60) == 0) {
         log_line("video_cb HW_FRAME_BUFFER_VALID call=%d %ux%u serial=%d", call, width, height, serial);
      }
   } else if (data == nullptr) {
      // Duplicate frame: deliberately do not increment the serial.
      if (call <= 10) log_line("video_cb duplicate call=%d %ux%u", call, width, height);
   } else {
      // A software core reached this shim by mistake.  There is no CPU buffer
      // here; hardware rendering is the only supported path.
      if (width && height) {
         g_frame_w.store((int)width);
         g_frame_h.store((int)height);
      }
      int serial = g_serial.fetch_add(1, std::memory_order_release) + 1;
      log_line("video_cb SOFTWARE data=%p call=%d %ux%u serial=%d", data, call, width, height, serial);
   }
}

static void append_audio(const int16_t* frames, size_t count) {
   if (!frames || count == 0) return;
   std::lock_guard<std::mutex> lock(g_audio_mutex);
   // Bound the queue to roughly two seconds.  A stalled consumer must not make
   // the emulator allocate without limit.
   const size_t max_frames = 96000;
   if (g_audio.size() / 2 + count > max_frames) {
      size_t drop = g_audio.size() / 2 + count - max_frames;
      if (drop >= g_audio.size() / 2) {
         g_audio.clear();
      } else {
         g_audio.erase(g_audio.begin(), g_audio.begin() + drop * 2);
      }
   }
   size_t old = g_audio.size();
   g_audio.resize(old + count * 2);
   std::memcpy(g_audio.data() + old, frames, count * 2 * sizeof(int16_t));
}

static void RETRO_CALLCONV audio_sample(int16_t left, int16_t right) {
   int16_t pair[2] = {left, right};
   append_audio(pair, 1);
}

static size_t RETRO_CALLCONV audio_batch(const int16_t* data, size_t frames) {
   append_audio(data, frames);
   return frames;
}

static void RETRO_CALLCONV input_poll(void) {
}

static int16_t RETRO_CALLCONV input_state(unsigned port, unsigned device, unsigned index, unsigned id) {
   if (port > 1) return 0;
   PadState& p = g_pads[port];
   if (device == RETRO_DEVICE_JOYPAD) {
      if (id == RETRO_DEVICE_ID_JOYPAD_MASK) {
         return (int16_t)p.keys.load(std::memory_order_relaxed);
      }
      if (id < 16) {
         return (p.keys.load(std::memory_order_relaxed) & (1 << id)) ? 1 : 0;
      }
      return 0;
   }
   if (device == RETRO_DEVICE_ANALOG) {
      if (index == RETRO_DEVICE_INDEX_ANALOG_LEFT) {
         if (id == RETRO_DEVICE_ID_ANALOG_X) return (int16_t)p.lx.load(std::memory_order_relaxed);
         if (id == RETRO_DEVICE_ID_ANALOG_Y) return (int16_t)p.ly.load(std::memory_order_relaxed);
      } else if (index == RETRO_DEVICE_INDEX_ANALOG_RIGHT) {
         if (id == RETRO_DEVICE_ID_ANALOG_X) return (int16_t)p.rx.load(std::memory_order_relaxed);
         if (id == RETRO_DEVICE_ID_ANALOG_Y) return (int16_t)p.ry.load(std::memory_order_relaxed);
      }
   }
   return 0;
}

// ---------------------------------------------------------------------------
// Core loading
// ---------------------------------------------------------------------------
template <typename T>
static bool core_symbol(const char* name, T& out) {
   out = reinterpret_cast<T>(GetProcAddress(g_core_module, name));
   if (!out) {
      log_line("core is missing %s", name);
      return false;
   }
   return true;
}

static bool load_core(const char* path) {
   std::wstring wide;
   int n = MultiByteToWideChar(CP_UTF8, 0, path, -1, nullptr, 0);
   if (n <= 0) return false;
   wide.resize((size_t)n);
   MultiByteToWideChar(CP_UTF8, 0, path, -1, wide.data(), n);
   g_core_module = LoadLibraryW(wide.c_str());
   if (!g_core_module) {
      log_line("LoadLibrary failed for %s (%lu)", path, GetLastError());
      return false;
   }
   return core_symbol("retro_init", g_retro_init) &&
          core_symbol("retro_deinit", g_retro_deinit) &&
          core_symbol("retro_get_system_info", g_retro_get_system_info) &&
          core_symbol("retro_get_system_av_info", g_retro_get_system_av_info) &&
          core_symbol("retro_set_environment", g_retro_set_environment) &&
          core_symbol("retro_set_video_refresh", g_retro_set_video_refresh) &&
          core_symbol("retro_set_audio_sample", g_retro_set_audio_sample) &&
          core_symbol("retro_set_audio_sample_batch", g_retro_set_audio_sample_batch) &&
          core_symbol("retro_set_input_poll", g_retro_set_input_poll) &&
          core_symbol("retro_set_input_state", g_retro_set_input_state) &&
          core_symbol("retro_set_controller_port_device", g_retro_set_controller_port_device) &&
          core_symbol("retro_load_game", g_retro_load_game) &&
          core_symbol("retro_unload_game", g_retro_unload_game) &&
          core_symbol("retro_run", g_retro_run) &&
          core_symbol("retro_serialize_size", g_retro_serialize_size) &&
          core_symbol("retro_serialize", g_retro_serialize) &&
          core_symbol("retro_unserialize", g_retro_unserialize);
}

static void parse_options(const char* text) {
   std::lock_guard<std::mutex> lock(g_opt_mutex);
   if (!text) return;
   const char* p = text;
   while (*p) {
      const char* end = std::strchr(p, '\n');
      std::string line(p, end ? (size_t)(end - p) : std::strlen(p));
      size_t eq = line.find('=');
      if (eq != std::string::npos) {
         std::string key = line.substr(0, eq);
         std::string value = line.substr(eq + 1);
         if (!key.empty()) g_options[key] = value;
      }
      if (!end) break;
      p = end + 1;
   }
}

// ---------------------------------------------------------------------------
// Exported API (same wr_* surface as wcore_retro.dll plus optional extras)
// ---------------------------------------------------------------------------
extern "C" {

__declspec(dllexport) void wr_set_shared_texture(int texture, int width, int height) {
   g_external_tex = (GLuint)texture;
   g_external_w = width;
   g_external_h = height;
}

__declspec(dllexport) int wr_shared_texture_ok(void) {
   return g_direct ? 1 : 0;
}

__declspec(dllexport) int wr_video_texture(void) {
   return (g_direct && g_tex) ? (int)g_tex : 0;
}

__declspec(dllexport) void wr_texture_size(int* width, int* height) {
   if (width) *width = g_tex_w;
   if (height) *height = g_tex_h;
}

__declspec(dllexport) int wr_frame_serial(void) {
   return g_serial.load(std::memory_order_acquire);
}

// Debug-only: prove the FBO -> shared-texture path without Dolphin.  A test
// frontend can clear the shim's FBO red and then sample the same texture from
// its own context.  If this is black, context sharing or FBO ownership is bad.
__declspec(dllexport) int wr_debug_clear(float r, float g, float b, float a) {
   if (!g_fbo || !g_gl) return 0;
   std::lock_guard<std::mutex> lock(g_run_mutex);
   if (wglGetCurrentContext() != g_gl) {
      if (!wglMakeCurrent(g_hdc, g_gl)) return 0;
   }
   p_glBindFramebuffer(GL_FRAMEBUFFER, g_fbo);
   // Dolphin can leave scissor/color-mask state behind; a debug clear must not
   // inherit it.  glClear is affected by scissor and the colour write mask.
   glDisable(GL_SCISSOR_TEST);
   glDisable(GL_BLEND);
   glDisable(GL_DEPTH_TEST);
   glDisable(GL_STENCIL_TEST);
   glDisable(GL_CULL_FACE);
   glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
   glViewport(0, 0, g_tex_w, g_tex_h);
   glClearColor(r, g, b, a);
   glClear(GL_COLOR_BUFFER_BIT);
   glFinish();
   log_line("debug clear FBO %u to %.2f %.2f %.2f %.2f", g_fbo, r, g, b, a);
   return 1;
}

__declspec(dllexport) int wr_create(const char* core_path, const char* game_path,
                                    const char* system_dir, const char* save_dir,
                                    const char* options, const char* log_path) {
   if (g_running) return 4;
   g_system_dir = system_dir ? system_dir : "";
   g_save_dir = save_dir ? save_dir : "";
   g_quit.store(false);

   if (log_path && *log_path) {
      std::wstring wide;
      int n = MultiByteToWideChar(CP_UTF8, 0, log_path, -1, nullptr, 0);
      if (n > 0) {
         wide.resize((size_t)n);
         MultiByteToWideChar(CP_UTF8, 0, log_path, -1, wide.data(), n);
         g_log_file = _wfopen(wide.c_str(), L"w");
      }
   }
   log_line("starting Dolphin shim");
   if (!core_path || !*core_path) {
      log_line("no core path");
      return 1;
   }
   if (!load_core(core_path)) {
      log_line("could not load the core");
      return 1;
   }

   parse_options(options);

   if (!create_context()) {
      log_line("could not create the OpenGL context");
      return 4;
   }
   if (!create_fbo()) {
      log_line("could not create the screen framebuffer");
      return 4;
   }

   g_retro_set_environment(environment);
   g_retro_set_video_refresh(video_cb);
   g_retro_set_audio_sample(audio_sample);
   g_retro_set_audio_sample_batch(audio_batch);
   g_retro_set_input_poll(input_poll);
   g_retro_set_input_state(input_state);
   g_retro_init();

   retro_game_info game = {};
   game.path = game_path && *game_path ? game_path : nullptr;
   game.data = nullptr;
   game.size = 0;
   game.meta = nullptr;
   if (!g_retro_load_game(&game)) {
      // Dolphin leaves boot threads behind when a malformed DOL fails to
      // boot, and retro_deinit can wait forever on them.  Keep the module
      // loaded and let the process own it rather than hanging the game thread.
      log_line("retro_load_game failed; leaving the core loaded");
      destroy_fbo();
      release_context();
      return 3;
   }
   g_running = true;

   g_retro_set_controller_port_device(0, RETRO_DEVICE_JOYPAD);
   g_retro_set_controller_port_device(1, RETRO_DEVICE_JOYPAD);

   retro_system_av_info av = {};
   g_retro_get_system_av_info(&av);
   if (av.timing.fps > 0.0) g_fps.store(av.timing.fps);
   if (av.timing.sample_rate > 0.0) g_sample_rate.store((int)(av.timing.sample_rate + 0.5));
   if (av.geometry.base_width && av.geometry.base_height) {
      g_frame_w.store((int)av.geometry.base_width);
      g_frame_h.store((int)av.geometry.base_height);
   }
   log_line("core ready: %.3f fps, %d Hz audio, frame %dx%d, texture %dx%d, direct=%d",
            g_fps.load(), g_sample_rate.load(), g_frame_w.load(), g_frame_h.load(),
            g_tex_w, g_tex_h, g_direct ? 1 : 0);

   // Restore Minecraft's context if we temporarily replaced it.  The emulator
   // thread will make this context current for the first frame.
   if (g_mc_gl && g_mc_dc) {
      wglMakeCurrent(g_mc_dc, g_mc_gl);
   } else if (wglGetCurrentContext() == g_gl) {
      wglMakeCurrent(nullptr, nullptr);
   }

   if (!g_hw_set) {
      log_line("the core did not request a hardware renderer; refusing software fallback");
      return 3;
   }
   return 0;
}

__declspec(dllexport) void wr_destroy(void) {
   if (!g_running && !g_core_module) {
      return;
   }
   std::lock_guard<std::mutex> lock(g_run_mutex);
   bool was_running = g_running;
   if (wglGetCurrentContext() != g_gl && g_hdc && g_gl) {
      wglMakeCurrent(g_hdc, g_gl);
   }
   if (was_running) {
      g_running = false;
      if (g_hw_render.context_destroy) g_hw_render.context_destroy();
      g_retro_unload_game();
      g_retro_deinit();
   }
   destroy_fbo();
   release_context();
   if (g_core_module && was_running) {
      FreeLibrary(g_core_module);
      g_core_module = nullptr;
   } else if (g_core_module) {
      log_line("leaving a failed-boot core module loaded to avoid a deinit hang");
      g_core_module = nullptr;
   }
   {
      std::lock_guard<std::mutex> alock(g_audio_mutex);
      g_audio.clear();
   }
   g_hw_set = false;
   g_context_ready = false;
   g_direct = false;
   g_external_tex = 0;
   g_external_w = g_external_h = 0;
   log_line("Dolphin shim closed");
   if (g_log_file) {
      std::fclose(g_log_file);
      g_log_file = nullptr;
   }
}

__declspec(dllexport) void wr_run_frame(void) {
   if (!g_running) return;
   std::lock_guard<std::mutex> lock(g_run_mutex);
   if (!g_context_ready) {
      if (!wglMakeCurrent(g_hdc, g_gl)) {
         log_line("could not make the GL context current on the emulator thread (%lu)", GetLastError());
         return;
      }
      p_glBindFramebuffer(GL_FRAMEBUFFER, g_fbo);
      glViewport(0, 0, g_tex_w, g_tex_h);
      if (g_hw_render.context_reset) {
         log_line("calling context_reset");
         g_hw_render.context_reset();
      }
      g_context_ready = true;
      p_glBindFramebuffer(GL_FRAMEBUFFER, g_fbo);
   }

   g_retro_run();

   // Dolphin's GL backend renders directly to the system framebuffer (our
   // FBO).  A finish makes the texture visible to Minecraft's shared context
   // without any CPU readback.  The emulator thread pays this cost, never the
   // Minecraft render thread.
   p_glBindFramebuffer(GL_FRAMEBUFFER, g_fbo);
   glFinish();
   if (g_pub_fbo && g_frame_pending.exchange(false)) {
      // publish the finished frame in one short GPU copy, then tell Java about it
      p_glBindFramebuffer(GL_READ_FRAMEBUFFER, g_fbo);
      p_glBindFramebuffer(GL_DRAW_FRAMEBUFFER, g_pub_fbo);
      // Dolphin leaves its scissor box on, and a blit obeys it: only part of the frame was copied
      // (bands, cut-off picture, old and new frames mixed = flicker)
      GLboolean scissor = glIsEnabled(GL_SCISSOR_TEST);
      GLboolean mask[4];
      glGetBooleanv(GL_COLOR_WRITEMASK, mask);
      if (scissor) glDisable(GL_SCISSOR_TEST);
      glColorMask(GL_TRUE, GL_TRUE, GL_TRUE, GL_TRUE);
      p_glBlitFramebuffer(0, 0, g_tex_w, g_tex_h, 0, 0, g_tex_w, g_tex_h, GL_COLOR_BUFFER_BIT, GL_NEAREST);
      glColorMask(mask[0], mask[1], mask[2], mask[3]);
      if (scissor) glEnable(GL_SCISSOR_TEST);
      p_glBindFramebuffer(GL_FRAMEBUFFER, g_fbo);
      glFinish();
      g_serial.fetch_add(1, std::memory_order_release);
   }
   int n = g_run_count.fetch_add(1) + 1;
   if (n <= 5 || (n % 60) == 0) {
      log_line("wr_run_frame n=%d serial=%d size=%dx%d", n, g_serial.load(), g_frame_w.load(),
               g_frame_h.load());
   }
}

__declspec(dllexport) void wr_set_input(int keys, int lx, int ly, int rx, int ry) {
   g_pads[0].keys.store(keys);
   g_pads[0].lx.store(lx);
   g_pads[0].ly.store(ly);
   g_pads[0].rx.store(rx);
   g_pads[0].ry.store(ry);
}

__declspec(dllexport) void wr_set_input_port(int port, int keys, int lx, int ly, int rx, int ry) {
   if (port < 0 || port > 1) return;
   g_pads[port].keys.store(keys);
   g_pads[port].lx.store(lx);
   g_pads[port].ly.store(ly);
   g_pads[port].rx.store(rx);
   g_pads[port].ry.store(ry);
}

__declspec(dllexport) void wr_set_options(const char* options) {
   parse_options(options);
}

__declspec(dllexport) int wr_dump_options(const char* path) {
   (void)path;
   return 0;
}

__declspec(dllexport) void wr_video_size(int* width, int* height) {
   if (width) *width = g_frame_w.load(std::memory_order_relaxed);
   if (height) *height = g_frame_h.load(std::memory_order_relaxed);
}

__declspec(dllexport) int wr_copy_video(int* dst, int max_pixels) {
   if (!g_fbo || !dst || max_pixels <= 0) return 0;
   int w = g_frame_w.load(std::memory_order_relaxed);
   int h = g_frame_h.load(std::memory_order_relaxed);
   if (w <= 0 || h <= 0 || w * h > max_pixels) return 0;

   std::lock_guard<std::mutex> lock(g_run_mutex);
   if (wglGetCurrentContext() != g_gl) {
      if (!wglMakeCurrent(g_hdc, g_gl)) return 0;
   }
   p_glBindFramebuffer(GL_READ_FRAMEBUFFER, g_fbo);
   p_glReadBuffer(GL_COLOR_ATTACHMENT0);
   glPixelStorei(GL_PACK_ALIGNMENT, 4);
   std::vector<uint8_t> tmp((size_t)w * h * 4);
   glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, tmp.data());
   for (int y = 0; y < h; ++y) {
      const uint8_t* src = tmp.data() + (size_t)(h - 1 - y) * w * 4;
      uint32_t* row = reinterpret_cast<uint32_t*>(dst) + (size_t)y * w;
      for (int x = 0; x < w; ++x) {
         row[x] = 0xFF000000u | ((uint32_t)src[x * 4] << 16) |
                  ((uint32_t)src[x * 4 + 1] << 8) | (uint32_t)src[x * 4 + 2];
      }
   }
   return w * h;
}

__declspec(dllexport) int wr_audio_rate(void) {
   return g_sample_rate.load();
}

__declspec(dllexport) int wr_read_audio(int16_t* dst, int max_frames) {
   if (!dst || max_frames <= 0) return 0;
   std::lock_guard<std::mutex> lock(g_audio_mutex);
   int available = (int)(g_audio.size() / 2);
   int n = std::min(available, max_frames);
   if (n > 0) {
      std::memcpy(dst, g_audio.data(), (size_t)n * 2 * sizeof(int16_t));
      g_audio.erase(g_audio.begin(), g_audio.begin() + n * 2);
   }
   return n;
}

__declspec(dllexport) double wr_fps(void) {
   return g_fps.load();
}

__declspec(dllexport) int wr_version(void) {
   return 2;
}

__declspec(dllexport) int wr_save_state(const char* path) {
   if (!path || !g_running) return 0;
   std::lock_guard<std::mutex> lock(g_run_mutex);
   size_t size = g_retro_serialize_size();
   if (!size) return 0;
   std::vector<uint8_t> data(size);
   if (!g_retro_serialize(data.data(), size)) return 0;
   std::wstring wide;
   int n = MultiByteToWideChar(CP_UTF8, 0, path, -1, nullptr, 0);
   if (n <= 0) return 0;
   wide.resize((size_t)n);
   MultiByteToWideChar(CP_UTF8, 0, path, -1, wide.data(), n);
   FILE* f = _wfopen(wide.c_str(), L"wb");
   if (!f) return 0;
   size_t wrote = std::fwrite(data.data(), 1, size, f);
   std::fclose(f);
   return wrote == size ? 1 : 0;
}

__declspec(dllexport) int wr_load_state(const char* path) {
   if (!path || !g_running) return 0;
   std::lock_guard<std::mutex> lock(g_run_mutex);
   std::wstring wide;
   int n = MultiByteToWideChar(CP_UTF8, 0, path, -1, nullptr, 0);
   if (n <= 0) return 0;
   wide.resize((size_t)n);
   MultiByteToWideChar(CP_UTF8, 0, path, -1, wide.data(), n);
   FILE* f = _wfopen(wide.c_str(), L"rb");
   if (!f) return 0;
   std::fseek(f, 0, SEEK_END);
   long size = std::ftell(f);
   std::fseek(f, 0, SEEK_SET);
   if (size <= 0) {
      std::fclose(f);
      return 0;
   }
   std::vector<uint8_t> data((size_t)size);
   size_t got = std::fread(data.data(), 1, (size_t)size, f);
   std::fclose(f);
   if (got != (size_t)size) return 0;
   return g_retro_unserialize(data.data(), (size_t)size) ? 1 : 0;
}

} // extern "C"



