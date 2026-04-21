package com.windfire.windchat.features;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.api.WindChatAddonRegistry;
import com.windfire.windchat.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;

import java.util.*;
import java.util.regex.Pattern;


public class ChatTabManager {


    private static final Pattern CLAN_BRACKET = Pattern.compile("^\\[[\\w]+\\] \\S+: .+");

    private static final List<Pattern> DEATH_PATTERNS = List.of(
            Pattern.compile("\\bwas rekt by\\b"),
            Pattern.compile("\\bwas blown up by\\b"),
            Pattern.compile("\\brekt\\b.+\\bgot a double kill\\b"),
            Pattern.compile("\\brekt\\b.+\\bgot a TRIPLE kill\\b"),
            Pattern.compile("\\brekt\\b.+\\bgot an ULTRA KILL\\b"),
            Pattern.compile("\\bsuffocated in a wall\\b"),
            Pattern.compile("\\btried to swim in lava\\b"),
            Pattern.compile("\\bdied because of\\b"),
            Pattern.compile("\\bsucked\\b.+\\bdry\\b"),
            Pattern.compile("\\bwithered away\\b"),
            Pattern.compile("\\bwas slain by\\b"),
            Pattern.compile("\\bfell from a high place\\b"),
            Pattern.compile("\\bfell out of the world\\b"),
            Pattern.compile("\\bwas fireballed by\\b"),
            Pattern.compile("\\bwas doomed to fall\\b"),
            Pattern.compile("\\bwas shot by\\b"),
            Pattern.compile("\\bwas stung to death\\b"),
            Pattern.compile("\\bexperienced kinetic energy\\b"),
            Pattern.compile("\\bwent up in flames\\b"),
            Pattern.compile("\\bis on RAMPAGE\\b"),
            Pattern.compile("\\bwas zapped by\\b"),
            Pattern.compile("\\bdrowned\\b"),
            Pattern.compile("\\bborrowed soul of\\b"),
            Pattern.compile("\\bobliterated by a sonically-charged shriek\\b"),
            Pattern.compile("^\\S+ died$"),
            Pattern.compile("\\bwas killed by magic\\b"),
            Pattern.compile("\\bwas burned to a crisp while fighting\\b")
    );

    private static final List<Pattern> EVENT_PATTERNS = List.of(
            Pattern.compile("begins in (1 hour|30 minutes|5 minutes)\\.?$"),
            Pattern.compile("Snowmen invade"),
            Pattern.compile("Snowmen melt away"),
            Pattern.compile("Labyrinth event has (started|ended)"),
            Pattern.compile("Team (red|aqua) wins the beef event"),
            Pattern.compile("wins the abyssal event"),
            Pattern.compile("Attack on Giant Event has begun"),
            Pattern.compile("Attack on Giant Event ends"),
            Pattern.compile("Fox Hunt has begun"),
            Pattern.compile("Fox Hunt event ends"),
            Pattern.compile("^\\d+\\) \\S+ -- \\d+ foxes"),
            Pattern.compile("Fishing event ends"),
            Pattern.compile("^\\d+\\) \\S+ -- \\d+ fish$"),
            Pattern.compile("Battle for Minewind"),
            Pattern.compile("minewind\\.com/battle"),
            Pattern.compile("hold the Minewind City"),
            Pattern.compile("take the Minewind City"),
            Pattern.compile("(Fascism|Communism|Capitalism).+has been lifted"),
            Pattern.compile("Team (red|aqua) wins the Team Deathmatch event"),
            Pattern.compile("Free-For-All"),
            Pattern.compile("^\\d+\\) \\S+ -- \\d+ kills$"),
            Pattern.compile("Free-For-All event ends")
    );


    public static volatile boolean isReplaying = false;

    private static ChatTab currentTab = ChatTab.MAIN;
    private static final Map<ChatTab, Boolean> unread = new EnumMap<>(ChatTab.class);

    static {
        for (ChatTab t : ChatTab.values()) unread.put(t, false);
    }


    public static ChatTab classify(String original) {
        if (DmTabManager.isDm(original)) return null;
        if (!WindChatClient.config.tabsEnabled) return ChatTab.MAIN;

        String stripped = original
                .replaceFirst("^(§.)+", "")
                .replaceAll("§r", "")
                .replaceAll("§[0-9a-fA-FklmnorKLMNOR](?=\\s)", "");

        if (CLAN_BRACKET.matcher(stripped).find()) {
            String myTag = WindChatClient.config.myClanTag.strip();
            boolean matchesMyTag = myTag.isEmpty() || stripped.toLowerCase()
                    .startsWith("[" + myTag.toLowerCase() + "] ");
            if (matchesMyTag) {
                if (WindChatClient.config.showClanTab)        return ChatTab.CLAN;
                if (WindChatClient.config.filterClanFromMain) return ChatTab.GLOBAL;
            }
        }

        for (Pattern p : DEATH_PATTERNS) {
            if (p.matcher(stripped).find()) {
                if (WindChatClient.config.showDeathsTab)         return ChatTab.DEATHS;
                if (WindChatClient.config.filterDeathsFromMain)  return ChatTab.GLOBAL;
                break;
            }
        }

        for (Pattern p : EVENT_PATTERNS) {
            if (p.matcher(stripped).find()) {
                if (WindChatClient.config.showEventsTab)         return ChatTab.EVENTS;
                if (WindChatClient.config.filterEventsFromMain)  return ChatTab.GLOBAL;
                break;
            }
        }

        return ChatTab.MAIN;
    }



    public static void addToTab(ChatTab tab, Text message, boolean isMention) {

        if (tab != currentTab) {
            switch (tab) {
                case DEATHS   -> { /* deaths tab never shows a badge */ }
                case MENTIONS -> { /* mention badge handled below */ }
                case MAIN     -> { if (isMention) unread.put(ChatTab.MAIN, true); }
                default       -> unread.put(tab, true);
            }
        }

        if (isMention && WindChatClient.config.showMentionsTab
                && currentTab != ChatTab.MENTIONS) {
            unread.put(ChatTab.MENTIONS, true);
        }
    }



    public static boolean shouldShowInVanilla(ChatTab tab) {
        if (DmTabManager.isViewingDm()) return false;
        if (WindChatAddonRegistry.getInstance().isViewingAddonTab()) return false;
        if (currentTab == ChatTab.GLOBAL) return true; // Global shows everything
        return tab == currentTab;
    }

    public static ChatTab getCurrentTab()      { return currentTab; }
    public static boolean hasUnread(ChatTab t) { return Boolean.TRUE.equals(unread.get(t)); }


    public static void switchTab(ChatTab tab) {
        WindChatAddonRegistry.getInstance().clearActiveAddonTab();
        DmTabManager.clearActiveDm();
        currentTab = tab;
        unread.put(tab, false);
        rebuildVanillaChat();
    }


    public static void rebuildVanillaChat() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null) return;
        ChatHud chatHud = mc.inGameHud.getChatHud();
        if (!(chatHud instanceof ChatHudAccessor accessor)) return;

        List<ChatHudLine> msgs = accessor.windchat$getMessages();       // newest-first
        List<ChatHudLine.Visible> vis = accessor.windchat$getVisibleMessages();

        vis.clear();
        if (msgs.isEmpty()) return;

        isReplaying = true;
        try {


            for (int i = msgs.size() - 1; i >= 0; i--) {
                ChatHudLine line = msgs.get(i);
                if (shouldIncludeInCurrentTab(line)) {
                    accessor.windchat$addVisibleMessage(line);
                }
            }
        } finally {
            isReplaying = false;
        }
    }


    public static boolean shouldIncludeInCurrentTab(ChatHudLine line) {
        String plain = line.content().getString();

        String stripped = plain.replaceAll("§[0-9a-fA-FklmnorKLMNOR]", "").trim();

        if (stripped.contains("acceptTP")) return true;

        if (currentTab == ChatTab.GLOBAL) return true;



        String forClassify = stripped.replaceFirst(
                "^\\[\\d{1,2}:\\d{2}(?:\\s*[AaPp][Mm])?\\]\\s*", "");

        if (currentTab == ChatTab.MENTIONS) {
            return ChatNotifyManager.isMention(forClassify);
        }

        ChatTab tab = classify(forClassify);
        return tab == currentTab;
    }


    public static void appendLiveNotification(Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null) return;
        ChatHud chatHud = mc.inGameHud.getChatHud();
        if (!(chatHud instanceof ChatHudAccessor accessor)) return;

        int ticks = mc.inGameHud.getTicks();
        ChatHudLine line = new ChatHudLine(ticks, message, null, null);

        accessor.windchat$getMessages().add(0, line);

        isReplaying = true;
        try {
            accessor.windchat$addVisibleMessage(line);
        } finally {
            isReplaying = false;
        }
    }


    public static void refreshChatWith(List<Text> messages) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null) return;
        ChatHud chatHud = mc.inGameHud.getChatHud();
        if (!(chatHud instanceof ChatHudAccessor accessor)) return;

        accessor.windchat$getVisibleMessages().clear();
        if (messages == null || messages.isEmpty()) return;

        int ticks = mc.inGameHud.getTicks();
        isReplaying = true;
        try {
            for (Text msg : messages) {
                ChatHudLine line = new ChatHudLine(ticks, msg, null, null);
                accessor.windchat$addVisibleMessage(line);
            }
        } finally {
            isReplaying = false;
        }
    }


    public static void onWorldJoin() {
        isReplaying = false;
        currentTab  = ChatTab.MAIN;
        WindChatAddonRegistry.getInstance().clearActiveAddonTab();
        for (ChatTab t : ChatTab.values()) unread.put(t, false);
    }


    public static List<ChatTab> visibleTabs() {
        List<ChatTab> tabs = new ArrayList<>();
        tabs.add(ChatTab.MAIN);
        if (WindChatClient.config.tabsEnabled) {
            if (WindChatClient.config.showGlobalTab)   tabs.add(ChatTab.GLOBAL);
            if (WindChatClient.config.showClanTab)     tabs.add(ChatTab.CLAN);
            if (WindChatClient.config.showEventsTab)   tabs.add(ChatTab.EVENTS);
            if (WindChatClient.config.showDeathsTab)   tabs.add(ChatTab.DEATHS);
            if (WindChatClient.config.showMentionsTab) tabs.add(ChatTab.MENTIONS);
        }
        return tabs;
    }
}