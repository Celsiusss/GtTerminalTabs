package net.celsiuss.gtterminaltabs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/** The flat, translucent look shared by the tab panel and its widgets, so it reads the same over any terminal. */
public final class FlatStyle {

    public static final int PANEL_FILL = 0xD8181818;
    public static final int PANEL_BORDER = 0x50FFFFFF;
    public static final int DIVIDER = 0x20FFFFFF;

    public static final int FILL = 0x28FFFFFF;
    public static final int FILL_HOVER = 0x48FFFFFF;
    public static final int FILL_DISABLED = 0x14FFFFFF;
    public static final int BORDER = 0x30FFFFFF;
    // AE2's highlight for the selected crafting CPU.
    public static final int SELECTED = 0xFF00D5FF;

    public static final int TRACK = 0x30000000;
    public static final int KNOB = 0x60FFFFFF;
    public static final int KNOB_ACTIVE = 0xA0FFFFFF;
    public static final int KNOB_DISABLED = 0x20FFFFFF;

    public static final int TEXT_BRIGHT = 0xFFFFFF;
    public static final int TEXT = 0xE0E0E0;
    public static final int TEXT_HOVER = 0xFFFFA0;
    public static final int TEXT_DISABLED = 0xA0A0A0;
    // For actions that delete something.
    public static final int TEXT_DANGER = 0xFF5555;
    public static final int TEXT_DANGER_HOVER = 0xFF8888;
    public static final int FILL_DANGER_HOVER = 0x48FF4040;
    public static final int SLOT_HOVER = 0x80FFFFFF;

    private FlatStyle() {}

    /** The text colours have no alpha, which drawRect would treat as fully transparent. */
    public static int opaque(int rgb) {
        return 0xFF000000 | rgb;
    }

    public static void drawOutline(int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, top + 1, color);
        Gui.drawRect(left, bottom - 1, right, bottom, color);
        Gui.drawRect(left, top + 1, left + 1, bottom - 1, color);
        Gui.drawRect(right - 1, top + 1, right, bottom - 1, color);
    }

    /** A translucent box with a 1px outline. */
    public static void drawBox(int left, int top, int right, int bottom, int fill, int border) {
        Gui.drawRect(left + 1, top + 1, right - 1, bottom - 1, fill);
        drawOutline(left, top, right, bottom, border);
    }

    /** Draws a square texture pixel for pixel, faded to the given alpha. */
    public static void drawTexture(ResourceLocation texture, int x, int y, int size, float alpha) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, alpha);
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(texture);
        Gui.func_146110_a(x, y, 0, 0, size, size, size, size);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /** The click a vanilla button makes, for widgets that aren't buttons. */
    public static void playClick() {
        Minecraft.getMinecraft()
            .getSoundHandler()
            .playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
    }
}
