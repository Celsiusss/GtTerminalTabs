package net.celsiuss.gtterminaltabs.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.item.ItemStack;

import appeng.client.gui.widgets.GuiScrollbar;

/**
 * A vertical list of full-width buttons showing a window of {@code entries} by name, or in grid mode a grid of
 * square icon-only buttons, with AE2's scrollbar on the right. Only the visible cells exist as buttons; scrolling
 * relabels them, so button ids map to cells, not entries. All widgets are buttons, so the screen draws them in
 * its normal button pass, under slots and tooltips.
 */
public class ScrollableButtonList {

    // Each list row's height; grid cells are square at this size, stretched a little to fill the width.
    private static final int CELL_SIZE = 20;
    private static final int CELL_GAP = 2;
    private static final int ICON_INSET = 3;
    private static final int ICON_SIZE = ItemIcons.ICON_SIZE;
    private static final int LABEL_GAP = 4;
    private static final int SCROLLBAR_GAP = 3;
    private static final int KNOB_HEIGHT = 15;
    private static final int KNOB_INSET = 2;

    private final int buttonIdBase;
    private final List<TabEntry> entries;
    private final List<GuiButton> cellButtons = new ArrayList<>();
    // Kept across layouts so the scroll position survives resizes and terminal reinitialisation.
    private final GuiScrollbar scrollbar = new GuiScrollbar();

    private int x;
    private int y;
    private int width;
    private int height;
    private int selected = -1;
    private boolean grid;
    private int columns = 1;
    private int visibleRows;
    private boolean scrollable;
    // Tracked here because GuiScrollbar.release() only exists in newer AE2 builds than GTNH 2.8.4's.
    private boolean dragging;

    public ScrollableButtonList(int buttonIdBase, List<TabEntry> entries) {
        this.buttonIdBase = buttonIdBase;
        this.entries = entries;
    }

    /** Takes effect on the next {@link #layout}. */
    public void setGrid(boolean grid) {
        this.grid = grid;
    }

    /**
     * Recreates the widgets for the given bounds, in draw order. Call again after adding or removing entries or
     * switching modes.
     */
    public List<GuiButton> layout(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        int scrolledWidth = width - scrollbar.getWidth() - SCROLLBAR_GAP;
        int fitRows = (height + CELL_GAP) / (CELL_SIZE + CELL_GAP);
        // Count columns as if the scrollbar were showing, so the grid keeps its shape as tabs are added.
        columns = grid ? fitColumns(scrolledWidth) : 1;
        visibleRows = Math.max(0, Math.min(fitRows, totalRows()));
        scrollable = totalRows() > visibleRows;
        // Columns share the full width, so grid cells stretch a little rather than leave a gap by the scrollbar.
        int pitch = (scrollable ? scrolledWidth : width) + CELL_GAP;

        List<GuiButton> widgets = new ArrayList<>();
        cellButtons.clear();
        for (int row = 0; row < visibleRows; row++) {
            int cellY = y + row * (CELL_SIZE + CELL_GAP);
            for (int column = 0; column < columns; column++) {
                int cell = row * columns + column;
                int cellX = x + column * pitch / columns;
                int cellWidth = x + (column + 1) * pitch / columns - CELL_GAP - cellX;
                cellButtons.add(new CellButton(buttonIdBase + cell, cellX, cellY, cellWidth, CELL_SIZE, cell));
            }
        }
        widgets.addAll(cellButtons);

        scrollbar.setLeft(x + width - scrollbar.getWidth())
            .setTop(y)
            .setHeight(visibleRows * (CELL_SIZE + CELL_GAP) - CELL_GAP);
        scrollbar.setRange(0, totalRows() - visibleRows, 1);
        if (scrollable) widgets.add(new ScrollbarWidget());

        syncLabels();
        return widgets;
    }

    /** Returns the entry index for one of this list's buttons, or -1 if the button isn't ours. */
    public int entryIndexOf(GuiButton button) {
        int cell = cellButtons.indexOf(button);
        return cell < 0 ? -1 : firstVisible() + cell;
    }

    public int getSelected() {
        return selected;
    }

    /** Highlights the entry, scrolling it into view; -1 clears the selection. */
    public void setSelected(int index) {
        selected = index;
        if (index < 0 || cellButtons.isEmpty()) return;
        int row = index / columns;
        int first = scrollbar.getCurrentScroll();
        if (row < first) scrollbar.setCurrentScroll(row);
        else if (row >= first + visibleRows) scrollbar.setCurrentScroll(row - visibleRows + 1);
        syncLabels();
    }

    /** Like {@link #setSelected} but leaves the scroll position alone, for highlights driven every frame. */
    public void highlight(int index) {
        selected = index;
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    /** @param notches wheel delta in notches; positive scrolls up */
    public void scroll(int notches) {
        // Not GuiScrollbar.wheel(), which clamps to one row per event and drops fast scrolls.
        scrollbar.setCurrentScroll(scrollbar.getCurrentScroll() - notches);
        syncLabels();
    }

    /** Returns true if the click landed on the scrollbar and was handled. */
    public boolean mousePressed(int mouseX, int mouseY) {
        if (!scrollable || !scrollbar.contains(mouseX, mouseY)) return false;
        scrollbar.click(null, mouseX, mouseY);
        dragging = true;
        syncLabels();
        return true;
    }

    public void mouseDragged(int mouseY) {
        if (!dragging) return;
        scrollbar.clickMove(mouseY);
        syncLabels();
    }

    public void mouseReleased() {
        dragging = false;
    }

    /** Re-reads entry names into the cell buttons, e.g. after an entry was renamed. */
    public void syncLabels() {
        int offset = firstVisible();
        for (int cell = 0; cell < cellButtons.size(); cell++) {
            int index = offset + cell;
            GuiButton button = cellButtons.get(cell);
            // Only the grid's last row can run past the end.
            button.visible = index < entries.size();
            button.displayString = button.visible ? entries.get(index).name : "";
        }
    }

    private int firstVisible() {
        return scrollbar.getCurrentScroll() * columns;
    }

    private int totalRows() {
        return (entries.size() + columns - 1) / columns;
    }

    private static int fitColumns(int width) {
        return Math.max(1, (width + CELL_GAP) / (CELL_SIZE + CELL_GAP));
    }

    private boolean anyIcon() {
        for (TabEntry entry : entries) {
            if (entry.icon != null) return true;
        }
        return false;
    }

    private class CellButton extends TooltipButton {

        private final int cell;

        CellButton(int id, int x, int y, int width, int height, int cell) {
            super(id, x, y, width, height, "");
            this.cell = cell;
        }

        @Override
        protected int labelLeft() {
            // Reserve the icon column on every row once any entry has an icon, so labels line up.
            return anyIcon() ? xPosition + ICON_INSET + ICON_SIZE + LABEL_GAP : xPosition + LABEL_GAP;
        }

        @Override
        protected boolean centerLabel() {
            return false;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) return;
            boolean isSelected = selected == firstVisible() + cell;
            drawBackground(mouseX, mouseY, isSelected ? FlatStyle.SELECTED : FlatStyle.BORDER);
            int iconY = yPosition + (height - ICON_SIZE) / 2;
            if (!grid) {
                ItemIcons.draw(mc, icon(), xPosition + ICON_INSET, iconY);
                drawLabel(mc);
                return;
            }
            ItemIcons.draw(mc, icon(), xPosition + (width - ICON_SIZE) / 2, iconY);
            if (icon() == null && !displayString.isEmpty()) {
                // Iconless tabs would be blank squares, so they show their initial instead.
                String initial = displayString.substring(0, displayString.offsetByCodePoints(0, 1));
                drawCenteredString(
                    mc.fontRenderer,
                    initial,
                    xPosition + width / 2,
                    yPosition + (height - mc.fontRenderer.FONT_HEIGHT) / 2,
                    FlatStyle.TEXT);
            }
        }

        @Override
        public String getMessage() {
            return grid ? displayString : super.getMessage();
        }

        private ItemStack icon() {
            int index = firstVisible() + cell;
            if (index >= entries.size()) return null;
            TabIcon icon = entries.get(index).icon;
            return icon == null ? null : icon.toStack();
        }
    }

    /**
     * Draws the track and knob; GuiScrollbar.draw needs an AEBaseGui. Disabled, since input arrives through
     * {@link #mousePressed} and friends.
     */
    private class ScrollbarWidget extends GuiButton {

        ScrollbarWidget() {
            super(-1, scrollbar.getLeft(), scrollbar.getTop(), scrollbar.getWidth(), scrollbar.getHeight(), "");
            this.enabled = false;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            int right = xPosition + width;
            FlatStyle.drawBox(xPosition, yPosition, right, yPosition + height, FlatStyle.TRACK, FlatStyle.BORDER);
            int range = Math.max(1, totalRows() - visibleRows);
            int travel = height - 2 * KNOB_INSET - KNOB_HEIGHT;
            int knobTop = yPosition + KNOB_INSET + scrollbar.getCurrentScroll() * travel / range;
            int knobBottom = knobTop + KNOB_HEIGHT;
            boolean hovered = mouseX >= xPosition && mouseX < right && mouseY >= knobTop && mouseY < knobBottom;
            drawRect(
                xPosition + KNOB_INSET,
                knobTop,
                right - KNOB_INSET,
                knobBottom,
                hovered ? FlatStyle.KNOB_ACTIVE : FlatStyle.KNOB);
        }
    }
}
