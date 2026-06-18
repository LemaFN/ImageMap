package com.imagecanvas.storage;

import com.imagecanvas.Main;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

public class ImageMapStorage {
    private final Main plugin;
    private final File configFile;
    private YamlConfiguration config;

    public ImageMapStorage(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "maps.yml");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Konnte maps.yml nicht erstellen!");
            }
        }
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveMaps() {
        // Hier wird später die Logik eingefügt, die alle aktiven Map-IDs speichert
        try {
            config.save(configFile);
            plugin.getLogger().info("Karten-Daten wurden erfolgreich in maps.yml gespeichert.");
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Speichern der Karten!");
        }
    }

    public void loadMaps() {
        // Hier wird beim Serverstart geprüft, welche Karten geladen werden müssen
        this.config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("Karten-Daten wurden geladen.");
    }
}
