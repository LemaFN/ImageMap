package com.imagecanvas.web;

import com.imagecanvas.Main;
import com.imagecanvas.image.ImageManager;
import com.imagecanvas.image.ImageProcessor;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebServer {

    private final Main plugin;
    private HttpServer server;
    private final int port = 8080;
    private final ImageManager imageManager;

    public WebServer(Main plugin) {
        this.plugin = plugin;
        this.imageManager = new ImageManager(plugin); // Hier wird der Manager aus dem image-Ordner geladen
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/upload", new ImageUploadHandler(plugin, imageManager));
            server.setExecutor(null);
            server.start();
            plugin.getLogger().info("Integrierter Webserver wurde auf Port " + port + " gestartet.");
        } catch (IOException e) {
            plugin.getLogger().severe("Webserver konnte nicht gestartet werden: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Webserver wurde gestoppt.");
        }
    }

    static class ImageUploadHandler implements HttpHandler {
        private final Main plugin;
        private final ImageManager imageManager;

        public ImageUploadHandler(Main plugin, ImageManager imageManager) {
            this.plugin = plugin;
            this.imageManager = imageManager;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS-Header aktivieren – extrem wichtig, damit deine Webseite auf GitHub Pages Zugriff bekommt!
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            // Vorab-Prüfung des Browsers (CORS Preflight) abfangen
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                try {
                    // Daten aus der Anfrage auslesen
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    String query = br.readLine();
                    
                    Map<String, String> params = parseQuery(query);
                    String url = params.get("url");
                    String name = params.get("name");
                    String size = params.get("size"); // Kommt als z.B. "3x3" an

                    if (url == null || name == null || size == null) {
                        sendResponse(exchange, 400, "{\"error\":\"Fehlende Parameter\"}");
                        return;
                    }

                    // Rastergröße aufteilen (Spalten und Zeilen)
                    String[] sizeParts = size.split("x");
                    int cols = Integer.parseInt(sizeParts[0]);
                    int rows = Integer.parseInt(sizeParts[1]);

                    plugin.getLogger().info("Empfange Bild-Anfrage über Webserver: " + name + " (" + size + ")");

                    // 1. Bild von der URL herunterladen und auf das richtige Raster zuschneiden
                    BufferedImage originalImage = ImageProcessor.downloadImage(url);
                    BufferedImage resizedImage = ImageProcessor.resizeImage(originalImage, cols, rows);

                    // 2. Minecraft-Karten generieren und IDs erhalten
                    List<Integer> generatedMapIds = imageManager.createMapGrid(name, resizedImage, cols, rows);

                    // 3. Die neuen Karten direkt im Speichersystem (maps.yml) registrieren!
                    plugin.getStorage().registerImage(name, generatedMapIds, cols, rows);

                    // 4. Die generierten IDs als Textkette für die Webseite zusammenbauen
                    StringBuilder idsBuilder = new StringBuilder();
                    for (int i = 0; i < generatedMapIds.size(); i++) {
                        idsBuilder.append(generatedMapIds.get(i));
                        if (i < generatedMapIds.size() - 1) idsBuilder.append(",");
                    }

                    // Erfolgsmeldung zurück an das Web-Interface senden
                    String response = "{\"status\":\"success\", \"message\":\"Karten erfolgreich generiert!\", \"ids\":\"" + idsBuilder.toString() + "\", \"cols\":" + cols + ", \"rows\":" + rows + "}";
                    sendResponse(exchange, 200, response);

                } catch (Exception e) {
                    sendResponse(exchange, 500, "{\"error\":\"Fehler beim Verarbeiten des Bildes: " + e.getMessage() + "\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Methode nicht erlaubt. Bitte POST nutzen.\"}");
            }
        }

        // Hilfsfunktion, um die gesendeten Formulardaten zu entwirren
        private Map<String, String> parseQuery(String query) {
            Map<String, String> result = new HashMap<>();
            if (query == null) return result;
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    result.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                } else {
                    result.put(pair[0], "");
                }
            }
            return result;
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }
}
