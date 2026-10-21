package net.celsiuss.gtterminaltabs.client;

import net.celsiuss.gtterminaltabs.GtTerminalTabs;

import appeng.core.localization.Localization;

/** Lang keys for our GUI strings, mirroring AE2's {@code GuiText}: {@code gui.gtterminaltabs.<Name>}. */
public enum GtGuiText implements Localization {

    Settings,
    GridView,
    ListView,
    HideTabs,
    ShowTabs,
    TabSettings,
    NoTabs,
    PanelEmpty,
    Name,
    SearchValue,
    Icon,
    IconHint,
    IconClear,
    NewTab,
    AddTab,
    RemoveTab,
    MoveUp,
    MoveDown,
    Done;

    @Override
    public String getUnlocalized() {
        return "gui." + GtTerminalTabs.MODID + "." + name();
    }
}
