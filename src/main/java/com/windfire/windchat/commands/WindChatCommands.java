package com.windfire.windchat.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.config.ChatMacro;
import com.windfire.windchat.config.ColorRule;
import com.windfire.windchat.config.RegexFilterEntry;
import com.windfire.windchat.features.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;

import java.util.List;

public class WindChatCommands {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {


        dispatcher.register(ClientCommandManager.literal("wmute")
            .then(ClientCommandManager.literal("clan")
                .then(ClientCommandManager.argument("tag", StringArgumentType.word())
                    .executes(ctx -> {
                        String tag = StringArgumentType.getString(ctx, "tag");
                        boolean ok = MuteManager.muteClan(tag);
                        msg(ctx.getSource(), ok ? "§aMuted clan: §f" + tag : "§eAlready muted: §f" + tag);
                        return 1;
                    })))
            .then(ClientCommandManager.argument("player", StringArgumentType.word())
                .executes(ctx -> {
                    String player = dashes(StringArgumentType.getString(ctx, "player"));
                    boolean ok = MuteManager.mutePlayer(player);
                    msg(ctx.getSource(), ok ? "§aMuted player: §f" + player : "§eAlready muted: §f" + player);
                    return 1;
                })));

        dispatcher.register(ClientCommandManager.literal("wunmute")
            .then(ClientCommandManager.literal("clan")
                .then(ClientCommandManager.argument("tag", StringArgumentType.word())
                    .executes(ctx -> {
                        String tag = StringArgumentType.getString(ctx, "tag");
                        boolean ok = MuteManager.unmuteClan(tag);
                        msg(ctx.getSource(), ok ? "§aUnmuted clan: §f" + tag : "§eNot muted: §f" + tag);
                        return 1;
                    })))
            .then(ClientCommandManager.argument("player", StringArgumentType.word())
                .executes(ctx -> {
                    String player = dashes(StringArgumentType.getString(ctx, "player"));
                    boolean ok = MuteManager.unmutePlayer(player);
                    msg(ctx.getSource(), ok ? "§aUnmuted player: §f" + player : "§eNot muted: §f" + player);
                    return 1;
                })));

        dispatcher.register(ClientCommandManager.literal("wfilter")
            .then(ClientCommandManager.literal("add")
                .then(ClientCommandManager.argument("id", StringArgumentType.word())
                    .then(ClientCommandManager.argument("regex", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String id    = dashes(StringArgumentType.getString(ctx, "id"));
                            String regex = StringArgumentType.getString(ctx, "regex");
                            String err   = RegexFilterManager.addFilter(id, regex);
                            msg(ctx.getSource(), err != null ? "§c" + err : "§aFilter added: §f" + id + " §7(" + regex + ")");
                            return 1;
                        }))))
            .then(ClientCommandManager.literal("remove")
                .then(ClientCommandManager.argument("id", StringArgumentType.word())
                    .executes(ctx -> {
                        String id = StringArgumentType.getString(ctx, "id");
                        boolean ok = RegexFilterManager.removeFilter(id);
                        msg(ctx.getSource(), ok ? "§aRemoved filter: §f" + id : "§eFilter not found: §f" + id);
                        return 1;
                    })))
            .then(ClientCommandManager.literal("list")
                .executes(ctx -> {
                    List<RegexFilterEntry> filters = RegexFilterManager.getFilters();
                    if (filters.isEmpty()) { msg(ctx.getSource(), "§7No active filters."); }
                    else {
                        msg(ctx.getSource(), "§6Active filters:");
                        filters.forEach(e -> msg(ctx.getSource(), "  §e" + e.id + " §7→ §f" + e.pattern));
                    }
                    return 1;
                })));

        dispatcher.register(ClientCommandManager.literal("wcolor")
            .then(ClientCommandManager.literal("add")
                .then(ClientCommandManager.argument("keyword", StringArgumentType.word())
                    .then(ClientCommandManager.argument("hex", StringArgumentType.word())
                        .then(ClientCommandManager.argument("scope", StringArgumentType.word())
                            .executes(ctx -> {
                                String keyword = dashes(StringArgumentType.getString(ctx, "keyword"));
                                String hex     = StringArgumentType.getString(ctx, "hex");
                                String scope   = StringArgumentType.getString(ctx, "scope").toUpperCase();
                                if (!scope.equals("LINE") && !scope.equals("WORD") && !scope.equals("CLAN")) {
                                    msg(ctx.getSource(), "§cScope must be LINE, WORD, or CLAN.");
                                    return 0;
                                }
                                ChatColorManager.addRule(keyword, hex.replace("#", ""), scope);
                                msg(ctx.getSource(), "§aColor rule added: §f" + keyword + " §7→ #" + hex + " (" + scope + ")"  + (scope.equals("CLAN") ? " §7[main chat only]" : ""));
                                return 1;
                            })))))
            .then(ClientCommandManager.literal("remove")
                .then(ClientCommandManager.argument("keyword", StringArgumentType.word())
                    .executes(ctx -> {
                        String keyword = dashes(StringArgumentType.getString(ctx, "keyword"));
                        boolean ok = ChatColorManager.removeRule(keyword);
                        msg(ctx.getSource(), ok ? "§aRemoved color rule: §f" + keyword : "§eRule not found: §f" + keyword);
                        return 1;
                    })))
            .then(ClientCommandManager.literal("list")
                .executes(ctx -> {
                    List<ColorRule> rules = WindChatClient.config.colorRules;
                    if (rules.isEmpty()) { msg(ctx.getSource(), "§7No color rules."); }
                    else {
                        msg(ctx.getSource(), "§6Color rules:");
                        rules.forEach(r -> {
                            int rgb = parseHexColor(r.hexColor);
                            String colored = net.minecraft.text.Text.literal("#" + r.hexColor)
                                    .styled(s -> s.withColor(rgb)).getString();

                            ctx.getSource().sendFeedback(
                                net.minecraft.text.Text.empty()
                                    .append(net.minecraft.text.Text.literal("  §8[§6WindChat§8] §r  §e" + r.keyword + " §7→ "))
                                    .append(net.minecraft.text.Text.literal("#" + r.hexColor)
                                        .styled(s -> s.withColor(rgb)))
                                    .append(net.minecraft.text.Text.literal(" §7(" + r.scope + ")"))
                            );
                        });
                    }
                    return 1;
                })));


        dispatcher.register(ClientCommandManager.literal("wmacro")
            .then(ClientCommandManager.literal("set")
                .then(ClientCommandManager.argument("key", StringArgumentType.word())
                    .then(ClientCommandManager.argument("command", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String keyArg  = StringArgumentType.getString(ctx, "key");
                            String command = StringArgumentType.getString(ctx, "command");
                            int keyCode    = ChatMacroManager.parseKeyArg(keyArg);
                            if (keyCode == 0) {
                                msg(ctx.getSource(), "§cUnknown key: §f" + keyArg
                                    + "  §7Try a name like NUMPAD7, F1, or A — or a raw GLFW code.");
                                return 0;
                            }
                            ChatMacroManager.setMacro(keyCode, command);
                            msg(ctx.getSource(), "§aMacro set: §f"
                                + ChatMacroManager.codeToDisplayName(keyCode) + " §7→ §f" + command);
                            return 1;
                        }))))
            .then(ClientCommandManager.literal("clear")
                .then(ClientCommandManager.argument("key", StringArgumentType.word())
                    .executes(ctx -> {
                        String keyArg = StringArgumentType.getString(ctx, "key");
                        int keyCode   = ChatMacroManager.parseKeyArg(keyArg);
                        if (keyCode == 0) {
                            msg(ctx.getSource(), "§cUnknown key: §f" + keyArg);
                            return 0;
                        }
                        boolean ok = ChatMacroManager.removeMacro(keyCode);
                        msg(ctx.getSource(), ok
                            ? "§aCleared macro for §f" + ChatMacroManager.codeToDisplayName(keyCode)
                            : "§eNo macro bound to §f" + ChatMacroManager.codeToDisplayName(keyCode));
                        return 1;
                    })))
            .then(ClientCommandManager.literal("list")
                .executes(ctx -> {
                    List<ChatMacro> macros = WindChatClient.config.macros;
                    if (macros.isEmpty()) { msg(ctx.getSource(), "§7No macros set."); }
                    else {
                        msg(ctx.getSource(), "§6Macros:");
                        macros.forEach(m -> msg(ctx.getSource(),
                            "  §e" + ChatMacroManager.codeToDisplayName(m.keyCode) + " §7→ §f" + m.command));
                    }
                    return 1;
                })));

        dispatcher.register(ClientCommandManager.literal("wtimestamp")
            .then(ClientCommandManager.literal("off").executes(ctx -> {
                WindChatClient.config.timestampMode = "OFF";
                WindChatClient.config.save();
                msg(ctx.getSource(), "§7Timestamps disabled.");
                return 1;
            }))
            .then(ClientCommandManager.literal("12h").executes(ctx -> {
                WindChatClient.config.timestampMode = "12H";
                WindChatClient.config.save();
                msg(ctx.getSource(), "§aTimestamps set to 12-hour (AM/PM).");
                return 1;
            }))
            .then(ClientCommandManager.literal("24h").executes(ctx -> {
                WindChatClient.config.timestampMode = "24H";
                WindChatClient.config.save();
                msg(ctx.getSource(), "§aTimestamps set to 24-hour (military).");
                return 1;
            })));

        dispatcher.register(ClientCommandManager.literal("wnotify")
            .then(ClientCommandManager.literal("dm")
                .then(onOff(ctx -> {
                    boolean on = ctx.getArgument("toggle", String.class).equalsIgnoreCase("on");
                    WindChatClient.config.notifyOnDM = on;
                    WindChatClient.config.save();
                    msg(ctx.getSource(), "§" + (on ? "a" : "7") + "DM notifications " + (on ? "enabled" : "disabled") + ".");
                    return 1;
                })))
            .then(ClientCommandManager.literal("mention")
                .then(onOff(ctx -> {
                    boolean on = ctx.getArgument("toggle", String.class).equalsIgnoreCase("on");
                    WindChatClient.config.notifyOnMention = on;
                    WindChatClient.config.save();
                    msg(ctx.getSource(), "§" + (on ? "a" : "7") + "Mention notifications " + (on ? "enabled" : "disabled") + ".");
                    return 1;
                })))
            .then(ClientCommandManager.literal("event")
                .then(onOff(ctx -> {
                    boolean on = ctx.getArgument("toggle", String.class).equalsIgnoreCase("on");
                    WindChatClient.config.notifyOnEvent = on;
                    WindChatClient.config.save();
                    msg(ctx.getSource(), "§" + (on ? "a" : "7") + "Event notifications " + (on ? "enabled" : "disabled") + ".");
                    return 1;
                })))
            .then(ClientCommandManager.literal("sound")
                .then(onOff(ctx -> {
                    boolean on = ctx.getArgument("toggle", String.class).equalsIgnoreCase("on");
                    WindChatClient.config.notifySound = on;
                    WindChatClient.config.save();
                    msg(ctx.getSource(), "§" + (on ? "a" : "7") + "Notification sound " + (on ? "enabled" : "disabled") + ".");
                    return 1;
                }))));

        dispatcher.register(ClientCommandManager.literal("wtrigger")
            .then(ClientCommandManager.literal("add")
                .then(ClientCommandManager.argument("word", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String word = StringArgumentType.getString(ctx, "word");
                        if (!WindChatClient.config.triggerWords.contains(word)) {
                            WindChatClient.config.triggerWords.add(word);
                            WindChatClient.config.save();
                        }
                        msg(ctx.getSource(), "§aTrigger added: §f" + word);
                        return 1;
                    })))
            .then(ClientCommandManager.literal("remove")
                .then(ClientCommandManager.argument("word", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String word = StringArgumentType.getString(ctx, "word");
                        boolean ok  = WindChatClient.config.triggerWords.remove(word);
                        if (ok) WindChatClient.config.save();
                        msg(ctx.getSource(), ok ? "§aRemoved trigger: §f" + word : "§eTrigger not found: §f" + word);
                        return 1;
                    })))
            .then(ClientCommandManager.literal("list")
                .executes(ctx -> {
                    var words = WindChatClient.config.triggerWords;
                    if (words.isEmpty() || words.stream().allMatch(String::isBlank))
                        msg(ctx.getSource(), "§7No trigger words set.");
                    else
                        msg(ctx.getSource(), "§6Trigger words: §f" + String.join("§7, §f", words));
                    return 1;
                })));




        dispatcher.register(ClientCommandManager.literal("wtab")
            .then(ClientCommandManager.literal("main")
                .executes(ctx -> { ChatTabManager.switchTab(ChatTab.MAIN); return 1; }))
            .then(ClientCommandManager.literal("global")
                .executes(ctx -> { ChatTabManager.switchTab(ChatTab.GLOBAL); return 1; })
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    WindChatClient.config.showGlobalTab = true; WindChatClient.config.save();
                    msg(ctx.getSource(), "§aGlobal tab enabled."); return 1; }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    WindChatClient.config.showGlobalTab = false; WindChatClient.config.save();
                    msg(ctx.getSource(), "§7Global tab hidden."); return 1; })))
            .then(ClientCommandManager.literal("clan")
                .executes(ctx -> { ChatTabManager.switchTab(ChatTab.CLAN); return 1; })
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    WindChatClient.config.showClanTab = true; WindChatClient.config.save();
                    msg(ctx.getSource(), "§aClan tab enabled."); return 1; }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    WindChatClient.config.showClanTab = false; WindChatClient.config.save();
                    msg(ctx.getSource(), "§7Clan tab hidden."); return 1; })))
            .then(ClientCommandManager.literal("events")
                .executes(ctx -> { ChatTabManager.switchTab(ChatTab.EVENTS); return 1; })
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    WindChatClient.config.showEventsTab = true; WindChatClient.config.save();
                    msg(ctx.getSource(), "§aEvents tab enabled."); return 1; }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    WindChatClient.config.showEventsTab = false; WindChatClient.config.save();
                    msg(ctx.getSource(), "§7Events tab hidden."); return 1; })))
            .then(ClientCommandManager.literal("deaths")
                .executes(ctx -> { ChatTabManager.switchTab(ChatTab.DEATHS); return 1; })
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    WindChatClient.config.showDeathsTab = true; WindChatClient.config.save();
                    msg(ctx.getSource(), "§aDeaths tab enabled."); return 1; }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    WindChatClient.config.showDeathsTab = false; WindChatClient.config.save();
                    msg(ctx.getSource(), "§7Deaths tab hidden."); return 1; })))
            .then(ClientCommandManager.literal("mentions")
                .executes(ctx -> { ChatTabManager.switchTab(ChatTab.MENTIONS); return 1; })
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    WindChatClient.config.showMentionsTab = true; WindChatClient.config.save();
                    msg(ctx.getSource(), "§aMentions tab enabled."); return 1; }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    WindChatClient.config.showMentionsTab = false; WindChatClient.config.save();
                    msg(ctx.getSource(), "§7Mentions tab hidden."); return 1; })))
            .then(ClientCommandManager.literal("dms")
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    WindChatClient.config.showDmsTab = true; WindChatClient.config.save();
                    msg(ctx.getSource(), "§aDM tabs enabled."); return 1; }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    WindChatClient.config.showDmsTab = false; WindChatClient.config.save();
                    msg(ctx.getSource(), "§7DM tabs hidden."); return 1; })))

            .then(ClientCommandManager.literal("pin")
                .then(ClientCommandManager.argument("username", StringArgumentType.word())
                    .suggests(onlinePlayers())
                    .executes(ctx -> {
                        String user = StringArgumentType.getString(ctx, "username");
                        boolean ok  = DmTabManager.pin(user);
                        msg(ctx.getSource(), ok
                            ? "§aPinned DM tab: §f" + user + " §7(will never auto-close)"
                            : "§e" + user + " §7is already pinned.");
                        return 1;
                    })))
            .then(ClientCommandManager.literal("unpin")
                .then(ClientCommandManager.argument("username", StringArgumentType.word())
                    .suggests((ctx, b) -> {

                        WindChatClient.config.pinnedDmUsers.stream()
                            .filter(n -> n.toLowerCase().startsWith(b.getRemainingLowerCase()))
                            .forEach(b::suggest);
                        return b.buildFuture();
                    })
                    .executes(ctx -> {
                        String user = StringArgumentType.getString(ctx, "username");
                        boolean ok  = DmTabManager.unpin(user);
                        msg(ctx.getSource(), ok
                            ? "§aUnpinned: §f" + user
                            : "§e" + user + " §7is not pinned.");
                        return 1;
                    })))

            .then(ClientCommandManager.argument("username", StringArgumentType.word())
                .suggests((ctx, b) -> {
                    net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                    java.util.Set<String> seen = new java.util.LinkedHashSet<>();

                    com.windfire.windchat.features.DmTabManager.getKnownPartners().stream()
                        .filter(n -> n.toLowerCase().startsWith(b.getRemainingLowerCase()))
                        .forEach(seen::add);

                    if (mc != null && mc.getNetworkHandler() != null) {
                        mc.getNetworkHandler().getPlayerList().stream()
                            .map(e -> e.getProfile().getName())
                            .filter(n -> n.toLowerCase().startsWith(b.getRemainingLowerCase()))
                            .forEach(seen::add);
                    }
                    seen.forEach(b::suggest);
                    return b.buildFuture();
                })
                .executes(ctx -> {
                    String user = StringArgumentType.getString(ctx, "username");

                    if (isStaticTabName(user)) {
                        msg(ctx.getSource(), "§c" + user + " is a reserved tab name.");
                        return 0;
                    }
                    DmTabManager.ToggleResult result = DmTabManager.toggleTab(user);
                    switch (result) {
                        case OPENED    -> msg(ctx.getSource(), "§aOpened DM tab: §f" + user);
                        case CLOSED    -> msg(ctx.getSource(), "§7Closed DM tab: §f" + user);
                        case NO_HISTORY -> msg(ctx.getSource(),
                            "§cNo DM history with §f" + user + "§c this session. Send them a message first.");
                        case PINNED    -> msg(ctx.getSource(),
                            "§e" + user + " §7is pinned — use §f/wtab unpin " + user + " §7to close.");
                    }
                    return 1;
                })));

        dispatcher.register(ClientCommandManager.literal("wpost")
            .then(ClientCommandManager.literal("set")
                .then(ClientCommandManager.argument("interval", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        for (String s : new String[]{"1m","5m","10m","15m","30m","1h"}) builder.suggest(s);
                        return builder.buildFuture();
                    })
                    .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String intervalStr = StringArgumentType.getString(ctx, "interval");
                            String message     = stripQuotes(StringArgumentType.getString(ctx, "message"));
                            int minutes = parseIntervalMinutes(intervalStr);
                            if (minutes < 1 || minutes > 60) {
                                msg(ctx.getSource(), "§cInterval must be 1m-60m or 1h. Examples: 5m, 30m, 1h");
                                return 0;
                            }
                            WindChatClient.config.autoPostIntervalMinutes = minutes;
                            WindChatClient.config.autoPostMessage         = message;
                            WindChatClient.config.save();
                            com.windfire.windchat.features.AutoPostManager.reset();
                            msg(ctx.getSource(), "§aAuto-post message set!");
                            msg(ctx.getSource(), "§7Every §f" + intervalLabel(minutes) + "§7: §f" + message);
                            return 1;
                        }))))
            .then(ClientCommandManager.literal("on").executes(ctx -> {
                if (WindChatClient.config.autoPostMessage == null
                        || WindChatClient.config.autoPostMessage.isBlank()) {
                    msg(ctx.getSource(), "§cNo message set. Use /wpost set <interval> <message> first.");
                    return 0;
                }
                WindChatClient.config.autoPostEnabled = true;
                WindChatClient.config.save();
                com.windfire.windchat.features.AutoPostManager.reset();
                int min = WindChatClient.config.autoPostIntervalMinutes;
                msg(ctx.getSource(), "§aAuto-posting every " + intervalLabel(min) + ".");
                return 1;
            }))
            .then(ClientCommandManager.literal("off").executes(ctx -> {
                WindChatClient.config.autoPostEnabled = false;
                WindChatClient.config.save();
                msg(ctx.getSource(), "§7Auto-post disabled.");
                return 1;
            }))
            .then(ClientCommandManager.literal("status").executes(ctx -> {
                var cfg = WindChatClient.config;
                if (!cfg.autoPostEnabled) {
                    msg(ctx.getSource(), "§7Auto-post is §coff§7.");
                } else {
                    msg(ctx.getSource(), "§aAuto-post §aon §7— every §f"
                            + intervalLabel(cfg.autoPostIntervalMinutes) + "§7: §f" + cfg.autoPostMessage);
                }
                return 1;
            }))
            .then(ClientCommandManager.literal("remove").executes(ctx -> {
                WindChatClient.config.autoPostEnabled = false;
                WindChatClient.config.autoPostMessage = "";
                WindChatClient.config.save();
                com.windfire.windchat.features.AutoPostManager.reset();
                msg(ctx.getSource(), "§aAuto-post message cleared.");
                return 1;
            })));

        dispatcher.register(ClientCommandManager.literal("wconfig")
            .executes(ctx -> {
                net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
                mc.send(() -> mc.setScreen(
                    com.windfire.windchat.gui.WindChatConfigScreen.create(null)));
                return 1;
            }));
    }








    private static com.mojang.brigadier.suggestion.SuggestionProvider<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>
    onlinePlayers() {
        return (ctx, builder) -> {
            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
            if (mc != null && mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().getPlayerList().stream()
                    .map(e -> e.getProfile().getName())
                    .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                    .forEach(builder::suggest);
            }
            return builder.buildFuture();
        };
    }

    private static int parseHexColor(String hex) {
        try { return (int) Long.parseLong(hex.replace("#", ""), 16); }
        catch (NumberFormatException e) { return 0xFFFFFF; }
    }

    private static boolean isStaticTabName(String name) {
        return switch (name.toLowerCase()) {
            case "main","global","clan","events","deaths","mentions","dms","pin","unpin","on","off","sound" -> true;
            default -> false;
        };
    }

    private static int parseIntervalMinutes(String token) {
        if (token == null || token.isBlank()) return -1;
        token = token.trim().toLowerCase();
        try {
            if (token.endsWith("h")) return (int)(Float.parseFloat(token.replace("h","")) * 60);
            if (token.endsWith("m")) return (int) Float.parseFloat(token.replace("m",""));
            return Integer.parseInt(token); // bare number = minutes
        } catch (NumberFormatException e) { return -1; }
    }

    private static String intervalLabel(int minutes) {
        if (minutes >= 60 && minutes % 60 == 0) return (minutes / 60) + " hour" + (minutes / 60 == 1 ? "" : "s");
        return minutes + " minute" + (minutes == 1 ? "" : "s");
    }

    private static void msg(FabricClientCommandSource src, String text) {
        src.sendFeedback(Text.literal("§8[§bWindChat§8] §r" + text));
    }

    private static com.mojang.brigadier.builder.ArgumentBuilder<
            FabricClientCommandSource,
            com.mojang.brigadier.builder.RequiredArgumentBuilder<FabricClientCommandSource, String>>
    onOff(com.mojang.brigadier.Command<FabricClientCommandSource> cmd) {
        return ClientCommandManager.argument("toggle", StringArgumentType.word())
                .suggests((ctx, builder) -> { builder.suggest("on"); builder.suggest("off"); return builder.buildFuture(); })
                .executes(cmd);
    }



    private static String stripQuotes(String s) {
        if (s == null) return "";
        s = s.strip();
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            s = s.substring(1, s.length() - 1);  // remove outer quotes
        }
        return s;
    }

    private static String dashes(String s) {
        return s.replace('-', ' ');
    }
}
