package net.celsiuss.gtterminaltabs.client;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.util.EnumChatFormatting;

import codechicken.nei.SearchField;
import codechicken.nei.SearchTokenParser;
import codechicken.nei.SearchTokenParser.ISearchParserProvider;
import codechicken.nei.SearchTokenParser.SearchMode;
import codechicken.nei.api.ItemFilter;

/** How a tab's search string is read. Both use NEI's search engine; they differ in which searches need a prefix. */
public enum SearchSyntax {

    /** Exactly like NEI's search box, with the player's NEI settings. */
    NEI,
    /**
     * Plain words match item names only, and every other search needs its own prefix, whatever the player has set and
     * with no prefix redefined. Stricter than NEI's defaults, which also match tooltips, ore dictionary names and
     * identifiers without one. NEI's pattern and space settings still apply.
     */
    PREFIXES;

    // NEI's item name search, the only one without a prefix.
    private static final char NAME_PREFIX = '\0';
    private static final Field providersAccess = Reflection
        .findField(SearchTokenParser.class, "searchProviders", "searches set to Never won't work in tabs");
    private static List<ISearchParserProvider> prefixesSource;
    private static SearchTokenParser prefixesParser;

    /** The syntax the settings ask for. */
    private static SearchSyntax current() {
        return TabsConfig.Option.ALWAYS_USE_PREFIXES.get() ? PREFIXES : NEI;
    }

    /** The syntax for one tab, which can ask for prefixes on its own. */
    public static SearchSyntax forEntry(TabEntry entry) {
        return entry != null && entry.alwaysUsePrefixes ? PREFIXES : current();
    }

    /** A parser for this syntax, for compiling a filter or highlighting a search. */
    public SearchTokenParser parser() {
        if (this == NEI) return SearchField.searchParser;
        // NEI hands out a new list whenever its settings, searches or language change, so ours is rebuilt only then.
        // Kept between compiles so an unchanged search compiles to the same filter, which NEI's parser caches.
        List<ISearchParserProvider> current = SearchField.searchParser.getProviders();
        if (current != prefixesSource) {
            prefixesSource = current;
            prefixesParser = new SearchTokenParser();
            for (ISearchParserProvider provider : allProviders()) {
                prefixesParser.addProvider(new FixedModeProvider(provider));
            }
        }
        return prefixesParser;
    }

    /**
     * Every search registered with NEI, in order. NEI's getProviders leaves out the ones the player set to Never, and
     * only has one per prefix; our parser picks per language and prefix the same way NEI does, from the full list.
     */
    @SuppressWarnings("unchecked")
    private static List<ISearchParserProvider> allProviders() {
        List<ISearchParserProvider> all = Reflection.get(providersAccess, SearchField.searchParser, List.class);
        return all != null ? all : SearchField.searchParser.getProviders();
    }

    public ItemFilter compile(String search) {
        return parser().getFilter(search);
    }

    /** One of NEI's searches with its search mode pinned instead of following the player's setting. */
    private static final class FixedModeProvider implements ISearchParserProvider {

        private final ISearchParserProvider delegate;

        FixedModeProvider(ISearchParserProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public ItemFilter getFilter(String searchText) {
            return delegate.getFilter(searchText);
        }

        @Override
        public List<net.minecraft.client.resources.Language> getMatchingLanguages() {
            return delegate.getMatchingLanguages();
        }

        @Override
        public char getPrefix() {
            return delegate.getPrefix();
        }

        @Override
        public EnumChatFormatting getHighlightedColor() {
            return delegate.getHighlightedColor();
        }

        @Override
        public SearchMode getSearchMode() {
            return delegate.getPrefix() == NAME_PREFIX ? SearchMode.ALWAYS : SearchMode.PREFIX;
        }
    }
}
