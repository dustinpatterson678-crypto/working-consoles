package com.dustin.wcore.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuHook implements ModMenuApi {
   public ConfigScreenFactory<?> getModConfigScreenFactory() {
      return ConsoleSettingsScreen::new;
   }
}
