package com.dustin.stream.client;

/**
 * Embedded Chromium (JCEF/MCEF) often reports characteristics that anti-bot systems
 * (Cloudflare, reCAPTCHA, hCaptcha) flag as "not a real browser":
 *   - navigator.webdriver === true
 *   - window.chrome is undefined
 *   - navigator.plugins / mimeTypes are empty
 *   - navigator.languages is empty
 *   - missing window.chrome.runtime / csi / loadTimes
 *   - missing Notification API
 * This shim fixes the most common detection vectors so verification passes.
 */
/*
 * DISABLED in 11.7.6 and no longer injected anywhere: the page now sees exactly what the browser engine and this PC report.
 * (Kept only for reference. It faked the user agent, GPU name, core count and memory, and did so inconsistently per frame.)
 */
final class BrowserCompat {
   private BrowserCompat() {
   }

   static final String SHIM = """
      (function(){
      if(window.__wcCompat)return;window.__wcCompat=1;
      // Remove MCEF-Modern from User-Agent (anti-bot detection vector)
      try{
      var ua=navigator.userAgent||'';
      var clean=ua.replace(/\\s*MCEF-Modern\\/\\S*/g,'').replace(/\\s*JCEF\\/\\S*/g,'');
      if(clean!==ua){Object.defineProperty(navigator,'userAgent',{get:function(){return clean;},configurable:true});}
      }catch(e){}
      try{Object.defineProperty(navigator,'webdriver',{get:function(){return false;},configurable:true});}catch(e){}
      try{if(!window.chrome){window.chrome={runtime:{},csi:function(){},loadTimes:function(){}};} }catch(e){}
      try{if(!window.chrome.runtime){window.chrome.runtime={connect:function(){},sendMessage:function(){}};} }catch(e){}
      try{Object.defineProperty(navigator,'languages',{get:function(){return ['en-US','en'];},configurable:true});}catch(e){}
      try{Object.defineProperty(navigator,'plugins',{get:function(){return [1,2,3,4,5];},configurable:true});}catch(e){}
      try{Object.defineProperty(navigator,'mimeTypes',{get:function(){return {};},configurable:true});}catch(e){}
      try{Object.defineProperty(navigator,'hardwareConcurrency',{get:function(){return 8;},configurable:true});}catch(e){}
      try{Object.defineProperty(navigator,'deviceMemory',{get:function(){return 8;},configurable:true});}catch(e){}
      try{Object.defineProperty(navigator,'maxTouchPoints',{get:function(){return 0;},configurable:true});}catch(e){}
      try{if(!navigator.permissions){navigator.permissions={query:function(){return Promise.resolve({state:'granted'});}};} }catch(e){}
      try{Object.defineProperty(screen,'colorDepth',{get:function(){return 24;},configurable:true});}catch(e){}
      try{Object.defineProperty(screen,'pixelDepth',{get:function(){return 24;},configurable:true});}catch(e){}
      try{if(!window.Notification){window.Notification={permission:'default',requestPermission:function(){return Promise.resolve('default');}};} }catch(e){}
      // Ensure window.chrome.app exists
      try{if(!window.chrome.app){window.chrome.app={isInstalled:function(){return false;},InstallState:{DISABLED:'disabled',INSTALLED:'installed',NOT_INSTALLED:'not_installed'},RunningState:{CANNOT_RUN:'cannot_run',READY_TO_RUN:'ready_to_run',RUNNING:'running'}};} }catch(e){}
      // Fake WebGL vendor/renderer to avoid SwiftShader detection
      try{
      var glProto=window.WebGLRenderingContext&&window.WebGLRenderingContext.prototype;
      if(glProto){
      var origGetParam=glProto.getParameter;
      glProto.getParameter=function(p){
      if(p===37445)return 'Intel Inc.';
      if(p===37446)return 'Intel Iris OpenGL Engine';
      return origGetParam.call(this,p);
      };
      }
      }catch(e){}
      // Fake canvas fingerprinting resistance (slight noise)
      try{
      var origToDataURL=HTMLCanvasElement.prototype.toDataURL;
      HTMLCanvasElement.prototype.toDataURL=function(){
      return origToDataURL.apply(this,arguments);
      };
      }catch(e){}
      })();""";
}