package com.imagecanvas.storage;

import com.imagecanvas.Main;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImageMapStorage {
    private final Main plugin;
    private final File configFile;
    private YamlConfiguration config;

    public ImageMapStorage(Main plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "maps.yml");
        
        // Erstellt die maps.yml Datei im Plugin-Ordner, falls sie noch nicht existiert
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
     * Registriert ein neues Bild mit seinen zugehörigen Minecraft-Karten-IDs und der Rastergröße.
     * Wird direkt vom Webserver aufgerufen, sobald ein Bild verarbeitet wurde.
     */
    public void registerImage(String name, List<Integer> mapIds, int cols, int rows) {
        String path = "images." + name;
        config.set(path + ".ids", mapIds);
        config.set(path + ".cols", cols);
        config.set(path + ".rows", rows);
        
        // Direkt auf die Festplatte speichern, damit nichts verloren geht
        saveMaps();
    }

    /**
     * Speichert den aktuellen Stand der Konfiguration auf die Festplatte.
     */
    public void saveMaps() {
        try {
            config.save(configFile);
            plugin.getLogger().info("Karten-Daten wurden erfolgreich in maps.yml gespeichert.");
        } catch (IOException e) {
            plugin.getLogger().severe("Fehler beim Schreiben in die maps.yml!");
        }
    }

    /**
     * Lädt die Karten-Daten beim Serverstart neu ein.
     */
    public void loadMaps() {
        this.config = YamlConfiguration.loadConfiguration(configFile);
        if (config.contains("images")) {
            plugin.getLogger().info("Gespeicherte Karten-Daten wurden erfolgreich aus der maps.yml geladen.");
        }
    }

    /**
     * Gibt eine Liste aller registrierten Bildnamen zurück.
     */
    public List<String> getSavedImages() {
        if (!config.contains("images")) return new ArrayList<>();
        return new ArrayList<>(config.getConfigurationSection("images").getKeys(false));
    }

    /**
     * Holt die Karten-IDs für ein bestimmtes Bild.
     */
    public List<Integer> getMapIds(String name) {
        return config.getIntegerList("images." + name + ".ids");
    }

    /**
     * Holt die Spaltenanzahl für ein bestimmtes Bild.
     */
    public int getCols(String name) {
        return config.getInt("images." + name + ".cols");
    }

    /**
     * Holt die Zeilenanzahl für ein bestimmtes Bild.
     */
    public int getRows(String name) {
        return config.getInt("images." + name + ".rows");
    }
}
