package com.windfire.windchat.features;

import com.windfire.windchat.WindChatClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.*;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ChatNotifyManager {

    private static final int HIGHLIGHT_COLOR = 0xFF8800; // orange


    public static boolean isMention(String original) {
        var cfg = WindChatClient.config;
        if (!cfg.notifyOnMention) return false;
        return bodyContainsTrigger(original, cfg.triggerWords) || isOwnNameInBody(original);
    }


    public static boolean check(String original, ChatTab tab) {
        var cfg = WindChatClient.config;
        boolean shouldPing = false;

        if (cfg.notifyOnDM && tab == null) shouldPing = true;

        if (!shouldPing && cfg.notifyOnEvent && tab == ChatTab.EVENTS) shouldPing = true;

        boolean isMention = false;
        if (cfg.notifyOnMention && tab == ChatTab.MAIN) {
            isMention = bodyContainsTrigger(original, cfg.triggerWords)
                    || isOwnNameInBody(original);
            if (isMention) shouldPing = true;
        }

        if (shouldPing) playPing();


        return isMention;
    }


    public static void playPingPublic() {
        playPing();
    }


    public static Text applyMentionHighlight(Text processed, String original) {
        var cfg = WindChatClient.config;
        if (!cfg.notifyOnMention) return processed;

        if (hasInteractiveContent(processed)) return processed;

        java.util.List<String> words = new java.util.ArrayList<>(cfg.triggerWords);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            String ownName = mc.player.getName().getString();
            if (!ownName.isBlank()) words.add(ownName);
        }

        if (!bodyContainsTrigger(original, words)) return processed;

        String flat      = processed.getString();
        int    colonIdx  = flat.indexOf(": ");
        int    bodyStart = (colonIdx >= 0) ? colonIdx + 2 : 0;

        MutableText result = Text.empty();
        highlightSiblings(processed, words, bodyStart, new int[]{0}, result);
        return result;
    }

    private static void highlightSiblings(Text node, List<String> triggers,
                                          int bodyStart, int[] consumed, MutableText out) {
        String literal = directLiteral(node);
        Style  style   = node.getStyle();

        if (!literal.isEmpty()) {
            int nodeStart = consumed[0];
            int nodeEnd   = nodeStart + literal.length();
            consumed[0]   = nodeEnd;

            if (nodeEnd <= bodyStart) {
                out.append(Text.literal(literal).setStyle(style));
            } else {
                int inBodyOffset = Math.max(0, bodyStart - nodeStart);
                if (inBodyOffset > 0) {
                    out.append(Text.literal(literal.substring(0, inBodyOffset)).setStyle(style));
                }
                String body = literal.substring(inBodyOffset);
                appendHighlighted(body, style, triggers, out);
            }
        }

        for (Text sibling : node.getSiblings()) {
            highlightSiblings(sibling, triggers, bodyStart, consumed, out);
        }
    }

    private static void appendHighlighted(String text, Style baseStyle,
                                          List<String> triggers, MutableText out) {
        String lower = text.toLowerCase();
        int pos = 0;

        while (pos < text.length()) {
            int bestIdx = -1, bestLen = 0;
            for (String word : triggers) {
                if (word.isBlank()) continue;
                int idx = lower.indexOf(word.toLowerCase(), pos);
                if (idx >= 0 && (bestIdx < 0 || idx < bestIdx
                        || (idx == bestIdx && word.length() > bestLen))) {
                    bestIdx = idx;
                    bestLen = word.length();
                }
            }
            if (bestIdx < 0) {
                out.append(Text.literal(text.substring(pos)).setStyle(baseStyle));
                break;
            }
            if (bestIdx > pos)
                out.append(Text.literal(text.substring(pos, bestIdx)).setStyle(baseStyle));
            out.append(Text.literal(text.substring(bestIdx, bestIdx + bestLen))
                    .setStyle(baseStyle
                            .withColor(HIGHLIGHT_COLOR)
                            .withBold(true)));
            pos = bestIdx + bestLen;
        }
    }

    private static String directLiteral(Text node) {
        if (node.getContent() instanceof PlainTextContent.Literal lit) {
            return lit.string();
        }
        return "";
    }

    private static boolean isOwnNameInBody(String original) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return false;
        String ownName = mc.player.getName().getString().toLowerCase();
        if (ownName.isBlank()) return false;



        String stripped = original.toLowerCase();
        if (stripped.startsWith(ownName + " <- ")) return false;
        if (stripped.startsWith("claimed mail:"))    return false;





        int colonIdx = original.indexOf(": ");
        if (colonIdx < 0) return false; // no ": " → not a player chat message
        String body = original.substring(colonIdx + 2);
        return body.toLowerCase().contains(ownName);
    }


    private static boolean hasInteractiveContent(Text node) {
        var style = node.getStyle();
        var ce = style.getClickEvent();
        if (ce != null && ce.getAction() == net.minecraft.text.ClickEvent.Action.RUN_COMMAND)
            return true;
        var he = style.getHoverEvent();
        if (he != null && he.getAction() == net.minecraft.text.HoverEvent.Action.SHOW_ITEM)
            return true;
        for (Text s : node.getSiblings()) {
            if (hasInteractiveContent(s)) return true;
        }
        return false;
    }


    private static boolean bodyContainsTrigger(String original, List<String> triggers) {
        int colonIdx = original.indexOf(": ");
        if (colonIdx < 0) return false; // not a player chat message — skip
        String body  = original.substring(colonIdx + 2);
        String lower = body.toLowerCase();
        for (String word : triggers) {
            if (!word.isBlank() && lower.contains(word.toLowerCase())) return true;
        }
        return false;
    }

    private static void playPing() {
        if (!WindChatClient.config.notifySound) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world == null || mc.player == null) return;
        BlockPos pos = mc.player.getBlockPos();
        mc.world.playSound(mc.player, pos,
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.MASTER, 0.5f, 1.8f);
    }
}