package com.windfire.windchat.mixin;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.gui.ChatBoxRenderer;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(ChatHud.class)
public abstract class ChatHudClickMixin {

    @ModifyVariable(
        method = "getTextStyleAt",
        at = @At("HEAD"),
        argsOnly = true,
        index = 3
    )
    private double windchat$adjustClickY(double y) {
        if (!WindChatClient.config.tabsEnabled) return y;
        return y + ChatBoxRenderer.TAB_BAR_HEIGHT;
    }
}
