package net.celsiuss.gtterminaltabs.client;

import net.minecraft.client.Minecraft;

/** A tab at the top of a screen that switches between its pages, outlined like the selected tab when it is current. */
public class PageButton extends TooltipButton {

    private final boolean current;

    public PageButton(int id, int x, int y, int width, int height, String label, boolean current) {
        super(id, x, y, width, height, label);
        this.current = current;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        drawBackground(mouseX, mouseY, current ? FlatStyle.SELECTED : FlatStyle.BORDER);
        drawLabel(mc);
    }

    @Override
    protected int textColor() {
        return current ? FlatStyle.TEXT_BRIGHT : super.textColor();
    }
}
