package net.celsiuss.gtterminaltabs.client;

import java.util.Map;
import java.util.WeakHashMap;

import net.celsiuss.gtterminaltabs.GtTerminalTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import appeng.client.gui.widgets.MEGuiTextField;

/**
 * Shows the active category on the terminal's search box, so a forgotten category can't pass for missing items: a
 * badge at the end of the box with the tab's icon shrunk to fit, or a funnel for a tab without one, and a tooltip
 * naming the category. Hovering the badge turns it into an x that clears the category.
 */
public final class SearchBoxIndicator {

    // Fits inside the box's border; it overlaps the text area by a pixel, in the space AE2 keeps for the caret.
    private static final int BADGE_SIZE = 8;
    private static final int BADGE_INSET = 1;
    private static final ResourceLocation FILTER_ICON = new ResourceLocation(
        GtTerminalTabs.MODID,
        "textures/gui/filter_small.png");
    private static final int FILTER_ICON_SIZE = 8;

    // AE2 sets each box's tooltip once, so it is kept here while the category's tooltip replaces it.
    private static final Map<MEGuiTextField, String> originalTooltips = new WeakHashMap<>();

    private SearchBoxIndicator() {}

    /** Names the category in the search box's tooltip while one is active, and puts AE2's back afterwards. */
    public static void updateTooltip(Terminal terminal) {
        MEGuiTextField box = terminal.searchBox();
        if (box == null) return;
        TabEntry active = CategoryFilter.active();
        if (active == null) {
            String original = originalTooltips.remove(box);
            if (original != null) box.setMessage(original);
            return;
        }
        if (!originalTooltips.containsKey(box)) originalTooltips.put(box, box.getMessage());
        box.setMessage(
            categoryTooltip(
                active,
                box.getText()
                    .isEmpty()));
    }

    /**
     * AE2 draws the first line as a title with a gap under it and greys the rest. The title says what the name is, so
     * it doesn't read as the search box's own name; the hints only list the right-clicks that do something right now.
     */
    private static String categoryTooltip(TabEntry active, boolean searchEmpty) {
        String name = EnumChatFormatting.AQUA + CategoryFilter.displayName(active) + EnumChatFormatting.RESET;
        StringBuilder tooltip = new StringBuilder().append(String.format(GtGuiText.ActiveCategory.getLocal(), name))
            .append('\n');
        if (searchEmpty) {
            tooltip.append(GtGuiText.HintShowAll.getLocal());
        } else {
            tooltip.append(GtGuiText.HintClearSearch.getLocal())
                .append('\n')
                .append(GtGuiText.HintShowAllAgain.getLocal());
        }
        return tooltip.toString();
    }

    public static void draw(Terminal terminal, int mouseX, int mouseY) {
        TabEntry active = CategoryFilter.active();
        MEGuiTextField box = terminal.searchBox();
        if (active == null || box == null || !box.isVisible()) return;
        badge(box).draw(active, mouseX, mouseY);
    }

    private static Badge badge(MEGuiTextField box) {
        return new Badge(box.x, box.y, box.w, box.h, BADGE_SIZE, BADGE_INSET);
    }

    /**
     * The category's badge at the end of a search box, shared with NEI's: the tab's icon, or the x that clears the
     * category while hovered. It takes clicks over the box's full height, not just over the icon.
     */
    static final class Badge {

        private final int left;
        private final int top;
        private final int size;
        private final int boxTop;
        private final int boxBottom;

        /** Centred in a box at x, y of width w and height h, {@code inset} pixels in from its right edge. */
        Badge(int x, int y, int w, int h, int size, int inset) {
            this.size = size;
            this.left = x + w - inset - size;
            this.top = y + (h - size) / 2;
            this.boxTop = y;
            this.boxBottom = y + h;
        }

        boolean contains(int mouseX, int mouseY) {
            return mouseX >= left && mouseX < left + size && mouseY >= boxTop && mouseY < boxBottom;
        }

        void draw(TabEntry active, int mouseX, int mouseY) {
            if (contains(mouseX, mouseY)) {
                drawCross(left, top, size, FlatStyle.opaque(FlatStyle.TEXT_HOVER));
                return;
            }
            ItemStack icon = active.icon == null ? null : active.icon.toStack();
            if (icon != null) {
                ItemIcons.drawScaled(Minecraft.getMinecraft(), icon, left, top, size);
                return;
            }
            // Drawn pixel for pixel: scaled, a funnel this small loses rows unevenly.
            int offset = (size - FILTER_ICON_SIZE) / 2;
            FlatStyle.drawTexture(FILTER_ICON, left + offset, top + offset, FILTER_ICON_SIZE, 1.0f);
        }
    }

    private static void drawCross(int left, int top, int size, int color) {
        // Odd, so the two strokes cross on a single pixel.
        int cross = (size * 2 / 3) | 1;
        left += (size - cross) / 2;
        top += (size - cross) / 2;
        for (int i = 0; i < cross; i++) {
            Gui.drawRect(left + i, top + i, left + i + 1, top + i + 1, color);
            Gui.drawRect(left + cross - 1 - i, top + i, left + cross - i, top + i + 1, color);
        }
    }

    /**
     * Sees a click on a search box before AE2 does. Clicking the badge, or right-clicking the terminal's box once it is
     * already empty, clears the category. Returns true if the click was used.
     */
    public static boolean mouseClicked(MEGuiTextField box, int mouseX, int mouseY, int button) {
        if (CategoryFilter.active() == null || !box.isMouseIn(mouseX, mouseY)) return false;
        Terminal terminal = Terminal.of(Minecraft.getMinecraft().currentScreen);
        if (terminal == null || terminal.searchBox() != box) return false;

        boolean clear = button == 0 ? badge(box).contains(mouseX, mouseY)
            : button == 1 && box.getText()
                .isEmpty();
        if (!clear) return false;
        clearCategory(terminal);
        return true;
    }

    /** Clears the category from a badge, with the click sound a button would make. */
    public static void clearCategory(Terminal terminal) {
        FlatStyle.playClick();
        CategoryFilter.select(terminal, null);
    }
}
