package net.celsiuss.gtterminaltabs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** Draws item icons from outside the slot pass, e.g. inside buttons, restoring the GL state the GUI expects. */
public final class ItemIcons {

    public static final int ICON_SIZE = 16;
    public static final int SLOT_SIZE = ICON_SIZE + 2;

    private static final RenderItem RENDER_ITEM = new RenderItem();

    private ItemIcons() {}

    public static void draw(Minecraft mc, ItemStack stack, int x, int y) {
        if (stack == null) return;
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        // Block items render inside out without depth testing, which GuiContainer turns off for the button pass.
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        RenderHelper.enableGUIStandardItemLighting();
        try {
            RENDER_ITEM.renderItemAndEffectIntoGUI(mc.fontRenderer, mc.getTextureManager(), stack, x, y);
        } catch (RuntimeException ignored) {
            // A broken item renderer must not take the whole screen down with it.
        } finally {
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            if (!depth) GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }
}
