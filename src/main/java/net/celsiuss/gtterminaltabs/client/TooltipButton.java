package net.celsiuss.gtterminaltabs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;

import appeng.client.gui.widgets.ITooltip;

/**
 * A flat button that AE2 renders a tooltip for. Unless a tooltip is set, it shows the label, but only when the label
 * doesn't fit on the button. AE2 only draws these tooltips on its own screens; {@link GuiTabSettings} draws them
 * itself.
 */
public class TooltipButton extends GuiButton implements ITooltip {

    private static final int LABEL_PADDING = 4;

    private String tooltip;
    private boolean tooltipHidden;

    public TooltipButton(int id, int x, int y, int width, int height, String label) {
        super(id, x, y, width, height, label);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        drawBackground(mouseX, mouseY, FlatStyle.BORDER);
        drawLabel(mc);
    }

    /** Updates the hover state and draws the flat box every button in the mod sits on. */
    protected void drawBackground(int mouseX, int mouseY, int border) {
        field_146123_n = mouseX >= xPosition && mouseY >= yPosition
            && mouseX < xPosition + width
            && mouseY < yPosition + height;
        int fill = !enabled ? FlatStyle.FILL_DISABLED : field_146123_n ? FlatStyle.FILL_HOVER : FlatStyle.FILL;
        FlatStyle.drawBox(xPosition, yPosition, xPosition + width, yPosition + height, fill, border);
    }

    /** Draws the label centred in the label area, cut short with ".." if it doesn't fit; the tooltip has it all. */
    protected void drawLabel(Minecraft mc) {
        int color = packedFGColour != 0 ? packedFGColour : textColor();
        int labelLeft = labelLeft();
        int available = labelWidth();
        String label = displayString;
        if (mc.fontRenderer.getStringWidth(label) > available) {
            label = mc.fontRenderer.trimStringToWidth(label, available - mc.fontRenderer.getStringWidth("..")) + "..";
        }
        int labelY = yPosition + (height - mc.fontRenderer.FONT_HEIGHT) / 2;
        if (centerLabel()) {
            drawCenteredString(mc.fontRenderer, label, (labelLeft + xPosition + width) / 2, labelY, color);
        } else {
            drawString(mc.fontRenderer, label, labelLeft, labelY, color);
        }
    }

    /** The text colour for the button's current state; call after {@link #drawBackground} updates the hover. */
    protected int textColor() {
        if (!enabled) return FlatStyle.TEXT_DISABLED;
        return field_146123_n ? FlatStyle.TEXT_HOVER : FlatStyle.TEXT;
    }

    private int labelWidth() {
        return xPosition + width - labelLeft() - LABEL_PADDING;
    }

    /** Left edge of the area the label is centred in; subclasses move it to make room for an icon. */
    protected int labelLeft() {
        return xPosition;
    }

    protected boolean centerLabel() {
        return true;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    /** Hides the tooltip for now, such as while a menu is open over the button. */
    public void setTooltipHidden(boolean hidden) {
        this.tooltipHidden = hidden;
    }

    @Override
    public final String getMessage() {
        return tooltipHidden ? "" : message();
    }

    /** The tooltip, or an empty string for none. */
    protected String message() {
        if (tooltip != null) return tooltip;
        boolean truncated = Minecraft.getMinecraft().fontRenderer.getStringWidth(displayString) > labelWidth();
        return truncated ? displayString : "";
    }

    @Override
    public int xPos() {
        return xPosition;
    }

    @Override
    public int yPos() {
        return yPosition;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}
