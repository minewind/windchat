package com.windfire.windchat.features;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.features.ChatColorManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DmTabManager {


    private static final Pattern DM_RECEIVED = Pattern.compile("^([A-Za-z0-9_.]+) >> .+");
    private static final Pattern DM_SENT     = Pattern.compile("^>> ([A-Za-z0-9_.]+): .+");
    private static final long    TIMEOUT_MS  = 150_000L; // 2.5 min (only active when timeout enabled)
    private static final int     MAX_HISTORY = 16384;

    private static final Map<String, ArrayDeque<Text>> history      = new LinkedHashMap<>();
    private static final Map<String, Long>             lastActivity = new LinkedHashMap<>();
    private static final Set<String>                   unread       = new LinkedHashSet<>();

    private static final Set<String>                   manuallyOpened = new LinkedHashSet<>();
    private static String activeDmUser = null;


    public static String extractPartner(String original) {
        Matcher mr = DM_RECEIVED.matcher(original);
        if (mr.matches()) return mr.group(1);
        Matcher ms = DM_SENT.matcher(original);
        if (ms.matches()) return ms.group(1);
        return null;
    }

    public static boolean isDm(String original) {
        return extractPartner(original) != null;
    }


    public static void addMessage(String partner, Text message) {


        Text colored = ChatColorManager.applyDmColor(message);
        history.computeIfAbsent(partner, k -> new ArrayDeque<>()).addLast(colored);
        ArrayDeque<Text> buf = history.get(partner);
        while (buf.size() > MAX_HISTORY) buf.pollFirst();
        lastActivity.put(partner, System.currentTimeMillis());

        manuallyOpened.add(partner);

        if (partner.equals(activeDmUser)) {

            ChatTabManager.refreshChatWith(new ArrayList<>(buf));
        } else {
            unread.add(partner);
        }
    }



    public static List<String> visiblePartners() {
        if (!WindChatClient.config.showDmsTab) return List.of();
        var cfg = WindChatClient.config;
        long now = System.currentTimeMillis();

        java.util.LinkedHashSet<String> visible = new java.util.LinkedHashSet<>();

        for (String pinned : cfg.pinnedDmUsers) {
            visible.add(pinned);
        }

        for (String partner : history.keySet()) {
            if (isPinned(partner)) {

            } else if (manuallyOpened.contains(partner)) {
                visible.add(partner);
            } else if (cfg.dmTabTimeoutEnabled) {
                Long last = lastActivity.get(partner);
                if (last != null && (now - last) < TIMEOUT_MS) {
                    visible.add(partner);
                }
            }
        }
        return new ArrayList<>(visible);
    }


    public static java.util.Collection<String> getKnownPartners() {
        return java.util.Collections.unmodifiableSet(history.keySet());
    }


    public static boolean hasHistory(String partner) {
        return history.containsKey(partner) && !history.get(partner).isEmpty();
    }


    public static boolean isPinned(String partner) {
        return WindChatClient.config.pinnedDmUsers.stream()
                .anyMatch(p -> p.equalsIgnoreCase(partner));
    }


    public static boolean pin(String partner) {
        if (isPinned(partner)) return false;
        WindChatClient.config.pinnedDmUsers.add(partner);
        WindChatClient.config.save();
        manuallyOpened.add(partner); // ensure visible immediately this session
        return true;
    }


    public static boolean unpin(String partner) {
        boolean removed = WindChatClient.config.pinnedDmUsers
                .removeIf(p -> p.equalsIgnoreCase(partner));
        if (removed) WindChatClient.config.save();
        return removed;
    }



    public static ToggleResult toggleTab(String partner) {

        if (!hasHistory(partner) && !isPinned(partner)) return ToggleResult.NO_HISTORY;
        if (isPinned(partner))    return ToggleResult.PINNED;
        if (manuallyOpened.contains(partner)) {
            manuallyOpened.remove(partner);

            if (partner.equals(activeDmUser)) {
                activeDmUser = null;
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.inGameHud != null) ChatTabManager.switchTab(ChatTab.MAIN);
            }
            return ToggleResult.CLOSED;
        } else {
            manuallyOpened.add(partner);
            switchToDm(partner);
            return ToggleResult.OPENED;
        }
    }

    public enum ToggleResult { OPENED, CLOSED, NO_HISTORY, PINNED }


    public static String getActiveUser()       { return activeDmUser; }
    public static boolean isViewingDm()        { return activeDmUser != null; }
    public static boolean hasUnread(String p)  { return unread.contains(p); }

    public static void switchToDm(String partner) {
        activeDmUser = partner;
        unread.remove(partner);
        lastActivity.put(partner, System.currentTimeMillis()); // reset timeout on manual switch
        rebuildChat(partner);
    }

    public static void clearActiveDm() { activeDmUser = null; }

    public static boolean shouldShowInVanilla(String partner) {
        return partner.equals(activeDmUser);
    }


    public static void onTick() {

        if (!WindChatClient.config.dmTabTimeoutEnabled) return;
        if (activeDmUser == null) return;
        if (isPinned(activeDmUser)) return; // pinned tabs never time out

        Long last = lastActivity.get(activeDmUser);
        if (last == null || (System.currentTimeMillis() - last) >= TIMEOUT_MS) {
            manuallyOpened.remove(activeDmUser);
            activeDmUser = null;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.inGameHud != null) ChatTabManager.switchTab(ChatTab.MAIN);
        }
    }


    public static void broadcastToAllDmTabs(net.minecraft.text.Text message) {
        for (var entry : history.entrySet()) {
            ArrayDeque<net.minecraft.text.Text> buf = entry.getValue();
            buf.addLast(message);
            while (buf.size() > 4096) buf.pollFirst();
        }
    }


    private static void rebuildChat(String partner) {
        List<Text> raw = new ArrayList<>(history.getOrDefault(partner, new ArrayDeque<>()));

        List<Text> coloured = new ArrayList<>(raw.size());
        for (Text msg : raw) coloured.add(ChatColorManager.applyDmColor(msg));

        ChatTabManager.refreshChatWith(coloured);
    }
}