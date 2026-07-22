package io.gdx.cdda.bn.nextgen.view;

import java.util.ArrayList;
import java.util.List;

/** Shortcut reference rows for {@link KeybindHelpDialog} in the map editor. */
public final class MapEditorKeybindHelp {

    private MapEditorKeybindHelp() {}

    public static List<KeybindHelpDialog.HelpRow> build(
        final boolean overmapAvailable,
        final boolean inOvermapMode,
        final boolean multiFloorBuilding
    ) {
        return build(overmapAvailable, inOvermapMode, multiFloorBuilding, false);
    }

    public static List<KeybindHelpDialog.HelpRow> build(
        final boolean overmapAvailable,
        final boolean inOvermapMode,
        final boolean multiFloorBuilding,
        final boolean localExploreActive
    ) {
        final List<KeybindHelpDialog.HelpRow> rows = new ArrayList<>();

        rows.add(KeybindHelpDialog.HelpRow.section("General"));
        rows.add(KeybindHelpDialog.HelpRow.bind("F1 / H", "Open / close this help"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Esc", "Close dialog, clear filter, or main menu"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Arrow keys", "Pan camera"));
        rows.add(KeybindHelpDialog.HelpRow.bind("+ / -", "Zoom in / out"));
        rows.add(KeybindHelpDialog.HelpRow.bind("C", "Center camera on grid"));
        rows.add(KeybindHelpDialog.HelpRow.bind("F3", "Toggle pointer debug logging"));
        rows.add(KeybindHelpDialog.HelpRow.bind("F5", "Reload active tileset"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Space + drag", "Pan (any tool)"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Wheel on canvas", "Zoom"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Wheel on palette", "Scroll terrain list"));

        rows.add(KeybindHelpDialog.HelpRow.section("Toolbar (bottom)"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Paint", "Left-click / drag to paint active layer"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Pan", "Middle or right-drag to move view"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Pick", "Sample terrain or furniture under cursor"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Ter / Furn", "Toggle terrain vs furniture brush"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Spawns", "Toggle item/monster spawn overlay"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Chunks", "Toggle OMT chunk border lines"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Save / Load", "Write / read maps/autosave.json"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Mapgen", "Open json mapgen import picker"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Grid", "Cycle submap size preset"));
        rows.add(KeybindHelpDialog.HelpRow.bind("OMT", "Toggle overmap mode (same as M)"));
        rows.add(KeybindHelpDialog.HelpRow.bind("< / >", "Previous / next tileset"));

        rows.add(KeybindHelpDialog.HelpRow.section("Submap editing"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Left drag", "Paint with active brush"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Right click", "Eyedropper (terrain or furniture)"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Click filter row", "Focus palette filter (sidebar)"));
        rows.add(KeybindHelpDialog.HelpRow.bind("L", "Cycle terrain / furniture brush layer"));
        rows.add(KeybindHelpDialog.HelpRow.bind("O", "Toggle spawn marker overlay"));
        rows.add(KeybindHelpDialog.HelpRow.bind("B", "Toggle OMT chunk border lines"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Ctrl+S / Ctrl+O", "Save / load map file"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Ctrl+G", "Import json mapgen"));
        rows.add(KeybindHelpDialog.HelpRow.bind("G", "Cycle grid preset (10…64)"));
        rows.add(KeybindHelpDialog.HelpRow.bind("F", "Toggle furniture sprite visibility"));
        rows.add(KeybindHelpDialog.HelpRow.bind("P", "Toggle sprite animation"));
        if (overmapAvailable) {
            rows.add(KeybindHelpDialog.HelpRow.bind("M", "Switch to overmap mode"));
        }
        if (multiFloorBuilding) {
            rows.add(KeybindHelpDialog.HelpRow.section("Building floors"));
            rows.add(KeybindHelpDialog.HelpRow.bind("PgUp / PgDn", "Previous / next floor"));
            rows.add(KeybindHelpDialog.HelpRow.bind("[ / ]", "Previous / next floor"));
            rows.add(KeybindHelpDialog.HelpRow.bind("T", "Toggle upper-floor cutaway"));
        }
        rows.add(KeybindHelpDialog.HelpRow.bind("Ctrl+Shift+C", "Copy building piece layout (submap) or overmap JSON (overmap mode)"));

        if (overmapAvailable) {
            rows.add(KeybindHelpDialog.HelpRow.section("Overmap mode (M)"));
            rows.add(KeybindHelpDialog.HelpRow.bind("Click", "Select OMT cell"));
            rows.add(KeybindHelpDialog.HelpRow.bind("Ctrl+Shift+C", "Copy overmap JSON to clipboard + maps/overmap_export.json"));
            rows.add(KeybindHelpDialog.HelpRow.bind("Enter", "Visit — stitch neighborhood and walkaround"));
            rows.add(KeybindHelpDialog.HelpRow.bind("G", "Choose region_settings profile"));
            rows.add(KeybindHelpDialog.HelpRow.bind("[ / ]", "Smaller / larger overmap (8…256)"));
            rows.add(KeybindHelpDialog.HelpRow.bind("R", "Regenerate layout (new seed)"));
            rows.add(KeybindHelpDialog.HelpRow.bind("M", "Return to submap view"));
            rows.add(KeybindHelpDialog.HelpRow.bind("Esc", "Clear OMT selection"));
        }

        if (localExploreActive) {
            rows.add(KeybindHelpDialog.HelpRow.section("Local walkaround (after Enter)"));
            rows.add(KeybindHelpDialog.HelpRow.bind("Left drag", "Pan — loads nearby OMTs as focus moves"));
            rows.add(KeybindHelpDialog.HelpRow.bind("Arrow keys", "Pan — same neighborhood restitch"));
            rows.add(KeybindHelpDialog.HelpRow.bind("Middle / right drag", "Pan without changing tool"));
            rows.add(KeybindHelpDialog.HelpRow.bind("M", "Leave walkaround / return to overmap"));
        }

        rows.add(KeybindHelpDialog.HelpRow.section("Mapgen picker (Ctrl+G)"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Up / Down", "Move selection"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Enter", "Import selected mapgen"));
        rows.add(KeybindHelpDialog.HelpRow.bind("R", "Random weighted variant"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Type", "Filter list"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Esc", "Cancel"));

        rows.add(KeybindHelpDialog.HelpRow.section("Region picker (overmap G)"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Up / Down", "Move selection"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Enter", "Apply region and regenerate"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Type", "Filter list"));
        rows.add(KeybindHelpDialog.HelpRow.bind("Esc", "Cancel"));

        if (inOvermapMode) {
            rows.add(KeybindHelpDialog.HelpRow.section("Note"));
            rows.add(KeybindHelpDialog.HelpRow.bind("", "You are in overmap mode — [ ] and R apply here."));
        }

        return rows;
    }
}
