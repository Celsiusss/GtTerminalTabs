package net.celsiuss.gtterminaltabs.client;

import java.lang.reflect.Field;

import net.minecraft.client.gui.GuiTextField;

import appeng.api.config.SearchBoxMode;
import appeng.api.config.Settings;
import appeng.core.AEConfig;
import codechicken.nei.FormattedTextField;
import codechicken.nei.ItemList;
import codechicken.nei.LayoutManager;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.SearchField;
import codechicken.nei.TextField;
import codechicken.nei.api.ItemFilter;

/**
 * Applies the active category to NEI's item panel as well, while a terminal is open and AE2 is set to sync its search
 * with NEI. NEI's search box then shows the category the way the terminal's does.
 */
public class NeiCategoryFilter implements ItemFilter.ItemFilterProvider {

    private static final ItemFilter EVERYTHING = item -> true;
    private static final String WITHOUT_IT = "NEI's search box won't show the category";
    private static final Field textFieldAccess = Reflection.findField(TextField.class, "field", WITHOUT_IT);
    private static final Field placeholderAccess = Reflection
        .findField(FormattedTextField.class, "placeholder", WITHOUT_IT);
    private static final Field formattedTextAccess = Reflection
        .findField(FormattedTextField.class, "formattedText", WITHOUT_IT);

    // NEI asks for this from its filtering thread, so it only ever gets a finished filter.
    private volatile ItemFilter filter = EVERYTHING;
    private TabEntry appliedEntry;
    private ItemFilter appliedFilter;
    private String originalPlaceholder;

    @Override
    public ItemFilter getFilter() {
        return filter;
    }

    /** Call with the terminal every frame one is drawn, and with null when any other screen opens. */
    public void update(Terminal terminal) {
        TabEntry entry = terminal != null && terminal.followsNeiSync() && isSyncEnabled() ? CategoryFilter.active()
            : null;
        // The same compiled search as the terminal's, so both follow edits and settings changes together.
        ItemFilter wanted = entry == null ? null : CategoryFilter.filter();
        appliedEntry = entry;
        if (wanted != appliedFilter) {
            appliedFilter = wanted;
            filter = wanted == null ? EVERYTHING : wanted;
            ItemList.updateFilter.restart();
        }
        setPlaceholder(entry == null ? null : placeholderFor(entry));
    }

    private static String placeholderFor(TabEntry entry) {
        return String.format(GtGuiText.SearchIn.getLocal(), CategoryFilter.displayName(entry));
    }

    /** Puts the category's badge at the end of NEI's search box while its items are filtered. */
    public void draw(int mouseX, int mouseY) {
        SearchField box = shownSearchBox();
        if (box != null) badge(box).draw(appliedEntry, mouseX, mouseY);
    }

    /** Whether the point is on the badge in NEI's search box, which clears the category when clicked. */
    public boolean badgeContains(int mouseX, int mouseY) {
        SearchField box = shownSearchBox();
        return box != null && badge(box).contains(mouseX, mouseY);
    }

    private SearchField shownSearchBox() {
        if (appliedEntry == null || NEIClientConfig.isHidden() || !NEIClientConfig.isEnabled()) return null;
        SearchField box = LayoutManager.searchField;
        // NEI hides the box with the widget's own flag; the text field inside it stays visible.
        return box != null && box.isVisible() ? box : null;
    }

    // NEI's box is taller than AE2's, so its badge can be bigger, up to the 12px that still leaves room for the text.
    private static SearchBoxIndicator.Badge badge(SearchField box) {
        int size = Math.max(8, Math.min(12, box.h - 6));
        return new SearchBoxIndicator.Badge(box.x, box.y, box.w, box.h, size, 3);
    }

    /** Whether AE2 copies the terminal's search into NEI's, as set by the terminal's search mode button. */
    private static boolean isSyncEnabled() {
        Enum<?> mode = AEConfig.instance.settings.getSetting(Settings.SEARCH_MODE);
        return mode == SearchBoxMode.NEI_AUTOSEARCH || mode == SearchBoxMode.NEI_MANUAL_SEARCH;
    }

    /** Shows the hint in NEI's empty search box, or puts NEI's own back when given null. */
    private void setPlaceholder(String hint) {
        GuiTextField field = textField();
        if (!(field instanceof FormattedTextField) || placeholderAccess == null) return;
        FormattedTextField formatted = (FormattedTextField) field;
        if (hint != null) {
            if (originalPlaceholder == null) originalPlaceholder = readPlaceholder(formatted);
            formatted.setPlaceholder(hint);
            if (formatted.getText()
                .isEmpty()) clearFormattedText(formatted);
        } else if (originalPlaceholder != null) {
            formatted.setPlaceholder(originalPlaceholder);
            originalPlaceholder = null;
        }
    }

    /**
     * NEI only draws the placeholder while its highlighted copy of the text is empty, but its search highlighter turns
     * empty text into a lone colour code. Once something has been typed and cleared, the placeholder never comes back
     * until this copy is emptied again; with the raw text also empty, NEI doesn't rebuild it.
     */
    private static void clearFormattedText(FormattedTextField field) {
        Reflection.set(formattedTextAccess, field, "");
    }

    private static String readPlaceholder(FormattedTextField field) {
        String placeholder = Reflection.get(placeholderAccess, field, String.class);
        return placeholder != null ? placeholder : "";
    }

    private static GuiTextField textField() {
        SearchField box = LayoutManager.searchField;
        return box == null ? null : Reflection.get(textFieldAccess, box, GuiTextField.class);
    }
}
