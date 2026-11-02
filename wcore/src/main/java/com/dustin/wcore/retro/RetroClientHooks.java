package com.dustin.wcore.retro;

import java.util.function.Consumer;

public final class RetroClientHooks {
   public static Consumer<RetroConsoleBlockEntity> TICK = be -> {};

   private RetroClientHooks() {
   }
}
