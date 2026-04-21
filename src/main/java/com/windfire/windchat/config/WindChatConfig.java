package com.windfire.windchat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WindChatConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("windchat.json");


    public boolean notifyOnDM      = true;
    public boolean notifyOnMention = true;
    public boolean notifyOnEvent   = true;

    public boolean notifySound      = true;
    public List<String> triggerWords = new ArrayList<>(List.of(""));


    public List<String> mutedPlayers = new ArrayList<>();
    public List<String> mutedClans   = new ArrayList<>();
    public boolean filterHeartReplacement = true;

    public String timestampMode = "24H";

    public boolean tabsEnabled   = true;
    public boolean showGlobalTab = true;
    public boolean showClanTab   = true;
    public boolean showEventsTab = true;
    public boolean chatAlwaysVisible = false;

    public boolean filterClanFromMain   = true;
    public boolean filterDeathsFromMain = true;
    public boolean filterEventsFromMain = true;

    public boolean showDmsTab           = true;

    public boolean dmTabTimeoutEnabled   = false;

    public List<String> pinnedDmUsers    = new ArrayList<>();

    public String myClanTag = "";
    public boolean showDeathsTab    = true;
    public boolean showMentionsTab  = true;


    public boolean hideWelcome = false;

    public boolean hideVotes   = false;

    public boolean hideSharpen = false;

    public int chatWidthOffset = 0;
    public int chatHeightOffset = 0;

    public List<RegexFilterEntry> regexFilters = new ArrayList<>();

    public List<ColorRule> colorRules = new ArrayList<>();

    public boolean autoPostEnabled        = false;

    public int     autoPostIntervalMinutes = 30;
    public String  autoPostMessage         = "";

    public List<ChatMacro> macros = new ArrayList<>();

    public static WindChatConfig load() {
        if (CONFIG_PATH.toFile().exists()) {
            try (Reader r = new FileReader(CONFIG_PATH.toFile())) {
                WindChatConfig cfg = GSON.fromJson(r, WindChatConfig.class);
                if (cfg != null) return cfg;
            } catch (Exception e) {
                System.err.println("[WindChat] Failed to load config: " + e.getMessage());
            }
        }
        WindChatConfig defaults = new WindChatConfig();
        defaults.save();
        return defaults;
    }

    public void save() {
        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, w);
        } catch (Exception e) {
            System.err.println("[WindChat] Failed to save config: " + e.getMessage());
        }
    }
}
