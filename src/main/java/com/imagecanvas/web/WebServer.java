package com.imagecanvas.web;

import com.imagecanvas.Main;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class WebServer {

    private final Main plugin;
    private HttpServer server;
    private final int port = 8080; // Du kannst den Port bei Bedarf anpassen

    public WebServer(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {
        try {
            // Erstellt den HTTP-Server auf dem angegebenen Port
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Hier definieren wir den Endpunkt, auf den der HTML-Generator zugreift
            server.createContext("/upload", new ImageUploadHandler(plugin));
            
            server.setExecutor(null); // Standard-Executor nutzen
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

    // Ein einfacher Handler für den Upload-Empfang
    static class ImageUploadHandler implements HttpHandler {
        private final Main plugin;

        public ImageUploadHandler(Main plugin) {
            this.plugin = plugin;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS-Header hinzufügen, damit dein HTML-Generator (der auf GitHub Pages läuft) zugreifen darf
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // Hier wird später die Logik eingebaut, die das Bild empfängt und verarbeitet
            String response = "{\"status\":\"bereit\", \"message\":\"Server empfängt Daten\"}";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
