package dev.imagecanvas;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Haelt alle Bilder und ist der Kern des Neustart-Fixes:
 * Map-IDs + Bildquelle werden in data.yml bzw. images/ gespeichert, und der Renderer
 * wird beim Start (und bei jedem MapInitializeEvent) wieder an die Karten gehaengt.
 */
public final class ImageStore {

    public record Entry(String name, String url, int cols, int rows, boolean hd, List<Integer> mapIds) {
    }

    private final ImageCanvasPlugin plugin;
    private final File dataFile;
    private final File imageDir;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final Map<Integer, TileRenderer> renderers = new ConcurrentHashMap<>();

    public ImageStore(ImageCanvasPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        this.imageDir = new File(plugin.getDataFolder(), "images");
    }

    public File sourceFile(String name) {
        return new File(imageDir, name + ".png");
    }

    public Entry get(String name) {
        return entries.get(name.toLowerCase(Locale.ROOT));
    }

    public List<Entry> all() {
        return new ArrayList<>(entries.values());
    }

    public List<String> names() {
        return new ArrayList<>(entries.keySet());
    }

    public void put(Entry entry) {
        entries.put(entry.name(), entry);
    }

    public void register(int mapId, TileRenderer renderer) {
        renderers.put(mapId, renderer);
    }

    /** Liest data.yml und baut alle Kacheln im Hintergrund neu auf, danach werden die Renderer angehaengt. */
    public void load() {
        entries.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = yaml.getConfigurationSection("images");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection s = section.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                String name = key.toLowerCase(Locale.ROOT);
                entries.put(name, new Entry(
                        name,
                        s.getString("url", ""),
                        s.getInt("cols", 1),
                        s.getInt("rows", 1),
                        s.getBoolean("hd", false),
                        List.copyOf(s.getIntegerList("maps"))));
            }
        }

        List<Entry> snapshot = new ArrayList<>(entries.values());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<Integer, TileRenderer> built = new HashMap<>();
            for (Entry entry : snapshot) {
                try {
                    BufferedImage source = loadSource(entry);
                    BufferedImage[] tiles = ImageUtil.buildTiles(source, entry.cols(), entry.rows(), entry.hd());
                    for (int i = 0; i < tiles.length && i < entry.mapIds().size(); i++) {
                        built.put(entry.mapIds().get(i), new TileRenderer(tiles[i]));
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Bild '" + entry.name() + "' konnte nicht geladen werden: " + e.getMessage());
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                renderers.putAll(built);
                attachAll();
                plugin.getLogger().info(snapshot.size() + " Bild(er) geladen, " + built.size() + " Karten verbunden.");
            });
        });
    }

    public void reload() {
        load();
    }

    private BufferedImage loadSource(Entry entry) throws IOException, InterruptedException {
        File file = sourceFile(entry.name());
        if (file.isFile()) {
            BufferedImage image = ImageUtil.read(file);
            if (image != null) {
                return image;
            }
        }
        // Lokale Datei fehlt oder ist kaputt: erneut von der URL holen
        BufferedImage image = ImageUtil.download(URI.create(entry.url()));
        ImageUtil.savePng(image, file);
        return image;
    }

    public void attachAll() {
        for (int id : renderers.keySet()) {
            MapView view = Bukkit.getMap(id);
            if (view == null) {
                plugin.getLogger().warning("Karte #" + id + " existiert nicht mehr (Weltdaten geloescht?).");
                continue;
            }
            attach(view);
        }
    }

    /** Ersetzt die Standard-Renderer einer Karte durch unseren Renderer (idempotent). */
    public void attach(MapView view) {
        TileRenderer renderer = renderers.get(view.getId());
        if (renderer == null || view.getRenderers().contains(renderer)) {
            return;
        }
        for (MapRenderer old : new ArrayList<>(view.getRenderers())) {
            view.removeRenderer(old);
        }
        view.setTrackingPosition(false);
        view.addRenderer(renderer);
    }

    public void forget(UUID playerId) {
        for (TileRenderer renderer : renderers.values()) {
            renderer.forget(playerId);
        }
    }

    public boolean remove(String name) {
        Entry entry = entries.remove(name.toLowerCase(Locale.ROOT));
        if (entry == null) {
            return false;
        }
        for (int id : entry.mapIds()) {
            TileRenderer renderer = renderers.remove(id);
            MapView view = Bukkit.getMap(id);
            if (view != null && renderer != null) {
                view.removeRenderer(renderer);
            }
        }
        sourceFile(entry.name()).delete();
        save();
        return true;
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Entry entry : entries.values()) {
            String base = "images." + entry.name();
            yaml.set(base + ".url", entry.url());
            yaml.set(base + ".cols", entry.cols());
            yaml.set(base + ".rows", entry.rows());
            yaml.set(base + ".hd", entry.hd());
            yaml.set(base + ".maps", entry.mapIds());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("data.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    /** Karten-Items des Bildes in Zeilen-Reihenfolge (Index = row * cols + col). */
    public List<ItemStack> mapItems(Entry entry) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < entry.mapIds().size(); i++) {
            MapView view = Bukkit.getMap(entry.mapIds().get(i));
            if (view == null) {
                plugin.getLogger().warning("Karte #" + entry.mapIds().get(i) + " von '" + entry.name() + "' fehlt.");
                continue;
            }
            ItemStack item = new ItemStack(Material.FILLED_MAP);
            MapMeta meta = (MapMeta) item.getItemMeta();
            meta.setMapView(view);
            int col = (i % entry.cols()) + 1;
            int row = (i / entry.cols()) + 1;
            meta.displayName(Component.text(entry.name() + " [" + col + "," + row + "]")
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
            items.add(item);
        }
        return items;
    }
}
