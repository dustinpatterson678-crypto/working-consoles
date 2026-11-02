package com.dustin.stream.diag;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.dimaskama.mcef.api.MCEFBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read-only diagnostics ("[WC3]" capability probe on every page/frame, plus the older "[WC2]" checks): while a TV browser is on a browser-game page, logs (to the game log, as "[WC2] ...") whether the
 * page is cross-origin isolated and why not. Only reads page state; changes nothing on the page.
 */
public final class CoiDiag {
   private CoiDiag() {
   }

   private static final Logger LOG = LoggerFactory.getLogger("working-consoles-stream");
   private static final class Entry {
      final WeakReference<MCEFBrowser> ref;
      final long created = System.currentTimeMillis();
      int step;
      Entry(MCEFBrowser b) {
         this.ref = new WeakReference<>(b);
      }
   }

   private static final long[] FPS_AT_MS = {1000, 3000, 6000, 12000, 24000};
   private static final List<Entry> BROWSERS = new CopyOnWriteArrayList<>();
   private static boolean started;

   private static final String JS = "(function(){try{"
      + "var n=(performance.getEntriesByType('navigation')||[])[0]||{};"
      + "var sw=navigator.serviceWorker;"
      + "var o={coi:self.crossOriginIsolated,sab:typeof SharedArrayBuffer,secure:self.isSecureContext,swApi:!!sw,controlled:!!(sw&&sw.controller),"
      + "navType:n.type,workerStart:n.workerStart,oac:self.originAgentClustered,"
      + "policy:(document.featurePolicy&&document.featurePolicy.allowsFeature?document.featurePolicy.allowsFeature('cross-origin-isolated'):'n/a'),"
      + "reloadedBySelf:sessionStorage.getItem('coiReloadedBySelf'),coepFailed:sessionStorage.getItem('coiCoepHasFailed')};"
      + "var s=JSON.stringify(o);if(window.__wc2s!==s){window.__wc2s=s;console.log('[WC2] '+s);}"
      + "if(sw&&sw.getRegistrations&&!window.__wc2rd){window.__wc2rd=1;sw.getRegistrations().then(function(rs){"
      + "console.log('[WC2] serviceWorker registrations='+rs.length+' '+rs.map(function(r){return (r.active?r.active.scriptURL:'inactive')+' scope='+r.scope;}).join(' | '));"
      + "}).catch(function(e){console.log('[WC2] getRegistrations failed '+e);});}"
      + "if(!window.__wc2t){window.__wc2t=1;try{var w=new Worker(URL.createObjectURL(new Blob(['onmessage=function(e){postMessage(e.data.byteLength)}'],{type:'text/javascript'})));"
      + "var sb=new SharedArrayBuffer(16);w.onmessage=function(e){console.log('[WC2] SharedArrayBuffer to worker: OK '+e.data);w.terminate();};"
      + "w.onerror=function(e){console.log('[WC2] SharedArrayBuffer to worker: worker error '+e.message);};w.postMessage(sb);}"
      + "catch(e){console.log('[WC2] SharedArrayBuffer to worker: FAILED '+e);}}"
      + "}catch(e){console.log('[WC2] diag error '+e);}})();";


   /** Read-only browser capability probe, run once per page/frame; logs "[WC3] ..." lines. Changes nothing on the page. */
   private static final String PROBE = "(function(){try{if(window.__wc3)return;window.__wc3=1;"
      + "var P=function(t){console.log('[WC3] '+t);};var o={};"
      + "function t(k,f){try{o[k]=f();}catch(e){o[k]='ERR '+e;}}"
      + "t('frame',function(){return (window.top===window?'top':'iframe')+' '+location.origin;});"
      + "t('ua',function(){return navigator.userAgent;});t('vendor',function(){return navigator.vendor;});"
      + "t('platform',function(){return navigator.platform;});t('lang',function(){return navigator.language;});"
      + "t('cookieEnabled',function(){return navigator.cookieEnabled;});t('webdriver',function(){return navigator.webdriver;});"
      + "t('brands',function(){var d=navigator.userAgentData;return d?JSON.stringify(d.brands):'none';});"
      + "t('coi',function(){return self.crossOriginIsolated;});t('sab',function(){return typeof SharedArrayBuffer;});"
      + "t('secure',function(){return self.isSecureContext;});t('chromeObj',function(){return typeof window.chrome;});"
      + "t('visible',function(){return document.visibilityState+' focus='+document.hasFocus();});"
      + "t('hw',function(){return navigator.hardwareConcurrency+' cores, mem='+navigator.deviceMemory;});"
      + "t('screen',function(){return screen.width+'x'+screen.height+' dpr='+devicePixelRatio+' inner='+innerWidth+'x'+innerHeight;});"
      + "t('tz',function(){return Intl.DateTimeFormat().resolvedOptions().timeZone+' offset='+new Date().getTimezoneOffset();});"
      + "P('identity '+JSON.stringify(o));o={};"
      + "t('localStorage',function(){localStorage.setItem('__wc3','1');var r=localStorage.getItem('__wc3')==='1';localStorage.removeItem('__wc3');return r;});"
      + "t('sessionStorage',function(){sessionStorage.setItem('__wc3','1');var r=sessionStorage.getItem('__wc3')==='1';sessionStorage.removeItem('__wc3');return r;});"
      + "t('cookie',function(){var iframe=window.top!==window;document.cookie='__wc3=1; path=/'+(iframe?'; SameSite=None; Secure':'; SameSite=Lax');"
      + "var r=document.cookie.indexOf('__wc3=1')>=0;document.cookie='__wc3=; path=/; max-age=0'+(iframe?'; SameSite=None; Secure':'');return r+(iframe?' (cross-site iframe cookie)':'');});"
      + "t('canvas2d',function(){return !!document.createElement('canvas').getContext('2d');});"
      + "t('webgl',function(){var c=document.createElement('canvas').getContext('webgl');if(!c)return false;var x=c.getExtension('WEBGL_debug_renderer_info');return x?c.getParameter(x.UNMASKED_RENDERER_WEBGL):'yes';});"
      + "t('webgl2',function(){return !!document.createElement('canvas').getContext('webgl2');});"
      + "t('webgpu',function(){return !!navigator.gpu;});t('offscreenCanvas',function(){return typeof OffscreenCanvas;});"
      + "t('audioCtx',function(){var A=window.AudioContext||window.webkitAudioContext;if(!A)return false;var a=new A();var s=a.state;a.close();return s;});"
      + "t('webrtc',function(){if(!window.RTCPeerConnection)return false;var p=new RTCPeerConnection();p.close();return true;});"
      + "t('serviceWorker',function(){return !!navigator.serviceWorker;});t('wasm',function(){return typeof WebAssembly;});"
      + "t('cryptoSubtle',function(){return !!(window.crypto&&crypto.subtle);});t('worker',function(){return typeof Worker;});"
      + "t('mediaDevices',function(){return !!navigator.mediaDevices;});t('permissions',function(){return !!navigator.permissions;});"
      + "t('plugins',function(){return navigator.plugins.length;});t('widevine',function(){return typeof navigator.requestMediaKeySystemAccess;});"
      + "P('capabilities '+JSON.stringify(o));"
      + "try{var r=indexedDB.open('__wc3');r.onsuccess=function(){P('indexedDB true');try{r.result.close();indexedDB.deleteDatabase('__wc3');}catch(e){}};"
      + "r.onerror=function(){P('indexedDB FAILED '+r.error);};}catch(e){P('indexedDB FAILED '+e);}"
      + "var n=0;function E(k,m){if(n++<15)P('event '+k+' '+m);}"
      + "addEventListener('error',function(e){E('error',(e.message||'')+' @'+(e.filename||(e.target&&(e.target.src||e.target.href))||'')+':'+(e.lineno||''));},true);"
      + "addEventListener('securitypolicyviolation',function(e){E('csp',e.violatedDirective+' blocked='+e.blockedURI);});"
      + "addEventListener('unhandledrejection',function(e){E('rejection',String(e.reason));});"
      + "}catch(e){console.log('[WC3] probe error '+e);}})();";

   public static synchronized void track(MCEFBrowser browser) {
      if (browser == null) return;
      BROWSERS.add(new Entry(browser));
      if (started) return;
      started = true;
      Thread t = new Thread(CoiDiag::loop, "wc-coi-diag");
      t.setDaemon(true);
      t.start();
   }


   private static void probeFrames(CefBrowser cb) {
      try {
         CefFrame main = cb.getMainFrame();
         if (main != null && main.getURL() != null && main.getURL().startsWith("http")) {
            main.executeJavaScript(PROBE, main.getURL(), 0);
         }
         java.util.Vector<String> ids = cb.getFrameIdentifiers();
         if (ids == null) return;
         for (String id : ids) {
            CefFrame f = cb.getFrameByIdentifier(id);
            if (f != null && !f.isMain() && f.getURL() != null && f.getURL().startsWith("http")) {
               f.executeJavaScript(PROBE, f.getURL(), 0);
            }
         }
      } catch (Throwable ignored) {
      }
   }

   private static void loop() {
      long tick = 0;
      while (true) {
         try {
            Thread.sleep(1000);
            tick++;
            for (Entry e : BROWSERS) {
               MCEFBrowser b = e.ref.get();
               if (b == null) {
                  BROWSERS.remove(e);
                  continue;
               }
               CefBrowser cb = b.getCefBrowser();
               if (cb == null) continue;
               long age = System.currentTimeMillis() - e.created;
               // The embedded browser draws at 30 frames per second by default; ask for 60 (repeated a few times: it only
               // takes effect once the browser has finished starting up).
               if (e.step < FPS_AT_MS.length && age >= FPS_AT_MS[e.step]) {
                  e.step++;
                  cb.setWindowlessFrameRate(60);
                  if (e.step == FPS_AT_MS.length) {
                     cb.getWindowlessFrameRate().thenAccept(r -> LOG.info("Browser frame rate limit: {} fps", r));
                  }
               }
               if (tick % 8 == 0) {
                  String url = cb.getURL();
                  if (url != null && url.contains("mitchellhynes.com") && cb.getMainFrame() != null) {
                     cb.getMainFrame().executeJavaScript(JS, url, 0);
                  }
               }
               if (tick % 3 == 0 && age > 2000) {
                  probeFrames(cb);
               }
            }
         } catch (InterruptedException ex) {
            return;
         } catch (Throwable ignored) {
         }
      }
   }
}
