package com.imagecanvas;

import com.imagecanvas.image.ImageManager;
import com.imagecanvas.storage.ImageMapStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Main extends JavaPlugin implements CommandExecutor, TabCompleter {

    private ImageMapStorage storage;
    private ImageManager imageManager;

    @Override
    public void onEnable() {
        // Speicher und Manager initialisieren
        this.storage = new ImageMapStorage(this);
        this.imageManager = new ImageManager(this);

        // Befehl und Tab-Vervollständigung registrieren
        if (this.getCommand("imagecanvas") != null) {
            this.getCommand("imagecanvas").setExecutor(this);
            this.getCommand("imagecanvas").setTabCompleter(this);
        }

        getLogger().info("ImageCanvas erfolgreich gestartet! Befehle sind bereit.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ImageCanvas gestoppt.");
    }

    public ImageMapStorage getStorage() {
        return storage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl kann nur von Spielern genutzt werden!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§7[§aImageCanvas§7] Benutze: §e/imagecanvas upload §7oder §e/imagecanvas create");
            return true;
        }

        // Sub-Befehl: /imagecanvas upload
        if (args[0].equalsIgnoreCase("upload")) {
            // Wir generieren einen zufälligen Key für die Session (wie im Video)
            String sessionKey = UUID.randomUUID().toString().substring(0, 8);
            
            // HIER KOMMT DEINE GITHUB PAGES URL HIN! 
            // Ersetze "lemafn.github.io/ImageMap" mit deiner echten GitHub-Pages-Adresse
            String websiteUrl = "https://lemafn.github.io/ImageMap/?key=" + sessionKey;

            player.sendMessage("");
            player.sendMessage("§aClick the link below to upload an image:");
            player.sendMessage("§b" + websiteUrl);
            player.sendMessage("§7(Click to open in browser)");
            player.sendMessage("");
            return true;
        }

        // Sub-Befehl: /imagecanvas create <Name> <URL> <Größe>
        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 4) {
                player.sendMessage("§cFehler! Benutze: /imagecanvas create <Name> <Bild-URL> <Spalten>x<Zeilen>");
                return true;
            }

            String name = args[1];
            String url = args[2];
            String size = args[3]; // z.B. 11x11

            try {
                String[] parts = size.split("x");
                int cols = Integer.parseInt(parts[0]);
                int rows = Integer.parseInt(parts[1]);

                player.sendMessage("§7[§aImageCanvas§7] Lade Bild herunter und generiere Karten (" + cols + "x" + rows + ")...");
                
                // Hier rufen wir asynchron das Herunterladen und Erstellen auf (bauen wir im nächsten Schritt in den Manager)
                // Damit der Server während des Downloads nicht laggt!
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    try {
                        java.awt.image.BufferedImage img = com.imagecanvas.image.ImageProcessor.downloadImage(url);
                        java.awt.image.BufferedImage resized = com.imagecanvas.image.ImageProcessor.resizeImage(img, cols, rows);
                        
                        List<Integer> mapIds = imageManager.createMapGrid(name, resized, cols, rows);
                        storage.registerImage(name, mapIds, cols, rows);

                        player.sendMessage("§a§lErfolg! §eBild verarbeitet. Karten-IDs: " + mapIds.toString());
                        player.sendMessage("§7Du kannst die Karten nun platzieren.");
                    } catch (Exception e) {
                        player.sendMessage("§cFehler beim Verarbeiten des Bildes: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                player.sendMessage("§cFalsches Format für die Größe! Beispiel: 3x3 oder 11x11");
            }
            return true;
        }

        return false;
    }

    // Das hier sorgt dafür, dass im Chat die Vorschläge aufploppen!
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("upload", "create");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("create")) {
            return Arrays.asList("1x1", "2x2", "3x3", "11x11");
        }
        return new ArrayList<>();
    }
}
