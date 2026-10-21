package net.celsiuss.gtterminaltabs.mixins.late;

import java.util.ArrayList;

import net.celsiuss.gtterminaltabs.client.CategoryFilter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import appeng.client.me.ItemRepo;

/**
 * Applies the active category as AE2 builds the terminal's view, after its own search, view cell and mode filters.
 * The method is named without a descriptor because its parameters differ between AE2 versions; in each, the view's
 * add is its only ArrayList add. Required, so a future AE2 without it fails loudly when the class loads rather
 * than silently leaving the grid unfiltered.
 */
@Mixin(value = ItemRepo.class, remap = false)
public abstract class MixinItemRepo {

    @Redirect(
        method = "addEntriesToView",
        at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"),
        require = 1)
    private boolean gtterminaltabs$filterCategory(ArrayList<Object> view, Object stack) {
        return CategoryFilter.accepts(stack) && view.add(stack);
    }
}
