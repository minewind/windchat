package com.windfire.windchat.config;

public class ChatMacro {
    public int keyCode;
    public String command;
    public String label;

    public ChatMacro() {}

    public ChatMacro(int keyCode, String command, String label) {
        this.keyCode = keyCode;
        this.command = command;
        this.label   = label;
    }
}
