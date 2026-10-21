package net.celsiuss.gtterminaltabs.client;

import net.minecraft.client.Minecraft;

/** A full-width row with a checkbox and a label, for one on/off setting. The tooltip carries its description. */
public class CheckboxButton extends TooltipButton {

    private static final int BOX_SIZE = 10;
    private static final int BOX_INSET = 4;
    private static final int LABEL_GAP = 5;
    // A check mark on the box's 8px interior, as pixel rows of one or two inclusive {left, right} spans.
    private static final int[][] CHECK = { { 6, 7 }, { 5, 6 }, { 0, 1, 4, 5 }, { 1, 2, 3, 4 }, { 2, 3 } };

    private boolean checked;

    public CheckboxButton(int id, int x, int y, int width, int height, String label, String tooltip, boolean checked) {
        super(id, x, y, width, height, label);
        this.checked = checked;
        setTooltip(tooltip);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        drawBackground(mouseX, mouseY, FlatStyle.BORDER);
        int boxLeft = xPosition + BOX_INSET;
        int boxTop = yPosition + (height - BOX_SIZE) / 2;
        FlatStyle.drawBox(
            boxLeft,
            boxTop,
            boxLeft + BOX_SIZE,
            boxTop + BOX_SIZE,
            FlatStyle.TRACK,
            checked ? FlatStyle.SELECTED : FlatStyle.BORDER);
        if (checked) {
            int color = FlatStyle.opaque(FlatStyle.TEXT_BRIGHT);
            int markTop = boxTop + 1 + (BOX_SIZE - 2 - CHECK.length) / 2;
            for (int row = 0; row < CHECK.length; row++) {
                int[] spans = CHECK[row];
                for (int i = 0; i < spans.length; i += 2) {
                    drawRect(
                        boxLeft + 1 + spans[i],
                        markTop + row,
                        boxLeft + 2 + spans[i + 1],
                        markTop + row + 1,
                        color);
                }
            }
        }
        drawLabel(mc);
    }

    @Override
    protected int labelLeft() {
        return xPosition + BOX_INSET + BOX_SIZE + LABEL_GAP;
    }

    @Override
    protected boolean centerLabel() {
        return false;
    }
}
