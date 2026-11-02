package com.dustin.stream.client;

import java.nio.IntBuffer;
import java.util.List;
import net.minecraft.core.BlockPos;
import org.cef.browser.CefFrame;
import org.lwjgl.sdl.SDLGamepad;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLStdinc;

/**
 * Controllers on the Smart TV.
 *
 * Minecraft has no gamepad layer of its own, so this reads the pad straight from the same SDL
 * that Minecraft already runs on, then feeds the values into the page as an ordinary browser
 * gamepad ({@code navigator.getGamepads}). Websites and web games that already support
 * controllers simply work - nothing to change on the game's side. For pages that never touch the
 * Gamepad API (normal websites), the page shim additionally turns the D-pad / left stick into
 * arrow keys and A/B into Enter/Escape, so a controller can still drive menus and video players.
 *
 * Input flows only while this screen is the one being used (the controls screen), exactly like
 * the mouse and the keyboard.
 */
final class BrowserGamepad {
   /** How far a stick must move before it counts as movement. */
   private static final float DEAD = 0.12F;
   /** At most one state push per this many milliseconds (the game runs 60 FPS, the block tick 20). */
   private static final long PUSH_MS = 8L;
   private static final Object LOCK = new Object();

   /** Browser "standard mapping" order, filled with SDL button names (LT/RT come from the trigger axes). */
   private static final int[] BROWSER_BUTTONS = {
      SDLGamepad.SDL_GAMEPAD_BUTTON_SOUTH,          // 0  A / Cross
      SDLGamepad.SDL_GAMEPAD_BUTTON_EAST,           // 1  B / Circle
      SDLGamepad.SDL_GAMEPAD_BUTTON_WEST,           // 2  X / Square
      SDLGamepad.SDL_GAMEPAD_BUTTON_NORTH,          // 3  Y / Triangle
      SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER,  // 4  LB / L1
      SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER, // 5  RB / R1
      -1, -1,                                        // 6, 7 LT / RT (analog, from axes)
      SDLGamepad.SDL_GAMEPAD_BUTTON_BACK,           // 8  Back / View / Share
      SDLGamepad.SDL_GAMEPAD_BUTTON_START,          // 9  Start / Menu / Options
      SDLGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK,     // 10 L3
      SDLGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK,    // 11 R3
      SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP,        // 12
      SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN,      // 13
      SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT,      // 14
      SDLGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT,     // 15
      SDLGamepad.SDL_GAMEPAD_BUTTON_GUIDE           // 16 Home / Guide / PS
   };

   private static boolean tried;
   private static boolean live;
   private static long pad;      // opened SDL gamepad, 0 = none
   private static String name = "";
   private static long nextScan;
   private static long lastPush;
   private static final float[] AXES = new float[4];
   private static final float[] VALUES = new float[17];

   private BrowserGamepad() {
   }

   /** The controller's name, or null when none is connected - shown in the on-screen hint. */
   static String padName() {
      synchronized (LOCK) {
         return live && pad != 0 ? name : null;
      }
   }

   /** Runs 20x per second for every browser session near the player: follows the pad, hands it to the used screen. */
   static void tick(BlockPos pos, Browsers.Session s) {
      synchronized (LOCK) {
         if (!ensure()) {
            return;
         }
         if (pos.equals(InputFx.controlling)) {
            use(s);
         } else {
            release(s);
         }
      }
   }

   /** Runs every drawn frame of the screen you are using, so pad input feels like 60 FPS input. */
   static void frame(Browsers.Session s) {
      synchronized (LOCK) {
         if (!ensure()) {
            return;
         }
         use(s);
      }
   }

   private static void use(Browsers.Session s) {
      scan();
      if (pad == 0) {
         release(s);
         return;
      }
      long now = System.currentTimeMillis();
      if (now - lastPush < PUSH_MS) {
         return;
      }
      lastPush = now;
      try {
         read();
      } catch (Throwable t) {
         close();
         release(s);
         return;
      }
      s.padSent = true;
      script(s, payload());
   }

   /** Tells the page the pad went away (once). */
   private static void release(Browsers.Session s) {
      if (!s.padSent) {
         return;
      }
      s.padSent = false;
      script(s, "{p:0}");
   }

   private static boolean ensure() {
      if (tried) {
         return live;
      }
      tried = true;
      try {
         live = SDLInit.SDL_InitSubSystem(SDLInit.SDL_INIT_GAMEPAD) || SDLInit.SDL_WasInit(SDLInit.SDL_INIT_GAMEPAD) != 0;
      } catch (Throwable t) {
         live = false;
      }
      return live;
   }

   /** Opens the first controller SDL can see, and re-runs when it is unplugged; at most once per second. */
   private static void scan() {
      if (pad != 0) {
         if (SDLGamepad.SDL_GamepadConnected(pad)) {
            return;
         }
         close();
      }
      long now = System.currentTimeMillis();
      if (now < nextScan) {
         return;
      }
      nextScan = now + 1000L;
      IntBuffer ids = null;
      try {
         ids = SDLGamepad.SDL_GetGamepads();
         if (ids != null) {
            for (int i = 0; i < ids.limit(); i++) {
               int id = ids.get(i);
               if (id == 0) {
                  continue;
               }
               long h = SDLGamepad.SDL_OpenGamepad(id);
               if (h != 0) {
                  pad = h;
                  String n = SDLGamepad.SDL_GetGamepadName(h);
                  name = n == null || n.isBlank() ? "Controller" : n;
                  lastPush = 0L;
                  break;
               }
            }
         }
      } finally {
         if (ids != null) {
            SDLStdinc.SDL_free(ids);
         }
      }
   }

   private static void close() {
      if (pad != 0) {
         try {
            SDLGamepad.SDL_CloseGamepad(pad);
         } catch (Throwable ignored) {
         }
         pad = 0;
         name = "";
      }
   }

   private static void read() {
      SDLGamepad.SDL_UpdateGamepads();
      stick(SDLGamepad.SDL_GAMEPAD_AXIS_LEFTX, SDLGamepad.SDL_GAMEPAD_AXIS_LEFTY, 0);
      stick(SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTX, SDLGamepad.SDL_GAMEPAD_AXIS_RIGHTY, 2);
      for (int i = 0; i < BROWSER_BUTTONS.length; i++) {
         VALUES[i] = BROWSER_BUTTONS[i] < 0 ? 0F : (SDLGamepad.SDL_GetGamepadButton(pad, BROWSER_BUTTONS[i]) ? 1F : 0F);
      }
      VALUES[6] = trigger(SDLGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER);
      VALUES[7] = trigger(SDLGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER);
   }

   /** SDL and the browser's "standard mapping" agree: left -1 / right 1, up -1 / down 1. */
   private static void stick(int axisX, int axisY, int out) {
      float x = axis(axisX);
      float y = axis(axisY);
      float m = (float)Math.sqrt(x * x + y * y);
      if (m < DEAD) {
         AXES[out] = 0F;
         AXES[out + 1] = 0F;
         return;
      }
      float k = (m - DEAD) / (1F - DEAD) / m;
      AXES[out] = clamp(x * k);
      AXES[out + 1] = clamp(y * k);
   }

   private static float axis(int which) {
      return clamp(SDLGamepad.SDL_GetGamepadAxis(pad, which) / 32767F);
   }

   private static float trigger(int which) {
      return clamp(SDLGamepad.SDL_GetGamepadAxis(pad, which) / 32767F);
   }

   private static float clamp(float v) {
      return v < -1F ? -1F : v > 1F ? 1F : v;
   }

   private static String payload() {
      StringBuilder b = new StringBuilder(192).append("{p:1,n:\"").append(clean(name)).append("\",a:[");
      for (int i = 0; i < 4; i++) {
         if (i > 0) {
            b.append(',');
         }
         dec(b, AXES[i]);
      }
      b.append("],v:[");
      for (int i = 0; i < VALUES.length; i++) {
         if (i > 0) {
            b.append(',');
         }
         if (i == 6 || i == 7) {
            dec(b, VALUES[i]);
         } else {
            b.append(VALUES[i] > 0.5F ? '1' : '0');
         }
      }
      return b.append("]}").toString();
   }

   private static String clean(String s) {
      return s.replace("\\", "").replace("\"", "'");
   }

   /** 3-decimal number without the cost of String.format (this runs every frame). */
   private static void dec(StringBuilder b, float v) {
      int i = Math.round(v * 1000F);
      if (i == 0) {
         b.append('0');
         return;
      }
      if (i < 0) {
         b.append('-');
         i = -i;
      }
      b.append(i / 1000).append('.');
      int frac = i % 1000;
      if (frac < 100) {
         b.append('0');
      }
      if (frac < 10) {
         b.append('0');
      }
      b.append(frac);
   }

   /** Hands one state to the page, in every frame of it (games often live in an iframe). */
   private static void script(Browsers.Session s, String payload) {
      js(s, "window.__wcPad&&window.__wcPad(" + payload + ")");
   }

   /** Runs one piece of JavaScript in every frame of the page (falls back to the main frame). */
   static void js(Browsers.Session s, String code) {
      List<CefFrame> frames = s.frames;
      if (frames.isEmpty()) {
         try {
            CefFrame main = s.browser.getCefBrowser().getMainFrame();
            if (main != null) {
               main.executeJavaScript(code, "wc-pad", 0);
            }
         } catch (Throwable ignored) {
         }
         return;
      }
      for (CefFrame frame : frames) {
         try {
            frame.executeJavaScript(code, "wc-pad", 0);
         } catch (Throwable ignored) {
         }
      }
   }

   /** F9 on the controls screen: draws (or hides) the on-page controller check panel. */
   static void toggleOverlay(Browsers.Session s) {
      overlayOn = !overlayOn;
      js(s, OVERLAY);
      js(s, "window.__wcOverlay&&window.__wcOverlay(" + (overlayOn ? "1" : "0") + ");");
   }

   private static boolean overlayOn;

   /**
    * The page half, injected with the key/focus shim. It answers navigator.getGamepads() with a
    * live "standard" pad built from whatever the player does on the real controller, fires the
    * connect/disconnect events engines listen for, and - only on pages that never poll the
    * Gamepad API - walks menus with arrow keys and scrolls with the right stick.
    */
   static final String SHIM = """
      (function(){
      if(window.__wcPadShim)return;window.__wcPadShim=1;
      var st=window.__wcPadState;
      if(!st){st={a:[0,0,0,0],v:[],p:0,t:0,u:0,n:'',f:0,log:[],fa:0};window.__wcPadState=st;}
      if(!st.log)st.log=[];
      function now(){return (window.performance&&performance.now)?performance.now():Date.now();}
      function ev(t){try{var d=new Date();st.log.unshift(('0'+d.getHours()).slice(-2)+':'+('0'+d.getMinutes()).slice(-2)+':'+('0'+d.getSeconds()).slice(-2)+' '+t);if(st.log.length>12)st.log.length=12;}catch(e){}}
      var pad={
      id:'Working Consoles Controller (standard mapping)',
      index:0,
      mapping:'standard',
      get connected(){return !!st.p;},
      get timestamp(){return st.t||0;},
      get axes(){var a=st.a||[];return [+a[0]||0,+a[1]||0,+a[2]||0,+a[3]||0];},
      get buttons(){var v=st.v||[],o=[],i,x;for(i=0;i<17;i++){x=+v[i]||0;o.push({pressed:x>0.5,touched:x>0,value:x});}return o;},
      get touches(){return [];},
      vibrationActuator:{effects:['dual-rumble'],playEffect:function(){return Promise.resolve('complete');},reset:function(){return Promise.resolve('complete');}}
      };
      var orig=null;
      try{orig=Navigator.prototype.getGamepads;}catch(e){}
      function getPads(){
      var out=[],i,p;
      try{if(orig){var real=orig.call(navigator)||[];for(i=0;i<real.length;i++){p=real[i];if(p){out.push(p);}}}}catch(e){}
      st.u=now();
      if(st.p){pad.index=0;out.unshift(pad);}
      return out;
      }
      function fire(type){
      var e=null;
      try{e=new GamepadEvent(type,{gamepad:pad});}catch(x){
      try{e=new Event(type);Object.defineProperty(e,'gamepad',{value:pad});}catch(y){}
      }
      if(!e)return;
      try{window.dispatchEvent(e);}catch(x){}
      }
      window.__wcPad=function(j){
      if(!j)return;
      if(j.a)st.a=j.a;
      if(j.v)st.v=j.v;
      if(j.n){st.n=String(j.n);try{pad.id=st.n;}catch(e){}}
      st.t=now();
      var was=!!st.p;st.p=!!j.p;
      if(st.p!==was){fire(st.p?'gamepadconnected':'gamepaddisconnected');ev(st.p?'controller connected':'controller released');}
      };
      try{Navigator.prototype.getGamepads=getPads;}catch(e){
      try{Object.defineProperty(navigator,'getGamepads',{value:getPads,configurable:true});}catch(x){}
      }
      try{if(!Navigator.prototype.webkitGetGamepads){Navigator.prototype.webkitGetGamepads=getPads;}}catch(e){}
      try{if(!navigator.webkitGetGamepads){Object.defineProperty(navigator,'webkitGetGamepads',{value:getPads,configurable:true});}}catch(e){}
      var KEYS={0:['Enter',13],1:['Escape',27],8:['Escape',27],9:['Enter',13],12:['ArrowUp',38],13:['ArrowDown',40],14:['ArrowLeft',37],15:['ArrowRight',39]};
      var held={};
      function send(type,k){
      var info=KEYS[k];if(!info)return;
      var e=null;
      try{e=new KeyboardEvent(type,{key:info[0],code:info[0],keyCode:info[1],which:info[1],bubbles:true,cancelable:true});}catch(x){
      try{e=new Event(type,{bubbles:true,cancelable:true});Object.defineProperty(e,'key',{value:info[0]});Object.defineProperty(e,'keyCode',{value:info[1]});Object.defineProperty(e,'which',{value:info[1]});}catch(y){return;}
      }
      try{if(type==='keydown')ev('sent '+info[0]);}catch(x){}
      try{(document.activeElement||document.body||document).dispatchEvent(e);}catch(x){}
      }
      function releaseKeys(){
      var k,ks=[];for(k in held){ks.push(k);}
      for(var i=0;i<ks.length;i++){delete held[ks[i]];send('keyup',ks[i]);}
      }
      function editing(){
      var a=document.activeElement;if(!a)return false;
      var n=a.tagName;
      if(n==='TEXTAREA'||n==='SELECT')return true;
      if(a.isContentEditable)return true;
      if(n==='INPUT'){var ty=String(a.type||'text').toLowerCase();return ['hidden','button','submit','reset','checkbox','radio','file','range','color','image'].indexOf(ty)<0;}
      return false;
      }
      function fsAbort(){var e=null;try{e=new DOMException('The user aborted a request.','AbortError');}catch(x){e=new Error('aborted');}return e;}
function fsHandle(f){
      var url=(f&&f._url)?f._url:null;
      try{if(!url&&f instanceof Blob)url=URL.createObjectURL(f);}catch(e){}
      if(!url)url=(f&&f.url)?f.url:'about:blank';
      return {kind:'file',name:f.name,size:f.size||0,type:f.type||'application/octet-stream',lastModified:f.lastModified||Date.now(),
      getFile:function(){return Promise.resolve(f);},
      isSameEntry:function(o){return Promise.resolve(!!o&&o.name===f.name);},
      createWritable:function(){return Promise.reject(fsAbort());},
      queryPermission:function(){return Promise.resolve('granted');},
      requestPermission:function(){return Promise.resolve('granted');},
      text:function(){return (f.text?f.text():Promise.resolve(''));},
      arrayBuffer:function(){return (f.arrayBuffer?f.arrayBuffer():Promise.resolve(new ArrayBuffer(0)));},
      stream:function(){return (f.stream?f.stream():Promise.resolve(null));},
      slice:function(s,e,ct){if(f.slice)return f.slice(s,e,ct);var sl={size:Math.max(0,(e||f.size||0)-(s||0)),type:ct||f.type||'application/octet-stream',_url:url};sl.arrayBuffer=function(){return fetch(url,{headers:{'Range':'bytes='+(s||0)+'-'+((e||f.size||0)-1)}}).then(function(r){return r.arrayBuffer();});};sl.text=function(){return fetch(url,{headers:{'Range':'bytes='+(s||0)+'-'+((e||f.size||0)-1)}}).then(function(r){return r.text();});};sl.stream=function(){return fetch(url,{headers:{'Range':'bytes='+(s||0)+'-'+((e||f.size||0)-1)}}).then(function(r){return r.body;});};return sl;},
      url:url};
      }
      function fsAccept(o){
      var out=[],seen={},i,m,exts,j,e;
      try{var ts=o&&o.types;if(ts){for(i=0;i<ts.length;i++){var acc=ts[i]&&ts[i].accept;if(acc){for(m in acc){exts=acc[m];if(exts){for(j=0;j<exts.length;j++){e=String(exts[j]);if(/^\\.[a-z0-9]{1,8}$/i.test(e)&&!seen[e]){seen[e]=1;out.push(e);}}}}}}}}
      catch(x){}
      return out.join(',');
      }
      function fsPick(o,multiple){
      return new Promise(function(res,rej){
      try{
      var inp=document.createElement('input');
      inp.type='file';
      if(multiple)inp.multiple=true;
      var acc=fsAccept(o);
      inp.accept=acc||'.iso,.bin,.img,.chd,.cso,.cue,.zip,.7z,.rar,.gz';
      inp.style.cssText='position:fixed;left:-9999px;top:0;width:1px;height:1px;opacity:0;';
      var done=0;
      function cleanup(){try{inp.remove();}catch(e){}}
      inp.addEventListener('change',function(){
      if(done)return;done=1;
      var fs=inp.files,arr=[],i;
      for(i=0;i<(fs?fs.length:0);i++)arr.push(fsHandle(fs[i]));
      cleanup();
      if(arr.length){ev('file picked for the page: '+arr[0].name+(arr.length>1?' (+'+(arr.length-1)+' more)':''));res(arr);}
      else{rej(fsAbort());}
      });
      window.addEventListener('focus',function(){setTimeout(function(){if(!done&&(!inp.files||!inp.files.length)){done=1;cleanup();rej(fsAbort());}},700);},{once:true});
      (document.body||document.documentElement).appendChild(inp);
      ev('file picker requested by the page');
      inp.click();
      }catch(e){rej(e);}
      });
      }
      try{
      window.showOpenFilePicker=function(o){return fsPick(o,false);};
      if(!window.showSaveFilePicker){window.showSaveFilePicker=function(o){
      return Promise.resolve({kind:'file',name:(o&&o.suggestedName)||'download.bin',
      getFile:function(){return Promise.reject(fsAbort());},
      createWritable:function(){var parts=[];
      return Promise.resolve({
      write:function(d){parts.push(d);return Promise.resolve();},
      close:function(){
      try{
      var blob=parts.length===1&&(parts[0] instanceof Blob)?parts[0]:new Blob(parts);
      var url=URL.createObjectURL(blob);
      var a=document.createElement('a');a.href=url;a.download=(o&&o.suggestedName)||'download.bin';
      (document.body||document.documentElement).appendChild(a);a.click();
      ev('saved a file the page made: '+a.download);
      setTimeout(function(){try{URL.revokeObjectURL(url);a.remove();}catch(e){}},4000);
      }catch(e){}
      return Promise.resolve();
      },
      abort:function(){return Promise.resolve();}});
      },
      queryPermission:function(){return Promise.resolve('granted');},
      requestPermission:function(){return Promise.resolve('granted');}});
      };}
      }catch(e){}
      setInterval(function(){
      try{
      var alive=!!st.p&&(st.f||(now()-(st.u||0))>2000);
      if(st.p&&alive!==st.fa){st.fa=alive;ev(alive?'arrow-key fallback ON':'arrow-key fallback off');}
      if(!alive||editing()){releaseKeys();return;}
      var v=st.v||[],a=st.a||[],want={};
      var ax=+a[0]||0,ay=+a[1]||0;
      if(+v[12]>0.5||ay<-0.45)want[12]=1;
      if(+v[13]>0.5||ay>0.45)want[13]=1;
      if(+v[14]>0.5||ax<-0.45)want[14]=1;
      if(+v[15]>0.5||ax>0.45)want[15]=1;
      if(+v[0]>0.5)want[0]=1;
      if(+v[1]>0.5)want[1]=1;
      var k,ks=[];
      for(k in want){if(!held[k]){held[k]=1;send('keydown',k);}}
      for(k in held){if(!want[k]){ks.push(k);}}
      for(var i=0;i<ks.length;i++){delete held[ks[i]];send('keyup',ks[i]);}
      var ry=+a[3]||0;
      if(Math.abs(ry)>0.2){try{window.scrollBy(0,Math.round(ry*24));}catch(x){}}
      }catch(x){}
      },33);
      })();
      """;

   /**
    * The F9 panel: shows, right on the page you are looking at, whether the controller bridge is
    * installed, which pad the mod sees, which buttons are down, whether the game is actually
    * reading the pad, and what the shim has been doing. It also carries a file-picker test button
    * (proves the choose-file window works before a game even asks) and a switch that forces the
    * arrow-key fallback on for games that watch the pad API but never act on it.
    */
   static final String OVERLAY = """
      (function(){
      if(!window.__wcOverlay){
      var el=null,timer=null;
      var NAMES=['A','B','X','Y','LB','RB','LT','RT','Back','Start','L3','R3','Up','Dn','Lf','Rt','Guide'];
      function now(){return (window.performance&&performance.now)?performance.now():Date.now();}
      function stt(){return window.__wcPadState||null;}
      function cell(i,v){
      var on=(i===6||i===7)?v>0.05:v>0.5;
      return '<span style="display:inline-block;min-width:34px;margin:1px;padding:1px 3px;text-align:center;border:1px solid '+(on?'#7ee787':'#3d5566')+';border-radius:3px;background:'+(on?'#1f6f2f':'#141a20')+';color:'+(on?'#eaffea':'#9fb2c0')+'">'+NAMES[i]+'</span>';
      }
      function bar(v){
      var w=Math.round((v+1)/2*110);
      return '<span style="display:inline-block;width:120px;height:8px;background:#141a20;border:1px solid #3d5566;vertical-align:middle"><span style="display:inline-block;width:'+w+'px;height:8px;background:#4aa3ff"></span></span> <span style="color:#9fb2c0">'+Math.round(v*100)+'%</span>';
      }
      function draw(){
      if(!el)return;
      var s=stt(),p=!!(s&&s.p),v=(s&&s.v)||[],a=(s&&s.a)||[0,0,0,0];
      var shim=window.__wcPadShim===1;
      var age=p?Math.round(now()-((s&&s.u)||0)):0;
      var reads=p&&age<2000;
      var h='';
      h+='<div style="font-weight:bold;color:#fff">Controller check <span style="color:#7d919f">(F9 closes)</span></div>';
      h+='<div>bridge: <span style="color:'+(shim?'#7ee787':'#ff7b72')+'">'+(shim?'installed':'NOT INSTALLED')+'</span> &nbsp; pad: <span style="color:'+(p?'#7ee787':'#ff7b72')+'">'+((p&&s.n)?s.n:'none seen')+'</span></div>';
      h+='<div>game reads pad: <span style="color:'+(reads?'#7ee787':'#ffd866')+'">'+(p?(reads?'yes ('+age+' ms ago)':'no - arrow-key fallback ON'):'-')+'</span></div>';
      h+='<div style="margin:3px 0">';
      for(var i=0;i<17;i++){h+=cell(i,+v[i]||0);}
      h+='</div>';
      h+='<div>left stick &nbsp;x '+bar(+a[0]||0)+' y '+bar(+a[1]||0)+'</div>';
      h+='<div>right stick x '+bar(+a[2]||0)+' y '+bar(+a[3]||0)+'</div>';
      h+='<div style="margin-top:4px"><button data-act="pick" style="background:#141a20;color:#d7e2ea;border:1px solid #3d5566;border-radius:3px;padding:2px 6px;cursor:pointer;margin-right:6px">Test file picker</button>';
      h+='<button data-act="force" style="background:#141a20;color:#d7e2ea;border:1px solid #3d5566;border-radius:3px;padding:2px 6px;cursor:pointer">Key fallback: '+((s&&s.f)?'FORCED':'auto')+'</button></div>';
      h+='<div style="margin-top:4px;border-top:1px solid #3d5566;padding-top:3px">';
      var log=(s&&s.log)||[];
      if(!log.length){h+='<span style="color:#7d919f">no events yet</span>';}
      for(var j=0;j<log.length;j++){h+='<div>'+log[j]+'</div>';}
      h+='</div>';
      el.innerHTML=h;
      }
      function build(){
      el=document.createElement('div');
      el.id='wc-pad-overlay';
      el.style.cssText='position:fixed;left:6px;top:6px;z-index:2147483647;width:340px;max-height:80%;overflow:auto;background:rgba(8,10,14,.9);color:#d7e2ea;font:11px/1.5 Consolas,monospace;padding:8px 10px;border:1px solid #3d5566;border-radius:6px;';
      el.addEventListener('click',function(e){
      e.stopPropagation();
      var t=e.target||{},act=t.getAttribute?t.getAttribute('data-act'):null;
      if(!act)return;
      var s=stt();
      if(act==='force'){
      if(s){s.f=s.f?0:1;try{s.log.unshift('key fallback '+(s.f?'FORCED ON':'back to auto'));}catch(x){}}
      draw();
      }
      if(act==='pick'){
      try{
      var inp=document.createElement('input');
      inp.type='file';
      inp.accept='.iso,.bin,.img,.chd,.cso,.cue,.zip,.7z,.rar,.gz';
      inp.style.cssText='position:fixed;left:-9999px;top:0;width:1px;height:1px;opacity:0;';
      inp.addEventListener('change',function(){
      var n=inp.files?inp.files.length:0;
      if(s){try{s.log.unshift('picker test: got '+n+' file(s)');}catch(x){}}
      setTimeout(function(){try{inp.remove();}catch(x){}},500);
      draw();
      });
      (document.body||document.documentElement).appendChild(inp);
      inp.click();
      if(s){try{s.log.unshift('picker test: asked for a file');}catch(x){}}
      }catch(x){}
      draw();
      }
      });
      (document.body||document.documentElement).appendChild(el);
      }
      window.__wcOverlay=function(on){
      window.__wcOverlayOn=on?1:0;
      if(window.top!==window)return;
      if(on){
      if(!el)build();
      el.style.display='block';
      if(!timer)timer=setInterval(draw,100);
      draw();
      }else{
      if(timer){clearInterval(timer);timer=null;}
      if(el)el.style.display='none';
      }
      };
      }
      window.__wcOverlay(window.__wcOverlayOn||0);
      })();
      """;
}
