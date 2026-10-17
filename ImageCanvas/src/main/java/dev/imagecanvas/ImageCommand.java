package dev.imagecanvas;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ImageCommand implements TabExecutor {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final Pattern SIZE = Pattern.compile("(\\d{1,2})x(\\d{1,2})");
    private static final int MAX_SIDE = 16;
    private static final int MAX_TILES = 100;

    private final ImageCanvasPlugin plugin;
    private final ImageStore store;

    public ImageCommand(ImageCanvasPlugin plugin, ImageStore store) {
        this.plugin = plugin;
        this.store = store;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "upload" -> upload(sender);
            case "create" -> create(sender, args);
            case "give" -> give(sender, args);
            case "delete" -> delete(sender, args);
            case "list" -> list(sender);
            case "reload" -> {
                store.reload();
                send(sender, "Bilder werden neu geladen ...", NamedTextColor.GREEN);
            }
            default -> usage(sender);
        }
        return true;
    }

    /** Schickt einen anklickbaren Link zur Upload-Webseite (Adresse steht in config.yml). */
    private void upload(CommandSender sender) {
        String base = plugin.getConfig().getString("upload-url", "https://lemafn.github.io/ImageMap/");
        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String url = base + (base.contains("?") ? "&" : "?") + "key=" + key;

        Component link = Component.text(url, NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text("Klicken zum Oeffnen", NamedTextColor.GRAY)));

        sender.sendMessage(Component.empty());
        send(sender, "Klicke den Link, lade dein Bild hoch und fuege den fertigen Befehl hier im Chat ein:",
                NamedTextColor.GREEN);
        sender.sendMessage(link);
        sender.sendMessage(Component.empty());
    }

    private void usage(CommandSender sender) {
        send(sender, "/imagemap upload  (Link zur Upload-Seite)", NamedTextColor.YELLOW);
        send(sender, "/imagemap create <name> <url> [BxH] [-auto] [-HD]", NamedTextColor.YELLOW);
        send(sender, "/imagemap give <name> | delete <name> | list | reload", NamedTextColor.YELLOW);
    }

    // ---------------------------------------------------------------- create

    private void create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "Dieser Befehl ist nur ingame nutzbar.", NamedTextColor.RED);
            return;
        }
        if (args.length < 3) {
            send(sender, "/imagemap create <name> <url> [BxH] [-auto] [-HD]", NamedTextColor.RED);
            return;
        }

        final String name = args[1].toLowerCase(Locale.ROOT);
        if (!NAME.matcher(name).matches()) {
            send(sender, "Name: 1-32 Zeichen aus a-z, 0-9, _ und -.", NamedTextColor.RED);
            return;
        }
        if (store.get(name) != null) {
            send(sender, "Der Name '" + name + "' ist schon vergeben.", NamedTextColor.RED);
            return;
        }

        final URI uri = parseUri(args[2]);
        if (uri == null) {
            send(sender, "Ungueltige URL (nur http/https).", NamedTextColor.RED);
            return;
        }

        int cols = 1;
        int rows = 1;
        boolean auto = false;
        boolean hd = false;
        for (int i = 3; i < args.length; i++) {
            String arg = args[i].toLowerCase(Locale.ROOT);
            if (arg.equals("-auto")) {
                auto = true;
            } else if (arg.equals("-hd")) {
                hd = true;
            } else {
                Matcher m = SIZE.matcher(arg);
                if (!m.matches()) {
                    send(sender, "Unbekannte Angabe '" + args[i] + "'. Groesse z.B. 3x2.", NamedTextColor.RED);
                    return;
                }
                cols = Integer.parseInt(m.group(1));
                rows = Integer.parseInt(m.group(2));
            }
        }
        if (cols < 1 || rows < 1 || cols > MAX_SIDE || rows > MAX_SIDE || cols * rows > MAX_TILES) {
            send(sender, "Groesse: 1-" + MAX_SIDE + " pro Seite, maximal " + MAX_TILES + " Karten.", NamedTextColor.RED);
            return;
        }

        ItemFrame anchor = null;
        if (auto && cols * rows > 1) {
            Entity target = player.getTargetEntity(6);
            if (!(target instanceof ItemFrame frame)) {
                send(sender, "Fuer -auto: schau auf den Item-Frame oben links im Raster (max. 6 Bloecke).", NamedTextColor.RED);
                return;
            }
            if (rightOf(frame.getFacing()) == null) {
                send(sender, "-auto funktioniert nur mit Item-Frames an Waenden.", NamedTextColor.RED);
                return;
            }
            anchor = frame;
        }

        final UUID uuid = player.getUniqueId();
        final World world = player.getWorld();
        final String url = args[2];
        final int fCols = cols;
        final int fRows = rows;
        final boolean fHd = hd;
        final ItemFrame fAnchor = anchor;

        send(sender, "Lade Bild herunter ...", NamedTextColor.GRAY);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                BufferedImage image = ImageUtil.download(uri);
                ImageUtil.savePng(image, store.sourceFile(name));
                BufferedImage[] tiles = ImageUtil.buildTiles(image, fCols, fRows, fHd);
                Bukkit.getScheduler().runTask(plugin,
                        () -> finishCreate(uuid, world, name, url, fCols, fRows, fHd, tiles, fAnchor));
            } catch (Exception e) {
                plugin.getLogger().warning("Upload von '" + name + "' fehlgeschlagen: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        send(p, "Fehler: " + e.getMessage(), NamedTextColor.RED);
                    }
                });
            }
        });
    }

    private void finishCreate(UUID uuid, World world, String name, String url, int cols, int rows,
                              boolean hd, BufferedImage[] tiles, ItemFrame anchor) {
        Player player = Bukkit.getPlayer(uuid);
        if (store.get(name) != null) {
            if (player != null) {
                send(player, "Der Name '" + name + "' wurde inzwischen vergeben.", NamedTextColor.RED);
            }
            return;
        }

        List<Integer> ids = new ArrayList<>();
        for (BufferedImage tile : tiles) {
            MapView view = Bukkit.createMap(world);
            int id = view.getId();
            store.register(id, new TileRenderer(tile));
            store.attach(view);
            ids.add(id);
        }
        ImageStore.Entry entry = new ImageStore.Entry(name, url, cols, rows, hd, List.copyOf(ids));
        store.put(entry);
        store.save();

        if (player == null) {
            return;
        }
        List<ItemStack> items = store.mapItems(entry);
        List<ItemStack> leftover = items;
        int placed = 0;
        if (anchor != null && anchor.isValid()) {
            leftover = autoPlace(anchor, cols, rows, items);
            placed = items.size() - leftover.size();
        }
        giveItems(player, leftover);

        String text = "'" + name + "' erstellt (" + cols + "x" + rows + ", " + ids.size() + " Karten).";
        if (anchor != null) {
            text += " " + placed + " Frames befuellt, " + leftover.size() + " Karten im Inventar.";
        }
        send(player, text, NamedTextColor.GREEN);
    }

    // ------------------------------------------------------------ auto-place

    /** Rechts-Richtung aus Sicht des Betrachters, der auf den Frame schaut (nur Wand-Frames). */
    private static BlockFace rightOf(BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.WEST;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            case WEST -> BlockFace.SOUTH;
            default -> null;
        };
    }

    private List<ItemStack> autoPlace(ItemFrame anchor, int cols, int rows, List<ItemStack> items) {
        BlockFace face = anchor.getFacing();
        BlockFace right = rightOf(face);
        List<ItemStack> leftover = new ArrayList<>();
        if (right == null) {
            return new ArrayList<>(items);
        }
        Block base = anchor.getLocation().getBlock();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int index = r * cols + c;
                if (index >= items.size()) {
                    continue;
                }
                Block block = base.getRelative(right, c).getRelative(BlockFace.DOWN, r);
                ItemFrame frame = findFrame(block, face);
                if (frame == null) {
                    leftover.add(items.get(index));
                } else {
                    frame.setRotation(Rotation.NONE);
                    frame.setItem(items.get(index));
                }
            }
        }
        return leftover;
    }

    private ItemFrame findFrame(Block block, BlockFace face) {
        for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 0.4, 0.4, 0.4)) {
            if (entity instanceof ItemFrame frame && frame.getFacing() == face) {
                return frame;
            }
        }
        return null;
    }

    // ------------------------------------------------------- give/delete/list

    private void give(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "Dieser Befehl ist nur ingame nutzbar.", NamedTextColor.RED);
            return;
        }
        if (args.length < 2) {
            send(sender, "/imagemap give <name>", NamedTextColor.RED);
            return;
        }
        ImageStore.Entry entry = store.get(args[1]);
        if (entry == null) {
            send(sender, "Kein Bild mit dem Namen '" + args[1] + "'.", NamedTextColor.RED);
            return;
        }
        List<ItemStack> items = store.mapItems(entry);
        giveItems(player, items);
        send(sender, items.size() + " Karten von '" + entry.name() + "' erhalten.", NamedTextColor.GREEN);
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, "/imagemap delete <name>", NamedTextColor.RED);
            return;
        }
        if (store.remove(args[1])) {
            send(sender, "'" + args[1].toLowerCase(Locale.ROOT) + "' geloescht. Bereits aufgehaengte Karten werden leer.",
                    NamedTextColor.GREEN);
        } else {
            send(sender, "Kein Bild mit dem Namen '" + args[1] + "'.", NamedTextColor.RED);
        }
    }

    private void list(CommandSender sender) {
        List<ImageStore.Entry> all = store.all();
        if (all.isEmpty()) {
            send(sender, "Noch keine Bilder vorhanden.", NamedTextColor.GRAY);
            return;
        }
        for (ImageStore.Entry e : all) {
            send(sender, e.name() + " - " + e.cols() + "x" + e.rows() + (e.hd() ? " (HD)" : ""), NamedTextColor.YELLOW);
        }
    }

    private void giveItems(Player player, List<ItemStack> items) {
        if (items.isEmpty()) {
            return;
        }
        for (ItemStack rest : player.getInventory().addItem(items.toArray(new ItemStack[0])).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), rest);
        }
    }

    // ------------------------------------------------------------------ misc

    private static URI parseUri(String raw) {
        try {
            URI uri = URI.create(raw);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                return null;
            }
            return uri;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void send(CommandSender sender, String text, NamedTextColor color) {
        sender.sendMessage(Component.text(text, color));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("upload", "create", "give", "delete", "list", "reload"), args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && (sub.equals("give") || sub.equals("delete"))) {
            return filter(store.names(), args[1]);
        }
        if (sub.equals("create") && args.length >= 4) {
            return filter(List.of("1x1", "2x2", "3x3", "4x4", "-auto", "-HD"), args[args.length - 1]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(p)) {
                result.add(option);
            }
        }
        return result;
    }
}
