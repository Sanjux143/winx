package com.winlator.core;

import com.winlator.winhandler.WinHandler;
import com.winlator.xserver.Window;

import java.util.Timer;
import java.util.TimerTask;

public abstract class Workarounds {
    // Workaround for applications that don't work mouse/keyboard
    public static void onMapWindow(final WinHandler winHandler, Window window) {
        final String className = window.getClassName();
        if (className.equals("twfc.exe")) {
            (new Timer()).schedule(new TimerTask() {
                @Override
                public void run() {
                    winHandler.bringToFront(className, true);
                }
            }, 500);
        }
    }
}
