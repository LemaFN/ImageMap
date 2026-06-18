package com.imagecanvas;

import org.bukkit.plugin.java.JavaPlugin;
import com.imagecanvas.storage.ImageMapStorage;
import com.imagecanvas.placement.AutoPlacementHandler;

public class Main extends JavaPlugin {

    private ImageMapStorage storage;
    private AutoPlacementHandler placementHandler;

    @Override
    public void onEnable() {
        // 1. Erstellt den Plugin-Ordner (plugins/ImageCanvas/), falls er fehlt
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // 2. Speicher-System starten
        this.storage = new ImageMapStorage(this);
        
        // 3. Platzierungs-System starten
        this.placementHandler = new AutoPlacementHandler(this);
        
        // 4. Karten-Daten aus der maps.yml laden
        storage.loadMaps();
        
        // 5. Die geladenen Karten in der Welt wieder sichtbar machen (Anti-Verschwinde-Fix!)
        placementHandler.restoreAllMaps();
        
        getLogger().info("=========================================");
        getLogger().info("ImageCanvas wurde erfolgreich gestartet!");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        // Beim Stoppen oder Reloaden des Servers alle Daten sichern
        if (storage != null) {
            storage.saveMaps();
        }
        getLogger().info("ImageCanvas wurde gestoppt und alle Karten wurden gesichert.");
    }

    public ImageMapStorage getStorage() {
        return storage;
    }

    public AutoPlacementHandler getPlacementHandler() {
        return placementHandler;
    }
}
