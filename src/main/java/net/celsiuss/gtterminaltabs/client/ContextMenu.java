package net.celsiuss.gtterminaltabs.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

/**
 * A small flat menu that opens at the mouse over the terminal and NEI. Whoever opens it keeps it, and drops it to
 * close.
 */
public class ContextMenu {

    private static final int ROW_HEIGHT = 12;
    private static final int PADDING = 2;
    private static final int TEXT_PADDING = 6;
    private static final int ICON_SIZE = 7;
    private static final int ICON_GAP = 4;
    // A bin with a lid and three slats, as pixel rows of one or more inclusive {left, right} spans.
    private static final int[][] BIN = { { 2, 4 }, { 0, 6 }, { 1, 1, 3, 3, 5, 5 }, { 1, 1, 3, 3, 5, 5 },
        { 1, 1, 3, 3, 5, 5 }, { 1, 1, 3, 3, 5, 5 }, { 1, 5 } };

    private final List<Entry> entries = new ArrayList<>();
    private int x;
    private int y;
    private int width;
    private int height;

    public ContextMenu add(String label, boolean enabled, Runnable action) {
        entries.add(new Entry(label, enabled, action, false));
        return this;
    }

    /** Adds an entry that deletes something, in red with a bin beside it. */
    public ContextMenu addDestructive(String label, boolean enabled, Runnable action) {
        entries.add(new Entry(label, enabled, action, true));
        return this;
    }

    /** Places the menu at the point, moved left or up where it would run off the screen. */
    public ContextMenu open(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int textWidth = 0;
        for (Entry entry : entries) textWidth = Math.max(textWidth, font.getStringWidth(entry.label));
        width = textWidth + 2 * TEXT_PADDING + iconColumn();
        height = entries.size() * ROW_HEIGHT + 2 * PADDING;
        x = Math.max(0, Math.min(mouseX, screenWidth - width));
        y = Math.max(0, Math.min(mouseY, screenHeight - height));
        return this;
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** Runs the enabled entry under the point, if any. Returns true if one ran. */
    public boolean click(int mouseX, int mouseY) {
        Entry entry = entryAt(mouseX, mouseY);
        if (entry == null || !entry.enabled) return false;
        FlatStyle.playClick();
        entry.action.run();
        return true;
    }

    public void draw(int mouseX, int mouseY) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        FlatStyle.drawBox(x, y, x + width, y + height, FlatStyle.PANEL_FILL, FlatStyle.PANEL_BORDER);
        Entry hovered = entryAt(mouseX, mouseY);
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            int rowTop = y + PADDING + i * ROW_HEIGHT;
            boolean hover = entry == hovered && entry.enabled;
            int color;
            if (!entry.enabled) {
                color = FlatStyle.TEXT_DISABLED;
            } else if (entry.destructive) {
                color = hover ? FlatStyle.TEXT_DANGER_HOVER : FlatStyle.TEXT_DANGER;
            } else {
                color = hover ? FlatStyle.TEXT_HOVER : FlatStyle.TEXT;
            }
            if (hover) {
                int fill = entry.destructive ? FlatStyle.FILL_DANGER_HOVER : FlatStyle.FILL_HOVER;
                Gui.drawRect(x + 1, rowTop, x + width - 1, rowTop + ROW_HEIGHT, fill);
            }
            if (entry.destructive) drawBin(x + TEXT_PADDING, rowTop + (ROW_HEIGHT - BIN.length) / 2, color);
            font.drawStringWithShadow(
                entry.label,
                x + TEXT_PADDING + iconColumn(),
                rowTop + (ROW_HEIGHT - font.FONT_HEIGHT) / 2 + 1,
                color);
        }
    }

    private static void drawBin(int left, int top, int color) {
        color = FlatStyle.opaque(color);
        for (int row = 0; row < BIN.length; row++) {
            int[] spans = BIN[row];
            for (int i = 0; i < spans.length; i += 2) {
                Gui.drawRect(left + spans[i], top + row, left + spans[i + 1] + 1, top + row + 1, color);
            }
        }
    }

    /** Room for the icons, kept on every row once any entry has one, so the labels line up. */
    private int iconColumn() {
        for (Entry entry : entries) {
            if (entry.destructive) return ICON_SIZE + ICON_GAP;
        }
        return 0;
    }

    private Entry entryAt(int mouseX, int mouseY) {
        if (mouseX < x || mouseX >= x + width) return null;
        int row = Math.floorDiv(mouseY - y - PADDING, ROW_HEIGHT);
        return row >= 0 && row < entries.size() ? entries.get(row) : null;
    }

    private static final class Entry {

        final String label;
        final boolean enabled;
        final Runnable action;
        final boolean destructive;

        Entry(String label, boolean enabled, Runnable action, boolean destructive) {
            this.label = label;
            this.enabled = enabled;
            this.action = action;
            this.destructive = destructive;
        }
    }
}
