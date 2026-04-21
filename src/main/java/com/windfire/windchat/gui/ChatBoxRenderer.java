package com.windfire.windchat.gui;

import com.windfire.windchat.WindChatClient;
import com.windfire.windchat.api.AddonTab;
import com.windfire.windchat.api.WindChatAddonRegistry;
import com.windfire.windchat.features.ChatTab;
import com.windfire.windchat.features.ChatTabManager;
import com.windfire.windchat.features.DmTabManager;
import com.windfire.windchat.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;


public class ChatBoxRenderer {




    public static final int BOX_LEFT = 2;
    public static final int BOX_BOTTOM_OFFSET = 40;
    public static final int TAB_BAR_HEIGHT = 12;
    private static final int LINE_HEIGHT = 9;
    private static final int TEXT_PAD = 3;
    private static final int COLOR_BOX_BG      = 0x90_000000;
    private static final int COLOR_LINE_BG     = 0x40_000000;
    private static final int COLOR_SCROLL_BAR  = 0xFF_5BA3E0;
    private static final int COLOR_BAR_BG      = 0xB0_111111;
    private static final int COLOR_ACTIVE_BG   = 0xE0_1E3A5F;
    private static final int COLOR_INACTIVE_BG = 0x88_1A1A1A;
    private static final int COLOR_HOVER_BG    = 0xCC_284F7A;
    private static final int COLOR_ACTIVE_BOT  = 0xFF_5BA3E0;
    private static final int COLOR_SEPARATOR   = 0xFF_2A4A6A;
    private static final int COLOR_TEXT_ACTIVE = 0xFF_FFFFFF;
    private static final int COLOR_TEXT_INACT  = 0xFF_AAAAAA;
    private static final int COLOR_TEXT_HOVER  = 0xFF_DDDDDD;
    private static final int BADGE_SIZE        = 5;
    private static final int TAB_PAD_X        = 5;
    private static final int TAB_GAP          = 1;
    private static final int MIN_CONTENT_HEIGHT = 45;
    private static volatile List<ChatTabsHud.TabBounds> tabBounds = new ArrayList<>();

    public static void render(DrawContext ctx, int currentTick,
                              int mouseX, int mouseY, boolean focused) {
        if (!WindChatClient.config.tabsEnabled) {

            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.inGameHud == null) return;
        ChatHud chatHud = mc.inGameHud.getChatHud();
        if (!(chatHud instanceof ChatHudAccessor accessor)) return;

        int width        = chatHud.getWidth();
        int contentH     = computeContentHeight(mc);
        int totalH       = contentH + TAB_BAR_HEIGHT;
        int screenH      = mc.getWindow().getScaledHeight();
        int boxBottom    = screenH - BOX_BOTTOM_OFFSET;
        int boxTop       = boxBottom - totalH;
        int boxRight     = BOX_LEFT + width;
        int msgAreaTop   = boxTop + TAB_BAR_HEIGHT;

        ctx.fill(BOX_LEFT, boxTop, boxRight, boxBottom, COLOR_BOX_BG);

        renderTabBar(ctx, currentTick, mouseX, mouseY,
                BOX_LEFT, boxTop, boxRight);

        List<ChatHudLine.Visible> lines = accessor.windchat$getVisibleMessages();
        if (lines == null || lines.isEmpty()) return;

        int scrolled   = accessor.windchat$getScrolledLines();
        int maxVisible = contentH / LINE_HEIGHT;
        double opacity = mc.options.getChatOpacity().getValue();

        ctx.enableScissor(BOX_LEFT, msgAreaTop, boxRight, boxBottom);

        int lineCount = Math.min(lines.size() - scrolled, maxVisible);
        for (int i = 0; i < lineCount; i++) {
            int idx = scrolled + i;
            if (idx >= lines.size()) break;

            ChatHudLine.Visible line = lines.get(idx);
            int age   = currentTick - line.addedTime();
            float alpha = computeAlpha(age, focused, opacity,
                    WindChatClient.config.chatAlwaysVisible);
            if (alpha <= 0f) continue;

            int lineBottom = boxBottom - (i * LINE_HEIGHT);
            int lineTop    = lineBottom - LINE_HEIGHT;

            int bgA = (int)(alpha * 64) << 24;
            ctx.fill(BOX_LEFT, lineTop, boxRight, lineBottom, bgA | 0x000000);

            int textColor = (int)(alpha * 255) << 24 | 0xFFFFFF;
            ctx.drawText(mc.textRenderer, line.content(),
                    BOX_LEFT + TEXT_PAD, lineTop + 1, textColor, false);
        }

        ctx.disableScissor();

        if (focused && scrolled > 0) {
            renderScrollIndicator(ctx, BOX_LEFT, msgAreaTop, boxRight, boxBottom,
                    scrolled, lines.size(), maxVisible);
        }
    }



    static void renderTabBar(DrawContext ctx, int currentTick,
                             int mouseX, int mouseY,
                             int barLeft, int barTop, int barRight) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int barBottom = barTop + TAB_BAR_HEIGHT;
        int tabTop    = barTop + 1;
        int tabBottom = barBottom - 1;
        int tabH      = tabBottom - tabTop;

        ctx.fill(barLeft, barTop, barRight, barBottom, COLOR_BAR_BG);

        ctx.fill(barLeft, barBottom - 1, barRight, barBottom, COLOR_SEPARATOR);

        double scale  = mc.getWindow().getScaleFactor();
        double mxScaled = mc.mouse.getX() / scale;
        double myScaled = mc.mouse.getY() / scale;

        WindChatAddonRegistry registry = WindChatAddonRegistry.getInstance();
        String activeDm      = DmTabManager.getActiveUser();
        String activeAddonId = registry.getActiveAddonTabId();

        int totalW = 1;
        for (ChatTab tab : ChatTabManager.visibleTabs()) {
            boolean hu = (tab != ChatTab.DEATHS) && ChatTabManager.hasUnread(tab);
            totalW += mc.textRenderer.getWidth(tab.displayName)
                    + TAB_PAD_X * 2 + (hu ? BADGE_SIZE + 3 : 0) + TAB_GAP;
        }
        for (String p : DmTabManager.visiblePartners()) {
            boolean hu = DmTabManager.hasUnread(p);
            totalW += mc.textRenderer.getWidth(p)
                    + TAB_PAD_X * 2 + (hu ? BADGE_SIZE + 3 : 0) + TAB_GAP;
        }
        for (AddonTab at : registry.getAddonTabs()) {
            boolean hu = at.hasUnread();
            totalW += mc.textRenderer.getWidth(at.getDisplayName())
                    + TAB_PAD_X * 2 + (hu ? BADGE_SIZE + 3 : 0) + TAB_GAP;
        }

        int effectiveRight = Math.max(barRight, barLeft + totalW);
        if (effectiveRight > barRight)
            ctx.fill(barRight, barTop, effectiveRight, barBottom, COLOR_BAR_BG);

        List<ChatTabsHud.TabBounds> newBounds = new ArrayList<>();
        int cursorX = barLeft + 1;

        for (ChatTab tab : ChatTabManager.visibleTabs()) {
            boolean hasUnread = (tab != ChatTab.DEATHS) && ChatTabManager.hasUnread(tab);
            boolean isActive  = (activeDm == null) && (activeAddonId == null)
                    && (tab == ChatTabManager.getCurrentTab());
            int textW = mc.textRenderer.getWidth(tab.displayName);
            int tabW  = textW + TAB_PAD_X * 2 + (hasUnread ? BADGE_SIZE + 3 : 0);
            boolean isHover = !isActive
                    && mxScaled >= cursorX && mxScaled < cursorX + tabW
                    && myScaled >= tabTop  && myScaled < tabBottom;
            drawTab(ctx, cursorX, tabTop, tabW, tabBottom, tabH,
                    isActive, isHover, COLOR_TEXT_ACTIVE, tab.displayName, hasUnread);
            newBounds.add(new ChatTabsHud.TabBounds(tab, null, null, cursorX, tabTop, tabW, tabH));
            cursorX += tabW + TAB_GAP;
        }

        for (String partner : DmTabManager.visiblePartners()) {
            boolean hasUnread = DmTabManager.hasUnread(partner);
            boolean isActive  = partner.equals(activeDm);
            int textW = mc.textRenderer.getWidth(partner);
            int tabW  = textW + TAB_PAD_X * 2 + (hasUnread ? BADGE_SIZE + 3 : 0);
            boolean isHover = !isActive
                    && mxScaled >= cursorX && mxScaled < cursorX + tabW
                    && myScaled >= tabTop  && myScaled < tabBottom;
            drawTab(ctx, cursorX, tabTop, tabW, tabBottom, tabH,
                    isActive, isHover, 0xFF_AADDFF, partner, hasUnread);
            newBounds.add(new ChatTabsHud.TabBounds(null, partner, null, cursorX, tabTop, tabW, tabH));
            cursorX += tabW + TAB_GAP;
        }

        for (AddonTab addonTab : registry.getAddonTabs()) {
            boolean hasUnread = addonTab.hasUnread();
            boolean isActive  = addonTab.getId().equals(activeAddonId);
            int textW = mc.textRenderer.getWidth(addonTab.getDisplayName());
            int tabW  = textW + TAB_PAD_X * 2 + (hasUnread ? BADGE_SIZE + 3 : 0);
            boolean isHover = !isActive
                    && mxScaled >= cursorX && mxScaled < cursorX + tabW
                    && myScaled >= tabTop  && myScaled < tabBottom;
            drawTab(ctx, cursorX, tabTop, tabW, tabBottom, tabH,
                    isActive, isHover, addonTab.getActiveColor(),
                    addonTab.getDisplayName(), hasUnread);
            newBounds.add(new ChatTabsHud.TabBounds(null, null, addonTab.getId(), cursorX, tabTop, tabW, tabH));
            cursorX += tabW + TAB_GAP;
        }

        tabBounds = newBounds;
    }


    public static List<ChatTabsHud.TabBounds> getTabBounds() { return tabBounds; }


    private static int computeContentHeight(MinecraftClient mc) {



        int h = mc.inGameHud.getChatHud().getHeight();
        return Math.max(h, MIN_CONTENT_HEIGHT);
    }

    private static float computeAlpha(int age, boolean focused,
                                      double chatOpacity, boolean alwaysVisible) {
        float baseAlpha = (float)(chatOpacity * 0.9 + 0.1);

        if (alwaysVisible || focused) return baseAlpha;

        if (age > 260) return 0f;
        float fade = age > 200 ? 1f - (age - 200) / 60f : 1f;
        return MathHelper.clamp(fade * baseAlpha, 0f, 1f);
    }

    private static void drawTab(DrawContext ctx, int x, int tabTop, int tabW,
                                int tabBottom, int tabH,
                                boolean isActive, boolean isHover,
                                int activeTextColor, String label, boolean hasUnread) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int bg = isActive ? COLOR_ACTIVE_BG : (isHover ? COLOR_HOVER_BG : COLOR_INACTIVE_BG);
        ctx.fill(x, tabTop, x + tabW, tabBottom, bg);

        if (isActive)
            ctx.fill(x, tabBottom - 1, x + tabW, tabBottom, COLOR_ACTIVE_BOT);
        int textColor = isActive ? activeTextColor
                : (isHover ? COLOR_TEXT_HOVER : COLOR_TEXT_INACT);
        ctx.drawText(mc.textRenderer, label,
                x + TAB_PAD_X, tabTop + (tabH - 8) / 2, textColor, false);
        if (hasUnread) {
            int bx = x + tabW - BADGE_SIZE - 2;
            int by = tabTop + 1;
            ctx.fill(bx - 1, by - 1, bx + BADGE_SIZE + 1, by + BADGE_SIZE + 1, 0xAA000000);
            ctx.fill(bx, by, bx + BADGE_SIZE, by + BADGE_SIZE, 0xFFFF3333);
        }
    }

    private static void renderScrollIndicator(DrawContext ctx,
                                              int left, int top, int right, int bottom,
                                              int scrolled, int totalLines, int maxVisible) {

        int areaH    = bottom - top;
        float ratio  = (float) scrolled / Math.max(1, totalLines - maxVisible);
        int barH     = Math.max(4, areaH / Math.max(1, totalLines / maxVisible));
        int barY     = top + (int)((areaH - barH) * ratio);
        ctx.fill(right - 2, barY, right, barY + barH, COLOR_SCROLL_BAR);
    }
}