package com.imagecanvas.storage;

import com.imagecanvas.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.entity.Player;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageMapStorage {
    private final Main plugin;
    private final File configFile;
    private final File imagesFolder;
    private YamlConfiguration config;

    public ImageMapStorage(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "maps.yml");
        this.imagesFolder = new File(plugin.getDataFolder(), "saved_images");

        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs();
        }

        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte die Datei maps.yml nicht erstellen!");
            }
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Speichert die Kartendaten und legt das Bild als PNG im Plugin-Ordner ab.
     */
    public void registerImage(String name, List<Integer> mapIds, int cols, int rows, BufferedImage fullImage) {
        String path = "images." + name;
        config.set(path + ".ids", mapIds);
        config.set(path + ".cols", cols);
        config.set(path + ".rows", rows);
        
        // Speichere das Orginal-Bild auf die Festplatte
        File imageFile = new File(imagesFolder, name + ".png");
        try {
            ImageIO.write(fullImage, "png", imageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Konnte das Bild " + name + ".png nicht speichern!");
        }

        saveMaps();
    }

    public void saveMaps() {
        try {
            config.save(configFile);
            plugin.getLogger().info("Karten-Daten wurden erfolgreich in maps.yml gespeichert.");
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Schreiben in die maps.yml!");
        }
    }

    /**
     * LÄDT UND RESTRUKTURIERT DIE MAPS BEIM SERVERSTART
     */
    public void loadMaps() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
        if (!config.contains("images")) return;

        int restoredMapsCount = 0;

        for (String name : config.getConfigurationSection("images").getKeys(false)) {
            List<Integer> mapIds = getMapIds(name);
            int cols = getCols(name);
            int rows = getRows(name);

            File imageFile = new File(imagesFolder, name + ".png");
            if (!imageFile.exists()) {
                plugin.getLogger().warning("Bild-Datei fuer " + name + " fehlt! Kann Karten nicht wiederherstellen.");
                continue;
            }

            try {
                BufferedImage fullImage = ImageIO.read(imageFile);
                if (fullImage == null) continue;

                int index = 0;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        if (index >= mapIds.size()) break;
                        int mapId = mapIds.get(index);

                        // Schneide das Teilstück für die Karte aus
                        BufferedImage subImage = fullImage.getSubimage(c * 128, r * 128, 128, 128);

                        @SuppressWarnings("deprecation")
                        MapView mapView = Bukkit.getMap(mapId);
                        if (mapView != null) {
                            // Alte Renderer löschen
                            for (MapRenderer renderer : mapView.getRenderers()) {
                                mapView.removeRenderer(renderer);
                            }
                            // Neuen Custom-Renderer aufschalten
                            mapView.addRenderer(new CustomMapRenderer(subImage));
                            restoredMapsCount++;
                        }
                        index++;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Fehler beim Wiederherstellen von Bild " + name + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Es wurden erfolgreich " + restoredMapsCount + " Karten-Abschnitte aus der Config geladen!");
    }

    public List<String> getSavedImages() {
        if (!config.contains("images")) return new ArrayList<>();
        return new ArrayList<>(config.getConfigurationSection("images").getKeys(false));
    }

    public List<Integer> getMapIds(String name) {
        return config.getIntegerList("images." + name + ".ids");
    }

    public int getCols(String name) {
        return config.getInt("images." + name + ".cols");
    }

    public int getRows(String name) {
        return config.getInt("images." + name + ".rows");
    }

    // Der Renderer, der das Bild auf die Minecraft-Karte zeichnet
    private static class CustomMapRenderer extends MapRenderer {
        private final BufferedImage image;
        private boolean rendered = false;

        public CustomMapRenderer(BufferedImage image) {
            super(true);
            this.image = image;
        }

        @Override
        public void render(MapView map, MapCanvas canvas, Player player) {
            if (!rendered) {
                canvas.drawImage(0, 0, image);
                rendered = true;
            }
        }
    }
}
