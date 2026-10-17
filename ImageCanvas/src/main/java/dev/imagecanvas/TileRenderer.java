package dev.imagecanvas;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Zeichnet genau eine 128x128-Kachel auf eine Karte (einmal pro Spieler). */
public final class TileRenderer extends MapRenderer {

    private final BufferedImage tile;
    private final Set<UUID> drawn = ConcurrentHashMap.newKeySet();

    public TileRenderer(BufferedImage tile) {
        super(true); // contextual: jeder Spieler hat seinen eigenen Canvas
        this.tile = tile;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
        if (!drawn.add(player.getUniqueId())) {
            return;
        }
        canvas.drawImage(0, 0, tile);
    }

    /** Beim Verlassen des Servers vergessen, damit das Bild beim naechsten Join neu gezeichnet wird. */
    public void forget(UUID playerId) {
        drawn.remove(playerId);
    }
}
