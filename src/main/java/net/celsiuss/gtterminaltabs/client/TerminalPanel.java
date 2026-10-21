package net.celsiuss.gtterminaltabs.client;

import java.util.ArrayList;
import java.util.List;

import net.celsiuss.gtterminaltabs.GtTerminalTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;

/**
 * The tab panel beside a terminal: where it sits, the widgets it adds to the terminal's buttonList, and what a point
 * on screen hits. Input and the actions behind the buttons live in {@link TerminalOverlayHandler}.
 */
public class TerminalPanel {

    public static final int SETTINGS_BUTTON_ID = 0x47540100;
    public static final int VIEW_BUTTON_ID = 0x47540101;
    public static final int HIDE_BUTTON_ID = 0x47540102;
    public static final int SHOW_ALL_BUTTON_ID = 0x47540103;
    private static final int PANEL_BUTTON_ID = 0x47540200;
    private static final int TAB_BUTTON_ID_BASE = 0x47540000;

    private static final ResourceLocation SETTINGS_ICON = new ResourceLocation(
        GtTerminalTabs.MODID,
        "textures/gui/settings.png");
    private static final ResourceLocation GRID_ICON = new ResourceLocation(
        GtTerminalTabs.MODID,
        "textures/gui/grid.png");
    private static final ResourceLocation LIST_ICON = new ResourceLocation(
        GtTerminalTabs.MODID,
        "textures/gui/list.png");
    private static final ResourceLocation HIDE_ICON = new ResourceLocation(
        GtTerminalTabs.MODID,
        "textures/gui/hide.png");
    private static final ResourceLocation SHOW_ICON = new ResourceLocation(
        GtTerminalTabs.MODID,
        "textures/gui/show.png");
    private static final ResourceLocation SHOW_ALL_ICON = new ResourceLocation(
        GtTerminalTabs.MODID,
        "textures/gui/showall.png");

    private static final int PANEL_WIDTH = 109;
    private static final int PANEL_HEIGHT = 118;
    private static final int PANEL_PADDING = 4;
    private static final int PANEL_GAP = 2;
    // AE2's view cell artwork starts this far into the column it reserves beside the terminal.
    private static final int VIEW_CELL_ART_INSET = 3;
    // AE2's column, measured from its view cell slots: the panel's left edge sits this far left of the slots, and the
    // artwork ends this far below the last one. Used for terminals that only have the slots to go by.
    private static final int VIEW_CELL_SLOT_INSET = 8;
    private static final int VIEW_CELL_ART_BELOW = 8;
    private static final int SLOT_SIZE = 16;
    private static final int MAX_PROBE = 256;

    private final TabStore store;
    private final ScrollableButtonList tabList;
    private final List<GuiButton> ownButtons = new ArrayList<>();
    // The terminal's own buttonList, so the panel can be rebuilt without reinitialising the whole terminal.
    private List<GuiButton> screenButtons;
    private IconButton showAllButton;
    private IconButton showTabsButton;
    private boolean relayoutPending;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public TerminalPanel(TabStore store) {
        this.store = store;
        this.tabList = new ScrollableButtonList(TAB_BUTTON_ID_BASE, store.entries());
    }

    /** Builds the panel beside the terminal and adds its widgets to the terminal's freshly cleared buttonList. */
    public void attach(Terminal terminal, List<GuiButton> buttonList) {
        ownButtons.clear();
        relayoutPending = false;
        screenButtons = buttonList;
        // Left-aligned with the view cell column and just below it, or at the top when there is none.
        Slot[] cells = terminal.viewCellSlots();
        if (cells != null) {
            panelX = terminal.left() + cells[0].xDisplayPosition - VIEW_CELL_SLOT_INSET;
            panelY = terminal.top() + cells[1].yDisplayPosition + SLOT_SIZE + VIEW_CELL_ART_BELOW + PANEL_GAP;
        } else {
            panelX = terminal.left() + terminal.xSize() + VIEW_CELL_ART_INSET;
            int columnBottom = findViewCellBottom(terminal);
            panelY = columnBottom > terminal.top() ? columnBottom + PANEL_GAP : terminal.top();
        }
        build();
        screenButtons.addAll(ownButtons);
    }

    /**
     * Whether the screen dropped the panel's widgets. The Wireless Crafting Terminal rebuilds itself a tick after
     * opening, and on some of its buttons, by clearing its buttonList and calling initGui without Forge's events.
     */
    public boolean lostWidgets() {
        return isAttached() && !ownButtons.isEmpty() && !screenButtons.containsAll(ownButtons);
    }

    /** Builds the panel again into the same buttonList, for a screen that dropped it. */
    public void reattach(Terminal terminal) {
        List<GuiButton> buttonList = screenButtons;
        buttonList.removeAll(ownButtons);
        attach(terminal, buttonList);
    }

    public void detach() {
        ownButtons.clear();
        relayoutPending = false;
        screenButtons = null;
    }

    public boolean isAttached() {
        return screenButtons != null;
    }

    /** Rebuilds the panel at the next {@link #update}, so it never changes in the middle of a click. */
    public void requestRelayout() {
        relayoutPending = true;
    }

    /** Applies a pending rebuild and highlights the active category. */
    public void update() {
        if (!isAttached()) return;
        if (relayoutPending) {
            relayoutPending = false;
            screenButtons.removeAll(ownButtons);
            ownButtons.clear();
            build();
            screenButtons.addAll(ownButtons);
        }
        TabEntry active = CategoryFilter.active();
        tabList.highlight(
            store.entries()
                .indexOf(active));
        if (showAllButton != null) showAllButton.enabled = active != null;
        if (showTabsButton != null) {
            // Collapsed, this button is all that's left to say the grid is filtered.
            showTabsButton.setHighlighted(active != null);
            showTabsButton.setTooltip(
                active == null ? GtGuiText.ShowTabs.getLocal()
                    : String.format(GtGuiText.ShowTabsFiltered.getLocal(), CategoryFilter.displayName(active)));
        }
    }

    private void build() {
        showAllButton = null;
        showTabsButton = null;
        if (store.isHidden()) {
            panelWidth = IconButton.SIZE;
            panelHeight = IconButton.SIZE;
            showTabsButton = new IconButton(HIDE_BUTTON_ID, panelX, panelY, SHOW_ICON, GtGuiText.ShowTabs.getLocal());
            ownButtons.add(showTabsButton);
            return;
        }
        panelWidth = PANEL_WIDTH;
        panelHeight = PANEL_HEIGHT;

        // Order matters: buttons draw in list order, so the background must come first.
        int footerTop = panelY + PANEL_HEIGHT - PANEL_PADDING - IconButton.SIZE;
        ownButtons.add(
            new PanelBackground(
                PANEL_BUTTON_ID,
                panelX,
                panelY,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                footerTop - PANEL_PADDING / 2 - 1));

        int innerWidth = PANEL_WIDTH - 2 * PANEL_PADDING;
        int listHeight = PANEL_HEIGHT - 3 * PANEL_PADDING - IconButton.SIZE;
        tabList.setGrid(store.isGrid());
        ownButtons.addAll(tabList.layout(panelX + PANEL_PADDING, panelY + PANEL_PADDING, innerWidth, listHeight));
        if (store.entries()
            .isEmpty()) {
            ownButtons.add(
                new HintLabel(
                    panelX + PANEL_PADDING,
                    panelY + PANEL_PADDING,
                    innerWidth,
                    listHeight,
                    GtGuiText.PanelEmpty.getLocal()));
        }

        ownButtons.add(
            new IconButton(
                SETTINGS_BUTTON_ID,
                panelX + PANEL_WIDTH - PANEL_PADDING - IconButton.SIZE,
                footerTop,
                SETTINGS_ICON,
                GtGuiText.Settings.getLocal()));
        boolean grid = store.isGrid();
        ownButtons.add(
            new IconButton(
                VIEW_BUTTON_ID,
                panelX + PANEL_WIDTH - 2 * PANEL_PADDING - 2 * IconButton.SIZE,
                footerTop,
                grid ? LIST_ICON : GRID_ICON,
                (grid ? GtGuiText.ListView : GtGuiText.GridView).getLocal()));
        ownButtons.add(
            new IconButton(
                HIDE_BUTTON_ID,
                panelX + PANEL_PADDING,
                footerTop,
                HIDE_ICON,
                GtGuiText.HideTabs.getLocal()));
        showAllButton = new IconButton(
            SHOW_ALL_BUTTON_ID,
            panelX + 2 * PANEL_PADDING + IconButton.SIZE,
            footerTop,
            SHOW_ALL_ICON,
            GtGuiText.ShowAll.getLocal());
        showAllButton.enabled = CategoryFilter.active() != null;
        ownButtons.add(showAllButton);
    }

    /** Hides the panel's tooltips, which AE2 would draw under an open menu, sticking out around it. */
    public void setTooltipsHidden(boolean hidden) {
        for (GuiButton button : ownButtons) {
            if (button instanceof TooltipButton) ((TooltipButton) button).setTooltipHidden(hidden);
        }
    }

    public boolean owns(GuiButton button) {
        return ownButtons.contains(button);
    }

    public GuiButton buttonAt(int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getMinecraft();
        for (GuiButton button : ownButtons) {
            if (button.mousePressed(mc, mouseX, mouseY)) return button;
        }
        return null;
    }

    /** Returns the tab behind the button, or null if it isn't a tab. */
    public TabEntry entryFor(GuiButton button) {
        int index = tabList.entryIndexOf(button);
        List<TabEntry> entries = store.entries();
        return index >= 0 && index < entries.size() ? entries.get(index) : null;
    }

    public boolean contains(int mouseX, int mouseY) {
        return isAttached() && mouseX >= panelX
            && mouseX < panelX + panelWidth
            && mouseY >= panelY
            && mouseY < panelY + panelHeight;
    }

    public boolean overlaps(int x, int y, int w, int h) {
        return isAttached() && x < panelX + panelWidth && x + w > panelX && y < panelY + panelHeight && y + h > panelY;
    }

    public boolean listContains(int mouseX, int mouseY) {
        return listShown() && tabList.contains(mouseX, mouseY);
    }

    /** @param notches wheel delta in notches; positive scrolls up */
    public void scrollList(int notches) {
        tabList.scroll(notches);
    }

    /** Returns true if the click landed on the list's scrollbar and was handled. */
    public boolean listMousePressed(int mouseX, int mouseY) {
        return listShown() && tabList.mousePressed(mouseX, mouseY);
    }

    public void listMouseDragged(int mouseY) {
        if (listShown()) tabList.mouseDragged(mouseY);
    }

    public void listMouseReleased() {
        tabList.mouseReleased();
    }

    /** The list keeps its last layout while hidden, so its bounds must not be trusted then. */
    private boolean listShown() {
        return isAttached() && !store.isHidden();
    }

    /**
     * Asks the terminal itself how far down the view cell column beside it reaches, using the same area it reports
     * to NEI rather than guessing from slot positions. Returns guiTop when there is no column.
     */
    private static int findViewCellBottom(Terminal terminal) {
        int probeX = terminal.left() + terminal.xSize();
        int bottom = terminal.top();
        int limit = bottom + MAX_PROBE;
        while (bottom < limit && terminal.hidesNeiArea(probeX, bottom, 1, 1)) {
            bottom++;
        }
        return bottom;
    }
}
