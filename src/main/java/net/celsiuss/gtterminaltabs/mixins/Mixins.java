package net.celsiuss.gtterminaltabs.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    ME_UPDATES_WHILE_EDITING(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.AE2)
        .addClientMixins("MixinPacketMEInventoryUpdate")),
    CATEGORY_FILTER(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.AE2)
        .addClientMixins("MixinItemRepo", "MixinMEGuiTextField")),
    FLUID_CATEGORY_FILTER(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.AE2_FLUID_CRAFTING)
        .addClientMixins("MixinFluidRepo", "MixinEssentiaRepo")),
    ESSENTIA_CATEGORY_FILTER(new MixinBuilder().setPhase(Phase.LATE)
        .addRequiredMod(TargetMods.THAUMIC_ENERGISTICS)
        .addClientMixins("MixinGuiEssentiaCellTerminal"));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
