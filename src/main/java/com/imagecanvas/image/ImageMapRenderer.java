package com.imagecanvas.image;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class ImageMapRenderer extends MapRenderer {

    private final byte[] mapColors;
    private boolean rendered = false;

    // Wir übergeben dem Renderer direkt das fertige Minecraft-Farben-Array
    public ImageMapRenderer(byte[] mapColors) {
        this.mapColors = mapColors;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        // Wenn die Karte für den Spieler bereits gezeichnet wurde, müssen wir es nicht blockweise wiederholen (spart FPS!)
        if (rendered) return;

        int index = 0;
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                // Wir zeichnen das Pixel direkt auf das Canvas der Karte
                canvas.setPixel(x, y, mapColors[index++]);
            }
        }

        rendered = true;
    }
}
