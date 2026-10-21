package net.celsiuss.gtterminaltabs.mixins.late;

import net.celsiuss.gtterminaltabs.client.SearchBoxIndicator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.client.gui.widgets.MEGuiTextField;

/**
 * The terminal hands clicks to its search box before NEI's input handlers see them, so clicks that clear the category
 * from the box are caught here.
 */
@Mixin(value = MEGuiTextField.class, remap = false)
public abstract class MixinMEGuiTextField {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 1)
    private void gtterminaltabs$clearCategory(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (SearchBoxIndicator.mouseClicked((MEGuiTextField) (Object) this, mouseX, mouseY, button)) ci.cancel();
    }
}
