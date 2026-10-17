package dev.imagecanvas;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ImageCanvasPlugin extends JavaPlugin {

    private ImageStore store;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getDataFolder().mkdirs();

        store = new ImageStore(this);
        store.load();

        ImageCommand command = new ImageCommand(this, store);
        PluginCommand pluginCommand = getCommand("imagemap");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(new MapListener(store), this);
    }
}
