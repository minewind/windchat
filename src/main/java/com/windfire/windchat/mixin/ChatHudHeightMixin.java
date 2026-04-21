package com.windfire.windchat.mixin;

import com.windfire.windchat.WindChatClient;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ChatHud.class)
public class ChatHudHeightMixin {

    @Inject(method = "getHeight", at = @At("RETURN"), cancellable = true)
    private void windchat$adjustHeight(CallbackInfoReturnable<Integer> cir) {
        int offset = WindChatClient.config.chatHeightOffset;
        if (offset != 0) {

            cir.setReturnValue(Math.max(45, cir.getReturnValue() + offset));
        }
    }
}