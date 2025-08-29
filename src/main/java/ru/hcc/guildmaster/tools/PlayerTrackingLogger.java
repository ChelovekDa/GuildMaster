package ru.hcc.guildmaster.tools;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

/**
 * This class need to saving and restore from file spy-data.
 * It's'include the latest player coordinates, inventory storage and other.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
class PlayerTrackingLogger extends EventLogger {

    private static final String TRACK_DATA_DIR = "tracking";
    private static final int BASE_SLOTS_COUNT = 41;

    @Nullable
    private Location restoreLocation(FileConfiguration config) {
        if (!config.contains("location.world")) return null;

        World world = Bukkit.getWorld(Objects.requireNonNull(config.getString("location.world")));
        if (world == null) return null;

        double x = config.getDouble("location.x");
        double y = config.getDouble("location.y");
        double z = config.getDouble("location.z");
        float yaw = (float) config.getDouble("location.yaw");
        float pitch = (float) config.getDouble("location.pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    private static void check() {
        String path = "%s/%s".formatted(getPath(), TRACK_DATA_DIR);
        if (!Files.exists(Path.of(path))) new File(path).mkdirs();
    }

    @NotNull
    public String getTrackDataDir(@NotNull UUID uuid) {
        return "%s/%s/%s.yml".formatted(getPath(), TRACK_DATA_DIR, uuid.toString());
    }

    /**
     * This method needs to save all player data before starting the surveillance.
     * It's method save the player's inventory content and his coordinates before starting tracking.
     * 
     * @see #restoreData(Player)
     * @param player Tracker (player, who watches)
     */
    public boolean saveData(Player player) {
        check();
        File file = new File(getTrackDataDir(player.getUniqueId()));
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        Location loc = player.getLocation();

        config.set("location.world", loc.getWorld().getName());
        config.set("location.x", loc.getX());
        config.set("location.y", loc.getY());
        config.set("location.z", loc.getZ());
        config.set("location.yaw", loc.getYaw());
        config.set("location.pitch", loc.getPitch());

        PlayerInventory inventory = player.getInventory();

        // Saving common inventory (0-35 slots)
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            config.set("inventory." + i, item);
        }

        // Saving items in armor's slots (100-103 slots)
        ItemStack[] armor = inventory.getArmorContents();
        for (int i = 0; i < armor.length; i++) config.set("armor." + i, armor[i]);

        // Saving second (off) hand (slot 40)
        ItemStack offHand = inventory.getItemInOffHand();
        config.set("offhand", offHand);

        // Saving extra inventory content
        ItemStack[] extra = inventory.getExtraContents();
        for (int i = 0; i < extra.length; i++) config.set("extra." + i, extra);

        try {
            config.save(file);
            return true;
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Can't save inventory content!");
            log(Level.WARNING, colorizeMessage("Plugin can't save player's inventory content because:\n%s".formatted(e.getMessage()), Color.RED_BACKGROUND));
            return false;
        }
    }

    /**
     * This method needs to restore items from file return to the player's inventory.
     * It's restore all items with it saving slots.
     * Warning: previous inventory content will be deleted
     * 
     * @see #saveData(Player)  
     * @param player player
     */
    public boolean restoreData(Player player) {
        File file = new File(getTrackDataDir(player.getUniqueId()));
        if (!file.exists()) return false;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerInventory inventory = player.getInventory();

        Location location = restoreLocation(config);
        if (location == null) return false;

        inventory.clear();

        // Restore common inventory
        for (int i = 0; i < 36; i++) {
            ItemStack item = config.getItemStack("inventory." + i);
            inventory.setItem(i, item);
        }

        // Restoring armor
        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) armor[i] = config.getItemStack("armor." + i);
        inventory.setArmorContents(armor);

        // Restoring second (off) hand
        ItemStack offHand = config.getItemStack("offhand");
        inventory.setItemInOffHand(offHand);

        // Restoring extra slots
        int count = config.getKeys(false).size() - BASE_SLOTS_COUNT;
        if (count > 0) {
            ItemStack[] extras = new ItemStack[count];
            for (int i = 0; i < count; i++) extras[i] = config.getItemStack("extra." + i);
            inventory.setExtraContents(extras);
        }

        player.teleport(location);

        file.delete();
        return true;
    }

}
