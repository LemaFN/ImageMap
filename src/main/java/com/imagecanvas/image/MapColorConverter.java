package com.imagecanvas.image;

import java.awt.Color;

public class MapColorConverter {

    // Eine vereinfachte Liste der wichtigsten Standard-Minecraft-Kartenfarben (RGB)
    private static final Color[] MAP_COLORS = {
        new Color(0, 0, 0, 0),       // Transparent
        new Color(127, 178, 56),     // Gras / Grün
        new Color(247, 233, 163),    // Sand / Gelb
        new Color(199, 199, 199),    // Woll-Weiß / Hellgrau
        new Color(255, 0, 0),        // Lava / Rot
        new Color(160, 160, 255),    // Eis / Hellblau
        new Color(167, 167, 167),    // Eisen / Grau
        new Color(0, 124, 0),        // Blätter / Dunkelgrün
        new Color(255, 255, 255),    // Schnee / Weiß
        new Color(164, 168, 184),    // Ton / Graublau
        new Color(151, 109, 77),     // Erde / Braun
        new Color(112, 112, 112),    // Stein / Dunkelgrau
        new Color(64, 64, 255),      // Wasser / Blau
        new Color(143, 119, 85)      // Holz / Hellbraun
    };

    /**
     * Sucht die Minecraft-Kartenfarbe, die der Originalfarbe am ähnlichsten ist.
     */
    public static byte matchColor(Color color) {
        if (color.getAlpha() < 128) {
            return 0; // Transparent (ID 0)
        }

        int bestIndex = 1;
        double minDistance = Double.MAX_VALUE;

        for (int i = 1; i < MAP_COLORS.length; i++) {
            Color mcColor = MAP_COLORS[i];
            
            // Mathematische Berechnung des Farbabstands (CIE76-Annäherung)
            double rDiff = color.getRed() - mcColor.getRed();
            double gDiff = color.getGreen() - mcColor.getGreen();
            double bDiff = color.getBlue() - mcColor.getBlue();
            
            double distance = rDiff * rDiff + gDiff * gDiff + bDiff * bDiff;

            if (distance < minDistance) {
                minDistance = distance;
                bestIndex = i;
            }
        }

        // Minecraft-Kartenfarben-IDs werden im Byte-Format an den Client geschickt
        // Der Index wird mit 4 multipliziert und verschoben, um die Helligkeitsstufen zu treffen
        return (byte) (bestIndex * 4 + 2);
    }
}
