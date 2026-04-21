package com.windfire.windchat.api;

import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;


public class AddonTab {


    private static final int MAX_HISTORY = 4096;

    private final String id;
    private final String displayName;
    private final int activeColor;
    private final ArrayDeque<Text> history = new ArrayDeque<>();
    private boolean unread = false;
    public AddonTab(String id, String displayName, int activeColor) {
        this.id          = id;
        this.displayName = displayName;
        this.activeColor = activeColor;
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public int    getActiveColor() { return activeColor; }
    public boolean hasUnread()     { return unread; }

    public void addMessage(Text message, boolean isActive) {
        history.addLast(message);
        while (history.size() > MAX_HISTORY) history.pollFirst();
        if (!isActive) unread = true;
    }


    public void clearUnread() { unread = false; }


    public List<Text> getHistory() { return new ArrayList<>(history); }
}
