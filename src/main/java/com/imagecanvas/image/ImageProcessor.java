package com.imagecanvas.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

public class ImageProcessor {

    /**
     * Lädt ein Bild aus einer URL herunter.
     */
    public static BufferedImage downloadImage(String urlString) throws IOException {
        URL url = new URL(urlString);
        return ImageIO.read(url);
    }

    /**
     * Skaliert ein Bild auf die exakte Pixelgröße, die für das Kachel-Raster benötigt wird.
     * Jede Minecraft-Karte ist genau 128x128 Pixel groß.
     */
    public static BufferedImage resizeImage(BufferedImage originalImage, int cols, int rows) {
        int targetWidth = cols * 128;
        int targetHeight = rows * 128;

        Image resultingImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage outputImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(resultingImage, 0, 0, null);
        g2d.dispose();

        return outputImage;
    }

    /**
     * Schneidet ein bestimmtes 128x128 Pixel großes Segment (Kachel) aus dem Gesamtbild heraus.
     * * @param col Die Spalte des Segments (0 bis cols-1)
     * @param row Die Zeile des Segments (0 bis rows-1)
     */
    public static BufferedImage getSubImage(BufferedImage totalImage, int col, int row) {
        int x = col * 128;
        int y = row * 128;
        return totalImage.getSubimage(x, y, 128, 128);
    }

    /**
     * Konvertiert ein 128x128 Bild in ein Byte-Array aus Minecraft-Kartenfarben.
     */
    public static byte[] convertToMapColors(BufferedImage subImage) {
        byte[] colors = new byte[128 * 128];
        int index = 0;

        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                int rgb = subImage.getRGB(x, y);
                java.awt.Color color = new java.awt.Color(rgb, true);
                
                // Hier nutzen wir unseren MapColorConverter!
                colors[index++] = MapColorConverter.matchColor(color);
            }
        }
        return colors;
    }
}
