package net.celsiuss.gtterminaltabs.client;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

/**
 * A text field drawn as a flat box instead of vanilla's black one. Vanilla's own background is turned off and the
 * field sits inset in the box, so the box's padding is translated into clicks on the text.
 */
public class FlatTextField extends GuiTextField {

    private static final int INSET = 4;

    private final int boxX;
    private final int boxY;
    private final int boxWidth;
    private final int boxHeight;
    private final int textWidth;

    public FlatTextField(FontRenderer font, int x, int y, int width, int height) {
        super(font, x + INSET, y + (height - font.FONT_HEIGHT + 1) / 2, width - 2 * INSET, font.FONT_HEIGHT);
        setEnableBackgroundDrawing(false);
        this.boxX = x;
        this.boxY = y;
        this.boxWidth = width;
        this.boxHeight = height;
        this.textWidth = width - 2 * INSET;
    }

    public int getBoxX() {
        return boxX;
    }

    public int getBoxY() {
        return boxY;
    }

    @Override
    public void drawTextBox() {
        if (!getVisible()) return;
        FlatStyle.drawBox(
            boxX,
            boxY,
            boxX + boxWidth,
            boxY + boxHeight,
            FlatStyle.TRACK,
            isFocused() ? FlatStyle.SELECTED : FlatStyle.BORDER);
        super.drawTextBox();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (mouseX >= boxX && mouseX < boxX + boxWidth && mouseY >= boxY && mouseY < boxY + boxHeight) {
            mouseX = Math.max(xPosition, Math.min(mouseX, xPosition + textWidth - 1));
            mouseY = yPosition;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }
}
