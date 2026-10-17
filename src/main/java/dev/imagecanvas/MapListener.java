package dev.imagecanvas;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class MapListener implements Listener {

    private final ImageStore store;

    public MapListener(ImageStore store) {
        this.store = store;
    }

    /** Wird ausgeloest, sobald eine Karte (z.B. nach einem Neustart) geladen wird: Renderer wieder anhaengen. */
    @EventHandler
    public void onMapInitialize(MapInitializeEvent event) {
        store.attach(event.getMap());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        store.forget(event.getPlayer().getUniqueId());
    }
}
