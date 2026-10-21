package net.celsiuss.gtterminaltabs.client;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.celsiuss.gtterminaltabs.GtTerminalTabs;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

import appeng.api.storage.data.IDisplayRepo;
import appeng.client.gui.widgets.MEGuiTextField;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * A terminal screen the tab panel goes on. AE2 and each add-on keep their grid, search box and scrollbar differently
 * and mostly privately, so they are found by name; an add-on that isn't installed is never matched.
 */
public final class Terminal {

    private enum Kind {

        // Every AE2 item terminal, the portable cell and wireless terminal included, but not the security terminal.
        AE2("appeng.client.gui.implementations.GuiMEMonitorable", true, false),
        // AE2 Fluid Crafting's item, fluid and essentia terminals, wired and wireless.
        FLUID_CRAFTING("com.glodblock.github.client.gui.base.FCGuiMonitor", true, false),
        WIRELESS_CRAFTING("net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal", true, false),
        // Thaumic Energistics' terminals aren't built on AE2's screen, and rebuild their view in a method of their own.
        ARCANE_CRAFTING("thaumicenergistics.client.gui.GuiArcaneCraftingTerminal", false, true),
        ESSENTIA("thaumicenergistics.client.gui.GuiEssentiaCellTerminal", false, true);

        final String className;
        // Built on AE2's screen: an AE2 search box, AE2's scroll event, and AE2's setting for syncing with NEI.
        final boolean aeScreen;
        final boolean ownUpdateView;
        private boolean resolved;
        private Class<?> type;
        private Field repo;
        private Field searchField;
        private Method refresh;

        Kind(String className, boolean aeScreen, boolean ownUpdateView) {
            this.className = className;
            this.aeScreen = aeScreen;
            this.ownUpdateView = ownUpdateView;
        }

        Class<?> type() {
            if (!resolved) {
                resolved = true;
                try {
                    type = Class.forName(className, false, Terminal.class.getClassLoader());
                } catch (ClassNotFoundException e) {
                    return null;
                }
                String without = "picking a category won't refresh " + className;
                if (ownUpdateView) {
                    refresh = findMethod(type, "updateView", without);
                } else {
                    repo = Reflection.findField(type, "repo", without);
                    refresh = findMethod(type, "setScrollBar", without);
                    searchField = Reflection
                        .findField(type, "searchField", "its search box won't show the category: " + className);
                }
            }
            return type;
        }
    }

    private static final String SECURITY_TERMINAL = "appeng.client.gui.implementations.GuiSecurity";
    private static final Field guiLeftAccess = ReflectionHelper
        .findField(GuiContainer.class, "guiLeft", "field_147003_i");
    private static final Field guiTopAccess = ReflectionHelper
        .findField(GuiContainer.class, "guiTop", "field_147009_r");
    private static final Field xSizeAccess = ReflectionHelper.findField(GuiContainer.class, "xSize", "field_146999_f");
    // Null for screens that aren't terminals.
    private static final Map<Class<?>, Kind> kinds = new HashMap<>();

    private final GuiContainer gui;
    private final Kind kind;

    private Terminal(GuiContainer gui, Kind kind) {
        this.gui = gui;
        this.kind = kind;
    }

    /** The screen as a terminal, or null if the panel doesn't go on it. */
    public static Terminal of(GuiScreen screen) {
        if (!(screen instanceof GuiContainer)) return null;
        Class<?> type = screen.getClass();
        Kind kind;
        if (kinds.containsKey(type)) {
            kind = kinds.get(type);
        } else {
            kind = kindOf(type);
            kinds.put(type, kind);
        }
        return kind == null ? null : new Terminal((GuiContainer) screen, kind);
    }

    private static Kind kindOf(Class<?> type) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (c.getName()
                .equals(SECURITY_TERMINAL)) return null;
        }
        for (Kind kind : Kind.values()) {
            Class<?> base = kind.type();
            if (base != null && base.isAssignableFrom(type)) return kind;
        }
        return null;
    }

    public GuiContainer gui() {
        return gui;
    }

    /** Built on AE2's screen, which fires AE2's scroll event and replays right-clicks on buttons as left-clicks. */
    public boolean isAeScreen() {
        return kind.aeScreen;
    }

    /** Whether AE2's NEI sync setting applies, copying the terminal's search into NEI's. */
    public boolean followsNeiSync() {
        return kind.aeScreen;
    }

    /** The terminal's search box, or null for terminals whose search box isn't AE2's. */
    public MEGuiTextField searchBox() {
        return Reflection.get(kind.searchField, gui, MEGuiTextField.class);
    }

    /** Rebuilds the terminal's grid and resizes its scrollbar, after the category changed. */
    public void refresh() {
        IDisplayRepo repo = Reflection.get(kind.repo, gui, IDisplayRepo.class);
        if (repo != null) repo.updateView();
        if (kind.refresh == null) return;
        try {
            kind.refresh.invoke(gui);
        } catch (IllegalAccessException | InvocationTargetException e) {
            GtTerminalTabs.LOG.warn("Couldn't refresh {}", gui.getClass(), e);
        }
    }

    public int left() {
        return intField(guiLeftAccess);
    }

    public int top() {
        return intField(guiTopAccess);
    }

    public int xSize() {
        return intField(xSizeAccess);
    }

    /**
     * Whether the terminal asks NEI to keep its item panel off this area, as AE2 does for its view cell column. False
     * for terminals that don't say.
     */
    public boolean hidesNeiArea(int x, int y, int w, int h) {
        try {
            Method method = gui.getClass()
                .getMethod("hideItemPanelSlot", int.class, int.class, int.class, int.class);
            return (Boolean) method.invoke(gui, x, y, w, h);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    /**
     * The leftmost and the lowest of the terminal's view cell slots, for terminals that don't report their view cell
     * column to NEI, or null when it has none. Only the Wireless Crafting Terminal's slots are found by class name.
     */
    public Slot[] viewCellSlots() {
        if (hasNeiArea()) return null;
        Slot leftmost = null;
        Slot lowest = null;
        for (Object o : gui.inventorySlots.inventorySlots) {
            Slot slot = (Slot) o;
            if (!slot.getClass()
                .getSimpleName()
                .equals("SlotViewCell")) continue;
            if (leftmost == null || slot.xDisplayPosition < leftmost.xDisplayPosition) leftmost = slot;
            if (lowest == null || slot.yDisplayPosition > lowest.yDisplayPosition) lowest = slot;
        }
        return leftmost == null ? null : new Slot[] { leftmost, lowest };
    }

    private boolean hasNeiArea() {
        try {
            gui.getClass()
                .getMethod("hideItemPanelSlot", int.class, int.class, int.class, int.class);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private int intField(Field field) {
        try {
            return field.getInt(gui);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    private static Method findMethod(Class<?> owner, String name, String withoutIt) {
        try {
            Method method = owner.getDeclaredMethod(name);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            GtTerminalTabs.LOG.warn("{} has no method {}; {}", owner.getName(), name, withoutIt, e);
            return null;
        }
    }
}
