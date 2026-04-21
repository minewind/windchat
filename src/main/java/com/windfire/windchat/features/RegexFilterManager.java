package com.windfire.windchat.features;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.config.RegexFilterEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class RegexFilterManager {

    private record CompiledFilter(String id, Pattern pattern) {}
    private static List<CompiledFilter> cache = null;

    public static boolean shouldFilter(String raw) {
        ensureCompiled();
        for (CompiledFilter f : cache) {
            if (f.pattern().matcher(raw).find()) return true;
        }
        return false;
    }

    public static void invalidate() { cache = null; }

    public static String addFilter(String id, String regex) {
        try { Pattern.compile(regex); }
        catch (PatternSyntaxException e) { return "Invalid regex: " + e.getDescription(); }
        WindChatClient.config.regexFilters.add(new RegexFilterEntry(id, regex));
        WindChatClient.config.save();
        invalidate();
        return null;
    }

    public static boolean removeFilter(String id) {
        boolean removed = WindChatClient.config.regexFilters.removeIf(e -> e.id.equalsIgnoreCase(id));
        if (removed) { WindChatClient.config.save(); invalidate(); }
        return removed;
    }

    public static List<RegexFilterEntry> getFilters() {
        return WindChatClient.config.regexFilters;
    }

    private static void ensureCompiled() {
        if (cache != null) return;
        cache = new ArrayList<>();
        for (RegexFilterEntry e : WindChatClient.config.regexFilters) {
            try { cache.add(new CompiledFilter(e.id, Pattern.compile(e.pattern))); }
            catch (PatternSyntaxException ex) {
                System.err.println("[WindChat] Bad regex '" + e.id + "': " + ex.getMessage());
            }
        }
    }
}
