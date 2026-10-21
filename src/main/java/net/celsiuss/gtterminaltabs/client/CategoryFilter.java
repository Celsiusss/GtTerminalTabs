package net.celsiuss.gtterminaltabs.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;

import appeng.api.storage.data.IAEItemStack;
import codechicken.nei.api.ItemFilter;

/**
 * The active category: a tab whose search string filters the terminal's grid underneath whatever is typed in its
 * search box. Static so the terminals' repos can reach it from mixins, and so the choice lasts for the session.
 */
public final class CategoryFilter {

    private static TabEntry active;
    // The tab is edited in place by the settings screen, so its search can change under us.
    private static String compiledValue;
    private static SearchSyntax compiledSyntax;
    private static ItemFilter filter;
    // The view is rebuilt on every grid update, and NEI filters can read tooltips. Items are keyed like AE2's own
    // search cache, by copies: in its craftable view AE2 hands over a throwaway copy of each stack on every rebuild.
    // Fluids and essentia are keyed by their fluid, or by the aspect when it has no gas.
    private static final Map<Object, Boolean> cache = new HashMap<>();

    private CategoryFilter() {}

    /** The selected tab, or null when everything is shown. */
    public static TabEntry active() {
        return active;
    }

    /** Selects the category, or null for everything, and rebuilds the terminal's grid to match. */
    public static void select(Terminal terminal, TabEntry entry) {
        set(entry);
        // With Shift held AE2 pauses the view and keeps the old entries until it's released.
        terminal.refresh();
    }

    /** The tab's name, or its search when it has none. */
    public static String displayName(TabEntry entry) {
        return entry.name.trim()
            .isEmpty() ? entry.value : entry.name;
    }

    private static void set(TabEntry entry) {
        active = entry;
        // Compiled again on next use, which picks up changed NEI settings. NEI's parsers hand back the same filter for
        // an unchanged search, so the cache survives selecting the same search again.
        compiledValue = null;
        compiledSyntax = null;
        if (entry == null) {
            filter = null;
            cache.clear();
        }
    }

    /** Forgets every stack's result, which would otherwise hold a copy of each stack until the next search change. */
    public static void clearCache() {
        cache.clear();
    }

    /**
     * The active category's compiled search, or null when there is none. A new object only when the search or the
     * settings it is read with change, so holders can tell it changed.
     */
    public static ItemFilter filter() {
        if (active == null) return null;
        SearchSyntax syntax = SearchSyntax.forEntry(active);
        if (!active.value.equals(compiledValue) || syntax != compiledSyntax) {
            compiledValue = active.value;
            compiledSyntax = syntax;
            ItemFilter compiled = syntax.compile(compiledValue);
            if (compiled != filter) {
                filter = compiled;
                cache.clear();
            }
        }
        return filter;
    }

    /** Drops the category if its tab was removed. Returns true if it was. */
    public static boolean dropIfRemoved(List<TabEntry> entries) {
        if (active == null) return false;
        for (TabEntry entry : entries) {
            if (entry == active) return false;
        }
        set(null);
        return true;
    }

    /**
     * Whether an item grid may show the stack. Anything that isn't an item, such as fluids in newer AE2, always
     * passes, as does every stack outside a terminal: the network status screen builds its list with the same repo
     * class, and so does the security terminal.
     */
    public static boolean accepts(Object stack) {
        if (active == null || !(stack instanceof IAEItemStack) || !isTerminalScreen()) return true;
        IAEItemStack item = (IAEItemStack) stack;
        ItemFilter filter = filter();
        Boolean cached = cache.get(item);
        if (cached == null) {
            ItemStack itemStack = item.getItemStack();
            // A fluid shown as an item, as AE2 Fluid Crafting shows them in item terminals, matches as its fluid.
            Fluid fluid = StandIns.fluidOf(itemStack);
            cached = fluid != null ? acceptsStandIns(fluid, StandIns.forFluid(fluid)) : filter.matches(itemStack);
            cache.put(item.copy(), cached);
        }
        return cached;
    }

    /** Whether AE2 Fluid Crafting's fluid or essentia grid may show the stack, one of its fluid drops. */
    public static boolean acceptsFluidDrop(Object stack) {
        if (active == null || !(stack instanceof IAEItemStack) || !isTerminalScreen()) return true;
        Fluid fluid = StandIns.fluidOf(((IAEItemStack) stack).getItemStack());
        return fluid == null || acceptsStandIns(fluid, StandIns.forFluid(fluid));
    }

    /** Whether Thaumic Energistics' essentia grid may show the stack, one of its aspect stacks. */
    public static boolean acceptsAspectStack(Object stack) {
        if (active == null || !isTerminalScreen()) return true;
        Object aspect = StandIns.aspectOfStack(stack);
        if (aspect == null) return true;
        Fluid gas = StandIns.gasOf(aspect);
        return acceptsStandIns(gas != null ? gas : aspect, StandIns.forAspect(aspect));
    }

    private static boolean acceptsStandIns(Object key, List<ItemStack> standIns) {
        ItemFilter filter = filter();
        Boolean cached = cache.get(key);
        if (cached == null) {
            cached = false;
            for (ItemStack standIn : standIns) {
                if (filter.matches(standIn)) {
                    cached = true;
                    break;
                }
            }
            cache.put(key, cached);
        }
        return cached;
    }

    private static boolean isTerminalScreen() {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        // Grid updates reach the terminal behind the tab editor too.
        return Terminal.of(screen) != null || screen instanceof GuiTabSettings;
    }
}
