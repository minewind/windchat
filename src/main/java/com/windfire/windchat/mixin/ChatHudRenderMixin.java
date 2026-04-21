package com.windfire.windchat.mixin;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.gui.ChatBoxRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ChatHud.class)
public class ChatHudRenderMixin {

    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true
    )
    private void windchat$replaceRender(DrawContext context,
                                        int currentTick,
                                        int mouseX, int mouseY,
                                        boolean focused,
                                        CallbackInfo ci) {
        if (!WindChatClient.config.tabsEnabled) return;

        ChatBoxRenderer.render(context, currentTick, mouseX, mouseY, focused);
        ci.cancel();
    }
}