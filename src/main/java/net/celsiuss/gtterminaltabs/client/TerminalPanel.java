package net.celsiuss.gtterminaltabs.client;

import java.util.ArrayList;
import java.util.List;

import net.celsiuss.gtterminaltabs.GtTerminalTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.ResourceLocation;

import appeng.client.gui.implementations.GuiMEMonitorable;

/**
 * The tab panel beside a terminal: where it sits, the widgets it adds to the terminal's buttonList, and what a point
 * on screen hits. Input and the actions behind the buttons live in {@link TerminalOverlayHandler}.
 */
public class TerminalPanel {

    public static final int SETTINGS_BUTTON_ID = 0x47540100;
    public static final int VIEW_BUTTON_ID = 0x47540101;
    public static final int HIDE_BUTTON_ID = 0x47540102;
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

    private static final int PANEL_WIDTH = 109;
    private static final int PANEL_HEIGHT = 118;
    private static final int PANEL_PADDING = 4;
    private static final int PANEL_GAP = 2;
    // AE2's view cell artwork starts this far into the column it reserves beside the terminal.
    private static final int VIEW_CELL_ART_INSET = 3;
    private static final int MAX_PROBE = 256;

    private final TabStore store;
    private final ScrollableButtonList tabList;
    private final List<GuiButton> ownButtons = new ArrayList<>();
    // The terminal's own buttonList, so the panel can be rebuilt without reinitialising the whole terminal.
    private List<GuiButton> screenButtons;
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
    public void attach(GuiMEMonitorable terminal, List<GuiButton> buttonList) {
        ownButtons.clear();
        relayoutPending = false;
        screenButtons = buttonList;
        // Left-aligned with the view cell column and just below it, or at the top when there is none.
        panelX = terminal.getGuiLeft() + terminal.getXSize() + VIEW_CELL_ART_INSET;
        int columnBottom = findViewCellBottom(terminal);
        panelY = columnBottom > terminal.getGuiTop() ? columnBottom + PANEL_GAP : terminal.getGuiTop();
        build();
        screenButtons.addAll(ownButtons);
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

    /** Applies a pending rebuild and highlights the tab whose search matches the terminal's. */
    public void update(String search) {
        if (!isAttached()) return;
        if (relayoutPending) {
            relayoutPending = false;
            screenButtons.removeAll(ownButtons);
            ownButtons.clear();
            build();
            screenButtons.addAll(ownButtons);
        }
        tabList.highlight(indexOfSearch(search));
    }

    private int indexOfSearch(String search) {
        if (search == null) return -1;
        List<TabEntry> entries = store.entries();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).value.equals(search)) return i;
        }
        return -1;
    }

    private void build() {
        if (store.isHidden()) {
            panelWidth = IconButton.SIZE;
            panelHeight = IconButton.SIZE;
            ownButtons.add(new IconButton(HIDE_BUTTON_ID, panelX, panelY, SHOW_ICON, GtGuiText.ShowTabs.getLocal()));
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

    /** Returns the search string of the tab behind the button, or null if it isn't a tab. */
    public String searchFor(GuiButton button) {
        int index = tabList.entryIndexOf(button);
        List<TabEntry> entries = store.entries();
        return index >= 0 && index < entries.size() ? entries.get(index).value : null;
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
    private static int findViewCellBottom(GuiMEMonitorable terminal) {
        int probeX = terminal.getGuiLeft() + terminal.getXSize();
        int bottom = terminal.getGuiTop();
        int limit = bottom + MAX_PROBE;
        while (bottom < limit && terminal.hideItemPanelSlot(probeX, bottom, 1, 1)) {
            bottom++;
        }
        return bottom;
    }
}
