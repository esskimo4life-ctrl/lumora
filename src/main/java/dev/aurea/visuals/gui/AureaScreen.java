package dev.aurea.visuals.gui;

import java.util.Arrays;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import dev.aurea.visuals.AureaConfig;
import dev.aurea.visuals.AureaModule;

public final class AureaScreen extends Screen {
    private static final int PANEL = 0xF0161920;
    private static final int PANEL_BORDER = 0xFF343A46;
    private static final int CARD = 0xD921252E;
    private static final int CARD_HOVER = 0xE02B313C;
    private static final int MUTED = 0xFF9DA7B7;
    private static final int WHITE = 0xFFF1F4FA;
    private static final int SIDEBAR = 0xD51C2028;
    private AureaModule.Category selectedCategory = AureaModule.Category.VISUALS;
    private int scrollOffset = 0;
    private int panelX, panelY, panelW, panelH, sidebarW, mainX, cardsY;

    public AureaScreen() {
        super(Component.literal("Aurea Visuals"));
    }

    @Override
    protected void init() {
        calculateLayout();
    }

    private void calculateLayout() {
        panelW = Math.min(1000, Math.max(560, width - 24));
        panelH = Math.min(590, Math.max(390, height - 24));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        sidebarW = 154;
        mainX = panelX + sidebarW + 26;
        cardsY = panelY + 132;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        calculateLayout();

        graphics.fill(0, 0, width, height, 0xA8000000);
        graphics.fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, 0xF01A1E26, PANEL);
        graphics.outline(panelX, panelY, panelW, panelH, PANEL_BORDER);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + 3, AureaConfig.accentColor());

        drawHeader(graphics, mouseX, mouseY);
        drawSidebar(graphics, mouseX, mouseY);
        drawModules(graphics, mouseX, mouseY);
        drawFooter(graphics);
    }

    private void drawHeader(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = panelX + 22;
        int y = panelY + 21;
        int accent = AureaConfig.accentColor();

        g.fill(x, y + 2, x + 4, y + 25, accent);
        g.fill(x + 6, y + 2, x + 10, y + 16, 0xFF536174);
        g.fill(x + 12, y + 2, x + 16, y + 21, 0xFF778397);
        g.text(font, "AUREA", x + 25, y, WHITE, false);
        g.text(font, "VISUALS / CLIENT INTERFACE", x + 25, y + 13, MUTED, false);

        int versionX = panelX + panelW - 285;
        drawPill(g, versionX, panelY + 24, 84, 20, "FABRIC 26.1", 0xFF242A34, MUTED);
        drawTopButton(g, panelX + panelW - 190, panelY + 23, 68, 22, "ЦВЕТ", mouseX, mouseY);
        drawTopButton(g, panelX + panelW - 115, panelY + 23, 70, 22, "СБРОС", mouseX, mouseY);
        drawTopButton(g, panelX + panelW - 37, panelY + 23, 23, 22, "×", mouseX, mouseY);
        g.fill(panelX + 18, panelY + 64, panelX + panelW - 18, panelY + 65, 0x263B4350);
    }

    private void drawSidebar(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = panelX + 14;
        int y = panelY + 82;
        g.fill(x, y, x + sidebarW, panelY + panelH - 46, SIDEBAR);
        g.text(font, "РАЗДЕЛЫ", x + 13, y + 13, MUTED, false);

        AureaModule.Category[] categories = AureaModule.Category.values();
        for (int i = 0; i < categories.length; i++) {
            AureaModule.Category category = categories[i];
            int rowY = y + 34 + i * 40;
            boolean selected = selectedCategory == category;
            boolean hovered = inside(mouseX, mouseY, x + 7, rowY, sidebarW - 14, 34);
            if (selected) {
                g.fill(x + 7, rowY, x + sidebarW - 7, rowY + 34, 0xFF2C3441);
                g.fill(x + 7, rowY + 5, x + 9, rowY + 29, AureaConfig.accentColor());
            } else if (hovered) {
                g.fill(x + 7, rowY, x + sidebarW - 7, rowY + 34, 0x55353D49);
            }
            g.text(font, category.title(), x + 18, rowY + 8, selected ? WHITE : 0xFFD3D9E3, false);
            g.text(font, Integer.toString(countModules(category)), x + sidebarW - 25, rowY + 8, selected ? AureaConfig.accentColor() : MUTED, false);
        }

        int summaryY = panelY + panelH - 103;
        g.fill(x + 8, summaryY, x + sidebarW - 8, summaryY + 1, 0x263B4350);
        g.text(font, "АКТИВНО", x + 13, summaryY + 12, MUTED, false);
        g.text(font, enabledCount() + " / " + AureaModule.values().length, x + 13, summaryY + 28, WHITE, false);
        g.text(font, "без кейстроков", x + 13, summaryY + 46, 0xFF8590A1, false);
    }

    private void drawModules(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        List<AureaModule> modules = Arrays.stream(AureaModule.values())
                .filter(module -> module.category() == selectedCategory)
                .toList();
        int contentX = mainX;
        int contentRight = panelX + panelW - 18;
        int contentWidth = contentRight - contentX;

        g.text(font, selectedCategory.title().toUpperCase(), contentX, panelY + 84, WHITE, false);
        g.text(font, "МОДУЛЕЙ: " + modules.size(), contentX, panelY + 99, MUTED, false);
        g.text(font, "Нажми на карточку, чтобы включить или выключить модуль.", contentX, panelY + 114, 0xFF8590A1, false);

        int gapX = 12;
        int actualCardW = Math.max(145, (contentWidth - gapX) / 2);
        int rowH = 64;
        int gapY = 9;
        int maxRows = Math.max(1, (panelY + panelH - 45 - cardsY) / (rowH + gapY));
        int maxScroll = Math.max(0, ((modules.size() + 1) / 2 - maxRows) * (rowH + gapY));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        g.enableScissor(contentX, cardsY, contentRight, panelY + panelH - 43);
        for (int i = 0; i < modules.size(); i++) {
            AureaModule module = modules.get(i);
            int col = i % 2;
            int row = i / 2;
            int cx = contentX + col * (actualCardW + gapX);
            int cy = cardsY + row * (rowH + gapY) - scrollOffset;
            if (cy + rowH < cardsY || cy > panelY + panelH - 43) {
                continue;
            }

            boolean enabled = AureaConfig.isEnabled(module);
            boolean hovered = inside(mouseX, mouseY, cx, cy, actualCardW, rowH);
            g.fill(cx, cy, cx + actualCardW, cy + rowH, hovered ? CARD_HOVER : CARD);
            g.outline(cx, cy, actualCardW, rowH, enabled ? 0x554D83C7 : 0x403E4652);
            if (enabled) {
                g.fill(cx + 1, cy + 1, cx + 3, cy + rowH - 1, AureaConfig.accentColor());
            }

            int textWidth = actualCardW - 62;
            g.text(font, trim(module.title(), textWidth), cx + 11, cy + 12, enabled ? WHITE : 0xFFD8DEE8, false);
            g.text(font, trim(module.description(), textWidth), cx + 11, cy + 32, MUTED, false);
            drawToggle(g, cx + actualCardW - 43, cy + 23, enabled);
        }
        g.disableScissor();

        if (maxScroll > 0) {
            int trackX = contentRight - 3;
            int trackTop = cardsY;
            int trackBottom = panelY + panelH - 47;
            int trackH = trackBottom - trackTop;
            g.fill(trackX, trackTop, trackX + 2, trackBottom, 0x553F4754);
            int totalRows = (modules.size() + 1) / 2;
            int thumbH = Math.max(22, trackH * maxRows / Math.max(1, totalRows));
            int thumbY = trackTop + (trackH - thumbH) * scrollOffset / Math.max(1, maxScroll);
            g.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, AureaConfig.accentColor());
        }
    }

    private void drawFooter(GuiGraphicsExtractor g) {
        int y = panelY + panelH - 28;
        g.fill(panelX + 18, y - 9, panelX + panelW - 18, y - 8, 0x263B4350);
        g.text(font, "БИНД МЕНЮ: УПРАВЛЕНИЕ → КЛАВИШИ → AUREA VISUALS", panelX + 22, y, MUTED, false);
        g.text(font, "КОНФИГ СОХРАНЯЕТСЯ", panelX + panelW - 180, y, 0xFF7F8A9B, false);
    }

    private void drawTopButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        g.fill(x, y, x + w, y + h, hovered ? 0xFF384252 : 0xFF252B35);
        g.outline(x, y, w, h, hovered ? AureaConfig.accentColor() : 0xFF3A424F);
        int textW = font.width(label);
        g.text(font, label, x + (w - textW) / 2, y + 7, hovered ? WHITE : 0xFFD3D9E3, false);
    }

    private void drawPill(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, int bg, int fg) {
        g.fill(x, y, x + w, y + h, bg);
        g.outline(x, y, w, h, 0xFF38404D);
        g.text(font, label, x + 8, y + 6, fg, false);
    }

    private void drawToggle(GuiGraphicsExtractor g, int x, int y, boolean enabled) {
        int bg = enabled ? AureaConfig.accentColor() : 0xFF3A414D;
        g.fill(x, y, x + 32, y + 17, bg);
        g.fill(x + (enabled ? 17 : 2), y + 2, x + (enabled ? 29 : 14), y + 15, 0xFFF6F8FC);
        g.fill(x + 1, y + 1, x + 31, y + 2, 0x22FFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        int mx = (int) event.x();
        int my = (int) event.y();

        if (inside(mx, my, panelX + panelW - 42, panelY + 20, 30, 28)) {
            onClose();
            return true;
        }
        if (inside(mx, my, panelX + panelW - 190, panelY + 20, 68, 28)) {
            AureaConfig.cycleAccent();
            return true;
        }
        if (inside(mx, my, panelX + panelW - 115, panelY + 20, 70, 28)) {
            AureaConfig.reset();
            return true;
        }

        int sidebarX = panelX + 14;
        int sidebarY = panelY + 82;
        AureaModule.Category[] categories = AureaModule.Category.values();
        for (int i = 0; i < categories.length; i++) {
            int rowY = sidebarY + 34 + i * 40;
            if (inside(mx, my, sidebarX + 7, rowY, sidebarW - 14, 34)) {
                selectedCategory = categories[i];
                scrollOffset = 0;
                return true;
            }
        }

        List<AureaModule> modules = Arrays.stream(AureaModule.values())
                .filter(module -> module.category() == selectedCategory)
                .toList();
        int contentX = mainX;
        int contentRight = panelX + panelW - 18;
        int contentWidth = contentRight - contentX;
        int gapX = 12;
        int actualCardW = Math.max(145, (contentWidth - gapX) / 2);
        int rowH = 64;
        int gapY = 9;
        int contentBottom = panelY + panelH - 43;
        if (mx >= contentX && mx <= contentRight && my >= cardsY && my < contentBottom) {
            int col = Math.max(0, Math.min(1, (mx - contentX) / (actualCardW + gapX)));
            int row = (my - cardsY + scrollOffset) / (rowH + gapY);
            int index = row * 2 + col;
            if (index >= 0 && index < modules.size()) {
                int cardX = contentX + col * (actualCardW + gapX);
                int cardY = cardsY + row * (rowH + gapY) - scrollOffset;
                if (inside(mx, my, cardX, cardY, actualCardW, rowH)) {
                    AureaModule module = modules.get(index);
                    AureaConfig.setEnabled(module, !AureaConfig.isEnabled(module), Minecraft.getInstance());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= mainX && mouseY >= cardsY && mouseY < panelY + panelH - 43) {
            int rows = (int) Math.ceil(Arrays.stream(AureaModule.values())
                    .filter(module -> module.category() == selectedCategory).count() / 2.0);
            int rowH = 73;
            int visibleRows = Math.max(1, (panelY + panelH - 45 - cardsY) / rowH);
            int max = Math.max(0, (rows - visibleRows) * rowH);
            scrollOffset = Math.max(0, Math.min(max, scrollOffset - (int) Math.signum(verticalAmount) * rowH));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }

    private int countModules(AureaModule.Category category) {
        return (int) Arrays.stream(AureaModule.values()).filter(module -> module.category() == category).count();
    }

    private int enabledCount() {
        return (int) Arrays.stream(AureaModule.values()).filter(AureaConfig::isEnabled).count();
    }

    private boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private String trim(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String result = text;
        while (!result.isEmpty() && font.width(result + "…") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "…";
    }
}
