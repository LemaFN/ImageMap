package com.imagecanvas.image;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class ImageProcessor {

    /**
     * Skaliert das hochgeladene Bild exakt auf die benötigte Pixelgröße basierend auf den Minecraft-Karten.
     * Eine Minecraft-Karte ist exakt 128x128 Pixel groß.
     */
    public static BufferedImage resizeImage(BufferedImage originalImage, int cols, int rows) {
        int targetWidth = cols * 128;
        int targetHeight = rows * 128;

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        
        // Sorgt für eine saubere und scharfe Skalierung des Bildes
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.KEY_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        return resizedImage;
    }
}
