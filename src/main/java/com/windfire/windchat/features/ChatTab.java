package com.windfire.windchat.features;

public enum ChatTab {
    MAIN("Main"),
    GLOBAL("Global"),
    CLAN("Clan"),
    EVENTS("Events"),
    DEATHS("Deaths"),
    MENTIONS("Mentions");

    public final String displayName;

    ChatTab(String displayName) {
        this.displayName = displayName;
    }
}
