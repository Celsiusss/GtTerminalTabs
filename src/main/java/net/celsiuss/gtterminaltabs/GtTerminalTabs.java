package net.celsiuss.gtterminaltabs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = GtTerminalTabs.MODID,
    version = Tags.VERSION,
    name = "GtTerminalTabs",
    acceptedMinecraftVersions = "[1.7.10]",
    // Client-only features, so either side may run without it.
    acceptableRemoteVersions = "*",
    dependencies = "required-after:appliedenergistics2;required-after:NotEnoughItems;required-after:gtnhmixins")
public class GtTerminalTabs {

    public static final String MODID = "gtterminaltabs";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(
        clientSide = "net.celsiuss.gtterminaltabs.ClientProxy",
        serverSide = "net.celsiuss.gtterminaltabs.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}
