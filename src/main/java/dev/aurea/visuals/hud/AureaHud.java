package dev.aurea.visuals.hud;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import dev.aurea.visuals.AureaConfig;
import dev.aurea.visuals.AureaModule;

public final class AureaHud {
    private static final Deque<Integer> FPS_HISTORY = new ArrayDeque<>();
    private static long lastSampleAt = 0L;

    private AureaHud() {}

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.screen != null) {
            return;
        }

        int accent = AureaConfig.accentColor();
        boolean compact = AureaConfig.isEnabled(AureaModule.COMPACT_HUD);
        int rowHeight = compact ? 18 : 22;
        int rowGap = compact ? 3 : 4;
        int y = 12;

        for (AureaModule module : AureaModule.values()) {
            if (!module.hasHudText() || !AureaConfig.isEnabled(module)) {
                continue;
            }
            String text = module.hudText(client);
            if (text == null || text.isBlank()) {
                continue;
            }

            int maxWidth = Math.min(260, Math.max(120, client.getWindow().getGuiScaledWidth() - 24));
            while (client.font.width(text) > maxWidth - 20 && text.length() > 10) {
                text = text.substring(0, text.length() - 2) + "…";
            }
            int width = Math.min(maxWidth, client.font.width(text) + 20);
            graphics.fill(10, y, 10 + width, y + rowHeight, compact ? 0xA81A1E26 : 0xC21A1E26);
            graphics.fill(10, y, 12, y + rowHeight, accent);
            graphics.fill(12, y, 13, y + rowHeight, 0x33FFFFFF);
            graphics.text(client.font, text, 18, y + (compact ? 5 : 7), 0xFFF1F4FA, false);
            y += rowHeight + rowGap;
        }

        if (AureaConfig.isEnabled(AureaModule.CUSTOM_CROSSHAIR)) {
            drawCrosshair(graphics, client, accent);
        }
        if (AureaConfig.isEnabled(AureaModule.FPS_GRAPH)) {
            drawFpsGraph(graphics, client, accent);
        }
    }

    private static void drawCrosshair(GuiGraphicsExtractor graphics, Minecraft client, int accent) {
        int cx = client.getWindow().getGuiScaledWidth() / 2;
        int cy = client.getWindow().getGuiScaledHeight() / 2;
        graphics.outline(cx - 6, cy - 6, 12, 12, accent);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFF2F5FA);
        graphics.fill(cx - 10, cy, cx - 7, cy + 1, accent);
        graphics.fill(cx + 7, cy, cx + 10, cy + 1, accent);
        graphics.fill(cx, cy - 10, cx + 1, cy - 7, accent);
        graphics.fill(cx, cy + 7, cx + 1, cy + 10, accent);
    }

    private static void drawFpsGraph(GuiGraphicsExtractor graphics, Minecraft client, int accent) {
        long now = System.currentTimeMillis();
        if (now - lastSampleAt >= 250L) {
            FPS_HISTORY.addLast(client.getFps());
            while (FPS_HISTORY.size() > 40) {
                FPS_HISTORY.removeFirst();
            }
            lastSampleAt = now;
        }

        int guiWidth = client.getWindow().getGuiScaledWidth();
        int x = guiWidth - 150;
        int y = 12;
        int panelWidth = 140;
        int panelHeight = 42;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, 0xB51A1E26);
        graphics.fill(x, y, x + panelWidth, y + 2, accent);
        graphics.text(client.font, "FPS", x + 8, y + 7, 0xFFB8C1D1, false);
        graphics.text(client.font, Integer.toString(client.getFps()), x + panelWidth - 38, y + 7, 0xFFF1F4FA, false);

        int index = 0;
        int baseY = y + panelHeight - 5;
        for (int fps : FPS_HISTORY) {
            int height = Math.max(2, Math.min(18, fps / 8));
            int barX = x + 7 + index * 3;
            graphics.fill(barX, baseY - height, barX + 2, baseY, fps >= 60 ? accent : 0xFF7D8594);
            index++;
        }
    }
}
