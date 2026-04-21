package com.windfire.windchat.mixin;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.gui.ChatTabsHud;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ChatHud.class)
public class ChatHudWidthMixin {

    @Inject(
            method = "getWidth",
            at = @At("RETURN"),
            cancellable = true
    )
    private void windchat$adjustWidth(CallbackInfoReturnable<Integer> cir) {
        int vanilla  = cir.getReturnValue();
        int adjusted = vanilla + WindChatClient.config.chatWidthOffset;
        int result   = Math.max(adjusted, ChatTabsHud.MIN_TAB_BAR_WIDTH);
        cir.setReturnValue(result);
    }
}