package com.windfire.windchat.gui;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.config.ChatMacro;
import com.windfire.windchat.features.ChatMacroManager;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;


public class MacroEditScreen extends Screen {


    private static final int TITLE_Y        = 8;
    private static final int ADD_BTN_Y      = 24;
    private static final int LIST_TOP       = 50;
    private static final int LIST_BOTTOM_PAD = 32;
    private static final int ENTRY_H        = 28;
    private static final int H_PAD          = 10;
    private static final int KEY_BTN_W      = 120;
    private static final int DEL_BTN_W      = 54;
    private static final int GAP            = 4;


    private final Screen parent;

    private final List<MacroDraft> drafts = new ArrayList<>();

    private int listenIndex = -1;

    private double scrollOffset = 0;

    private ButtonWidget addButton;
    private ButtonWidget doneButton;

    private final List<TextFieldWidget> commandFields = new ArrayList<>();
    private final List<ButtonWidget>    keyButtons    = new ArrayList<>();
    private final List<ButtonWidget>    deleteButtons = new ArrayList<>();

    public MacroEditScreen(Screen parent) {
        super(Text.literal("Macro Editor"));
        this.parent = parent;

        for (ChatMacro m : WindChatClient.config.macros) {
            drafts.add(new MacroDraft(m.command, m.keyCode));
        }
    }


    @Override
    protected void init() {
        rebuildWidgets();
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }



    private void rebuildWidgets() {
        clearChildren();
        commandFields.clear();
        keyButtons.clear();
        deleteButtons.clear();

        addButton = addDrawableChild(ButtonWidget.builder(
                Text.literal("+ New Macro"),
                btn -> {
                    drafts.add(new MacroDraft("", 0));
                    scrollOffset = Math.max(0, drafts.size() * ENTRY_H - listAreaHeight());
                    rebuildWidgets();

                    if (!commandFields.isEmpty()) {
                        setFocused(commandFields.get(commandFields.size() - 1));
                    }
                })
                .dimensions(H_PAD, ADD_BTN_Y, 110, 20)
                .build());

        doneButton = addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                btn -> saveAndClose())
                .dimensions(width / 2 - 50, height - 26, 100, 20)
                .build());

        int cmdW = width - H_PAD * 2 - KEY_BTN_W - DEL_BTN_W - GAP * 2;
        int keyX = H_PAD + cmdW + GAP;
        int delX = keyX + KEY_BTN_W + GAP;

        for (int i = 0; i < drafts.size(); i++) {
            MacroDraft draft = drafts.get(i);
            int rowY = rowY(i);

            if (rowY + ENTRY_H < LIST_TOP || rowY > height - LIST_BOTTOM_PAD) continue;

            final int idx = i;

            TextFieldWidget tf = new TextFieldWidget(
                    textRenderer, H_PAD, rowY + 4, cmdW, 20, Text.literal("Command (e.g. /home vault)"));
            tf.setText(draft.command);
            tf.setMaxLength(256);
            tf.setPlaceholder(Text.literal("/command or message...").formatted(Formatting.DARK_GRAY));
            tf.setChangedListener(s -> {
                if (idx < drafts.size()) drafts.get(idx).command = s;
            });
            commandFields.add(addDrawableChild(tf));

            ButtonWidget kb = addDrawableChild(ButtonWidget.builder(
                    keyButtonLabel(i),
                    btn -> {
                        listenIndex = idx;
                        updateKeyButtonLabels();
                    })
                    .dimensions(keyX, rowY + 4, KEY_BTN_W, 20)
                    .build());
            keyButtons.add(kb);

            ButtonWidget db = addDrawableChild(ButtonWidget.builder(
                    Text.literal("✕").formatted(Formatting.RED),
                    btn -> {
                        drafts.remove(idx);
                        if (listenIndex == idx) listenIndex = -1;
                        else if (listenIndex > idx) listenIndex--;

                        double maxScroll = Math.max(0, drafts.size() * ENTRY_H - listAreaHeight());
                        scrollOffset = Math.min(scrollOffset, maxScroll);
                        rebuildWidgets();
                    })
                    .dimensions(delX, rowY + 4, DEL_BTN_W, 20)
                    .build());
            deleteButtons.add(db);
        }
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        renderBackground(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, TITLE_Y, 0xFFFFFF);

        context.fill(H_PAD, LIST_TOP - 3, width - H_PAD, LIST_TOP - 2, 0x55FFFFFF);
        context.fill(H_PAD, height - LIST_BOTTOM_PAD - 1, width - H_PAD, height - LIST_BOTTOM_PAD, 0x55FFFFFF);

        int cmdW = width - H_PAD * 2 - KEY_BTN_W - DEL_BTN_W - GAP * 2;
        int keyHeaderX = H_PAD + cmdW + GAP;
        context.drawTextWithShadow(textRenderer, Text.literal("Command").formatted(Formatting.GRAY),
                H_PAD, LIST_TOP - 14, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Key").formatted(Formatting.GRAY),
                keyHeaderX, LIST_TOP - 14, 0xFFFFFF);

        context.enableScissor(0, LIST_TOP, width, height - LIST_BOTTOM_PAD);
        super.render(context, mouseX, mouseY, delta);   // draws all drawableChildren incl. entries
        context.disableScissor();



        addButton.render(context, mouseX, mouseY, delta);
        doneButton.render(context, mouseX, mouseY, delta);

        drawScrollBar(context);

        if (drafts.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("No macros yet — click '+ New Macro'").formatted(Formatting.GRAY),
                    width / 2, LIST_TOP + 20, 0xFFFFFF);
        }
    }

    private void drawScrollBar(DrawContext context) {
        int totalH   = drafts.size() * ENTRY_H;
        int areaH    = listAreaHeight();
        if (totalH <= areaH) return;

        int trackX   = width - 5;
        int trackTop = LIST_TOP;
        int trackBot = height - LIST_BOTTOM_PAD;
        int trackH   = trackBot - trackTop;

        float thumbRatio = (float) areaH / totalH;
        int thumbH   = Math.max(20, (int) (trackH * thumbRatio));
        int thumbY   = trackTop + (int) ((trackH - thumbH) * scrollOffset / (totalH - areaH));

        context.fill(trackX, trackTop, trackX + 3, trackBot, 0x44FFFFFF);
        context.fill(trackX, thumbY,   trackX + 3, thumbY + thumbH, 0xBBFFFFFF);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {

        if (listenIndex >= 0) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {

                listenIndex = -1;
                updateKeyButtonLabels();
            } else {

                drafts.get(listenIndex).keyCode = keyCode;
                listenIndex = -1;
                updateKeyButtonLabels();
            }
            return true; // consume event
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double maxScroll = Math.max(0, drafts.size() * ENTRY_H - listAreaHeight());
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount * ENTRY_H));
        rebuildWidgets();
        return true;
    }



    private int rowY(int i) {
        return LIST_TOP + i * ENTRY_H - (int) scrollOffset;
    }

    private int listAreaHeight() {
        return height - LIST_TOP - LIST_BOTTOM_PAD;
    }


    private Text keyButtonLabel(int i) {
        if (i == listenIndex) {
            return Text.literal("> Press Key <")
                    .styled(s -> s.withColor(0xFFFF55).withUnderline(true));
        }
        int code = (i < drafts.size()) ? drafts.get(i).keyCode : 0;
        if (code == 0) {
            return Text.literal("Click to Set Key").formatted(Formatting.GRAY);
        }
        return Text.literal(ChatMacroManager.codeToDisplayName(code));
    }


    private void updateKeyButtonLabels() {
        for (int i = 0; i < keyButtons.size(); i++) {




            break;
        }
        rebuildWidgets();
    }


    private void saveAndClose() {
        WindChatClient.config.macros.clear();
        for (MacroDraft d : drafts) {
            if (!d.command.isBlank() && d.keyCode != 0) {
                WindChatClient.config.macros.add(new ChatMacro(
                        d.keyCode,
                        d.command,
                        ChatMacroManager.codeToDisplayName(d.keyCode)
                ));
            }
        }
        WindChatClient.config.save();
        MinecraftClient.getInstance().setScreen(parent);
    }



    private static class MacroDraft {
        String command;
        int keyCode;

        MacroDraft(String command, int keyCode) {
            this.command = command;
            this.keyCode = keyCode;
        }
    }
}
