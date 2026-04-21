package com.windfire.windchat.features;

import com.windfire.windchat.WindChatClient;

public class MuteManager {


    public static boolean shouldHide(String original) {
        var cfg = WindChatClient.config;

        if (cfg.filterHeartReplacement && isHeartMessage(original)) return true;

        for (String player : cfg.mutedPlayers) {
            if (isFromPlayer(original, player)) return true;
        }
        for (String clan : cfg.mutedClans) {
            if (isFromClan(original, clan)) return true;
        }
        return false;
    }


    private static boolean isHeartMessage(String original) {


        return stripFormatting(original).strip().startsWith("❤");
    }

    private static boolean isFromPlayer(String original, String player) {
        String lower     = original.toLowerCase();
        String playerLow = player.toLowerCase();

        if (lower.startsWith(playerLow + ": ")) return true;

        int dotIdx = lower.indexOf('.');
        if (dotIdx > 0 && lower.substring(dotIdx + 1).startsWith(playerLow + ": ")) return true;

        int closeIdx = lower.indexOf("] ");
        if (closeIdx > 0 && lower.substring(closeIdx + 2).startsWith(playerLow + ": ")) return true;

        return false;
    }

    private static boolean isFromClan(String original, String clan) {
        String lower   = original.toLowerCase();
        String clanLow = clan.toLowerCase();
        return lower.startsWith("[" + clanLow + "] ")
            || lower.startsWith(clanLow + ".");
    }


    private static final java.util.regex.Pattern WELCOME_PAT =
        java.util.regex.Pattern.compile("^Welcome .+!");
    private static final java.util.regex.Pattern VOTE_PAT =
        java.util.regex.Pattern.compile("^/vote -> \\S+:");
    private static final java.util.regex.Pattern SHARPEN_PAT =
        java.util.regex.Pattern.compile("\\bsharpened\\b.+\\bto \\+\\d+!");


    public static boolean shouldFilterBuiltin(String original) {
        var cfg = com.windfire.windchat.WindChatClient.config;



        String body = stripFormatting(stripTimestamp(original));
        if (cfg.hideWelcome && WELCOME_PAT.matcher(body).find())  return true;
        if (cfg.hideVotes   && VOTE_PAT.matcher(body).find())     return true;
        if (cfg.hideSharpen && SHARPEN_PAT.matcher(body).find())  return true;
        return false;
    }


    private static String stripTimestamp(String s) {
        return s.replaceFirst("^\\[\\d{1,2}:\\d{2}(?:\\s*[AaPp][Mm])?\\]\\s+", "");
    }


    private static String stripFormatting(String s) {
        return s.replaceAll("§.", "");
    }


    public static boolean mutePlayer(String name) {
        var list = WindChatClient.config.mutedPlayers;
        if (list.contains(name)) return false;
        list.add(name);
        WindChatClient.config.save();
        return true;
    }

    public static boolean unmutePlayer(String name) {
        boolean removed = WindChatClient.config.mutedPlayers.remove(name);
        if (removed) WindChatClient.config.save();
        return removed;
    }

    public static boolean muteClan(String tag) {
        var list = WindChatClient.config.mutedClans;
        if (list.contains(tag)) return false;
        list.add(tag);
        WindChatClient.config.save();
        return true;
    }

    public static boolean unmuteClan(String tag) {
        boolean removed = WindChatClient.config.mutedClans.remove(tag);
        if (removed) WindChatClient.config.save();
        return removed;
    }
}
