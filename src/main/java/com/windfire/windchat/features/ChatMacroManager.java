package com.windfire.windchat.features;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.config.ChatMacro;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.lwjgl.glfw.GLFW;

import java.util.*;


public class ChatMacroManager {



    private static final Map<Integer, String> CODE_TO_DISPLAY = new HashMap<>();

    private static final Map<String, Integer> INPUT_TO_CODE   = new HashMap<>();

    static {

        for (int i = 65; i <= 90; i++) {
            String ch = String.valueOf((char) i);   // "A".."Z"
            CODE_TO_DISPLAY.put(i, ch);
            INPUT_TO_CODE.put(ch, i);
        }

        for (int i = 48; i <= 57; i++) {
            String ch = String.valueOf((char) i);   // "0".."9"
            CODE_TO_DISPLAY.put(i, ch);
            INPUT_TO_CODE.put(ch, i);
        }

        for (int i = 0; i <= 9; i++) {
            int code = 320 + i;
            String display = "Numpad " + i;
            CODE_TO_DISPLAY.put(code, display);
            INPUT_TO_CODE.put("NUMPAD" + i,        code);  // NUMPAD7
            INPUT_TO_CODE.put("KP" + i,            code);  // KP7
            INPUT_TO_CODE.put("NUMPAD_" + i,       code);  // NUMPAD_7
        }

        addKey(330, "Numpad .",      "NUMPAD.", "NUMPAD_DECIMAL", "KP_DECIMAL");
        addKey(331, "Numpad /",      "NUMPAD/", "NUMPAD_DIVIDE",  "KP_DIVIDE");
        addKey(332, "Numpad *",      "NUMPAD*", "NUMPAD_MULTIPLY","KP_MULTIPLY");
        addKey(333, "Numpad -",      "NUMPAD-", "NUMPAD_SUBTRACT","KP_SUBTRACT");
        addKey(334, "Numpad +",      "NUMPAD+", "NUMPAD_ADD",     "KP_ADD");
        addKey(335, "Numpad Enter",  "NUMPAD_ENTER", "KP_ENTER");

        for (int i = 1; i <= 25; i++) {
            int code = 289 + i;
            CODE_TO_DISPLAY.put(code, "F" + i);
            INPUT_TO_CODE.put("F" + i, code);
        }

        addKey(256, "Escape",       "ESCAPE", "ESC");
        addKey(257, "Enter",        "ENTER",  "RETURN");
        addKey(258, "Tab",          "TAB");
        addKey(259, "Backspace",    "BACKSPACE");
        addKey(260, "Insert",       "INSERT");
        addKey(261, "Delete",       "DELETE",  "DEL");
        addKey(262, "Right",        "RIGHT",   "ARROW_RIGHT");
        addKey(263, "Left",         "LEFT",    "ARROW_LEFT");
        addKey(264, "Down",         "DOWN",    "ARROW_DOWN");
        addKey(265, "Up",           "UP",      "ARROW_UP");
        addKey(266, "Page Up",      "PAGE_UP", "PAGEUP");
        addKey(267, "Page Down",    "PAGE_DOWN","PAGEDOWN");
        addKey(268, "Home",         "HOME");
        addKey(269, "End",          "END");

        addKey(280, "Caps Lock",    "CAPS_LOCK",   "CAPSLOCK");
        addKey(281, "Scroll Lock",  "SCROLL_LOCK", "SCROLLLOCK");
        addKey(282, "Num Lock",     "NUM_LOCK",    "NUMLOCK");
        addKey(283, "Print Screen", "PRINT_SCREEN","PRINTSCREEN");
        addKey(284, "Pause",        "PAUSE");

        addKey(340, "Left Shift",   "LSHIFT", "LEFT_SHIFT");
        addKey(341, "Left Ctrl",    "LCTRL",  "LEFT_CTRL");
        addKey(342, "Left Alt",     "LALT",   "LEFT_ALT");
        addKey(344, "Right Shift",  "RSHIFT", "RIGHT_SHIFT");
        addKey(345, "Right Ctrl",   "RCTRL",  "RIGHT_CTRL");
        addKey(346, "Right Alt",    "RALT",   "RIGHT_ALT");

        addKey(32,  "Space",        "SPACE");
        addKey(39,  "Apostrophe",   "APOSTROPHE", "'");
        addKey(44,  "Comma",        "COMMA", ",");
        addKey(45,  "Minus",        "MINUS", "-");
        addKey(46,  "Period",       "PERIOD", ".");
        addKey(47,  "Slash",        "SLASH", "/");
        addKey(59,  "Semicolon",    "SEMICOLON", ";");
        addKey(61,  "Equal",        "EQUAL", "=");
        addKey(91,  "Left Bracket", "LEFT_BRACKET", "[");
        addKey(92,  "Backslash",    "BACKSLASH", "\\");
        addKey(93,  "Right Bracket","RIGHT_BRACKET", "]");
        addKey(96,  "Grave",        "GRAVE", "`");
    }

    private static void addKey(int code, String display, String... aliases) {
        CODE_TO_DISPLAY.put(code, display);

        for (String alias : aliases) {
            INPUT_TO_CODE.put(alias.toUpperCase(), code);
        }
    }



    public static String codeToDisplayName(int code) {
        return CODE_TO_DISPLAY.getOrDefault(code, "Key " + code);
    }


    public static int parseKeyArg(String arg) {
        if (arg == null || arg.isBlank()) return 0;

        try {
            int code = Integer.parseInt(arg.trim());
            return code > 0 ? code : 0;
        } catch (NumberFormatException ignored) {}

        Integer code = INPUT_TO_CODE.get(arg.trim().toUpperCase());
        return code != null ? code : 0;
    }


    public static Collection<String> allDisplayNames() {
        return Collections.unmodifiableCollection(CODE_TO_DISPLAY.values());
    }


    private static final Set<Integer> heldKeys = new HashSet<>();

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ChatMacroManager::onTick);
    }

    private static void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.currentScreen != null) {
            heldKeys.clear();
            return;
        }
        long window = mc.getWindow().getHandle();
        for (ChatMacro macro : WindChatClient.config.macros) {
            int key = macro.keyCode;
            boolean pressed = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
            if (pressed && !heldKeys.contains(key)) {
                heldKeys.add(key);
                executeMacro(mc.player, macro.command);
            } else if (!pressed) {
                heldKeys.remove(key);
            }
        }
    }

    private static void executeMacro(ClientPlayerEntity player, String command) {
        if (command.startsWith("/")) player.networkHandler.sendChatCommand(command.substring(1));
        else player.networkHandler.sendChatMessage(command);
    }


    public static void setMacro(int keyCode, String command) {
        var macros = WindChatClient.config.macros;
        macros.removeIf(m -> m.keyCode == keyCode);
        macros.add(new ChatMacro(keyCode, command, codeToDisplayName(keyCode)));
        WindChatClient.config.save();
    }

    public static boolean removeMacro(int keyCode) {
        boolean removed = WindChatClient.config.macros.removeIf(m -> m.keyCode == keyCode);
        if (removed) WindChatClient.config.save();
        return removed;
    }
}
