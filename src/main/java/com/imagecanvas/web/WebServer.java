package com.imagecanvas.web;

import com.imagecanvas.Main;
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
import java.util.Map;

public class WebServer {

    private final Main plugin;
    private HttpServer server;
    private final int port = 8080;

    public WebServer(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/upload", new ImageUploadHandler(plugin));
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

        public ImageUploadHandler(Main plugin) {
            this.plugin = plugin;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS-Header, damit der Browser auf GitHub Pages nicht blockiert
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                try {
                    // Parameter aus dem POST-Body lesen
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    String query = br.readLine();
                    
                    Map<String, String> params = parseQuery(query);
                    String url = params.get("url");
                    String name = params.get("name");
                    String size = params.get("size"); // z.B. "3x3"

                    if (url == null || name == null || size == null) {
                        sendResponse(exchange, 400, "{\"error\":\"Fehlende Parameter\"}");
                        return;
                    }

                    String[] sizeParts = size.split("x");
                    int cols = Integer.parseInt(sizeParts[0]);
                    int rows = Integer.parseInt(sizeParts[1]);

                    plugin.getLogger().info("Empfange Bild-Anfrage: " + name + " (" + size + ")");

                    // Bild im Hintergrund herunterladen und verarbeiten
                    BufferedImage originalImage = ImageProcessor.downloadImage(url);
                    BufferedImage resizedImage = ImageProcessor.resizeImage(originalImage, cols, rows);

                    // Hier speichern wir die Daten temporär im Plugin ab
                    // (Die genaue Map-Erstellung in Minecraft folgt im nächsten Schritt!)
                    
                    String response = "{\"status\":\"success\", \"message\":\"Bild erfolgreich verarbeitet! Grid: " + cols + "x" + rows + "\"}";
                    sendResponse(exchange, 200, response);

                } catch (Exception e) {
                    sendResponse(exchange, 500, "{\"error\":\"Fehler beim Verarbeiten: " + e.getMessage() + "\"}");
                }
            } else {
                sendResponse(exchange, 45, "{\"error\":\"Nur POST erlaubt\"}");
            }
        }

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
