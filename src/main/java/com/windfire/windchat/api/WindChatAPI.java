package com.windfire.windchat.api;

import net.minecraft.text.Text;


public interface WindChatAPI {


    void postToAddonTab(String tabId, Text message);


    void playNotificationPing();


    boolean isAddonTabActive(String tabId);
}
