package net.celsiuss.gtterminaltabs.client;

import java.io.IOException;
import java.util.Base64;

import net.celsiuss.gtterminaltabs.GtTerminalTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;

/**
 * An item to display, stored by registry name rather than numeric id so it survives id remapping between worlds.
 * NBT is kept because many GT items (cells, tools) differ only by NBT.
 */
public class TabIcon {

    public String item;
    public int meta;
    /**
     * Base64 of the gzipped binary NBT. Not 1.7.10's text form, which can't read back quotes or backslashes in
     * strings, byte arrays, or some numbers.
     */
    public String nbtData;
    /** The text form older files used; read once and replaced by {@link #nbtData}. */
    public String nbt;

    // Resolved lazily: items aren't registered yet when the store loads during preInit.
    private transient ItemStack stack;
    private transient boolean resolved;

    public TabIcon() {}

    public static TabIcon of(ItemStack source) {
        TabIcon icon = new TabIcon();
        icon.item = Item.itemRegistry.getNameForObject(source.getItem());
        icon.meta = source.getItemDamage();
        icon.nbtData = source.hasTagCompound() ? encode(source.getTagCompound()) : null;
        icon.stack = source.copy();
        icon.stack.stackSize = 1;
        icon.resolved = true;
        return icon;
    }

    /** Returns the stack to render, or null if the item no longer exists. */
    public ItemStack toStack() {
        if (resolved) return stack;
        resolved = true;
        Object found = item == null ? null : Item.itemRegistry.getObject(item);
        if (!(found instanceof Item)) return null;
        stack = new ItemStack((Item) found, 1, meta);
        // Keep the plain item if the tag is bad; it shouldn't cost the icon entirely.
        NBTTagCompound tag = nbtData != null ? decode(nbtData) : parseLegacy(nbt);
        if (tag != null) {
            stack.setTagCompound(tag);
            if (nbtData == null) {
                nbtData = encode(tag);
                nbt = null;
            }
        }
        return stack;
    }

    private static String encode(NBTTagCompound tag) {
        try {
            return Base64.getEncoder()
                .encodeToString(CompressedStreamTools.compress(tag));
        } catch (IOException e) {
            GtTerminalTabs.LOG.warn("Could not encode icon NBT {}", tag, e);
            return null;
        }
    }

    private NBTTagCompound decode(String data) {
        try {
            return CompressedStreamTools.func_152457_a(
                Base64.getDecoder()
                    .decode(data),
                NBTSizeTracker.field_152451_a);
        } catch (IOException | IllegalArgumentException e) {
            GtTerminalTabs.LOG.warn("Could not read the NBT of icon {}:{}", item, meta, e);
            return null;
        }
    }

    private NBTTagCompound parseLegacy(String text) {
        if (text == null) return null;
        try {
            NBTBase tag = JsonToNBT.func_150315_a(text);
            if (tag instanceof NBTTagCompound) return (NBTTagCompound) tag;
        } catch (NBTException e) {
            GtTerminalTabs.LOG.warn("Could not read the NBT of icon {}:{}", item, meta, e);
        }
        return null;
    }
}
