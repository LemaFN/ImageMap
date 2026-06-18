package com.imagecanvas;

import org.bukkit.plugin.java.JavaPlugin;
import com.imagecanvas.storage.ImageMapStorage;
import com.imagecanvas.placement.AutoPlacementHandler;
import com.imagecanvas.web.WebServer;

public class Main extends JavaPlugin {

    private ImageMapStorage storage;
    private AutoPlacementHandler placementHandler;
    private WebServer webServer; 

    @Override
    public void onEnable() {
        // 1. Ordnerstruktur erstellen
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // 2. Speicher-System starten
        this.storage = new ImageMapStorage(this);
        
        // 3. Platzierungs-System starten
        this.placementHandler = new AutoPlacementHandler(this);
        
        // 4. Webserver starten (damit er Bilder von der Webseite empfangen kann)
        this.webServer = new WebServer(this);
        this.webServer.start();
        
        // 5. Karten-Daten aus der maps.yml laden
        storage.loadMaps();
        
        // 6. Die geladenen Karten in der Welt wieder sichtbar machen
        placementHandler.restoreAllMaps();
        
        getLogger().info("=========================================");
        getLogger().info("ImageCanvas wurde erfolgreich gestartet!");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        // Webserver ordnungsgemäß stoppen
        if (webServer != null) {
            webServer.stop();
        }

        // Beim Stoppen alle Daten sichern
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
