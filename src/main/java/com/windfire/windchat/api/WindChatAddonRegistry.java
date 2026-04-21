package com.windfire.windchat.api;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.*;


public class WindChatAddonRegistry {



    private static WindChatAddonRegistry INSTANCE;

    public static WindChatAddonRegistry getInstance() {
        if (INSTANCE == null) INSTANCE = new WindChatAddonRegistry();
        return INSTANCE;
    }

    private WindChatAddonRegistry() {}



    private final List<WindChatAddon>        addons  = new ArrayList<>();
    private final Map<String, AddonTab>      tabs    = new LinkedHashMap<>();
    private       String                     activeAddonTabId = null;


    private static java.util.function.Consumer<Text> liveNotificationPusher = null;


    private static java.util.function.Consumer<java.util.List<Text>> chatRefresher = null;

    public static void setLiveNotificationPusher(java.util.function.Consumer<Text> pusher) {
        liveNotificationPusher = pusher;
    }

    public static void setChatRefresher(java.util.function.Consumer<java.util.List<Text>> refresher) {
        chatRefresher = refresher;
    }

    private static Runnable pingPlayer = null;

    public static void setPingPlayer(Runnable ping) {
        pingPlayer = ping;
    }




    public void initialize() {
        FabricLoader.getInstance()
                .getEntrypointContainers("windchat", WindChatAddon.class)
                .forEach(container -> {
                    WindChatAddon addon = container.getEntrypoint();
                    addons.add(addon);

                    try {
                        addon.onInitialize();
                    } catch (Exception e) {
                        System.err.println("[WindChat] Addon '" + addon.getAddonId()
                                + "' threw during onInitialize: " + e.getMessage());
                        e.printStackTrace();
                        return;
                    }

                    for (AddonTab tab : addon.getAddonTabs()) {
                        tabs.put(tab.getId(), tab);
                    }

                    System.out.println("[WindChat] Loaded addon: " + addon.getAddonId());
                });
    }



    public List<WindChatAddon>   getAddons()     { return Collections.unmodifiableList(addons); }
    public Collection<AddonTab>  getAddonTabs()  { return tabs.values(); }
    public Optional<AddonTab>    getTab(String id){ return Optional.ofNullable(tabs.get(id)); }

    public boolean isViewingAddonTab()           { return activeAddonTabId != null; }
    public String  getActiveAddonTabId()         { return activeAddonTabId; }




    public void switchToAddonTab(String id) {
        AddonTab tab = tabs.get(id);
        if (tab == null) return;

        activeAddonTabId = id;
        tab.clearUnread();
        rebuildAddonChat(tab);
    }


    public void clearActiveAddonTab() {
        activeAddonTabId = null;
    }


    private void rebuildAddonChat(AddonTab tab) {
        if (chatRefresher != null) chatRefresher.accept(tab.getHistory());
    }


    public WindChatAPI getAPI() { return API_IMPL; }

    private final WindChatAPI API_IMPL = new WindChatAPI() {

        @Override
        public void postToAddonTab(String tabId, Text message) {
            AddonTab tab = tabs.get(tabId);
            if (tab == null) {
                System.err.println("[WindChat] postToAddonTab: tab '" + tabId
                        + "' NOT FOUND. Registered: " + tabs.keySet());
                return;
            }

            boolean isActive = tabId.equals(activeAddonTabId);
            System.out.println("[WindChat] postToAddonTab OK: tabId='" + tabId
                    + "' isActive=" + isActive + " msg=" + message.getString());
            tab.addMessage(message, isActive);

            if (isActive) {
                rebuildAddonChat(tab);
            } else {
                if (liveNotificationPusher != null) {
                    liveNotificationPusher.accept(message);
                }
            }
        }

        @Override
        public void playNotificationPing() {
            if (pingPlayer != null) pingPlayer.run();
        }

        @Override
        public boolean isAddonTabActive(String tabId) {
            return tabId.equals(activeAddonTabId);
        }
    };
}