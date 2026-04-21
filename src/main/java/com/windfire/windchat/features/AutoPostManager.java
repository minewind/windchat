package com.windfire.windchat.features;

import com.windfire.windchat.WindChatClient;
import net.minecraft.client.MinecraftClient;


public class AutoPostManager {

    private static long lastPostMs = 0L;


    public static void onTick() {
        var cfg = WindChatClient.config;
        if (!cfg.autoPostEnabled) return;
        if (cfg.autoPostMessage == null || cfg.autoPostMessage.isBlank()) return;

        long intervalMs = (long) cfg.autoPostIntervalMinutes * 60_000L;
        long now        = System.currentTimeMillis();


        if (lastPostMs == 0L) {
            lastPostMs = now;
            return;
        }

        if ((now - lastPostMs) >= intervalMs) {
            sendMessage(cfg.autoPostMessage);
            lastPostMs = now;
        }
    }


    public static void reset() {
        lastPostMs = System.currentTimeMillis();
    }


    public static void sendNow() {
        var cfg = WindChatClient.config;
        if (cfg.autoPostMessage != null && !cfg.autoPostMessage.isBlank()) {
            sendMessage(cfg.autoPostMessage);
        }
        lastPostMs = System.currentTimeMillis();
    }


    private static void sendMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (message.startsWith("/")) {
            mc.player.networkHandler.sendChatCommand(message.substring(1));
        } else {
            mc.player.networkHandler.sendChatMessage(message);
        }
    }
}
