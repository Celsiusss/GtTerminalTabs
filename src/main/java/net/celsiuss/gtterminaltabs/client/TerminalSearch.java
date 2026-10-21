package net.celsiuss.gtterminaltabs.client;

import java.lang.reflect.Field;

import net.celsiuss.gtterminaltabs.GtTerminalTabs;

import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.client.gui.widgets.MEGuiTextField;

/**
 * Reads and sets a terminal's search text. The field is protected and its only public setter escapes the text for
 * item names, which would break search syntax.
 */
public final class TerminalSearch {

    private static final Field searchFieldAccess = findSearchField();

    private TerminalSearch() {}

    /** Sets the search text as if typed, so AE2 filters the grid and saves the string as usual. */
    public static void apply(GuiMEMonitorable terminal, String value) {
        if (searchFieldAccess == null) return;
        try {
            MEGuiTextField field = (MEGuiTextField) searchFieldAccess.get(terminal);
            field.setText(value);
            field.setCursorPositionEnd();
        } catch (IllegalAccessException e) {
            GtTerminalTabs.LOG.warn("Could not set the terminal search text", e);
        }
    }

    /** Returns the current search text, or null if it can't be read. */
    public static String get(GuiMEMonitorable terminal) {
        if (searchFieldAccess == null) return null;
        try {
            return ((MEGuiTextField) searchFieldAccess.get(terminal)).getText();
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static Field findSearchField() {
        try {
            Field field = GuiMEMonitorable.class.getDeclaredField("searchField");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            GtTerminalTabs.LOG.warn("AE2's terminal has no searchField; tabs will not apply searches", e);
            return null;
        }
    }
}
