package net.celsiuss.gtterminaltabs.client;

/** One tab: {@code name} and {@code icon} are shown on the button, {@code value} is the search string it applies. */
public class TabEntry {

    public String name;
    public String value;
    /** Null when the tab has no icon. */
    public TabIcon icon;

    public TabEntry(String name, String value, TabIcon icon) {
        this.name = name;
        this.value = value;
        this.icon = icon;
    }
}
