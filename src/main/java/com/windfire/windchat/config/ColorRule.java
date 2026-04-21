package com.windfire.windchat.config;


public class ColorRule {
    public String keyword;
    public String hexColor;
    public String scope;

    public ColorRule() {}

    public ColorRule(String keyword, String hexColor, String scope) {
        this.keyword  = keyword;
        this.hexColor = hexColor;
        this.scope    = scope;
    }
}
