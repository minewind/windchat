package com.windfire.windchat.mixin;

import com.windfire.windchat.gui.ChatTabsHud;
import net.minecraft.client.Mouse;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void windchat$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {

        if (button != 0 || action != 1) return;

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc == null || mc.currentScreen != null) return;

        double scaleFactor = mc.getWindow().getScaleFactor();
        double mouseX = mc.mouse.getX() / scaleFactor;
        double mouseY = mc.mouse.getY() / scaleFactor;

        if (ChatTabsHud.handleClick(mouseX, mouseY)) {
            ci.cancel();
        }
    }
}
