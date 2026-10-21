package net.celsiuss.gtterminaltabs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;

/**
 * A flat button showing a small symbol drawn from rectangles. Font glyphs for arrows come from a different Unicode
 * page than + and -, so they render thinner and at a different size.
 */
public class GlyphButton extends TooltipButton {

    public enum Glyph {
        PLUS,
        MINUS,
        UP,
        DOWN
    }

    private static final int ARM = 4;
    private static final int STROKE = 2;
    private static final int ARROW_ROWS = 4;

    private final Glyph glyph;

    public GlyphButton(int id, int x, int y, int size, Glyph glyph, String tooltip) {
        super(id, x, y, size, size, "");
        this.glyph = glyph;
        setTooltip(tooltip);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        drawBackground(mouseX, mouseY, FlatStyle.BORDER);
        int color = FlatStyle.opaque(textColor());
        int cx = xPosition + width / 2;
        int cy = yPosition + height / 2;
        switch (glyph) {
            case PLUS:
                Gui.drawRect(cx - 1, cy - ARM, cx + 1, cy + ARM, color);
                Gui.drawRect(cx - ARM, cy - 1, cx + ARM, cy + 1, color);
                break;
            case MINUS:
                Gui.drawRect(cx - ARM, cy - 1, cx + ARM, cy + 1, color);
                break;
            case UP:
            case DOWN:
                int top = cy - ARROW_ROWS / 2;
                for (int row = 0; row < ARROW_ROWS; row++) {
                    int half = 1 + (glyph == Glyph.UP ? row : ARROW_ROWS - 1 - row);
                    Gui.drawRect(cx - half, top + row, cx + half, top + row + 1, color);
                }
                break;
            default:
                break;
        }
    }
}
