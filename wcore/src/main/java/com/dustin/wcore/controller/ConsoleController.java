package com.dustin.wcore.controller;

/** Implemented by every item that can play a console (the wireless controller and each module's own pads). */
public interface ConsoleController {
   ControllerType controllerType();
}
