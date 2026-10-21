package net.celsiuss.gtterminaltabs.client;

import java.util.Arrays;
import java.util.List;

/**
 * The tabs a new player starts with, written to tabs.json the first time it is missing. Each matches mods by their
 * display name, which is what NEI's @ search reads; quotes keep a name with spaces together. They always use prefixes,
 * so they match the same items whatever the player's NEI settings.
 * <p>
 * Names are plain English: these are created in preInit, before mod translations load.
 */
final class DefaultTabs {

    private DefaultTabs() {}

    static List<TabEntry> create() {
        return Arrays.asList(
            // "energistics" covers Applied Energistics 2 and Thaumic Energistics; "ae2" covers AE2 Fluid Crafting,
            // AE2 Stuff and the AE2 Wireless Crafting Terminal.
            tab(
                "Applied Energistics",
                "@energistics | @ae2 | @betterp2p",
                "appliedenergistics2:tile.BlockController",
                0),
            // "forestry" also covers BiblioWoods Forestry Edition; the rest are Binnie's mods, Gendustry and Magic
            // Bees.
            tab(
                "Forestry",
                "@forestry | @\"binnie core\" | @\"extra bees\" | @\"extra trees\" | @botany | @genetics | @gendustry"
                    + " | @\"magic bees\"",
                "Forestry:beeCombs",
                0),
            tab(
                "Food",
                "@harvestcraft | @\"harvest the nether\" | @\"cooking for blockheads\" | @\"spice of life\"",
                "harvestcraft:hamburgerItem",
                0),
            // "thaum" covers Thaumcraft and its "Thaumic ..." add-ons. "electro" is Electro-Magic Tools and "wands" is
            // GTNH-TC-Wands, each one word because a "-" can start a NOT.
            tab(
                "Thaumcraft",
                "@thaum | @automagy | @\"forbidden magic\" | @\"tainted magic\" | @gadomancy | @\"witching gadgets\""
                    + " | @electro | @\"salis arcana\" | @wands",
                "Thaumcraft:ItemThaumometer",
                0),
            tab("Circuits", "$circuit", "IC2:itemPartCircuit", 0),
            tab("Wires & Cables", "$wireGt | $cableGt", "IC2:itemCable", 0),
            // By registry name: IC2's seed bags, which Crops++ also uses, and the seeds of CropsNH, which replaces IC2
            // crops from GTNH 2.9.
            tab("Seeds", "&ic2:itemcropseed | &cropsnh:genericseed", "minecraft:wheat_seeds", 0));
    }

    private static TabEntry tab(String name, String search, String iconItem, int iconMeta) {
        TabIcon icon = new TabIcon();
        icon.item = iconItem;
        icon.meta = iconMeta;
        // A missing mod just leaves the tab without an icon; TabIcon resolves lazily.
        TabEntry entry = new TabEntry(name, search, icon);
        entry.alwaysUsePrefixes = true;
        return entry;
    }
}
