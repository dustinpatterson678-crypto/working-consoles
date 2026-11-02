// Standalone smoke test for wcore_dolphin.dll.
//
// Creates a "fake Minecraft" WGL context, allocates a screen texture, shares
// it with the shim, runs a GameCube DOL for a few hundred frames, and checks
// that the texture contains a non-black picture.  This catches context sharing,
// FBO, frame-serial and threading mistakes without launching Minecraft.

#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#include <windows.h>
#include <GL/gl.h>
#include <dbghelp.h>
#include <tlhelp32.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>
#include <thread>
#include <vector>

#pragma comment(lib, "dbghelp.lib")

#define WGL_CONTEXT_MAJOR_VERSION_ARB 0x2091
#define WGL_CONTEXT_MINOR_VERSION_ARB 0x2092
#define WGL_CONTEXT_PROFILE_MASK_ARB 0x9126
#define WGL_CONTEXT_CORE_PROFILE_BIT_ARB 0x00000001
#define GL_FRAMEBUFFER 0x8D40
#define GL_COLOR_ATTACHMENT0 0x8CE0
#define GL_FRAMEBUFFER_COMPLETE 0x8CD5
#define GL_CLAMP_TO_EDGE 0x812F
#define GL_RGBA8 0x8058

typedef HGLRC(APIENTRY* PFNWGLCREATECONTEXTATTRIBSARBPROC)(HDC, HGLRC, const int*);
typedef void(APIENTRY* PFNGLGENFRAMEBUFFERSPROC)(GLsizei, GLuint*);
typedef void(APIENTRY* PFNGLBINDFRAMEBUFFERPROC)(GLenum, GLuint);
typedef void(APIENTRY* PFNGLFRAMEBUFFERTEXTURE2DPROC)(GLenum, GLenum, GLenum, GLuint, GLint);
typedef GLenum(APIENTRY* PFNGLCHECKFRAMEBUFFERSTATUSPROC)(GLenum);
typedef void(APIENTRY* PFNGLDELETEFRAMEBUFFERSPROC)(GLsizei, const GLuint*);

struct Api {
   HMODULE dll = nullptr;
   int (*create)(const char*, const char*, const char*, const char*, const char*, const char*) = nullptr;
   void (*destroy)() = nullptr;
   void (*run)() = nullptr;
   void (*setInput)(int, int, int, int, int) = nullptr;
   void (*setShared)(int, int, int) = nullptr;
   int (*sharedOk)() = nullptr;
   int (*videoTexture)() = nullptr;
   void (*textureSize)(int*, int*) = nullptr;
   int (*serial)() = nullptr;
   void (*videoSize)(int*, int*) = nullptr;
   int (*copyVideo)(int*, int) = nullptr;
   int (*audioRate)() = nullptr;
   int (*readAudio)(int16_t*, int) = nullptr;
   double (*fps)() = nullptr;
   int (*save)(const char*) = nullptr;
   int (*load)(const char*) = nullptr;
   int (*debugClear)(float, float, float, float) = nullptr;
};

static LRESULT CALLBACK wnd_proc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp) {
   return DefWindowProcW(hwnd, msg, wp, lp);
}

static void* gl_proc(const char* name) {
   void* p = reinterpret_cast<void*>(wglGetProcAddress(name));
   if (!p || p == reinterpret_cast<void*>(1) || p == reinterpret_cast<void*>(2) ||
       p == reinterpret_cast<void*>(3) || p == reinterpret_cast<void*>(-1)) {
      static HMODULE opengl = LoadLibraryW(L"opengl32.dll");
      p = opengl ? reinterpret_cast<void*>(GetProcAddress(opengl, name)) : nullptr;
   }
   return p;
}

struct FakeMc {
   HWND hwnd = nullptr;
   HDC dc = nullptr;
   HGLRC gl = nullptr;
   GLuint tex = 0;
   int width = 640;
   int height = 528;
   bool ok = false;

   bool init(bool makeTexture) {
      WNDCLASSEXW wc = {};
      wc.cbSize = sizeof(wc);
      wc.style = CS_OWNDC;
      wc.lpfnWndProc = wnd_proc;
      wc.hInstance = GetModuleHandleW(nullptr);
      wc.hCursor = LoadCursorW(nullptr, reinterpret_cast<LPCWSTR>(IDC_ARROW));
      wc.lpszClassName = L"WCoreDolphinFakeMC";
      if (!RegisterClassExW(&wc) && GetLastError() != ERROR_CLASS_ALREADY_EXISTS) {
         std::printf("RegisterClassEx failed %lu\n", GetLastError());
         return false;
      }
      hwnd = CreateWindowExW(0, L"WCoreDolphinFakeMC", L"fake-mc", WS_POPUP,
                             0, 0, 8, 8, nullptr, nullptr, GetModuleHandleW(nullptr), nullptr);
      if (!hwnd) return false;
      dc = GetDC(hwnd);
      PIXELFORMATDESCRIPTOR pfd = {};
      pfd.nSize = sizeof(pfd);
      pfd.nVersion = 1;
      pfd.dwFlags = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER;
      pfd.iPixelType = PFD_TYPE_RGBA;
      pfd.cColorBits = 32;
      pfd.cAlphaBits = 8;
      pfd.cDepthBits = 24;
      pfd.cStencilBits = 8;
      pfd.iLayerType = PFD_MAIN_PLANE;
      int format = ChoosePixelFormat(dc, &pfd);
      if (!format || !SetPixelFormat(dc, format, &pfd)) {
         std::printf("SetPixelFormat failed %lu\n", GetLastError());
         return false;
      }
      HGLRC temp = wglCreateContext(dc);
      if (!temp || !wglMakeCurrent(dc, temp)) {
         std::printf("temporary context failed %lu\n", GetLastError());
         return false;
      }
      auto createAttribs = reinterpret_cast<PFNWGLCREATECONTEXTATTRIBSARBPROC>(gl_proc("wglCreateContextAttribsARB"));
      if (!createAttribs) {
         std::printf("wglCreateContextAttribsARB missing\n");
         return false;
      }
      const int attribs[] = {
         WGL_CONTEXT_MAJOR_VERSION_ARB, 3,
         WGL_CONTEXT_MINOR_VERSION_ARB, 3,
         WGL_CONTEXT_PROFILE_MASK_ARB, WGL_CONTEXT_CORE_PROFILE_BIT_ARB,
         0
      };
      gl = createAttribs(dc, nullptr, attribs);
      wglMakeCurrent(nullptr, nullptr);
      wglDeleteContext(temp);
      if (!gl || !wglMakeCurrent(dc, gl)) {
         std::printf("core context failed %lu\n", GetLastError());
         return false;
      }
      std::printf("fake MC OpenGL: %s / %s\n", glGetString(GL_VERSION), glGetString(GL_RENDERER));
      if (makeTexture) {
         glGenTextures(1, &tex);
         glBindTexture(GL_TEXTURE_2D, tex);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
         glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, nullptr);
         glBindTexture(GL_TEXTURE_2D, 0);
         std::printf("fake MC texture: %u (%dx%d)\n", tex, width, height);
      }
      ok = true;
      return true;
   }

   int countNonBlack(const char* label = "texture") {
      if (!tex) return -1;
      if (!wglMakeCurrent(dc, gl)) return -2;
      std::vector<uint8_t> pixels((size_t)width * height * 4);
      glBindTexture(GL_TEXTURE_2D, tex);
      glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels.data());
      glBindTexture(GL_TEXTURE_2D, 0);
      int nonblack = 0;
      uint64_t sum = 0;
      for (size_t i = 0; i < pixels.size(); i += 4) {
         int v = pixels[i] + pixels[i + 1] + pixels[i + 2];
         sum += v;
         if (v > 8) ++nonblack;
      }
      std::printf("%s nonblack=%d/%d sum=%llu\n", label, nonblack, width * height, (unsigned long long)sum);
      FILE* f = nullptr;
      fopen_s(&f, "texture.ppm", "wb");
      if (f) {
         std::fprintf(f, "P6\n%d %d\n255\n", width, height);
         // glGetTexImage is bottom-up; write top-down for viewing.
         for (int y = height - 1; y >= 0; --y) {
            for (int x = 0; x < width; ++x) {
               const uint8_t* p = &pixels[((size_t)y * width + x) * 4];
               std::fwrite(p, 1, 3, f);
            }
         }
         std::fclose(f);
      }
      return nonblack;
   }
};

static std::atomic<long long> g_lastFrameMs{0};
static std::atomic<bool> g_stackDumped{false};

static long long now_ms() {
   return std::chrono::duration_cast<std::chrono::milliseconds>(
              std::chrono::steady_clock::now().time_since_epoch()).count();
}

static void dump_thread_stacks() {
   HANDLE process = GetCurrentProcess();
   SymSetOptions(SYMOPT_UNDNAME | SYMOPT_DEFERRED_LOADS | SYMOPT_LOAD_LINES);
   if (!SymInitialize(process, nullptr, TRUE)) {
      std::printf("SymInitialize failed %lu\n", GetLastError());
   }

   HANDLE snap = CreateToolhelp32Snapshot(TH32CS_SNAPTHREAD, 0);
   if (snap == INVALID_HANDLE_VALUE) {
      std::printf("thread snapshot failed %lu\n", GetLastError());
      return;
   }

   THREADENTRY32 te = {};
   te.dwSize = sizeof(te);
   if (Thread32First(snap, &te)) {
      do {
         if (te.th32OwnerProcessID != GetCurrentProcessId() ||
             te.th32ThreadID == GetCurrentThreadId()) {
            continue;
         }
         HANDLE thread = OpenThread(THREAD_SUSPEND_RESUME | THREAD_GET_CONTEXT |
                                    THREAD_QUERY_INFORMATION,
                                    FALSE, te.th32ThreadID);
         if (!thread) continue;
         DWORD suspended = SuspendThread(thread);
         CONTEXT ctx = {};
         ctx.ContextFlags = CONTEXT_FULL;
         if (GetThreadContext(thread, &ctx)) {
            std::printf("\n=== thread %lu (suspend=%lu) RIP=%016llX RSP=%016llX RBP=%016llX ===\n",
                        (unsigned long)te.th32ThreadID, (unsigned long)suspended,
                        (unsigned long long)ctx.Rip, (unsigned long long)ctx.Rsp,
                        (unsigned long long)ctx.Rbp);
            STACKFRAME64 frame = {};
            frame.AddrPC.Offset = ctx.Rip;
            frame.AddrPC.Mode = AddrModeFlat;
            frame.AddrStack.Offset = ctx.Rsp;
            frame.AddrStack.Mode = AddrModeFlat;
            frame.AddrFrame.Offset = ctx.Rbp;
            frame.AddrFrame.Mode = AddrModeFlat;
            for (int depth = 0; depth < 32; ++depth) {
               if (!StackWalk64(IMAGE_FILE_MACHINE_AMD64, process, thread, &frame, &ctx,
                                nullptr, SymFunctionTableAccess64, SymGetModuleBase64, nullptr)) {
                  break;
               }
               DWORD64 pc = frame.AddrPC.Offset;
               if (!pc) break;
               IMAGEHLP_MODULE64 mod = {};
               mod.SizeOfStruct = sizeof(mod);
               const char* module = "?";
               DWORD64 base = 0;
               if (SymGetModuleInfo64(process, pc, &mod)) {
                  module = mod.ModuleName[0] ? mod.ModuleName : mod.ImageName;
                  base = mod.BaseOfImage;
               }
               unsigned char symbol_buffer[sizeof(SYMBOL_INFO) + 256] = {};
               SYMBOL_INFO* symbol = reinterpret_cast<SYMBOL_INFO*>(symbol_buffer);
               symbol->SizeOfStruct = sizeof(SYMBOL_INFO);
               symbol->MaxNameLen = 255;
               DWORD64 displacement = 0;
               const char* name = nullptr;
               if (SymFromAddr(process, pc, &displacement, symbol)) {
                  name = symbol->Name;
               }
               DWORD64 function_base = 0;
               PRUNTIME_FUNCTION runtime_function =
                   RtlLookupFunctionEntry(pc, &function_base, nullptr);
               if (name) {
                  std::printf("  %s!%s+0x%llX", module, name,
                              (unsigned long long)displacement);
               } else {
                  std::printf("  %s+0x%llX", module, (unsigned long long)(pc - base));
               }
               if (runtime_function) {
                  std::printf(" pc=%016llX fn=%016llX rva=%llX",
                              (unsigned long long)pc,
                              (unsigned long long)(function_base +
                                                   runtime_function->BeginAddress),
                              (unsigned long long)runtime_function->BeginAddress);
               } else {
                  std::printf(" pc=%016llX fn=?", (unsigned long long)pc);
               }
               std::printf("\n");
            }
         }
         ResumeThread(thread);
         CloseHandle(thread);
      } while (Thread32Next(snap, &te));
   }
   CloseHandle(snap);
   SymCleanup(process);
}

static void start_watchdog() {
   std::thread([] {
      while (!g_stackDumped.load()) {
         std::this_thread::sleep_for(std::chrono::milliseconds(500));
         long long last = g_lastFrameMs.load();
         if (last && now_ms() - last > 6000) {
            std::printf("\n!!! no frame progress for %lld ms; dumping thread stacks\n",
                        (long long)(now_ms() - last));
            if (!g_stackDumped.exchange(true)) {
               dump_thread_stacks();
            }
            break;
         }
      }
   }).detach();
}

static bool load_api(Api& api, const char* path) {
   std::wstring wide;
   int n = MultiByteToWideChar(CP_UTF8, 0, path, -1, nullptr, 0);
   if (n <= 0) return false;
   wide.resize((size_t)n);
   MultiByteToWideChar(CP_UTF8, 0, path, -1, wide.data(), n);
   api.dll = LoadLibraryW(wide.c_str());
   if (!api.dll) {
      std::printf("could not load shim %s: %lu\n", path, GetLastError());
      return false;
   }
#define GET(alias, symbol) api.alias = reinterpret_cast<decltype(api.alias)>(GetProcAddress(api.dll, symbol))
   GET(create, "wr_create");
   GET(destroy, "wr_destroy");
   GET(run, "wr_run_frame");
   GET(setInput, "wr_set_input");
   GET(setShared, "wr_set_shared_texture");
   GET(sharedOk, "wr_shared_texture_ok");
   GET(videoTexture, "wr_video_texture");
   GET(textureSize, "wr_texture_size");
   GET(serial, "wr_frame_serial");
   GET(videoSize, "wr_video_size");
   GET(copyVideo, "wr_copy_video");
   GET(audioRate, "wr_audio_rate");
   GET(readAudio, "wr_read_audio");
   GET(fps, "wr_fps");
   GET(save, "wr_save_state");
   GET(load, "wr_load_state");
   GET(debugClear, "wr_debug_clear");
#undef GET
   std::printf("load_api create=%p destroy=%p run=%p serial=%p shared=%p\n",
               (void*)api.create, (void*)api.destroy, (void*)api.run,
               (void*)api.serial, (void*)api.setShared);
   return api.create && api.destroy && api.run && api.serial;
}

static std::string u8(const char* p) {
   return p ? std::string(p) : std::string();
}

int main(int argc, char** argv) {
   setvbuf(stdout, nullptr, _IONBF, 0);
   SetProcessDPIAware();
   const char* core = argc > 1 ? argv[1] : "C:\\Users\\dustin\\AppData\\Local\\Temp\\opencode\\dolphin\\core\\dolphin_libretro.dll";
   const char* game = argc > 2 ? argv[2] : "C:\\Users\\dustin\\AppData\\Local\\Temp\\opencode\\dolphin\\game\\neverball\\default.dol";
   const char* sys = argc > 3 ? argv[3] : "C:\\Users\\dustin\\AppData\\Local\\Temp\\opencode\\dolphin\\sys\\gamecube";
   const char* save = argc > 4 ? argv[4] : "C:\\Users\\dustin\\AppData\\Local\\Temp\\opencode\\dolphin\\save\\gamecube";
   int frames = argc > 5 ? std::atoi(argv[5]) : 300;
   bool useTexture = argc <= 6 || std::strcmp(argv[6], "fallback") != 0;

   FakeMc mc;
   if (!mc.init(useTexture)) return 1;

   Api api;
   if (!load_api(api, "wcore_dolphin.dll")) return 2;

   if (useTexture) {
      api.setShared(mc.tex, mc.width, mc.height);
   }
   std::printf("wr_create...\n");
   int rc = api.create(core, game, sys, save,
                       "dolphin_renderer=Hardware\ndolphin_efb_scale=1\ndolphin_cpu_core=JIT64\n"
                       "dolphin_main_cpu_thread=enabled\ndolphin_fastmem=enabled\n"
                       "dolphin_fastmem_arena=enabled\ndolphin_dsp_hle=enabled\ndolphin_skip_gc_bios=enabled\n",
                       "shim-test.log");
   std::printf("wr_create=%d shared_ok=%d video_tex=%u fps=%.3f audio=%d\n", rc,
               api.sharedOk ? api.sharedOk() : -1,
               api.videoTexture ? api.videoTexture() : 0,
               api.fps ? api.fps() : 0.0, api.audioRate ? api.audioRate() : 0);
   if (rc != 0) {
      api.destroy();
      return 3;
   }

   int tw = 0, th = 0;
   if (api.textureSize) api.textureSize(&tw, &th);
   std::printf("wr_texture_size=%dx%d\n", tw, th);

   int lastSerial = 0;
   int newFrames = 0;
   int maxW = 0, maxH = 0;
   double totalCallMs = 0.0;
   double maxCallMs = 0.0;
   int callsOver16 = 0;
   const auto frameDuration = std::chrono::microseconds(16683); // 59.94 Hz
   g_lastFrameMs.store(now_ms());
   start_watchdog();
   auto start = std::chrono::high_resolution_clock::now();
   for (int i = 0; i < frames; ++i) {
      auto deadline = start + frameDuration * i;
      std::this_thread::sleep_until(deadline);
      if (api.setInput) api.setInput(0, 0, 0, 0, 0);
      auto callStart = std::chrono::steady_clock::now();
      api.run();
      auto callEnd = std::chrono::steady_clock::now();
      double callMs = std::chrono::duration<double, std::milli>(callEnd - callStart).count();
      totalCallMs += callMs;
      maxCallMs = std::max(maxCallMs, callMs);
      if (callMs > 16.683) ++callsOver16;
      if (callMs > 100.0) {
         std::printf("frame %d retro_run took %.1f ms\n", i, callMs);
      }
      g_lastFrameMs.store(now_ms());
      int serial = api.serial();
      if (serial != lastSerial) {
         ++newFrames;
         lastSerial = serial;
         if (api.videoSize) api.videoSize(&maxW, &maxH);
      }
      if ((i % 60) == 0) {
         std::printf("frame %d serial=%d size=%dx%d\n", i, serial, maxW, maxH);
      }
   }
   auto end = std::chrono::high_resolution_clock::now();
   double secs = std::chrono::duration<double>(end - start).count();
   std::printf("ran %d frames in %.3fs (%.2f fps), new serials=%d, last serial=%d, size=%dx%d\n",
               frames, secs, frames / secs, newFrames, lastSerial, maxW, maxH);
   std::printf("retro_run timing avg=%.3f ms max=%.3f ms over16=%.1f%%\n",
               totalCallMs / frames, maxCallMs, 100.0 * callsOver16 / frames);

   int16_t audio[4096]; // 2048 stereo frames
   int got = api.readAudio ? api.readAudio(audio, 2048) : 0;
   std::printf("audio frames read=%d\n", got);

   if (useTexture) {
      int w = 0, h = 0;
      if (api.videoSize) api.videoSize(&w, &h);
      if (w > 0 && h > 0 && api.copyVideo) {
         std::vector<int> pixels((size_t)w * h);
         int n = api.copyVideo(pixels.data(), (int)pixels.size());
         int nonblack = 0;
         for (int v : pixels) {
            if (((v >> 16) & 0xFF) + ((v >> 8) & 0xFF) + (v & 0xFF) > 8) ++nonblack;
         }
         std::printf("shim-side FBO copy=%d nonblack=%d/%d\n", n, nonblack, w * h);
         FILE* f = nullptr;
         fopen_s(&f, "shim-fbo.ppm", "wb");
         if (f) {
            std::fprintf(f, "P6\n%d %d\n255\n", w, h);
            for (int y = 0; y < h; ++y) {
               for (int x = 0; x < w; ++x) {
                  int v = pixels[(size_t)y * w + x];
                  uint8_t p[3] = {(uint8_t)((v >> 16) & 0xFF), (uint8_t)((v >> 8) & 0xFF), (uint8_t)(v & 0xFF)};
                  std::fwrite(p, 1, 3, f);
               }
            }
            std::fclose(f);
         }
      }
      mc.countNonBlack("demo texture");
      std::printf("wr_debug_clear red=%d\n", api.debugClear ? api.debugClear(1.0f, 0.0f, 0.0f, 1.0f) : -1);
      int w2 = 0, h2 = 0;
      if (api.videoSize) api.videoSize(&w2, &h2);
      std::vector<int> red((size_t)w2 * h2);
      int redN = api.copyVideo ? api.copyVideo(red.data(), (int)red.size()) : 0;
      int redSum = 0;
      for (int v : red) redSum += (v >> 16) & 0xFF;
      std::printf("shim-side red clear copy=%d red-sum=%d\n", redN, redSum);
      mc.countNonBlack("red texture");
   } else {
      int w = 0, h = 0;
      if (api.videoSize) api.videoSize(&w, &h);
      if (w > 0 && h > 0) {
         std::vector<int> pixels((size_t)w * h);
         int n = api.copyVideo(pixels.data(), (int)pixels.size());
         int nonblack = 0;
         for (int v : pixels) {
            if (((v >> 16) & 0xFF) + ((v >> 8) & 0xFF) + (v & 0xFF) > 8) ++nonblack;
         }
         std::printf("fallback copy=%d nonblack=%d/%d\n", n, nonblack, w * h);
         FILE* f = nullptr;
         fopen_s(&f, "fallback.ppm", "wb");
         if (f) {
            std::fprintf(f, "P6\n%d %d\n255\n", w, h);
            for (int y = 0; y < h; ++y) {
               for (int x = 0; x < w; ++x) {
                  int v = pixels[(size_t)y * w + x];
                  uint8_t p[3] = {(uint8_t)((v >> 16) & 0xFF), (uint8_t)((v >> 8) & 0xFF), (uint8_t)(v & 0xFF)};
                  std::fwrite(p, 1, 3, f);
               }
            }
            std::fclose(f);
         }
      }
   }

   if (api.save) {
      int saved = api.save("shim-test.state");
      int loaded = api.load("shim-test.state");
      std::printf("save=%d load=%d\n", saved, loaded);
   }

   api.destroy();
   std::printf("closed\n");
   return 0;
}
