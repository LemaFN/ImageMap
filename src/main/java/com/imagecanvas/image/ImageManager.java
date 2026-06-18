package com.imagecanvas.image;

import com.imagecanvas.Main;
import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class ImageManager {

    private final Main plugin;

    public ImageManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Erstellt für jedes Segment eines Bildes eine echte Minecraft-Karte und befüllt sie.
     * @return Eine Liste von Integern, die die generierten Map-IDs enthält.
     */
    public List<Integer> createMapGrid(String name, BufferedImage resizedImage, int cols, int rows) {
        List<Integer> mapIds = new ArrayList<>();

        // Wir gehen das Raster durch (Reihe für Reihe, Spalte für Spalte)
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                
                // 1. Schneide das 128x128 Pixel große Stück für dieses Segment aus
                BufferedImage subImage = ImageProcessor.getSubImage(resizedImage, x, y);
                
                // 2. Wandle die Pixel in Minecraft-Kartenfarben (Bytes) um
                byte[] mapColors = ImageProcessor.convertToMapColors(subImage);

                // 3. Erstelle eine völlig neue, leere Map in der Minecraft-Welt
                MapView mapView = Bukkit.createMap(Bukkit.getWorlds().get(0));
                
                // 4. Entferne die Standard-Minecraft-Renderer (die normale Weltkarte), 
                // damit die Karte nicht die Landschaft zeichnet
                for (MapRenderer renderer : mapView.getRenderers()) {
                    mapView.removeRenderer(renderer);
                }

                // 5. Füge unseren eigenen Bild-Renderer hinzu
                mapView.addRenderer(new ImageMapRenderer(mapColors));

                // 6. Merke dir die ID dieser Karte
                int mapId = mapView.getId();
                mapIds.add(mapId);

                // 7. Protokollieren im Server-Log
                plugin.getLogger().info("Karte erstellt für '" + name + "' Segment [" + x + "," + y + "] mit ID: " + mapId);
            }
        }

        return mapIds;
    }
}
