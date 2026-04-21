package com.windfire.windchat.features;

import com.windfire.windchat.WindChatClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimestampManager {

    private static final DateTimeFormatter FMT_24H  = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_12H  = DateTimeFormatter.ofPattern("h:mm a");
    private static final int TIMESTAMP_COLOR = 0x888888;

    public static Text apply(Text original) {
        String mode = WindChatClient.config.timestampMode;
        if (mode == null || mode.equalsIgnoreCase("OFF")) return original;

        DateTimeFormatter fmt = mode.equalsIgnoreCase("12H") ? FMT_12H : FMT_24H;
        String ts = "[" + LocalTime.now().format(fmt) + "] ";
        MutableText stamp = Text.literal(ts).setStyle(Style.EMPTY.withColor(TIMESTAMP_COLOR));
        return Text.empty().append(stamp).append(original);
    }
}
