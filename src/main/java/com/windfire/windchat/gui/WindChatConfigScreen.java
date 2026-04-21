package com.windfire.windchat.gui;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.api.WindChatAddon;
import com.windfire.windchat.api.WindChatAddonRegistry;
import com.windfire.windchat.config.ChatMacro;
import com.windfire.windchat.config.ColorRule;
import com.windfire.windchat.config.RegexFilterEntry;
import com.windfire.windchat.config.WindChatConfig;
import com.windfire.windchat.features.AutoPostManager;
import com.windfire.windchat.features.ChatMacroManager;
import com.windfire.windchat.features.RegexFilterManager;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WindChatConfigScreen {

    private static final String SEP = ":::";

    public static Screen create(Screen parent) {
        WindChatConfig cfg = WindChatClient.config;

        var yacl = YetAnotherConfigLib.createBuilder()
                .title(Text.literal("WindChat Settings"))
                .save(() -> {
                    cfg.save();
                    for (WindChatAddon addon : WindChatAddonRegistry.getInstance().getAddons()) {
                        try { addon.onConfigSaved(); }
                        catch (Exception e) {
                            System.err.println("[WindChat] Addon '" + addon.getAddonId()
                                    + "' threw in onConfigSaved: " + e.getMessage());
                        }
                    }
                });




        yacl.category(ConfigCategory.createBuilder()
                .name(Text.literal("Notifications"))
                .option(bool("Notify on DM", true,
                        "Play a ping when another player sends you a DM.",
                        () -> cfg.notifyOnDM, v -> cfg.notifyOnDM = v))
                .option(bool("Notify on Mention", true,
                        "Ping when any trigger word appears in the message body.",
                        () -> cfg.notifyOnMention, v -> cfg.notifyOnMention = v))
                .option(bool("Notify on Event", true,
                        "Ping when an event message arrives.",
                        () -> cfg.notifyOnEvent, v -> cfg.notifyOnEvent = v))
                .option(bool("Notification Sound", true,
                        "Play the XP-orb ping sound for notifications.",
                        () -> cfg.notifySound, v -> cfg.notifySound = v))
                .group(strList("Trigger Words",
                        "Words that trigger a mention ping when seen in chat. Case-insensitive.",
                        cfg.triggerWords,
                        list -> { cfg.triggerWords.clear(); cfg.triggerWords.addAll(list); }))
                .option(Option.<String>createBuilder()
                        .name(Text.literal("Timestamp Mode"))
                        .description(OptionDescription.of(
                                Text.literal("OFF — no timestamp"),
                                Text.literal("12H — [8:23 PM]"),
                                Text.literal("24H — [20:23]")))
                        .binding("24H", () -> cfg.timestampMode, v -> cfg.timestampMode = v)
                        .controller(opt -> CyclingListControllerBuilder.create(opt)
                                .values(List.of("OFF", "12H", "24H"))
                                .valueFormatter(s -> Text.literal(switch (s) {
                                    case "OFF" -> "Off";
                                    case "12H" -> "12-Hour (AM/PM)";
                                    default    -> "24-Hour";
                                })))
                        .build())
                .option(bool("Persistent Chat", false,
                        "Always show the chat box and messages — disables the auto-fade after messages. Great for keeping chat visible after teleports.",
                        () -> cfg.chatAlwaysVisible, v -> cfg.chatAlwaysVisible = v))
                .option(Option.<Integer>createBuilder()
                        .name(Text.literal("Chat Width"))
                        .description(OptionDescription.of(
                                Text.literal("Expand or shrink the chat box width."),
                                Text.literal("0 = vanilla default  |  positive = wider  |  negative = narrower"),
                                Text.literal("Minimum is auto-enforced to fit the tab bar.")))
                        .binding(0, () -> cfg.chatWidthOffset, v -> cfg.chatWidthOffset = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(-100, 400).step(10)
                                .valueFormatter(v -> Text.literal(v > 0 ? "+" + v + "px" : v + "px")))
                        .build())
                .option(Option.<Integer>createBuilder()
                        .name(Text.literal("Chat Height"))
                        .description(OptionDescription.of(
                                Text.literal("Expand or shrink the chat box height."),
                                Text.literal("0 = vanilla default  |  positive = taller  |  negative = shorter")))
                        .binding(0, () -> cfg.chatHeightOffset, v -> cfg.chatHeightOffset = v)
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                .range(-100, 400).step(10)
                                .valueFormatter(v -> Text.literal(v > 0 ? "+" + v + "px" : v + "px")))
                        .build())
                .build());

        yacl.category(ConfigCategory.createBuilder()
                .name(Text.literal("Tabs"))
                .group(OptionGroup.createBuilder()
                        .name(Text.literal("Visibility"))
                        .option(bool("Enable Tabs", true, null,
                                () -> cfg.tabsEnabled, v -> cfg.tabsEnabled = v))
                        .option(bool("Show Global Tab", true, null,
                                () -> cfg.showGlobalTab, v -> cfg.showGlobalTab = v))
                        .option(bool("Show Clan Tab", true, null,
                                () -> cfg.showClanTab, v -> cfg.showClanTab = v))
                        .option(bool("Show Events Tab", true, null,
                                () -> cfg.showEventsTab, v -> cfg.showEventsTab = v))
                        .option(bool("Show Deaths Tab", true, null,
                                () -> cfg.showDeathsTab, v -> cfg.showDeathsTab = v))
                        .option(bool("Show Mentions Tab", true, null,
                                () -> cfg.showMentionsTab, v -> cfg.showMentionsTab = v))
                        .option(bool("Show DMs Tab", true, null,
                                () -> cfg.showDmsTab, v -> cfg.showDmsTab = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Text.literal("Clan"))
                        .option(str("My Clan Tag", "",
                                "Your clan tag prefix (e.g. TSC). Leave blank for all clans.",
                                () -> cfg.myClanTag, v -> cfg.myClanTag = v.strip()))
                        .option(bool("Filter Clan from Main", true,
                                "Keep clan messages out of Main chat, even when Clan tab is disabled.",
                                () -> cfg.filterClanFromMain, v -> cfg.filterClanFromMain = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Text.literal("Main Filters"))
                        .option(bool("Filter Deaths from Main", true,
                                "Keep death messages out of Main, even when Deaths tab is disabled.",
                                () -> cfg.filterDeathsFromMain, v -> cfg.filterDeathsFromMain = v))
                        .option(bool("Filter Events from Main", true,
                                "Keep event messages out of Main, even when Events tab is disabled.",
                                () -> cfg.filterEventsFromMain, v -> cfg.filterEventsFromMain = v))
                        .build())
                .group(OptionGroup.createBuilder()
                        .name(Text.literal("DMs"))
                        .option(bool("DM Tab Auto-Timeout", false,
                                "Auto-close DM tabs after 2.5 minutes of inactivity.",
                                () -> cfg.dmTabTimeoutEnabled, v -> cfg.dmTabTimeoutEnabled = v))
                        .build())

                .group(strList("Pinned DM Users",
                        "These DM tabs never auto-close.",
                        cfg.pinnedDmUsers,
                        list -> { cfg.pinnedDmUsers.clear(); cfg.pinnedDmUsers.addAll(list); }))
                .build());

        yacl.category(ConfigCategory.createBuilder()
                .name(Text.literal("Muting"))
                .option(bool("Filter Heart Replacement", true,
                        "Hide server-side muted player messages (shown as ❤❤❤: ❤❤❤❤❤).",
                        () -> cfg.filterHeartReplacement, v -> cfg.filterHeartReplacement = v))
                .option(bool("Hide Welcome Messages", false, null,
                        () -> cfg.hideWelcome, v -> cfg.hideWelcome = v))
                .option(bool("Hide Vote Notifications", false, null,
                        () -> cfg.hideVotes, v -> cfg.hideVotes = v))
                .option(bool("Hide Sharpen Announcements", false, null,
                        () -> cfg.hideSharpen, v -> cfg.hideSharpen = v))
                .group(strList("Muted Players", "One username per entry.",
                        cfg.mutedPlayers,
                        list -> { cfg.mutedPlayers.clear(); cfg.mutedPlayers.addAll(list); }))
                .group(strList("Muted Clans", "One clan tag per entry.",
                        cfg.mutedClans,
                        list -> { cfg.mutedClans.clear(); cfg.mutedClans.addAll(list); }))
                .build());

        List<String> colorStrings = cfg.colorRules.stream()
                .map(r -> r.keyword + SEP + r.hexColor + SEP + r.scope)
                .collect(Collectors.toList());

        yacl.category(ConfigCategory.createBuilder()
                .name(Text.literal("Colors"))
                .group(strList("Color Rules",
                        "Format: keyword:::hexcolor:::LINE|WORD|CLAN  —  e.g. dove:::ff44ff:::WORD",
                        colorStrings,
                        list -> {
                            cfg.colorRules.clear();
                            for (String s : list) {
                                String[] p = s.split(java.util.regex.Pattern.quote(SEP), 3);
                                if (p.length == 3)
                                    cfg.colorRules.add(new ColorRule(
                                            p[0].trim(), p[1].trim().replace("#",""),
                                            p[2].trim().toUpperCase()));
                            }
                        }))
                .build());

        List<String> filterStrings = cfg.regexFilters.stream()
                .map(e -> e.id + SEP + e.pattern)
                .collect(Collectors.toList());

        yacl.category(ConfigCategory.createBuilder()
                .name(Text.literal("Filters"))
                .group(strList("Regex Filters",
                        "Format: id:::regex  —  e.g. no-vote:::/vote ->  Matching messages are hidden.",
                        filterStrings,
                        list -> {
                            cfg.regexFilters.clear();
                            for (String s : list) {
                                String[] p = s.split(java.util.regex.Pattern.quote(SEP), 2);
                                if (p.length == 2 && !p[0].isBlank() && !p[1].isBlank()) {
                                    String err = RegexFilterManager.addFilter(p[0].trim(), p[1].trim());
                                    if (err == null)
                                        cfg.regexFilters.add(new RegexFilterEntry(p[0].trim(), p[1].trim()));
                                }
                            }
                        }))
                .build());

        List<String> macroStrings = cfg.macros.stream()
                .map(m -> ChatMacroManager.codeToDisplayName(m.keyCode) + SEP + m.command)
                .collect(Collectors.toList());

        yacl.category(ConfigCategory.createBuilder()
                .name(Text.literal("Macros"))
                .group(strList("Macros",
                        "Format: KEYNAME:::command  —  e.g. NUMPAD7:::/home vault  |  F5:::/spawn",
                        macroStrings,
                        list -> {
                            cfg.macros.clear();
                            for (String s : list) {
                                String[] p = s.split(java.util.regex.Pattern.quote(SEP), 2);
                                if (p.length == 2 && !p[0].isBlank() && !p[1].isBlank()) {
                                    int code = ChatMacroManager.parseKeyArg(p[0].trim());
                                    if (code > 0)
                                        cfg.macros.add(new ChatMacro(code, p[1].trim(),
                                                ChatMacroManager.codeToDisplayName(code)));
                                }
                            }
                        }))
                .build());

        yacl.category(ConfigCategory.createBuilder()
                .name(Text.literal("Auto-Post"))
                .option(bool("Enabled", false,
                        "Automatically post at the set interval. Set message via /wpost set 30m <message>.",
                        () -> cfg.autoPostEnabled,
                        v -> { cfg.autoPostEnabled = v; AutoPostManager.reset(); }))
                .option(Option.<Integer>createBuilder()
                        .name(Text.literal("Interval (minutes)"))
                        .description(OptionDescription.of(Text.literal("How often to post the message.")))
                        .binding(30, () -> cfg.autoPostIntervalMinutes,
                                v -> { cfg.autoPostIntervalMinutes = v; AutoPostManager.reset(); })
                        .controller(opt -> IntegerSliderControllerBuilder.create(opt).range(1, 60).step(1))
                        .build())
                .option(str("Message", "",
                        "The message posted automatically. Clear and save to remove.",
                        () -> cfg.autoPostMessage != null ? cfg.autoPostMessage : "",
                        v -> {
                            cfg.autoPostMessage = v.strip();
                            if (cfg.autoPostMessage.isBlank()) cfg.autoPostEnabled = false;
                            AutoPostManager.reset();
                        }))
                .build());

        for (WindChatAddon addon : WindChatAddonRegistry.getInstance().getAddons()) {
            try {
                addon.buildConfigCategory().ifPresent(yacl::category);
            } catch (Exception e) {
                System.err.println("[WindChat] Addon '" + addon.getAddonId()
                        + "' threw while building config category: " + e.getMessage());
            }
        }

        return yacl.build().generateScreen(parent);
    }


    private static Option<Boolean> bool(String name, boolean def, String tooltip,
                                        Supplier<Boolean> getter, Consumer<Boolean> setter) {
        var b = Option.<Boolean>createBuilder()
                .name(Text.literal(name))
                .binding(def, getter, setter)
                .controller(TickBoxControllerBuilder::create);
        if (tooltip != null) b.description(OptionDescription.of(Text.literal(tooltip)));
        return b.build();
    }

    private static Option<String> str(String name, String def, String tooltip,
                                      Supplier<String> getter, Consumer<String> setter) {
        var b = Option.<String>createBuilder()
                .name(Text.literal(name))
                .binding(def, getter, setter)
                .controller(StringControllerBuilder::create);
        if (tooltip != null) b.description(OptionDescription.of(Text.literal(tooltip)));
        return b.build();
    }


    private static ListOption<String> strList(String name, String tooltip,
                                              List<String> current,
                                              Consumer<List<String>> setter) {
        var b = ListOption.<String>createBuilder()
                .name(Text.literal(name))
                .binding(List.of(), () -> new ArrayList<>(current), setter)
                .controller(StringControllerBuilder::create)
                .initial("");
        if (tooltip != null) b.description(OptionDescription.of(Text.literal(tooltip)));
        return b.build();
    }
}