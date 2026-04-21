package com.windfire.windchat.config;

public class RegexFilterEntry {
    public String id;
    public String pattern;

    public RegexFilterEntry() {}

    public RegexFilterEntry(String id, String pattern) {
        this.id = id;
        this.pattern = pattern;
    }
}
