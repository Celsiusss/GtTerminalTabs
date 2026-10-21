package net.celsiuss.gtterminaltabs.mixins.late;

import java.util.ArrayList;

import net.celsiuss.gtterminaltabs.client.CategoryFilter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The same for AE2 Fluid Crafting's essentia terminal, which builds its view in its own private copy of the method. */
// Pseudo: the mod is optional, so the target isn't on the compile classpath.
@Pseudo
@Mixin(targets = "com.glodblock.github.client.me.EssentiaRepo", remap = false)
public abstract class MixinEssentiaRepo {

    @Redirect(
        method = "addEntriesToView",
        at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"),
        require = 1)
    private boolean gtterminaltabs$filterCategory(ArrayList<Object> view, Object stack) {
        return CategoryFilter.acceptsFluidDrop(stack) && view.add(stack);
    }
}
