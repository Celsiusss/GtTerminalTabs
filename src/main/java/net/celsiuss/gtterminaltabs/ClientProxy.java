package net.celsiuss.gtterminaltabs;

import java.io.File;

import net.celsiuss.gtterminaltabs.client.NeiCategoryFilter;
import net.celsiuss.gtterminaltabs.client.TabStore;
import net.celsiuss.gtterminaltabs.client.TabsConfig;
import net.celsiuss.gtterminaltabs.client.TerminalOverlayHandler;
import net.minecraftforge.common.MinecraftForge;

import codechicken.nei.api.API;
import codechicken.nei.guihook.GuiContainerManager;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    private TabStore tabStore;

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        File dir = new File(event.getModConfigurationDirectory(), GtTerminalTabs.MODID);
        TabsConfig.load(new File(dir, GtTerminalTabs.MODID + ".cfg"));
        tabStore = new TabStore(new File(dir, "tabs.json"));
        tabStore.load();
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        NeiCategoryFilter neiFilter = new NeiCategoryFilter();
        API.addItemFilter(neiFilter);
        TerminalOverlayHandler overlay = new TerminalOverlayHandler(tabStore, neiFilter);
        MinecraftForge.EVENT_BUS.register(overlay);
        API.registerNEIGuiHandler(overlay);
        GuiContainerManager.addDrawHandler(overlay);
        // Ahead of NEI's own handlers, which would otherwise take wheel input over our panel.
        GuiContainerManager.inputHandlers.addFirst(overlay);
    }
}
