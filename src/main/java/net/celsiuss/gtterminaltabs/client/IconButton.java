package net.celsiuss.gtterminaltabs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

/** A square button showing a 16x16 icon on the same flat background as the tab rows. */
public class IconButton extends TooltipButton {

    private static final int ICON_SIZE = ItemIcons.ICON_SIZE;
    public static final int SIZE = ItemIcons.SLOT_SIZE;

    private final ResourceLocation icon;

    public IconButton(int id, int x, int y, ResourceLocation icon, String tooltip) {
        super(id, x, y, SIZE, SIZE, "");
        this.icon = icon;
        setTooltip(tooltip);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!visible) return;
        drawBackground(mouseX, mouseY, FlatStyle.BORDER);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, enabled ? 1.0f : 0.5f);
        mc.getTextureManager()
            .bindTexture(icon);
        func_146110_a(xPosition + 1, yPosition + 1, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
