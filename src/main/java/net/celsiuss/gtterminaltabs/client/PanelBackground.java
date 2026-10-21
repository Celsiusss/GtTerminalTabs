package net.celsiuss.gtterminaltabs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

/**
 * Draws the panel background as part of the screen's button pass, so it sits under slots, item tooltips and
 * AE2's tooltips instead of on top of them. Disabled, so it never takes clicks.
 */
public class PanelBackground extends GuiButton {

    private final int dividerY;

    /** @param dividerY where a faint line separates the content from the footer, or -1 for none */
    public PanelBackground(int id, int x, int y, int width, int height, int dividerY) {
        super(id, x, y, width, height, "");
        this.dividerY = dividerY;
        this.enabled = false;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        int right = xPosition + width;
        FlatStyle
            .drawBox(xPosition, yPosition, right, yPosition + height, FlatStyle.PANEL_FILL, FlatStyle.PANEL_BORDER);
        if (dividerY >= 0) drawRect(xPosition + 1, dividerY, right - 1, dividerY + 1, FlatStyle.DIVIDER);
    }
}
