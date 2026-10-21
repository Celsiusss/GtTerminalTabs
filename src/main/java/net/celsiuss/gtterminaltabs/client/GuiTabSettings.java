package net.celsiuss.gtterminaltabs.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import appeng.client.gui.widgets.ITooltip;
import codechicken.nei.LayoutManager;
import codechicken.nei.api.INEIGuiHandler;

/**
 * Editor for the tab list, opened over a terminal. It is a GuiContainer only so NEI shows its item panel, which is
 * the icon picker: dragging an item onto the icon slot sets it, like AE2's fake slots. NEI registers GuiContainers
 * implementing INEIGuiHandler for drops automatically.
 * <p>
 * Closing it reopens the terminal screen without closing the container, the same way NEI's recipe screen returns;
 * AE2 treats that as a sub-GUI reopen and keeps its state.
 */
public class GuiTabSettings extends GuiContainer implements INEIGuiHandler {

    private static final int WIDTH = 260;
    private static final int HEIGHT = 156;
    private static final int PADDING = 6;
    private static final int LIST_WIDTH = 104;
    private static final int BUTTON_SIZE = 20;
    private static final int BUTTON_GAP = 2;
    private static final int FIELD_HEIGHT = 16;
    private static final int LABEL_GAP = 2;
    private static final int ICON_TEXT_GAP = 4;
    private static final int DONE_WIDTH = 60;
    private static final int SLOT_SIZE = ItemIcons.SLOT_SIZE;
    private static final int NAME_MAX_LENGTH = 64;
    private static final int VALUE_MAX_LENGTH = 256;
    private static final int ICON_TEXT_LINES = 2;

    private static final int LIST_ID_BASE = 100;
    private static final int ADD_ID = 0;
    private static final int REMOVE_ID = 1;
    private static final int UP_ID = 2;
    private static final int DOWN_ID = 3;
    private static final int DONE_ID = 4;
    private static final int NO_RELAYOUT = -2;

    private final GuiScreen parent;
    private final TabStore store;
    private final List<TabEntry> entries;
    private final ScrollableButtonList list;

    private FlatTextField nameField;
    private FlatTextField valueField;
    private GuiButton removeButton;
    private GuiButton upButton;
    private GuiButton downButton;
    private int iconX;
    private int iconY;
    private int footerTop;
    // Set by button presses that change the number of entries; applied once vanilla's click loop has finished.
    private int pendingSelection = NO_RELAYOUT;
    private boolean focusNameAfterRelayout;

    public GuiTabSettings(GuiScreen parent, TabStore store) {
        super(new EditorContainer());
        this.parent = parent;
        this.store = store;
        this.entries = store.entries();
        this.list = new ScrollableButtonList(LIST_ID_BASE, entries);
        this.xSize = WIDTH;
        this.ySize = HEIGHT;
        if (!entries.isEmpty()) list.setSelected(0);
    }

    /** The screen this editor returns to: the terminal it was opened from. */
    public GuiScreen getParent() {
        return parent;
    }

    @Override
    public void initGui() {
        super.initGui();
        // GuiContainer.initGui swapped in our dummy container; put the terminal's back so the server's slot updates
        // for it still land while the editor is open.
        if (parent instanceof GuiContainer) mc.thePlayer.openContainer = ((GuiContainer) parent).inventorySlots;
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int contentTop = guiTop + PADDING + fontRendererObj.FONT_HEIGHT + 2 * LABEL_GAP;
        int bottomRow = guiTop + HEIGHT - PADDING - BUTTON_SIZE;
        buttonList.addAll(list.layout(guiLeft + PADDING, contentTop, LIST_WIDTH, bottomRow - contentTop - PADDING));
        list.setSelected(list.getSelected());

        if (entries.isEmpty()) {
            buttonList.add(
                new HintLabel(
                    guiLeft + PADDING,
                    contentTop,
                    LIST_WIDTH,
                    bottomRow - contentTop - PADDING,
                    GtGuiText.NoTabs.getLocal()));
        }
        footerTop = bottomRow - PADDING / 2 - 1;

        int buttonX = guiLeft + PADDING;
        buttonList.add(
            new GlyphButton(
                ADD_ID,
                buttonX,
                bottomRow,
                BUTTON_SIZE,
                GlyphButton.Glyph.PLUS,
                GtGuiText.AddTab.getLocal()));
        removeButton = new GlyphButton(
            REMOVE_ID,
            buttonX += BUTTON_SIZE + BUTTON_GAP,
            bottomRow,
            BUTTON_SIZE,
            GlyphButton.Glyph.MINUS,
            GtGuiText.RemoveTab.getLocal());
        upButton = new GlyphButton(
            UP_ID,
            buttonX += BUTTON_SIZE + BUTTON_GAP,
            bottomRow,
            BUTTON_SIZE,
            GlyphButton.Glyph.UP,
            GtGuiText.MoveUp.getLocal());
        downButton = new GlyphButton(
            DOWN_ID,
            buttonX + BUTTON_SIZE + BUTTON_GAP,
            bottomRow,
            BUTTON_SIZE,
            GlyphButton.Glyph.DOWN,
            GtGuiText.MoveDown.getLocal());
        buttonList.add(removeButton);
        buttonList.add(upButton);
        buttonList.add(downButton);
        buttonList.add(
            new TooltipButton(
                DONE_ID,
                guiLeft + WIDTH - PADDING - DONE_WIDTH,
                bottomRow,
                DONE_WIDTH,
                BUTTON_SIZE,
                GtGuiText.Done.getLocal()));

        int fieldX = guiLeft + PADDING + LIST_WIDTH + PADDING;
        int fieldWidth = guiLeft + WIDTH - PADDING - fieldX;
        int lineHeight = fontRendererObj.FONT_HEIGHT + LABEL_GAP;
        int nameTop = contentTop + lineHeight;
        int valueTop = nameTop + FIELD_HEIGHT + PADDING + lineHeight;
        nameField = new FlatTextField(fontRendererObj, fieldX, nameTop, fieldWidth, FIELD_HEIGHT);
        nameField.setMaxStringLength(NAME_MAX_LENGTH);
        valueField = new FlatTextField(fontRendererObj, fieldX, valueTop, fieldWidth, FIELD_HEIGHT);
        valueField.setMaxStringLength(VALUE_MAX_LENGTH);
        iconX = fieldX;
        iconY = valueTop + FIELD_HEIGHT + PADDING + lineHeight;
        loadSelection();
    }

    @Override
    public void onGuiClosed() {
        // Not super: closing the dummy container would clear the cursor stack client-side.
        Keyboard.enableRepeatEvents(false);
        // Here rather than in close(): the editor also closes when the server closes the terminal, on death,
        // dimension change and disconnect.
        store.save();
    }

    /**
     * The editor has no slots, but vanilla still sends clicks outside the window to the server, against the dummy
     * container's window id 0: the player's own inventory.
     */
    @Override
    protected void handleMouseClick(Slot slot, int slotIndex, int button, int modifier) {}

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        nameField.updateCursorCounter();
        valueField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (hasSelection() && (isOverIconSlot(mouseX, mouseY) || isOverIconText(mouseX, mouseY))) {
            drawIconTooltip(mouseX, mouseY);
        } else {
            drawButtonTooltip(mouseX, mouseY);
        }
    }

    /** AE2 only draws ITooltip tooltips on its own screens, so mirror its format here: first line white, rest grey. */
    private void drawButtonTooltip(int mouseX, int mouseY) {
        for (Object o : buttonList) {
            if (!(o instanceof ITooltip)) continue;
            ITooltip widget = (ITooltip) o;
            if (!widget.isVisible() || mouseX < widget.xPos()
                || mouseY < widget.yPos()
                || mouseX >= widget.xPos() + widget.getWidth()
                || mouseY >= widget.yPos() + widget.getHeight()) continue;
            String message = widget.getMessage();
            if (message == null || message.isEmpty()) return;
            List<String> lines = new ArrayList<>(Arrays.asList(message.split("\n")));
            for (int i = 1; i < lines.size(); i++) lines.set(i, EnumChatFormatting.GRAY + lines.get(i));
            drawHoveringText(lines, mouseX, mouseY, fontRendererObj);
            return;
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int right = guiLeft + WIDTH;
        FlatStyle.drawBox(guiLeft, guiTop, right, guiTop + HEIGHT, FlatStyle.PANEL_FILL, FlatStyle.PANEL_BORDER);
        drawRect(guiLeft + 1, footerTop, right - 1, footerTop + 1, FlatStyle.DIVIDER);
        drawString(
            fontRendererObj,
            GtGuiText.TabSettings.getLocal(),
            guiLeft + PADDING,
            guiTop + PADDING,
            FlatStyle.TEXT_BRIGHT);
        if (!hasSelection()) return;

        drawFieldLabel(GtGuiText.Name.getLocal(), nameField.getBoxX(), nameField.getBoxY());
        drawFieldLabel(GtGuiText.SearchValue.getLocal(), valueField.getBoxX(), valueField.getBoxY());
        drawFieldLabel(GtGuiText.Icon.getLocal(), iconX, iconY);
        nameField.drawTextBox();
        valueField.drawTextBox();
        drawIconSlot(mouseX, mouseY);
    }

    private void drawFieldLabel(String label, int fieldX, int fieldY) {
        drawString(
            fontRendererObj,
            label,
            fieldX,
            fieldY - fontRendererObj.FONT_HEIGHT - LABEL_GAP,
            FlatStyle.TEXT_DISABLED);
    }

    private void drawIconSlot(int mouseX, int mouseY) {
        int right = iconX + SLOT_SIZE;
        int bottom = iconY + SLOT_SIZE;
        FlatStyle.drawBox(iconX, iconY, right, bottom, FlatStyle.TRACK, FlatStyle.BORDER);

        ItemStack stack = selectedIcon();
        ItemIcons.draw(mc, stack, iconX + 1, iconY + 1);
        if (isOverIconSlot(mouseX, mouseY)) drawRect(iconX + 1, iconY + 1, right - 1, bottom - 1, FlatStyle.SLOT_HOVER);

        List<String> lines = iconTextLines(stack);
        int lineY = iconY + (SLOT_SIZE - lines.size() * fontRendererObj.FONT_HEIGHT) / 2 + 1;
        for (String line : lines) {
            drawString(
                fontRendererObj,
                line,
                iconTextX(),
                lineY,
                stack != null ? FlatStyle.TEXT_BRIGHT : FlatStyle.TEXT_DISABLED);
            lineY += fontRendererObj.FONT_HEIGHT;
        }
    }

    /** The icon's name or the drag hint, wrapped beside the slot and cut short with ".."; the tooltip has it all. */
    private List<String> iconTextLines(ItemStack stack) {
        String text = stack != null ? stack.getDisplayName() : GtGuiText.IconHint.getLocal();
        int width = iconTextRight() - iconTextX();
        List<String> lines = fontRendererObj.listFormattedStringToWidth(text, width);
        if (lines.size() <= ICON_TEXT_LINES) return lines;
        List<String> shown = new ArrayList<>(lines.subList(0, ICON_TEXT_LINES));
        String last = shown.get(ICON_TEXT_LINES - 1);
        shown.set(
            ICON_TEXT_LINES - 1,
            fontRendererObj.trimStringToWidth(last, width - fontRendererObj.getStringWidth("..")) + "..");
        return shown;
    }

    private int iconTextX() {
        return iconX + SLOT_SIZE + ICON_TEXT_GAP;
    }

    private int iconTextRight() {
        return guiLeft + WIDTH - PADDING;
    }

    private boolean isOverIconText(int mouseX, int mouseY) {
        return mouseX >= iconTextX() && mouseX < iconTextRight() && mouseY >= iconY && mouseY < iconY + SLOT_SIZE;
    }

    @SuppressWarnings("unchecked")
    private void drawIconTooltip(int mouseX, int mouseY) {
        ItemStack stack = selectedIcon();
        if (stack == null) {
            drawHoveringText(Collections.singletonList(GtGuiText.IconHint.getLocal()), mouseX, mouseY, fontRendererObj);
            return;
        }
        List<String> lines = stack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
        lines.add(EnumChatFormatting.GRAY + GtGuiText.IconClear.getLocal());
        drawHoveringText(lines, mouseX, mouseY, fontRendererObj);
    }

    @Override
    public boolean handleDragNDrop(GuiContainer gui, int mouseX, int mouseY, ItemStack draggedStack, int button) {
        if (gui != this || draggedStack == null || !hasSelection() || !isOverIconSlot(mouseX, mouseY)) return false;
        // Like AE2's fake slots, copy the stack and leave NEI's drag alone so it can be dropped elsewhere too.
        entries.get(list.getSelected()).icon = TabIcon.of(draggedStack);
        return true;
    }

    @Override
    public boolean hideItemPanelSlot(GuiContainer gui, int x, int y, int w, int h) {
        return gui == this && x < guiLeft + WIDTH && x + w > guiLeft && y < guiTop + HEIGHT && y + h > guiTop;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        int index = list.entryIndexOf(button);
        if (index >= 0) {
            select(index);
            return;
        }
        int selected = list.getSelected();
        switch (button.id) {
            case ADD_ID:
                entries.add(new TabEntry(GtGuiText.NewTab.getLocal(), "", null));
                pendingSelection = entries.size() - 1;
                focusNameAfterRelayout = true;
                break;
            case REMOVE_ID:
                entries.remove(selected);
                pendingSelection = Math.min(selected, entries.size() - 1);
                break;
            case UP_ID:
                Collections.swap(entries, selected, selected - 1);
                select(selected - 1);
                break;
            case DOWN_ID:
                Collections.swap(entries, selected, selected + 1);
                select(selected + 1);
                break;
            case DONE_ID:
                close();
                break;
            default:
                break;
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            // GuiContainer's default would close the terminal's container along with this screen.
            close();
            return;
        }
        if (handleKey(typedChar, keyCode)) return;
        // Lets NEI's hotkeys over its item panel work; the inventory key would close the terminal, as above.
        if (keyCode != mc.gameSettings.keyBindInventory.getKeyCode()) super.keyTyped(typedChar, keyCode);
    }

    /**
     * Handles the editor's keys: Up/Down change the selection, Tab and Enter move between the fields, and anything
     * else goes to the focused field. Called ahead of NEI's key handlers, so typing a letter that is also an NEI
     * hotkey edits the field instead of triggering NEI.
     *
     * @return true if the editor consumed the key
     */
    public boolean handleKey(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE || !hasSelection()) return false;
        boolean editing = nameField.isFocused() || valueField.isFocused();
        // Leave keys to NEI while its search box has focus.
        if (!editing && LayoutManager.getInputFocused() != null) return false;

        int selected = list.getSelected();
        switch (keyCode) {
            case Keyboard.KEY_UP:
                if (selected > 0) select(selected - 1);
                return true;
            case Keyboard.KEY_DOWN:
                if (selected < entries.size() - 1) select(selected + 1);
                return true;
            case Keyboard.KEY_TAB:
                if (nameField.isFocused()) focus(valueField);
                else if (valueField.isFocused()) focus(nameField);
                else focus(isShiftKeyDown() ? valueField : nameField);
                return true;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                if (nameField.isFocused()) focus(valueField);
                else if (valueField.isFocused()) focus(null);
                else focus(nameField);
                return true;
            default:
                break;
        }
        if (!editing) return false;

        // setText cuts values longer than the field's limit, so only write back real edits; otherwise moving the
        // cursor through a long hand-edited value would save it shortened.
        TabEntry entry = entries.get(list.getSelected());
        String name = nameField.getText();
        String value = valueField.getText();
        if (nameField.textboxKeyTyped(typedChar, keyCode)) {
            if (!nameField.getText()
                .equals(name)) {
                entry.name = nameField.getText();
                list.syncLabels();
            }
        } else if (valueField.textboxKeyTyped(typedChar, keyCode)) {
            if (!valueField.getText()
                .equals(value)) entry.value = valueField.getText();
        }
        return true;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && list.mousePressed(mouseX, mouseY)) return;
        if (button == 1 && hasSelection() && isOverIconSlot(mouseX, mouseY)) {
            entries.get(list.getSelected()).icon = null;
            return;
        }
        // Fields first: a button press may rebuild them or set focus, which a later click-outside would undo.
        if (hasSelection()) {
            nameField.mouseClicked(mouseX, mouseY, button);
            valueField.mouseClicked(mouseX, mouseY, button);
        }
        super.mouseClicked(mouseX, mouseY, button);
        if (pendingSelection != NO_RELAYOUT) relayout();
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long heldTime) {
        super.mouseClickMove(mouseX, mouseY, button, heldTime);
        list.mouseDragged(mouseY);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        super.mouseMovedOrUp(mouseX, mouseY, button);
        if (button != -1) list.mouseReleased();
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        if (list.contains(mouseX, mouseY)) list.scroll(Integer.signum(wheel));
    }

    private void close() {
        mc.displayGuiScreen(parent);
    }

    /**
     * Rebuilds the widgets after the number of entries changed. Never call this from actionPerformed: vanilla walks
     * buttonList by index while dispatching, so a rebuilt list can shift the pressed button under the cursor again.
     */
    private void relayout() {
        list.setSelected(pendingSelection);
        pendingSelection = NO_RELAYOUT;
        initGui();
        if (focusNameAfterRelayout) {
            focusNameAfterRelayout = false;
            nameField.setFocused(true);
            valueField.setFocused(false);
            nameField.setCursorPositionZero();
            nameField.setSelectionPos(
                nameField.getText()
                    .length());
        }
    }

    /** Focuses one field with the cursor at the end, or neither when null. */
    private void focus(FlatTextField field) {
        nameField.setFocused(field == nameField);
        valueField.setFocused(field == valueField);
        if (field != null) field.setCursorPositionEnd();
    }

    private void select(int index) {
        list.setSelected(index);
        loadSelection();
    }

    private boolean hasSelection() {
        int selected = list.getSelected();
        return selected >= 0 && selected < entries.size();
    }

    private ItemStack selectedIcon() {
        if (!hasSelection()) return null;
        TabIcon icon = entries.get(list.getSelected()).icon;
        return icon == null ? null : icon.toStack();
    }

    private boolean isOverIconSlot(int mouseX, int mouseY) {
        return mouseX >= iconX && mouseX < iconX + SLOT_SIZE && mouseY >= iconY && mouseY < iconY + SLOT_SIZE;
    }

    private void loadSelection() {
        boolean has = hasSelection();
        int selected = list.getSelected();
        TabEntry entry = has ? entries.get(selected) : null;
        nameField.setText(has ? entry.name : "");
        valueField.setText(has ? entry.value : "");
        nameField.setCursorPositionZero();
        valueField.setCursorPositionZero();
        if (!has) {
            nameField.setFocused(false);
            valueField.setFocused(false);
        }
        removeButton.enabled = has;
        upButton.enabled = has && selected > 0;
        downButton.enabled = has && selected < entries.size() - 1;
    }

    /** Holds no slots; it exists only because GuiContainer needs one. */
    private static class EditorContainer extends Container {

        @Override
        public boolean canInteractWith(EntityPlayer player) {
            return true;
        }
    }
}
