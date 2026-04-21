package com.windfire.windchat.gui;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.api.WindChatAddonRegistry;
import com.windfire.windchat.features.ChatTab;
import com.windfire.windchat.features.ChatTabManager;
import com.windfire.windchat.features.DmTabManager;


public class ChatTabsHud {


    public record TabBounds(ChatTab tab, String dmUser, String addonTabId,
                            int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }


    public static boolean handleClick(double mouseX, double mouseY) {
        if (!WindChatClient.config.tabsEnabled) return false;
        for (TabBounds b : ChatBoxRenderer.getTabBounds()) {
            if (b.contains(mouseX, mouseY)) {
                if (b.addonTabId() != null) {
                    DmTabManager.clearActiveDm();
                    WindChatAddonRegistry.getInstance().switchToAddonTab(b.addonTabId());
                } else if (b.dmUser() != null) {
                    WindChatAddonRegistry.getInstance().clearActiveAddonTab();
                    DmTabManager.switchToDm(b.dmUser());
                } else {
                    ChatTabManager.switchTab(b.tab());
                }
                return true;
            }
        }
        return false;
    }


    public static final int MIN_TAB_BAR_WIDTH = 165;
}