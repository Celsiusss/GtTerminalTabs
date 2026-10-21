package net.celsiuss.gtterminaltabs.client;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.celsiuss.gtterminaltabs.GtTerminalTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Items that stand for a fluid or a Thaumcraft aspect, so a tab's NEI item search can be asked about them. A fluid or
 * aspect matches when any of its stand-ins does:
 * <ul>
 * <li>the fluid as NEI shows it: GregTech's fluid display item, or AE2 Fluid Crafting's without GregTech. It carries
 * the fluid's name and tooltip, but GregTech's or AE2 Fluid Crafting's mod name;</li>
 * <li>a filled container from the fluid's own mod, such as a bucket, so an @mod search finds the fluid's mod;</li>
 * <li>for essentia, Thaumic Energistics' aspect item, so @thaum finds it.</li>
 * </ul>
 * Everything here is optional and reached by name, since none of these mods is needed.
 */
final class StandIns {

    private static final Method gtFluidDisplay = findMethod(
        "GregTech fluid display",
        new String[] { "gregtech.api.util.GTUtility", "gregtech.api.util.GT_Utility" },
        "getFluidDisplayStack",
        FluidStack.class,
        boolean.class);
    private static final Method fcDisplayStack = findMethod(
        "AE2 Fluid Crafting fluid display",
        new String[] { "com.glodblock.github.common.item.ItemFluidDrop" },
        "newDisplayStack",
        FluidStack.class);
    private static final String FC_DROP = "com.glodblock.github.common.item.ItemFluidDrop";
    private static final String FC_PACKET = "com.glodblock.github.common.item.ItemFluidPacket";
    private static final Method fcFluidOfDrop = findMethod(
        "AE2 Fluid Crafting fluid drop",
        new String[] { FC_DROP },
        "getFluidStack",
        ItemStack.class);
    private static final Method fcFluidOfPacket = findMethod(
        "AE2 Fluid Crafting fluid packet",
        new String[] { FC_PACKET },
        "getFluidStack",
        ItemStack.class);
    private static final Method gasOfAspect = findMethod(
        "Thaumic Energistics essentia gas",
        new String[] { "thaumicenergistics.common.fluids.GaseousEssentia" },
        "getGasFromAspect",
        aspectClass());
    private static final Method aspectItem = findMethod(
        "Thaumic Energistics aspect item",
        new String[] { "thaumicenergistics.common.items.ItemCraftingAspect" },
        "createStackForAspect",
        aspectClass(),
        int.class);

    private static final Method aspectOfAspectStack = findInstanceMethod(
        "Thaumic Energistics aspect stack",
        "thaumicenergistics.api.storage.IAspectStack",
        "getAspect");

    private static final Map<Fluid, List<ItemStack>> fluidCache = new HashMap<>();

    private StandIns() {}

    /**
     * The fluid that one of AE2 Fluid Crafting's fluid drops or fluid packets stands for, or null if the stack is
     * neither. Both are AE2 Fluid Crafting's items, so asked as items they'd all match @ae2.
     */
    static Fluid fluidOf(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        String type = stack.getItem()
            .getClass()
            .getName();
        Method fluidOf = type.equals(FC_DROP) ? fcFluidOfDrop : type.equals(FC_PACKET) ? fcFluidOfPacket : null;
        FluidStack fluid = (FluidStack) invoke(fluidOf, stack);
        return fluid == null ? null : fluid.getFluid();
    }

    /** The aspect in one of Thaumic Energistics' aspect stacks, or null if it can't be read. */
    static Object aspectOfStack(Object stack) {
        if (aspectOfAspectStack == null || stack == null) return null;
        try {
            return aspectOfAspectStack.invoke(stack);
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            return null;
        }
    }

    /** Thaumic Energistics' gas for the aspect, or null if it has none. */
    static Fluid gasOf(Object aspect) {
        return (Fluid) invoke(gasOfAspect, aspect);
    }

    static List<ItemStack> forFluid(Fluid fluid) {
        List<ItemStack> standIns = fluidCache.get(fluid);
        if (standIns == null) {
            standIns = build(fluid);
            fluidCache.put(fluid, standIns);
        }
        return standIns;
    }

    /** An aspect without a gas still has Thaumic Energistics' aspect item. */
    static List<ItemStack> forAspect(Object aspect) {
        Fluid gas = gasOf(aspect);
        if (gas != null) return forFluid(gas);
        ItemStack item = (ItemStack) invoke(aspectItem, aspect, 1);
        return item == null ? Collections.emptyList() : Collections.singletonList(item);
    }

    private static List<ItemStack> build(Fluid fluid) {
        List<ItemStack> standIns = new ArrayList<>();
        FluidStack stack = new FluidStack(fluid, FluidContainerRegistry.BUCKET_VOLUME);
        ItemStack display = (ItemStack) invoke(gtFluidDisplay, stack, false);
        if (display == null) display = (ItemStack) invoke(fcDisplayStack, stack);
        if (display != null) standIns.add(display);

        ItemStack container = ownModContainer(fluid);
        if (container != null) standIns.add(container);

        Object aspect = aspectOfGas(fluid);
        if (aspect != null) {
            ItemStack item = (ItemStack) invoke(aspectItem, aspect, 1);
            if (item != null) standIns.add(item);
        }
        return standIns;
    }

    private static ItemStack ownModContainer(Fluid fluid) {
        String name = FluidRegistry.getDefaultFluidName(fluid);
        int colon = name == null ? -1 : name.indexOf(':');
        if (colon < 0) return null;
        String modId = name.substring(0, colon);
        for (FluidContainerRegistry.FluidContainerData data : FluidContainerRegistry
            .getRegisteredFluidContainerData()) {
            if (data.fluid == null || data.fluid.getFluid() != fluid || data.filledContainer == null) continue;
            Item item = data.filledContainer.getItem();
            GameRegistry.UniqueIdentifier id = item == null ? null : GameRegistry.findUniqueIdentifierFor(item);
            if (id != null && id.modId.equals(modId)) return data.filledContainer.copy();
        }
        return null;
    }

    /** Thaumic Energistics' gases know their aspect. */
    private static Object aspectOfGas(Fluid fluid) {
        try {
            Method getAspect = fluid.getClass()
                .getMethod("getAspect");
            return getAspect.getReturnType() == aspectClass() ? getAspect.invoke(fluid) : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Class<?> aspectClass() {
        try {
            return Class.forName("thaumcraft.api.aspects.Aspect", false, StandIns.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Object invoke(Method method, Object... args) {
        if (method == null) return null;
        try {
            return method.invoke(null, args);
        } catch (ReflectiveOperationException | IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }

    private static Method findInstanceMethod(String what, String className, String name) {
        try {
            return Class.forName(className, false, StandIns.class.getClassLoader())
                .getMethod(name);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (NoSuchMethodException e) {
            GtTerminalTabs.LOG.warn("No {} ({}.{}); essentia won't be filtered", what, className, name);
            return null;
        }
    }

    /** The static method on the first of the classes that exists, or null when the mod isn't there. */
    private static Method findMethod(String what, String[] classNames, String name, Class<?>... parameters) {
        for (Class<?> parameter : parameters) {
            if (parameter == null) return null;
        }
        for (String className : classNames) {
            try {
                Class<?> owner = Class.forName(className, false, StandIns.class.getClassLoader());
                return owner.getMethod(name, parameters);
            } catch (ClassNotFoundException e) {
                // Try the next name, or the mod isn't installed.
            } catch (NoSuchMethodException e) {
                GtTerminalTabs.LOG.warn("No {} ({}.{}); fluids will match fewer tab searches", what, className, name);
                return null;
            }
        }
        return null;
    }
}
