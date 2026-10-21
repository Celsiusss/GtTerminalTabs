package net.celsiuss.gtterminaltabs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

/** A square button showing a 16x16 icon on the same flat background as the tab rows. */
public class IconButton extends TooltipButton {

    private static final int ICON_SIZE = ItemIcons.ICON_SIZE;
    public static final int SIZE = ItemIcons.SLOT_SIZE;

    private final ResourceLocation icon;
    private boolean highlighted;

    public IconButton(int id, int x, int y, ResourceLocation icon, String tooltip) {
        super(id, x, y, SIZE, SIZE, "");
        this.icon = icon;
        setTooltip(tooltip);
    }

    /** Draws the button with the selected tab's outline. */
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        drawBackground(mouseX, mouseY, highlighted ? FlatStyle.SELECTED : FlatStyle.BORDER);
        FlatStyle.drawTexture(icon, xPosition + 1, yPosition + 1, ICON_SIZE, enabled ? 1.0f : 0.5f);
    }
}
