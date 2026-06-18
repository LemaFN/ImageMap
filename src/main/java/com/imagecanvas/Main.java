package com.imagecanvas;

import org.bukkit.plugin.java.JavaPlugin;
import com.imagecanvas.storage.ImageMapStorage;

public class Main extends JavaPlugin {

    private ImageMapStorage storage;

    @Override
    public void onEnable() {
        // Initialisiere den Storage-Ordner
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // Initialisiere die Speicher-Logik
        this.storage = new ImageMapStorage(this);
        
        // Lade die Karten beim Serverstart
        storage.loadMaps();
        
        getLogger().info("ImageCanvas wurde erfolgreich geladen und Daten wurden wiederhergestellt!");
    }

    @Override
    public void onDisable() {
        // Sicherstellen, dass alle Daten vor dem Herunterfahren gespeichert werden
        if (storage != null) {
            storage.saveMaps();
        }
        getLogger().info("ImageCanvas wurde gestoppt und Daten wurden gespeichert.");
    }

    public ImageMapStorage getStorage() {
        return storage;
    }
}
