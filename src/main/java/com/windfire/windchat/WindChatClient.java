package com.windfire.windchat;

import com.windfire.windchat.api.WindChatAddonRegistry;
import com.windfire.windchat.commands.WindChatCommands;
import com.windfire.windchat.config.WindChatConfig;
import com.windfire.windchat.features.ChatMacroManager;
import com.windfire.windchat.gui.ChatTabsHud;
import com.windfire.windchat.gui.WindChatConfigScreen;
import com.windfire.windchat.features.AutoPostManager;
import com.windfire.windchat.features.DmTabManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import com.windfire.windchat.features.ChatTabManager;
import com.windfire.windchat.features.ChatTab;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;


public class WindChatClient implements ClientModInitializer {


    public static WindChatConfig config;

    private static KeyBinding settingsKey;

    @Override
    public void onInitializeClient() {

        config = WindChatConfig.load();

        settingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.windchat.settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                "WindChat"
        ));




        WindChatAddonRegistry.setLiveNotificationPusher(
                com.windfire.windchat.features.ChatTabManager::appendLiveNotification);
        WindChatAddonRegistry.setChatRefresher(
                com.windfire.windchat.features.ChatTabManager::refreshChatWith);
        WindChatAddonRegistry.setPingPlayer(
                com.windfire.windchat.features.ChatNotifyManager::playPingPublic);
        WindChatAddonRegistry.getInstance().initialize();

        ChatMacroManager.init();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (settingsKey.wasPressed()) {
                mc.setScreen(WindChatConfigScreen.create(null));
            }
            DmTabManager.onTick();
            AutoPostManager.onTick();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> {
                    ChatTabManager.onWorldJoin();
                    DmTabManager.clearActiveDm();
                    ChatTabManager.switchTab(ChatTab.MAIN);
                })
        );

        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> WindChatCommands.register(dispatcher)
        );

        System.out.println("[WindChat] Loaded. Tabs: " + config.tabsEnabled
                + " | Timestamps: " + config.timestampMode
                + " | Macros: "     + config.macros.size()
                + " | Filters: "    + config.regexFilters.size()
                + " | Addons: "     + WindChatAddonRegistry.getInstance().getAddons().size());
    }
}
