package net.celsiuss.gtterminaltabs.client;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import net.celsiuss.gtterminaltabs.GtTerminalTabs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

/** The client's tab list and panel state, shared by every terminal and world, persisted as JSON. */
public class TabStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();
    private static final Type ENTRY_LIST = new TypeToken<List<TabEntry>>() {}.getType();

    private final File file;
    private final List<TabEntry> entries = new ArrayList<>();
    private boolean grid;
    private boolean hidden;

    public TabStore(File file) {
        this.file = file;
    }

    /** The live list; callers edit it in place and call {@link #save()}. */
    public List<TabEntry> entries() {
        return entries;
    }

    /** Whether the terminal panel shows tabs as a grid of icons rather than a list of named rows. */
    public boolean isGrid() {
        return grid;
    }

    public void setGrid(boolean grid) {
        this.grid = grid;
    }

    /** Whether the terminal panel is collapsed to its show button. */
    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void load() {
        entries.clear();
        grid = false;
        hidden = false;
        if (!file.isFile()) return;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement root = new JsonParser().parse(reader);
            // Older files are a bare array of tabs.
            JsonElement tabs = root;
            if (root.isJsonObject()) {
                JsonObject object = root.getAsJsonObject();
                grid = object.has("grid") && object.get("grid")
                    .getAsBoolean();
                hidden = object.has("hidden") && object.get("hidden")
                    .getAsBoolean();
                tabs = object.get("tabs");
            }
            List<TabEntry> loaded = GSON.fromJson(tabs, ENTRY_LIST);
            if (loaded == null) return;
            for (TabEntry entry : loaded) {
                if (entry == null) continue;
                entries.add(new TabEntry(nullToEmpty(entry.name), nullToEmpty(entry.value), entry.icon));
            }
        } catch (IOException | JsonParseException | IllegalStateException | UnsupportedOperationException e) {
            GtTerminalTabs.LOG.warn("Could not read {}, starting with no tabs", file, e);
        }
    }

    public void save() {
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs()) {
            GtTerminalTabs.LOG.warn("Could not create {}", dir);
            return;
        }
        // Written beside the file and moved over it, so a crash or full disk mid-write leaves the old file intact.
        Path target = file.toPath();
        Path temp = target.resolveSibling(file.getName() + ".tmp");
        try {
            JsonObject root = new JsonObject();
            root.addProperty("grid", grid);
            root.addProperty("hidden", hidden);
            root.add("tabs", GSON.toJsonTree(entries, ENTRY_LIST));
            Files.write(
                temp,
                GSON.toJson(root)
                    .getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            GtTerminalTabs.LOG.warn("Could not write {}", file, e);
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {}
        }
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
