package net.celsiuss.gtterminaltabs.mixins.late;

import java.util.ArrayList;

import net.celsiuss.gtterminaltabs.client.CategoryFilter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Applies the active category as Thaumic Energistics' essentia terminals build their view, after their own search;
 * the view's add is the method's only ArrayList add.
 */
// Pseudo: the mod is optional, so the target isn't on the compile classpath.
@Pseudo
@Mixin(targets = "thaumicenergistics.client.gui.GuiEssentiaCellTerminal", remap = false)
public abstract class MixinGuiEssentiaCellTerminal {

    @Redirect(
        method = "updateView",
        at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;add(Ljava/lang/Object;)Z"),
        require = 1)
    private boolean gtterminaltabs$filterCategory(ArrayList<Object> view, Object stack) {
        return CategoryFilter.acceptsAspectStack(stack) && view.add(stack);
    }
}
