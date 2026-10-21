package net.celsiuss.gtterminaltabs.client;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * The mod's own settings, kept in a Forge config file so they can also be edited by hand. Each is an on/off
 * {@link Option}, listed with a checkbox on the tab editor's Settings page in declaration order.
 */
public final class TabsConfig {

    private static final String CATEGORY = "general";

    private static Configuration config;

    private TabsConfig() {}

    public enum Option {

        ALWAYS_USE_PREFIXES("alwaysUsePrefixes", false, GtGuiText.OptionAlwaysPrefixes,
            GtGuiText.OptionAlwaysPrefixesInfo,
            "Require prefixes in every tab's search instead of following your NEI search settings.");

        private final String key;
        private final boolean defaultValue;
        private final GtGuiText label;
        private final GtGuiText info;
        private final String comment;
        private Property property;
        private boolean value;

        Option(String key, boolean defaultValue, GtGuiText label, GtGuiText info, String comment) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.label = label;
            this.info = info;
            this.comment = comment;
            this.value = defaultValue;
        }

        public boolean get() {
            return value;
        }

        public void set(boolean value) {
            this.value = value;
            if (property == null) return;
            property.set(value);
            config.save();
        }

        public String label() {
            return label.getLocal();
        }

        public String info() {
            return info.getLocal();
        }
    }

    public static void load(File file) {
        config = new Configuration(file);
        for (Option option : Option.values()) {
            option.property = config.get(CATEGORY, option.key, option.defaultValue, option.comment);
            option.value = option.property.getBoolean(option.defaultValue);
        }
        if (config.hasChanged()) config.save();
    }
}
