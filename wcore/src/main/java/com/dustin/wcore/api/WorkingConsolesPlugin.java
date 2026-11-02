package com.dustin.wcore.api;

/**
 * Entry point for addon mods. Implement it and list the class under the "workingconsoles" entrypoint in your
 * fabric.mod.json; Working Consoles calls register() once while it starts, so register your consoles and
 * controllers there with {@link WorkingConsolesApi}.
 */
@FunctionalInterface
public interface WorkingConsolesPlugin {
   void register();
}
