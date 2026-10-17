package dev.imagecanvas;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Download, Skalierung und Zerlegung von Bildern in 128x128-Kacheln. */
final class ImageUtil {

    static final int TILE = 128;
    private static final long MAX_BYTES = 25L * 1024 * 1024;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private ImageUtil() {
    }

    static BufferedImage download(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "ImageCanvas/1.0")
                .GET()
                .build();
        HttpResponse<InputStream> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream in = response.body()) {
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode());
            }
            byte[] data = in.readNBytes((int) MAX_BYTES + 1);
            if (data.length > MAX_BYTES) {
                throw new IOException("Bild ist groesser als 25 MB");
            }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) {
                throw new IOException("Kein unterstuetztes Bildformat (PNG, JPG, GIF, BMP)");
            }
            return image;
        }
    }

    static void savePng(BufferedImage image, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        if (!ImageIO.write(image, "png", file)) {
            throw new IOException("Bild konnte nicht gespeichert werden");
        }
    }

    static BufferedImage read(File file) throws IOException {
        return ImageIO.read(file);
    }

    /**
     * Skaliert das Bild (gestreckt) auf cols*128 x rows*128 und zerlegt es zeilenweise
     * (Index = row * cols + col) in 128x128-Kacheln.
     */
    static BufferedImage[] buildTiles(BufferedImage source, int cols, int rows, boolean hd) {
        int width = cols * TILE;
        int height = rows * TILE;

        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, width, height);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    hd ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }

        BufferedImage[] tiles = new BufferedImage[cols * rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                BufferedImage tile = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_ARGB);
                Graphics2D tg = tile.createGraphics();
                try {
                    tg.drawImage(scaled.getSubimage(c * TILE, r * TILE, TILE, TILE), 0, 0, null);
                } finally {
                    tg.dispose();
                }
                tiles[r * cols + c] = tile;
            }
        }
        return tiles;
    }
}
