package com.windfire.windchat.features;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.config.ColorRule;
import net.minecraft.text.*;
import net.minecraft.text.PlainTextContent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ChatColorManager {


    public static Text apply(Text processed, String original, ChatTab tab) {
        List<ColorRule> rules = WindChatClient.config.colorRules;
        if (rules.isEmpty()) return processed;


        if (hasInteractiveContent(processed)) return processed;

        String raw = processed.getString();

        for (ColorRule rule : rules) {
            String scope = (rule.scope == null) ? "LINE" : rule.scope.toUpperCase();
            int    color = parseHex(rule.hexColor);

            if (scope.equals("CLAN")) {



                if (tab != ChatTab.MAIN) continue;
                String tagLow = rule.keyword.toLowerCase().trim();
                if (tagLow.isEmpty()) continue;
                if (original.toLowerCase().startsWith(tagLow + ".")) {
                    return applyLine(processed, color);
                }
                continue;
            }

            Pattern p;
            try { p = Pattern.compile(rule.keyword, Pattern.CASE_INSENSITIVE); }
            catch (PatternSyntaxException e) { continue; }
            if (!p.matcher(raw).find()) continue;

            return scope.equals("LINE") ? applyLine(processed, color) : applyWord(raw, p, color);
        }
        return processed;
    }


    private static Text applyLine(Text processed, int color) {
        MutableText result = Text.empty();
        applyLineToNode(processed, color, result, true);
        return result;
    }

    private static void applyLineToNode(Text node, int color, MutableText out, boolean isRoot) {

        boolean isTimestamp = isTimestampNode(node);

        String literal = directLiteral(node);
        if (!literal.isEmpty()) {
            if (isTimestamp) {

                out.append(Text.literal(literal).setStyle(node.getStyle()));
            } else {

                Style colored = node.getStyle().withColor(color);
                out.append(Text.literal(literal).setStyle(colored));
            }
        }

        for (Text sibling : node.getSiblings()) {
            applyLineToNode(sibling, color, out, false);
        }
    }


    private static boolean isTimestampNode(Text node) {
        net.minecraft.text.TextColor tc = node.getStyle().getColor();
        return tc != null && tc.getRgb() == 0x888888;
    }

    private static String directLiteral(Text node) {
        if (node.getContent() instanceof PlainTextContent.Literal lit) return lit.string();
        return "";
    }

    private static Text applyWord(String raw, Pattern p, int color) {
        MutableText result = Text.empty();
        Matcher m = p.matcher(raw);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) result.append(Text.literal(raw.substring(last, m.start())));
            result.append(Text.literal(m.group()).styled(s -> s.withColor(color)));
            last = m.end();
        }
        if (last < raw.length()) result.append(Text.literal(raw.substring(last)));
        return result;
    }


    private static boolean hasInteractiveContent(Text node) {
        var ce = node.getStyle().getClickEvent();
        if (ce != null && ce.getAction() == net.minecraft.text.ClickEvent.Action.RUN_COMMAND) return true;
        for (Text s : node.getSiblings()) {
            if (hasInteractiveContent(s)) return true;
        }
        return false;
    }

    private static int parseHex(String hex) {
        try { return (int) Long.parseLong(hex.replace("#", ""), 16); }
        catch (NumberFormatException e) { return 0xFFFFFF; }
    }



    public static Text applyDmColor(Text processed) {
        if (hasInteractiveContent(processed)) return processed;
        return applyLine(processed, 0xFF55FF);
    }

    public static void addRule(String keyword, String hex, String scope) {
        WindChatClient.config.colorRules.add(new ColorRule(keyword, hex, scope));
        WindChatClient.config.save();
    }

    public static boolean removeRule(String keyword) {
        boolean removed = WindChatClient.config.colorRules
                .removeIf(r -> r.keyword.equalsIgnoreCase(keyword));
        if (removed) WindChatClient.config.save();
        return removed;
    }
}
