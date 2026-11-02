package com.dustin.wcore;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public final class ConsoleLinks {
   private static final List<Supplier<String>> ACTIVE = new CopyOnWriteArrayList<>();

   private ConsoleLinks() {
   }

   public static void register(Supplier<String> activeConsole) {
      ACTIVE.add(activeConsole);
   }

   public static String active() {
      for (Supplier<String> s : ACTIVE) {
         String name = s.get();
         if (name != null) {
            return name;
         }
      }

      return null;
   }
}
