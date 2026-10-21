package net.celsiuss.gtterminaltabs.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/** Grey text wrapped and centred in its bounds, drawn in the button pass like the panel background. */
public class HintLabel extends GuiButton {

    private static final int LINE_SPACING = 1;

    public HintLabel(int x, int y, int width, int height, String text) {
        super(-1, x, y, width, height, text);
        this.enabled = false;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        List<String> lines = mc.fontRenderer.listFormattedStringToWidth(displayString, width);
        int lineHeight = mc.fontRenderer.FONT_HEIGHT + LINE_SPACING;
        int top = yPosition + (height - lines.size() * lineHeight + LINE_SPACING) / 2;
        for (String line : lines) {
            drawCenteredString(mc.fontRenderer, line, xPosition + width / 2, top, FlatStyle.TEXT_DISABLED);
            top += lineHeight;
        }
    }
}
