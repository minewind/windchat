package com.windfire.windchat.mixin;

import com.windfire.windchat.gui.ChatTabsHud;
import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ChatScreen.class)
public abstract class ChatScreenClickMixin {

    @Inject(
        method = "mouseClicked",
        at = @At("HEAD"),
        cancellable = true
    )
    private void windchat$onMouseClicked(double mouseX, double mouseY,
                                         int button, CallbackInfoReturnable<Boolean> cir) {

        if (button != 0) return;

        if (ChatTabsHud.handleClick(mouseX, mouseY)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
