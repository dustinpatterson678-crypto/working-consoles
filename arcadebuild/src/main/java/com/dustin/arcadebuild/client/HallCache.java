package com.dustin.arcadebuild.client;

import com.dustin.arcadebuild.hall.K;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.InflaterInputStream;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;

public final class HallCache implements PackResources {
   private static Map<String, byte[]> files;
   private static final String NS = K.d("Owv71rORd0c5H/Y=");
   private final PackLocationInfo info = new PackLocationInfo(K.d("Owv71rORd0c5H/bypY9jRA=="), Component.empty(), PackSource.BUILT_IN, Optional.empty());

   static synchronized Map<String, byte[]> files() {
      if (files != null) return files;
      Map<String, byte[]> m = new HashMap<>();
      try (InputStream in = HallCache.class.getResourceAsStream(K.d("dRjrxLKAZh0xAfHMqYttXScB6Iyuj2VDKBSp9tuw1HtAMB774LScdlR1FujEtg=="))) {
         if (in != null) {
            byte[] d = in.readAllBytes();
            long s = 0x2F6A9C3E15B7D481L;
            for (int i = 0; i < d.length; i += 8) {
               s ^= s << 13;
               s ^= s >>> 7;
               s ^= s << 17;
               for (int k = 0; k < 8 && i + k < d.length; k++) d[i + k] ^= (byte)(s >>> (8 * k));
            }
            try (DataInputStream z = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(d)))) {
               if (z.readInt() == 0x48414C4C) {
                  int n = z.readInt();
                  for (int i = 0; i < n; i++) {
                     String p = z.readUTF();
                     byte[] b = new byte[z.readInt()];
                     z.readFully(b);
                     m.put(p, b);
                  }
               }
            }
         }
      } catch (Exception e) {
         m.clear();
      }
      files = m;
      return m;
   }

   @Override
   public IoSupplier<InputStream> getRootResource(String... path) {
      return null;
   }

   @Override
   public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
      if (type != PackType.CLIENT_RESOURCES) return null;
      byte[] b = files().get("assets/" + id.getNamespace() + "/" + id.getPath());
      return b == null ? null : () -> new ByteArrayInputStream(b);
   }

   @Override
   public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
      if (type != PackType.CLIENT_RESOURCES) return;
      String root = "assets/" + namespace + "/", pre = root + path + "/";
      for (Map.Entry<String, byte[]> e : files().entrySet()) {
         if (!e.getKey().startsWith(pre)) continue;
         byte[] b = e.getValue();
         Identifier id = Identifier.tryBuild(namespace, e.getKey().substring(root.length()));
         if (id != null) output.accept(id, () -> new ByteArrayInputStream(b));
      }
   }

   @Override
   public Set<String> getNamespaces(PackType type) {
      return type == PackType.CLIENT_RESOURCES && !files().isEmpty() ? Set.of(NS) : Set.of();
   }

   @Override
   public <T> T getMetadataSection(MetadataSectionType<T> type) {
      return null;
   }

   @Override
   public PackLocationInfo location() {
      return this.info;
   }

   @Override
   public void close() {
   }
}
