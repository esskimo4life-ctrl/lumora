package dev.astral.boost;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class AstraBoostScreen extends Screen {
    private String statusLine = "Выбери профиль — настройки применятся сразу";
    public AstraBoostScreen() { super(Component.literal("AstraBoost")); }

    @Override
    protected void init() {
        int buttonWidth = Math.min(260, this.width - 32);
        int x = (this.width - buttonWidth) / 2;
        int y = Math.max(74, this.height / 2 - 56);
        this.addRenderableWidget(Button.builder(Component.literal("Максимальный FPS"), button -> apply(AstraBoostConfig.Profile.ULTRA_FPS))
                .bounds(x, y, buttonWidth, 22).build());
        this.addRenderableWidget(Button.builder(Component.literal("Соревновательный"), button -> apply(AstraBoostConfig.Profile.COMPETITIVE))
                .bounds(x, y + 28, buttonWidth, 22).build());
        this.addRenderableWidget(Button.builder(Component.literal("Баланс"), button -> apply(AstraBoostConfig.Profile.BALANCED))
                .bounds(x, y + 56, buttonWidth, 22).build());
        this.addRenderableWidget(Button.builder(Component.literal("Автоприменение: " + (AstraBoostConfig.autoApply() ? "ВКЛ" : "ВЫКЛ")), button -> {
            AstraBoostConfig.toggleAutoApply();
            this.clearWidgets();
            this.init();
        }).bounds(x, y + 88, buttonWidth, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Закрыть"), button -> this.onClose())
                .bounds(x, y + 116, buttonWidth, 20).build());
    }

    private void apply(AstraBoostConfig.Profile profile) {
        AstraBoostConfig.selectProfile(profile);
        PerformanceTuner.ApplyResult result = PerformanceTuner.apply(
                Minecraft.getInstance(), AstraBoostConfig.settingsForActiveProfile()
        );
        this.statusLine = profile.displayName() + " · " + result.summary();
}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        centeredText(graphics, "ASTRABOOST", this.width / 2, 24, 0xFF77E6D0);
        centeredText(graphics, "Fabric 26.1.2 · Fold Craft Launcher", this.width / 2, 40, 0xFFFFFFFF);
        centeredText(graphics, "Профиль: " + AstraBoostConfig.profile().displayName(), this.width / 2, 55, 0xFFFFFFFF);
        centeredText(graphics, statusLine, this.width / 2, this.height - 28, 0xFFB7C8D1);
    }
    private void centeredText(GuiGraphicsExtractor graphics, String text, int centerX, int y, int color) {
        graphics.text(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
    }
}
