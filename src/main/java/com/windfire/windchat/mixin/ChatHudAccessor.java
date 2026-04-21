package com.windfire.windchat.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatHud.class)
public interface ChatHudAccessor {

    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> windchat$getVisibleMessages();

    @Accessor("messages")
    List<ChatHudLine> windchat$getMessages();

    @Accessor("scrolledLines")
    int windchat$getScrolledLines();

    @Invoker("addVisibleMessage")
    void windchat$addVisibleMessage(ChatHudLine line);
}