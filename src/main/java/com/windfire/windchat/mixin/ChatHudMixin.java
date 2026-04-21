package com.windfire.windchat.mixin;

import com.windfire.windchat.features.*;
import com.windfire.windchat.api.WindChatAddonRegistry;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {



    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 1
    )
    private Text windchat$processMessage(Text original) {
        if (ChatTabManager.isReplaying) return original;

        String raw = original.getString();
        ChatHudHolder.setOriginal(raw);

        ChatTab tab = ChatTabManager.classify(raw);
        ChatHudHolder.setTab(tab);

        Text timestamped = TimestampManager.apply(original);
        Text colored     = ChatColorManager.apply(timestamped, raw, tab != null ? tab : ChatTab.MAIN);
        Text highlighted = ChatNotifyManager.applyMentionHighlight(colored, raw);

        Text finalText = (tab == null)
                ? ChatColorManager.applyDmColor(highlighted)
                : highlighted;

        ChatHudHolder.setProcessed(finalText);
        return finalText;
    }



    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void windchat$intercept(Text message, CallbackInfo ci) {
        if (ChatTabManager.isReplaying) return;

        String original = ChatHudHolder.getOriginal();
        if (original == null) return;

        if (MuteManager.shouldHide(original))          { ChatHudHolder.clear(); ci.cancel(); return; }
        if (MuteManager.shouldFilterBuiltin(original)) { ChatHudHolder.clear(); ci.cancel(); return; }
        if (RegexFilterManager.shouldFilter(original)) { ChatHudHolder.clear(); ci.cancel(); return; }

        Text    processed = ChatHudHolder.getProcessed();
        Text    toStore   = processed != null ? processed : message;
        ChatTab tab       = ChatHudHolder.getTab();

        if (tab == null) {
            String partner = DmTabManager.extractPartner(original);
            if (partner != null) {
                DmTabManager.addMessage(partner, toStore);
                if (!DmTabManager.shouldShowInVanilla(partner)) ci.cancel();
            }
            ChatHudHolder.clear();
            return;
        }

        boolean isMention = ChatNotifyManager.check(original, tab);
        ChatHudHolder.setMention(isMention);

        ChatTabManager.addToTab(tab, toStore, isMention);

        ChatHudHolder.clearOriginalAndProcessed();
    }



    @Inject(
            method = "addVisibleMessage",
            at = @At("HEAD"),
            cancellable = true
    )
    private void windchat$filterVisible(ChatHudLine line, CallbackInfo ci) {
        if (ChatTabManager.isReplaying) return;

        ChatTab storedTab = ChatHudHolder.getTab();

        String plain = line.content().getString().replaceAll("§[0-9a-fA-FklmnorKLMNOR]", "").trim();
        if (plain.contains("acceptTP")) {
            ChatHudHolder.clear();
            return;
        }

        if (DmTabManager.isViewingDm() || WindChatAddonRegistry.getInstance().isViewingAddonTab()) {
            ci.cancel();
            ChatHudHolder.clear();
            return;
        }

        String plainForClassify = plain.replaceFirst(
                "^\\[\\d{1,2}:\\d{2}(?:\\s*[AaPp][Mm])?\\]\\s*", "");

        ChatTab tab = storedTab != null ? storedTab : ChatTabManager.classify(plainForClassify);
        if (tab == null) { ChatHudHolder.clear(); return; }

        if (ChatTabManager.getCurrentTab() == ChatTab.GLOBAL) {
            ChatHudHolder.clear();
            return;
        }

        if (ChatTabManager.getCurrentTab() == ChatTab.MENTIONS) {
            if (!ChatHudHolder.getMention() && !ChatNotifyManager.isMention(plain)) ci.cancel();
            ChatHudHolder.clear();
            return;
        }

        if (tab != ChatTabManager.getCurrentTab()) {
            ci.cancel();
        }

        ChatHudHolder.clear();
    }
}