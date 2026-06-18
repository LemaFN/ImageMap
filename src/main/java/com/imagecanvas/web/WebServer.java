package com.imagecanvas.web;

import com.imagecanvas.Main;
import com.imagecanvas.image.ImageManager;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
        this.imageManager = new ImageManager(plugin);
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
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                try {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                    }
                    
                    Map<String, String> params = parseQuery(sb.toString());
                    String base64Data = params.get("image_data");
                    String name = params.get("name");
                    String colsStr = params.get("cols");
                    String rowsStr = params.get("rows");

                    if (base64Data == null || name == null || colsStr == null || rowsStr == null) {
                        sendResponse(exchange, 400, "{\"error\":\"Fehlende Parameter!\"}");
                        return;
                    }

                    int cols = Integer.parseInt(colsStr);
                    int rows = Integer.parseInt(rowsStr);

                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
                    BufferedImage originalImage = ImageIO.read(bais);

                    if (originalImage == null) {
                        sendResponse(exchange, 400, "{\"error\":\"Ungueltiges Bildformat.\"}");
                        return;
                    }

                    BufferedImage resizedImage = com.imagecanvas.image.ImageProcessor.resizeImage(originalImage, cols, rows);
                    List<Integer> generatedMapIds = imageManager.createMapGrid(name, resizedImage, cols, rows);
                    plugin.getStorage().registerImage(name, generatedMapIds, cols, rows);

                    StringBuilder idsBuilder = new StringBuilder();
                    for (int i = 0; i < generatedMapIds.size(); i++) {
                        idsBuilder.append(generatedMapIds.get(i));
                        if (i < generatedMapIds.size() - 1) idsBuilder.append(",");
                    }

                    String response = "{\"status\":\"success\", \"message\":\"Bild empfangen!\", \"ids\":\"" + idsBuilder.toString() + "\"}";
                    sendResponse(exchange, 200, response);

                } catch (Exception e) {
                    sendResponse(exchange, 500, "{\"error\":\"Fehler: " + e.getMessage() + "\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Nur POST erlaubt.\"}");
            }
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> result = new HashMap<>();
            if (query == null || query.isEmpty()) return result;
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    result.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
                } else if (pair.length == 1) {
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
