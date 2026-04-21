package com.windfire.windchat.mixin;

import com.windfire.windchat.features.ChatTab;
import com.windfire.windchat.features.ChatTabManager;
import com.windfire.windchat.features.DmTabManager;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ChatScreen.class)
public abstract class ChatScreenSendMixin {

    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private void windchat$autoPrefix(String chatText, boolean addToHistory, CallbackInfo ci) {
        if (chatText == null || chatText.isBlank() || chatText.startsWith("/")) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;


        String dmUser = DmTabManager.getActiveUser();
        if (dmUser != null) {
            mc.player.networkHandler.sendChatCommand("msg " + dmUser + " " + chatText);
            if (addToHistory) mc.inGameHud.getChatHud().addToMessageHistory(chatText);
            mc.setScreen(null);
            ci.cancel();
            return;
        }


        if (ChatTabManager.getCurrentTab() == ChatTab.CLAN) {
            mc.player.networkHandler.sendChatCommand("c " + chatText);
            if (addToHistory) mc.inGameHud.getChatHud().addToMessageHistory(chatText);
            mc.setScreen(null);
            ci.cancel();
        }
    }
}
