package com.dustin.wcore.api;

/**
 * Client side of {@link WorkingConsolesPlugin}: list it under the "workingconsoles_client" entrypoint. Register
 * controller renderers and TV picture sources there with {@link WorkingConsolesClientApi}.
 */
@FunctionalInterface
public interface WorkingConsolesClientPlugin {
   void registerClient();
}
