package net.celsiuss.gtterminaltabs.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import appeng.api.events.GuiScrollEvent;
import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import codechicken.nei.guihook.IContainerDrawHandler;
import codechicken.nei.guihook.IContainerInputHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Connects the {@link TerminalPanel} to terminal screens through Forge, AE2 and NEI, and acts on its buttons. Every
 * {@link Terminal} gets the panel, AE2's own and the add-ons' alike.
 */
public class TerminalOverlayHandler implements INEIGuiHandler, IContainerInputHandler, IContainerDrawHandler {

    private final TabStore store;
    private final TerminalPanel panel;
    private final NeiCategoryFilter neiFilter;
    // The open menu, or null. Any click, scroll or Escape closes it.
    private ContextMenu menu;

    public TerminalOverlayHandler(TabStore store, NeiCategoryFilter neiFilter) {
        this.store = store;
        this.panel = new TerminalPanel(store);
        this.neiFilter = neiFilter;
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        // AE2 clears buttonList on every initGui and reinitalize(), so rebuild from scratch each time.
        Terminal terminal = Terminal.of(event.gui);
        menu = null;
        if (terminal == null) {
            panel.detach();
            // Includes the tab editor, where NEI's full item list is needed to pick icons.
            neiFilter.update(null);
            return;
        }
        panel.attach(terminal, event.buttonList);
        // Coming back from the tab editor the active tab may have been removed, and NEI's search settings may have
        // changed since the search was compiled. Selecting it again compiles afresh, which is cheap.
        boolean dropped = CategoryFilter.dropIfRemoved(store.entries());
        if (dropped || CategoryFilter.active() != null) CategoryFilter.select(terminal, CategoryFilter.active());
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui != null) return;
        // Otherwise the menu's actions keep the closed terminal until the next screen opens.
        menu = null;
        CategoryFilter.clearCache();
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Pre event) {
        Terminal terminal = Terminal.of(event.gui);
        if (terminal == null) return;
        if (panel.lostWidgets()) panel.reattach(terminal);
        panel.update();
        panel.setTooltipsHidden(menu != null);
        SearchBoxIndicator.updateTooltip(terminal);
        neiFilter.update(terminal);
    }

    /**
     * Left-clicks are pressed from {@link #mouseClicked}, so the only presses that reach here are right-clicks, which
     * AE2 replays as left-clicks on any button. AE2's actionPerformed must never see our buttons.
     */
    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (Terminal.of(event.gui) != null && panel.owns(event.button)) event.setCanceled(true);
    }

    private void press(Terminal terminal, GuiButton button) {
        Minecraft mc = Minecraft.getMinecraft();
        if (button.id == TerminalPanel.SETTINGS_BUTTON_ID && !canOpenEditor()) return;

        button.func_146113_a(mc.getSoundHandler());
        switch (button.id) {
            case TerminalPanel.SETTINGS_BUTTON_ID:
                mc.displayGuiScreen(new GuiTabSettings(terminal.gui(), store));
                return;
            case TerminalPanel.VIEW_BUTTON_ID:
                store.setGrid(!store.isGrid());
                store.save();
                panel.requestRelayout();
                return;
            case TerminalPanel.HIDE_BUTTON_ID:
                store.setHidden(!store.isHidden());
                store.save();
                panel.requestRelayout();
                return;
            case TerminalPanel.SHOW_ALL_BUTTON_ID:
                CategoryFilter.select(terminal, null);
                return;
            default:
                TabEntry entry = panel.entryFor(button);
                if (entry == null) return;
                // Clicking the active category again goes back to showing everything.
                CategoryFilter.select(terminal, entry == CategoryFilter.active() ? null : entry);
        }
    }

    /**
     * Screens built on AE2's scroll their grid from this event; cancelling it keeps the grid still while over our list.
     * It comes in whole notches, where NEI's wheel hook only has a direction.
     */
    @SubscribeEvent
    public void onAeScroll(GuiScrollEvent event) {
        if (!isOverList(event.guiScreen, event.mouseX, event.mouseY)) return;
        panel.scrollList(event.scrollAmount);
        event.setCanceled(true);
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return Terminal.of(gui) != null && panel.overlaps(x, y, w, h);
    }

    // NEI 2.8.44, which GTNH 2.8.4 ships, has no default implementations for these.

    @Override
    public VisiblityData modifyVisiblity(GuiContainer gui, VisiblityData currentVisibility) {
        return currentVisibility;
    }

    @Override
    public Iterable<Integer> getItemSpawnSlots(GuiContainer gui, ItemStack item) {
        return null;
    }

    @Override
    public List<TaggedInventoryArea> getInventoryAreas(GuiContainer gui) {
        return null;
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mouseX, int mouseY, ItemStack draggedStack, int button) {
        return false;
    }

    // NEI's input handlers see mouse input before the screen does. This handler must be first in NEI's list,
    // otherwise NEI's item panel (which sits under our panel) consumes the wheel and pages through items.

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mouseX, int mouseY, int scrolled) {
        menu = null;
        if (!isOverList(gui, mouseX, mouseY)) return false;
        // Screens built on AE2's scroll the list from onAeScroll, which AE2 fires afterwards; here it's only swallowed.
        Terminal terminal = Terminal.of(gui);
        if (!terminal.isAeScreen()) panel.scrollList(scrolled);
        return true;
    }

    @Override
    public boolean mouseClicked(GuiContainer gui, int mouseX, int mouseY, int button) {
        Terminal terminal = Terminal.of(gui);
        if (terminal == null) return false;
        // AE2 replays a right-click on a button as a left-click. Claiming that would leave the right-button release
        // unclaimed, and vanilla would drop one held item on it; let it through, vanilla cancels its own drag when
        // the release doesn't match the press.
        boolean replayed = button != Mouse.getEventButton();

        if (menu != null) {
            ContextMenu closed = menu;
            menu = null;
            boolean onMenu = closed.contains(mouseX, mouseY);
            if (!replayed) {
                // Any click closes the menu without reaching whatever is under it.
                if (button == 0 && onMenu) closed.click(mouseX, mouseY);
                return true;
            }
            // Claiming a replayed press would leave NEI waiting for a left release that never comes, and it would
            // swallow the next one. Only our buttons sit under the menu, and onActionPerformed cancels those.
            // Elsewhere, it may open another menu.
            if (onMenu) return false;
        }

        if (button == 0 && !replayed && neiFilter.badgeContains(mouseX, mouseY)) {
            SearchBoxIndicator.clearCategory(terminal);
            return true;
        }
        if (!panel.contains(mouseX, mouseY)) return false;
        if (replayed) {
            if (Mouse.getEventButton() == 1) openTabMenu(terminal, mouseX, mouseY);
            return false;
        }

        // Claim the rest: the panel is outside the terminal's bounds, so vanilla would take any click here as a
        // click outside the window and throw the held stack on release. Claiming also skips that release.
        if (button == 0 && !panel.listMousePressed(mouseX, mouseY)) {
            GuiButton pressed = panel.buttonAt(mouseX, mouseY);
            if (pressed != null) press(terminal, pressed);
        } else if (button == 1) {
            // Screens not built on AE2's don't replay right-clicks, so the menu opens from the right-click itself.
            openTabMenu(terminal, mouseX, mouseY);
        }
        return true;
    }

    private void openTabMenu(Terminal terminal, int mouseX, int mouseY) {
        GuiButton pressed = panel.buttonAt(mouseX, mouseY);
        TabEntry entry = pressed == null ? null : panel.entryFor(pressed);
        if (entry == null) return;
        boolean active = entry == CategoryFilter.active();
        menu = new ContextMenu()
            .add(
                (active ? GtGuiText.ShowAll : GtGuiText.ShowCategory).getLocal(),
                true,
                () -> CategoryFilter.select(terminal, active ? null : entry))
            .add(GtGuiText.EditTab.getLocal(), canOpenEditor(), () -> openEditor(terminal, entry))
            .addDestructive(GtGuiText.RemoveTab.getLocal(), true, () -> removeTab(terminal, entry))
            .open(mouseX, mouseY, terminal.gui().width, terminal.gui().height);
    }

    private void removeTab(Terminal terminal, TabEntry entry) {
        store.entries()
            .removeIf(each -> each == entry);
        store.save();
        if (CategoryFilter.dropIfRemoved(store.entries())) CategoryFilter.select(terminal, null);
        panel.requestRelayout();
    }

    private void openEditor(Terminal terminal, TabEntry entry) {
        // AE2 handles some clicks before NEI, so a stack can reach the cursor while the menu is open.
        if (!canOpenEditor()) return;
        Minecraft.getMinecraft()
            .displayGuiScreen(
                new GuiTabSettings(
                    terminal.gui(),
                    store,
                    store.entries()
                        .indexOf(entry)));
    }

    /** The editor can't show the cursor stack, and the server would drop it when the terminal closes. */
    private static boolean canOpenEditor() {
        return Minecraft.getMinecraft().thePlayer.inventory.getItemStack() == null;
    }

    @Override
    public void onMouseDragged(GuiContainer gui, int mouseX, int mouseY, int button, long heldTime) {
        if (Terminal.of(gui) != null) panel.listMouseDragged(mouseY);
    }

    @Override
    public void onMouseUp(GuiContainer gui, int mouseX, int mouseY, int button) {
        panel.listMouseReleased();
    }

    @Override
    public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
        if (menu != null && keyCode == Keyboard.KEY_ESCAPE) {
            // Escape closes the menu rather than the terminal under it.
            menu = null;
            return true;
        }
        return gui instanceof GuiTabSettings && ((GuiTabSettings) gui).handleKey(keyChar, keyCode);
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char keyChar, int keyCode) {}

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public void onMouseClicked(GuiContainer gui, int mouseX, int mouseY, int button) {}

    @Override
    public void onMouseScrolled(GuiContainer gui, int mouseX, int mouseY, int scrolled) {}

    @Override
    public void onPreDraw(GuiContainer gui) {}

    @Override
    public void renderObjects(GuiContainer gui, int mouseX, int mouseY) {}

    @Override
    public void postRenderObjects(GuiContainer gui, int mouseX, int mouseY) {
        Terminal terminal = Terminal.of(gui);
        if (terminal == null || CategoryFilter.active() == null) return;
        // After every handler's renderObjects: NEI may load its overlay after us, and it would draw its search box over
        // the badge.
        beginOverlay();
        SearchBoxIndicator.draw(terminal, mouseX, mouseY);
        neiFilter.draw(mouseX, mouseY);
        endOverlay();
    }

    /** The menu goes over everything the screen drew, NEI's and AE2's tooltips included. */
    @SubscribeEvent
    public void onDrawScreenPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (menu == null || Terminal.of(event.gui) == null) return;
        beginOverlay();
        GL11.glPushMatrix();
        // Tooltips are drawn at z 300.
        GL11.glTranslatef(0, 0, 300);
        menu.draw(event.mouseX, event.mouseY);
        GL11.glPopMatrix();
        endOverlay();
    }

    /**
     * Hands back exactly the state we were given: lighting left on without its lights set up draws NEI's items dark,
     * and drawRect and the icon renderer both change blend, texture and lighting state.
     */
    private static void beginOverlay() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_CURRENT_BIT);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private static void endOverlay() {
        GL11.glPopAttrib();
    }

    @Override
    public void renderSlotUnderlay(GuiContainer gui, Slot slot) {}

    @Override
    public void renderSlotOverlay(GuiContainer gui, Slot slot) {}

    private boolean isOverList(GuiScreen gui, int mouseX, int mouseY) {
        return Terminal.of(gui) != null && panel.listContains(mouseX, mouseY);
    }
}
