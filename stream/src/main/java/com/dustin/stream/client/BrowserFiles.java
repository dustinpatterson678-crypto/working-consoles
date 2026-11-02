package com.dustin.stream.client;

import java.awt.Frame;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.WeakHashMap;
import net.dimaskama.mcef.api.MCEFBrowser;
import net.minecraft.core.BlockPos;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefFileDialogCallback;
import org.cef.handler.CefDialogHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When a page asks you for a file - Halo Online asking for its ISO, a ROM for a web emulator, a
 * save file - Chromium hands the request to whatever CEF dialog handler is registered. In an
 * off-screen browser there is no built-in dialog, so without this nothing happens at all. This
 * opens a real "choose a file" window on the player's computer and hands the picked paths back.
 */
final class BrowserFiles implements CefDialogHandler {
   private static final Logger LOG = LoggerFactory.getLogger("working-consoles-stream");

   /** Files browser games tend to want: disc images, ROMs, archives. */
   private static final String[] GAME_FILES = {
      "iso", "bin", "cue", "img", "chd", "cso", "gcm", "nrg", "mdf", "ccd", "dvd",
      "zip", "7z", "rar", "tar", "gz",
      "gba", "gbc", "gb", "nds", "3ds", "cia", "nsp", "xci", "wbfs", "wad", "z64", "n64", "v64", "nes", "sfc", "smc", "gen", "md", "32x", "smd", "pce", "a26"
   };

   private static final Set<CefClient> READY = Collections.newSetFromMap(new WeakHashMap<>());
   private static File lastDir;

   /**
    * Each browser rides on its own CefClient, and the handle can still be filling in right when the
    * screen opens, so this runs both at open and on every tick until it sticks. Attaching twice is
    * harmless (CEF keeps, and answers, only the first handler).
    */
   static void ensure(MCEFBrowser browser) {
      if (browser == null) {
         return;
      }
      try {
         CefBrowser cb = browser.getCefBrowser();
         if (cb == null) {
            return;
         }
         CefClient client = cb.getClient();
         if (client == null) {
            return;
         }
         if (READY.contains(client)) {
            return;
         }
         client.addDialogHandler(new BrowserFiles());
         READY.add(client);
         LOG.info("File picker attached to the browser; in-page file requests will open a choose-file window.");
         tell(cb, "File picker ready: click a \"choose file\" button on the page.");
      } catch (Throwable t) {
         LOG.warn("Could not add the file picker to the browser", t);
      }
   }

   @Override
   public boolean onFileDialog(CefBrowser browser, FileDialogMode mode, String title, String defaultFilePath,
         Vector<String> acceptFilters, Vector<String> acceptExtensions, Vector<String> acceptDescriptions,
         CefFileDialogCallback callback) {
      LOG.info("Browser asked for a file: mode={} title={} default={} filters={} extensions={}", mode, title, defaultFilePath, acceptFilters, acceptExtensions);
      tell(browser, "Website picker path: mode=" + mode.name());
      java.awt.EventQueue.invokeLater(() -> pick(mode, title, defaultFilePath, acceptFilters, acceptExtensions, callback, browser));
      return true;
   }

   /** F8 on the controls screen: forces a file picker open for testing. */
   static void forceTestPicker(CefBrowser cb) {
      if (cb == null) return;
      tell(cb, "F8 picker path");
      CefFileDialogCallback callback = new CefFileDialogCallback() {
         @Override public void Continue(Vector<String> paths) {}
         @Override public void Cancel() {}
      };
      java.awt.EventQueue.invokeLater(() -> pick(FileDialogMode.FILE_DIALOG_OPEN, "Test file picker (F8)", null, null, null, callback, cb));
   }

   private static void pick(FileDialogMode mode, String title, String start, Vector<String> filters,
         Vector<String> extensions, CefFileDialogCallback callback, CefBrowser browser) {
      List<String> chosen = new ArrayList<>();
      BlockPos was = InputFx.controlling;
      try {
         tell(browser, "Opening native file picker...");
         // Write PowerShell script to a temp file to avoid escaping issues
         java.io.File scriptFile = new java.io.File(System.getProperty("java.io.tmpdir"), "wc_picker.ps1");
         java.io.FileWriter fw = new java.io.FileWriter(scriptFile);
         fw.write("Add-Type -AssemblyName System.Windows.Forms\n");
         fw.write("$d = New-Object System.Windows.Forms.OpenFileDialog\n");
         fw.write("$d.Title = 'Pick a file for the page'\n");
         fw.write("$d.Filter = 'All files (*.*)|*.*|ISO files (*.iso)|*.iso|BIN files (*.bin)|*.bin'\n");
         fw.write("$d.Multiselect = $false\n");
         fw.write("if ($d.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { Write-Output $d.FileName } else { Write-Output 'CANCELLED' }\n");
         fw.close();
         ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-STA", "-File", scriptFile.getAbsolutePath());
         pb.redirectErrorStream(false);
         Process p = pb.start();
         java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
         String line;
         while ((line = reader.readLine()) != null) {
            if (!line.trim().isEmpty() && !line.startsWith("CANCELLED")) {
               chosen.add(line.trim());
               tell(browser, "Read: " + line.trim());
            }
         }
         p.waitFor();
         tell(browser, "Picker returned " + chosen.size() + " file(s)");
      } catch (Throwable t) {
         LOG.warn("The file picker failed", t);
         StringWriter sw = new StringWriter();
         t.printStackTrace(new PrintWriter(sw));
         tell(browser, "File picker error: " + t.getClass().getSimpleName() + " — " + sw.toString().split("\\n")[0]);
      }
      try {
         if (chosen.isEmpty()) {
            callback.Cancel();
            tell(browser, "File picker cancelled.");
         } else {
            LOG.info("Handing {} file(s) to the page: {}", chosen.size(), chosen);
            tell(browser, "Picked: " + chosen.get(0) + (chosen.size() > 1 ? " (+" + (chosen.size() - 1) + " more)" : ""));
            callback.Continue(new Vector<>(chosen));
            // Register the picked file with the local file server so the page can fetch
            // real bytes (CEF OSR mode does not always hand usable File objects to the page).
            String fileUrl = BrowserFileServer.register(chosen.get(0));
            tell(browser, "File URL: " + fileUrl);
            String fileName = new java.io.File(chosen.get(0)).getName();
            long fileSize = new java.io.File(chosen.get(0)).length();
            // Give CEF a moment, then replace the input files with fetch-backed File-like
            // objects and dispatch a change event so the page (and our FS-Access polyfill) sees them.
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            try {
               String js = "window.__wcMakeFile=function(url,name,size){" +
                  "var o={name:name,size:size,type:'application/octet-stream',lastModified:Date.now(),_url:url};" +
                  "o.arrayBuffer=function(){return fetch(url).then(function(r){return r.arrayBuffer();});};" +
                  "o.text=function(){return fetch(url).then(function(r){return r.text();});};" +
                  "o.stream=function(){return new ReadableStream({start:function(ctrl){fetch(url).then(function(r){var reader=r.body.getReader();function pump(){reader.read().then(function(res){if(res.done){ctrl.close();return;}ctrl.enqueue(res.value);pump();});}}pump();}).catch(function(e){ctrl.error(e);});}});};" +
                  "o.slice=function(s,e,ct){" +
                     "s=s||0; if(e===undefined||e===null) e=size;" +
                     "var u=url+'&r='+(s)+'-'+(e-1);" +
                     "var sl={size:Math.max(0,e-s),type:ct||'application/octet-stream',_url:u,_s:s,_e:e};" +
                     "sl.arrayBuffer=function(){return fetch(u,{headers:{'Range':'bytes='+s+'-'+(e-1)}}).then(function(r){return r.arrayBuffer();});};" +
                     "sl.text=function(){return fetch(u,{headers:{'Range':'bytes='+s+'-'+(e-1)}}).then(function(r){return r.text();});};" +
                     "sl.stream=function(){return new ReadableStream({start:function(ctrl){fetch(u,{headers:{'Range':'bytes='+s+'-'+(e-1)}}).then(function(r){var reader=r.body.getReader();function pump(){reader.read().then(function(res){if(res.done){ctrl.close();return;}ctrl.enqueue(res.value);pump();});}}pump();}).catch(function(e){ctrl.error(e);});}});};" +
                     "sl.slice=function(s2,e2,ct2){return o.slice(s+(s2||0), (s2? s+s2 : s)+((e2!==undefined&&e2!==null)?e2:size)-(s2||0), ct2||ct||'application/octet-stream');};" +
                     "return sl;" +
                  "};" +
                  "return o;" +
                  "};" +
                  "var inputs=document.querySelectorAll('input[type=file]');" +
                  "for(var i=0;i<inputs.length;i++){" +
                  "  try{" +
                  "    console.log('[WC] Input '+i+' files before replace:', inputs[i].files?inputs[i].files.length:0, inputs[i].files&&inputs[i].files[0]?inputs[i].files[0].name+' size='+inputs[i].files[0].size:'none');" +
                  "    var f=window.__wcMakeFile('PLACEHOLDER_URL','PLACEHOLDER_NAME',PLACEHOLDER_SIZE);" +
                  "    Object.defineProperty(inputs[i],'files',{value:[f],writable:true,configurable:true});" +
                  "    console.log('[WC] Set fetch-backed file on input:', f.name, f.size);" +
                  "    console.log('[WC] Testing fetch to localhost...');" +
                  "    var xhr=new XMLHttpRequest();" +
                  "    xhr.open('GET','PLACEHOLDER_URL',true);" +
                  "    xhr.responseType='arraybuffer';" +
                  "    xhr.onload=function(){console.log('[WC] XHR OK:', xhr.status, xhr.response.byteLength, 'bytes');};" +
                  "    xhr.onerror=function(){console.error('[WC] XHR FAILED');};" +
                  "    xhr.send();" +
                  "    var e=new Event('change',{bubbles:true});" +
                  "    inputs[i].dispatchEvent(e);" +
                  "  }catch(x){console.error('[WC] file set error:',x);}" +
                  "}" +
                  "window.__wcPickedFile={url:'PLACEHOLDER_URL',name:'PLACEHOLDER_NAME',size:PLACEHOLDER_SIZE};" +
                  "if(window.__wcOnFilePicked)window.__wcOnFilePicked(window.__wcPickedFile);";
               js = js.replace("PLACEHOLDER_URL", fileUrl);
               js = js.replace("PLACEHOLDER_NAME", fileName.replace("'", "\\\'"));
               js = js.replace("PLACEHOLDER_SIZE", "" + fileSize);
               if (browser.hasDocument()) {
                  browser.executeJavaScript(js, "wc-file", 0);
               } else {
                  LOG.warn("Picked file cannot be handed to the page yet: browser document not ready");
               }
            } catch (Throwable t) {
               LOG.warn("Failed to hand the picked file to the page via script", t);
            }
         }
      } catch (Throwable t) {
         LOG.warn("Could not hand the picked file to the page", t);
         tell(browser, "Failed to send file to page: " + t.getClass().getSimpleName());
      }
      if (was != null) {
         Browsers.wantReopen(was);
      }
   }

   private static void tell(CefBrowser cb, String msg) {
      try {
         net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
         if (mc.player != null) {
            mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Browser] " + msg).withStyle(net.minecraft.ChatFormatting.AQUA));
         }
      } catch (Throwable ignored) {}
   }

   /** Pull the plain extensions out of the filters a page sent ("*.iso", ".iso", "iso"). */
   private static String[] clean(Vector<String> filters, Vector<String> extensions) {
      List<String> out = new ArrayList<>();
      collect(filters, out);
      collect(extensions, out);
      return out.toArray(new String[0]);
   }

   private static void collect(Vector<String> source, List<String> out) {
      if (source == null) {
         return;
      }
      for (String raw : source) {
         if (raw == null) {
            continue;
         }
         for (String part : raw.split("[\\s,;]+")) {
            String ext = part.trim().toLowerCase(java.util.Locale.ROOT);
            if (ext.startsWith("*")) {
               ext = ext.substring(1);
            }
            while (ext.startsWith(".")) {
               ext = ext.substring(1);
            }
            if (!ext.isEmpty() && ext.matches("[a-z0-9]{1,8}") && !out.contains(ext)) {
               out.add(ext);
            }
         }
      }
   }

   private static String joinExt(String[] exts) {
      StringBuilder sb = new StringBuilder();
      for (String e : exts) {
         if (sb.length() > 0) {
            sb.append(", ");
         }
         sb.append('.').append(e);
      }
      return sb.toString();
   }

   private static File startDir(String start) {
      try {
         if (start != null && !start.isBlank()) {
            File f = new File(start);
            File dir = f.isDirectory() ? f : f.getParentFile();
            if (dir != null && dir.isDirectory()) {
               return dir;
            }
         }
      } catch (Throwable ignored) {
      }
      return lastDir != null && lastDir.isDirectory() ? lastDir : null;
   }
}
