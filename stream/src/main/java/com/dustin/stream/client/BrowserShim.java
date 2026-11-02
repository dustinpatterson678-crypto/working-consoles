package com.dustin.stream.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;

/**
 * mcef-modern's JCEF off-screen path only ever delivers key presses as a "raw key down": Chromium
 * hears the keydown but never runs the default action that follows it, so Backspace, Delete, caret
 * movement and Tab focus do nothing in the page (DimasKama/mcef-modern#4, still open upstream).
 * The page does receive the keydown, so we slip in a small listener that performs those edits
 * itself. Every navigation wipes the page's scripts, so it is re-sent after each load, for every
 * frame (games often run in an iframe). The controller shim rides along at the same cadence.
 */
final class BrowserShim {
   private static final long EVERY_MS = 15000L;

   private BrowserShim() {
   }

   static void tick(Browsers.Session s) {
      try {
         CefBrowser cb = s.browser.getCefBrowser();
         if (!cb.hasDocument()) {
            return;
         }
         if (cb.isLoading()) {
            s.shimPending = true;
            return;
         }
         long now = System.currentTimeMillis();
         if (!s.shimPending && now - s.shimAt < EVERY_MS) {
            return;
         }
         s.shimPending = false;
         s.shimAt = now;
         List<CefFrame> frames = frames(cb);
         s.frames = frames;
         for (CefFrame frame : frames) {
            try {
               frame.executeJavaScript(SCRIPT, "wc-shim", 0);
               frame.executeJavaScript(BrowserGamepad.SHIM, "wc-pad", 0);
               frame.executeJavaScript(MEDIA, "wc-media", 0);
               frame.executeJavaScript("if(!window.__wcCoi){window.__wcCoi=1;console.log('[WC] COI=' + window.crossOriginIsolated + ' SAB=' + (typeof SharedArrayBuffer));}", "wc-coi", 0);
            } catch (Throwable ignored) {
            }
         }
      } catch (Throwable ignored) {
      }
   }

   private static List<CefFrame> frames(CefBrowser cb) {
      List<CefFrame> out = new ArrayList<>();
      try {
         CefFrame main = cb.getMainFrame();
         if (main != null) {
            out.add(main);
         }
      } catch (Throwable ignored) {
      }
      try {
         Vector<String> ids = cb.getFrameIdentifiers();
         if (ids != null) {
            for (String id : ids) {
               try {
                  CefFrame f = cb.getFrameByIdentifier(id);
                  if (f != null && !f.isMain()) {
                     out.add(f);
                  }
               } catch (Throwable ignored) {
               }
            }
         }
      } catch (Throwable ignored) {
      }
      return out;
   }

   /** The editing shim itself. One listener per document; guarded against double installs. */
   private static final String SCRIPT = """
      (function(){
      if(window.__wcShim)return;window.__wcShim=1;
      function isEditable(t){
      if(!t)return false;
      if(t.isContentEditable)return true;
      var n=t.tagName;
      if(n==='TEXTAREA')return true;
      if(n!=='INPUT')return false;
      var ty=String(t.type||'text').toLowerCase();
      return ['hidden','button','submit','reset','checkbox','radio','file','range','color','image'].indexOf(ty)<0;
      }
      function snap(t){
      try{
      if(t.isContentEditable){var s=window.getSelection();return s&&s.anchorNode?String(s.anchorOffset):'';}
      return String(t.value)+':'+t.selectionStart+':'+t.selectionEnd;
      }catch(e){return '';}
      }
      function editInput(t,k,shift){
      try{
      var s=t.selectionStart,n=t.selectionEnd,v=String(t.value);
      if(s===null||s===undefined)return false;
      if(k==='Backspace'){
      if(shift)return false;
      if(s!==n)t.setRangeText('',s,n,'end');else if(s>0)t.setRangeText('',s-1,s,'end');
      return true;
      }
      if(k==='Delete'){
      if(shift)return false;
      if(s!==n)t.setRangeText('',s,n,'end');else if(n<v.length)t.setRangeText('',n,n+1,'end');
      return true;
      }
      if(k==='ArrowLeft'||k==='ArrowRight'){
      var fwd=(k==='ArrowRight'),a,b;
      if(s===n){a=fwd?Math.min(v.length,s+1):Math.max(0,s-1);b=a;}
      else if(shift){a=s;b=n;if(fwd){if(b<v.length)b=b+1;else a=Math.min(v.length,a+1);}else{if(a>0)a=a-1;else b=Math.max(0,b-1);}}
      else{a=fwd?n:s;b=a;}
      t.setSelectionRange(a,b);
      return true;
      }
      if(k==='Home'){
      if(shift&&n>0)t.setSelectionRange(0,n);else t.setSelectionRange(0,0);
      return true;
      }
      if(k==='End'){
      if(shift&&s<v.length)t.setSelectionRange(s,v.length);else t.setSelectionRange(v.length,v.length);
      return true;
      }
      if(k==='Enter'){
      if(t.tagName==='TEXTAREA'){t.setRangeText('\\n',s,n,'end');return true;}
      if(t.tagName==='INPUT'&&t.form){
      try{if(t.form.requestSubmit)t.form.requestSubmit();else t.form.submit();return true;}catch(e){return false;}
      }
      return false;
      }
      }catch(e){}
      return false;
      }
      function editRich(k){
      try{
      if(k==='Backspace')return document.execCommand('delete');
      if(k==='Delete')return document.execCommand('forwardDelete');
      if(k==='Enter')return document.execCommand('insertLineBreak')||document.execCommand('insertText',false,'\\n');
      var sel=window.getSelection();
      if(!sel||!sel.modify)return false;
      if(k==='ArrowLeft'){sel.modify('move','backward','character');return true;}
      if(k==='ArrowRight'){sel.modify('move','forward','character');return true;}
      if(k==='Home'){sel.modify('move','backward','lineboundary');return true;}
      if(k==='End'){sel.modify('move','forward','lineboundary');return true;}
      }catch(e){}
      return false;
      }
      var FOCUSABLE='a[href],area[href],button,input,select,textarea,summary,iframe,[tabindex],[contenteditable="true"],audio[controls],video[controls]';
      function stepFocus(back){
      var all=document.querySelectorAll(FOCUSABLE),els=[];
      for(var i=0;i<all.length;i++){
      var e=all[i];
      if(e.disabled)continue;
      var ti=e.getAttribute('tabindex');
      if(ti!==null&&parseInt(ti,10)<0)continue;
      try{var r=e.getBoundingClientRect();if(r.width<2&&r.height<2)continue;var st=window.getComputedStyle(e);if(st.display==='none'||st.visibility==='hidden')continue;}catch(x){continue;}
      els.push(e);
      }
      if(!els.length)return;
      var cur=document.activeElement,idx=-1;
      for(var j=0;j<els.length;j++){if(els[j]===cur||els[j].contains(cur)){idx=j;break;}}
      var next=idx<0?(back?els.length-1:0):(idx+(back?-1:1)+els.length)%els.length;
      try{els[next].focus();}catch(x){}
      }
      document.addEventListener('keydown',function(e){
      if(e.defaultPrevented||e.isComposing)return;
      if(e.ctrlKey||e.altKey||e.metaKey)return;
      var k=e.key,t=e.target;
      if(!t)return;
      if(k==='Tab'){e.preventDefault();stepFocus(e.shiftKey);return;}
      if(k!=='Backspace'&&k!=='Delete'&&k!=='ArrowLeft'&&k!=='ArrowRight'&&k!=='Home'&&k!=='End'&&k!=='Enter')return;
      if(!isEditable(t))return;
      var before=snap(t);
      setTimeout(function(){
      if(snap(t)!==before)return;
      if(t.isContentEditable)editRich(k);else editInput(t,k,e.shiftKey);
      },0);
      },false);
      })();
      """;

   /**
    * Read-only media diagnostics, once per document. Logs what this embedded Chromium can play
    * (summary, top page only) and what each video/audio element reports when it loads, stalls or
    * fails. Media error codes: 1 aborted, 2 network, 3 decode, 4 source or codec not supported.
    */
   private static final String MEDIA = """
      (function(){
      if(window.__wcMedia)return;window.__wcMedia=1;
      function can(t){try{return document.createElement('video').canPlayType(t)||'no';}catch(e){return 'err';}}
      function mse(t){try{return (window.MediaSource&&MediaSource.isTypeSupported(t))?'yes':'no';}catch(e){return 'err';}}
      if(window.top===window){try{console.log('[WC] media: h264 '+can('video/mp4; codecs="avc1.42E01E, mp4a.40.2"')+' | webm vp9 '+can('video/webm; codecs="vp9, opus"')+' | mse h264 '+mse('video/mp4; codecs="avc1.42E01E, mp4a.40.2"')+' | '+location.hostname);}catch(e){}}
      var n=0;
      function say(e){try{var v=e.target;if(!v||(v.tagName!=='VIDEO'&&v.tagName!=='AUDIO'))return;if(n++>=6)return;
      console.log('[WC] '+v.tagName.toLowerCase()+' '+e.type+': ready='+v.readyState+' net='+v.networkState+' error='+(v.error?v.error.code:'none')+' src='+String(v.currentSrc||'').slice(0,90));}catch(x){}}
      ['error','loadedmetadata','playing','stalled'].forEach(function(t){document.addEventListener(t,say,true);});
      })();
      """;
}
