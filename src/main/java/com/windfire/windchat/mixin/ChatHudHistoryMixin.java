package com.windfire.windchat.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


@Mixin(ChatHud.class)
public abstract class ChatHudHistoryMixin {


    @ModifyConstant(
        method = "addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
        constant = @Constant(intValue = 100),
        require = 0
    )
    private int windchat$extendVisibleMessages(int original) {
        return 16384;
    }


    @ModifyConstant(
        method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
        constant = @Constant(intValue = 100),
        require = 0
    )
    private int windchat$extendStoredMessages(int original) {
        return 16384;
    }


    @ModifyConstant(
        method = "addMessage(Lnet/minecraft/text/Text;)V",
        constant = @Constant(intValue = 100),
        require = 0
    )
    private int windchat$extendPublicEntry(int original) {
        return 16384;
    }
}
