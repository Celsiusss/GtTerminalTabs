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
    ShowTabsFiltered,
    ShowAll,
    SearchIn,
    ShowCategory,
    EditTab,
    ActiveCategory,
    HintClearSearch,
    HintShowAll,
    HintShowAllAgain,
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
    Done,
    PageTabs,
    PageSettings,
    OptionAlwaysPrefixes,
    OptionAlwaysPrefixesInfo,
    TabAlwaysPrefixes,
    TabAlwaysPrefixesInfo,
    TabAlwaysPrefixesGlobal;

    @Override
    public String getUnlocalized() {
        return "gui." + GtTerminalTabs.MODID + "." + name();
    }
}
