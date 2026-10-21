package net.celsiuss.gtterminaltabs.mixins.late;

import java.util.ArrayList;

import net.celsiuss.gtterminaltabs.client.CategoryFilter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Applies the active category as AE2 Fluid Crafting builds its fluid terminals' view, after its own search. Both of
 * the method's adds go to the view: one for an @mod search, one for a name search.
 */
// Pseudo: the mod is optional, so the target isn't on the compile classpath.
@Pseudo
@Mixin(targets = "com.glodblock.github.client.me.FluidRepo", remap = false)
public abstract class MixinFluidRepo {

    @Redirect(
        method = "addEntriesToView",
        at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"),
        require = 1)
    private boolean gtterminaltabs$filterCategory(ArrayList<Object> view, Object stack) {
        return CategoryFilter.acceptsFluidDrop(stack) && view.add(stack);
    }
}
