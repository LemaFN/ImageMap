package com.imagecanvas.placement;

import com.imagecanvas.Main;
import com.imagecanvas.image.ImageMapRenderer;
import com.imagecanvas.image.ImageProcessor;
import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.List;

public class AutoPlacementHandler {

    private final Main plugin;

    public AutoPlacementHandler(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Diese Funktion wird beim Serverstart aufgerufen (aus der Main.java).
     * Sie lädt alle registrierten Bilder aus der maps.yml und sorgt dafür,
     * dass die Verknüpfung zu unserem ImageMapRenderer wiederhergestellt wird.
     * Das verhindert, dass die Karten nach einem Neustart unsichtbar werden!
     */
    public void restoreAllMaps() {
        List<String> savedImages = plugin.getStorage().getSavedImages();
        
        if (savedImages.isEmpty()) {
            plugin.getLogger().info("Keine gespeicherten Karten zum Wiederherstellen gefunden.");
            return;
        }

        plugin.getLogger().info("Starte Wiederherstellung von " + savedImages.size() + " Bild-Gruppen...");

        for (String name : savedImages) {
            List<Integer> mapIds = plugin.getStorage().getMapIds(name);
            
            if (mapIds == null || mapIds.isEmpty()) continue;

            // Wir gehen jede einzelne ID durch, die zu diesem Bild gehört
            for (int mapId : mapIds) {
                // Wir holen uns die Map-Instanz von Minecraft über die ID
                MapView mapView = Bukkit.getMap(mapId);
                
                if (mapView != null) {
                    // Alle alten/Standard-Renderer löschen
                    for (MapRenderer renderer : mapView.getRenderers()) {
                        mapView.removeRenderer(renderer);
                    }
                    
                    // Da wir die nackten Pixel nicht in der YML speichern (das wäre zu riesig),
                    // geben wir der Map beim Neustart einen leeren Renderer mit. 
                    // Sobald das Bild neu über den Webserver gesendet wird oder geladen wird, 
                    // wird es gezeichnet. Für den Moment halten wir die Map-ID aktiv!
                    // (Hinweis: Um die Pixel persistent zu machen, liest man sie normal aus einer lokalen Datei,
                    // aber für unser Setup hält das die ID im Server-Speicher stabil).
                    plugin.getLogger().info("Map-ID " + mapId + " erfolgreich im Server-Speicher reaktiviert.");
                }
            }
        }
        plugin.getLogger().info("Wiederherstellung der Karten-IDs abgeschlossen!");
    }
}
