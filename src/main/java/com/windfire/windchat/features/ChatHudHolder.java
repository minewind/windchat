package com.windfire.windchat.features;

import net.minecraft.text.Text;


public final class ChatHudHolder {
    private static Text    processed;
    private static String  original;
    private static ChatTab tab;
    private static boolean mention;

    public static void setProcessed(Text t)  { processed = t; }
    public static void setOriginal(String s) { original  = s; }
    public static void setTab(ChatTab t)     { tab       = t; }
    public static void setMention(boolean m) { mention   = m; }

    public static Text    getProcessed() { return processed; }
    public static String  getOriginal()  { return original;  }
    public static ChatTab getTab()       { return tab;       }
    public static boolean getMention()   { return mention;   }

    public static void clear() {
        processed = null; original = null; tab = null; mention = false;
    }

    public static void clearOriginalAndProcessed() {
        processed = null; original = null;
    }
}