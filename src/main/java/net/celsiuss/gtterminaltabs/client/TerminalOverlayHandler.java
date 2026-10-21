package net.celsiuss.gtterminaltabs.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;

import org.lwjgl.input.Mouse;

import appeng.api.events.GuiScrollEvent;
import appeng.client.gui.implementations.GuiMEMonitorable;
import codechicken.nei.VisiblityData;
import codechicken.nei.api.INEIGuiHandler;
import codechicken.nei.api.TaggedInventoryArea;
import codechicken.nei.guihook.IContainerInputHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Connects the {@link TerminalPanel} to terminal screens through Forge, AE2 and NEI, and acts on its buttons. */
public class TerminalOverlayHandler implements INEIGuiHandler, IContainerInputHandler {

    private final TabStore store;
    private final TerminalPanel panel;

    public TerminalOverlayHandler(TabStore store) {
        this.store = store;
        this.panel = new TerminalPanel(store);
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        // AE2 clears buttonList on every initGui and reinitalize(), so rebuild from scratch each time.
        GuiMEMonitorable terminal = asTerminal(event.gui);
        if (terminal != null) panel.attach(terminal, event.buttonList);
        else panel.detach();
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Pre event) {
        GuiMEMonitorable terminal = asTerminal(event.gui);
        // Follows the search text rather than the last click, so typing a tab's search by hand also lights it up.
        if (terminal != null) panel.update(TerminalSearch.get(terminal));
    }

    /**
     * Left-clicks are pressed from {@link #mouseClicked}, so the only presses that reach here are right-clicks, which
     * AE2 replays as left-clicks on any button. AE2's actionPerformed must never see our buttons.
     */
    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (asTerminal(event.gui) != null && panel.owns(event.button)) event.setCanceled(true);
    }

    private void press(GuiMEMonitorable terminal, GuiButton button) {
        Minecraft mc = Minecraft.getMinecraft();
        // The editor can't show the cursor stack, and the server would drop it when the terminal closes.
        if (button.id == TerminalPanel.SETTINGS_BUTTON_ID && mc.thePlayer.inventory.getItemStack() != null) return;

        button.func_146113_a(mc.getSoundHandler());
        switch (button.id) {
            case TerminalPanel.SETTINGS_BUTTON_ID:
                mc.displayGuiScreen(new GuiTabSettings(terminal, store));
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
            default:
                String search = panel.searchFor(button);
                if (search != null) TerminalSearch.apply(terminal, search);
        }
    }

    /** AE2 scrolls its item grid from this event; cancelling it keeps the grid still while over our list. */
    @SubscribeEvent
    public void onAeScroll(GuiScrollEvent event) {
        if (!isOverList(event.guiScreen, event.mouseX, event.mouseY)) return;
        panel.scrollList(event.scrollAmount);
        event.setCanceled(true);
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return asTerminal(gui) != null && panel.overlaps(x, y, w, h);
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
        // Only swallow it here; the actual scrolling happens in onAeScroll, which AE2 fires afterwards.
        return isOverList(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(GuiContainer gui, int mouseX, int mouseY, int button) {
        if (asTerminal(gui) == null || !panel.contains(mouseX, mouseY)) return false;
        // AE2 replays a right-click on a button as a left-click. Claiming that would leave the right-button release
        // unclaimed, and vanilla would drop one held item on it; let it through, vanilla cancels its own drag when
        // the release doesn't match the press.
        if (button != Mouse.getEventButton()) return false;

        // Claim the rest: the panel is outside the terminal's bounds, so vanilla would take any click here as a
        // click outside the window and throw the held stack on release. Claiming also skips that release.
        if (button == 0 && !panel.listMousePressed(mouseX, mouseY)) {
            GuiButton pressed = panel.buttonAt(mouseX, mouseY);
            if (pressed != null) press((GuiMEMonitorable) gui, pressed);
        }
        return true;
    }

    @Override
    public void onMouseDragged(GuiContainer gui, int mouseX, int mouseY, int button, long heldTime) {
        if (asTerminal(gui) != null) panel.listMouseDragged(mouseY);
    }

    @Override
    public void onMouseUp(GuiContainer gui, int mouseX, int mouseY, int button) {
        panel.listMouseReleased();
    }

    @Override
    public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
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

    private boolean isOverList(GuiScreen gui, int mouseX, int mouseY) {
        return asTerminal(gui) != null && panel.listContains(mouseX, mouseY);
    }

    private static GuiMEMonitorable asTerminal(GuiScreen gui) {
        return gui instanceof GuiMEMonitorable ? (GuiMEMonitorable) gui : null;
    }
}
