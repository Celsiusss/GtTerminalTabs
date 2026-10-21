package net.celsiuss.gtterminaltabs.mixins.late;

import java.util.List;

import net.celsiuss.gtterminaltabs.client.GuiTabSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.storage.data.IAEItemStack;
import appeng.client.gui.implementations.GuiMEMonitorable;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketMEInventoryUpdate;

/**
 * AE2 hands grid updates only to the current screen, and never resends what was missed. While the tab editor is
 * open over a terminal, pass them on to the terminal behind it so its counts are current when it returns.
 */
@Mixin(value = PacketMEInventoryUpdate.class, remap = false)
public abstract class MixinPacketMEInventoryUpdate {

    @Shadow
    @Final
    private List<IAEItemStack> list;

    @Inject(method = "clientPacketData", at = @At("HEAD"))
    private void gtterminaltabs$forwardToTerminal(INetworkInfo network, AppEngPacket packet, EntityPlayer player,
        CallbackInfo ci) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (list == null || !(screen instanceof GuiTabSettings)) return;
        GuiScreen parent = ((GuiTabSettings) screen).getParent();
        if (parent instanceof GuiMEMonitorable) ((GuiMEMonitorable) parent).postUpdate(list);
    }
}
