package ru.hcc.guildmaster.tools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hcc.guildmaster.GuildMaster;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

@SuppressWarnings("deprecation")
public class ToolMethods {

    private static final int MAX_LINE_LENGTH = 90;

    public static String setColor(String str) { return ChatColor.translateAlternateColorCodes('&', str); }

    public static String[] setColors(String[] str) {
        String[] data = new String[str.length];
        for (int i = 0; i < str.length; i++) data[i] = setColor(str[i]);
        return data;
    }

    public static String getHelpMessage() {
        return "&a&lGuildMaster&a plugin for Minecraft version &a&l1.21.8&a\n&6Created by &6&lHCC&6 (Home Creator Companiy)\n&aActually version of plugin: &a&l%s&f".formatted(GuildMaster.getPlugin(GuildMaster.class).getDescription().getVersion());
    }

    /**
     * This method needs to check if sender has some permission.
     * If CommandSender is Player result will be creating after checking all permissions.
     * If CommandSender is Console result is true.
     * @param perm Permission
     * @param sender CommandSender
     * @return Result after checking
     */
    public static boolean hasPerm(String perm, CommandSender sender) {
        boolean result = false;
        if (sender instanceof Player){
            if (perm.isEmpty()) result = true;
            else result = sender.hasPermission(perm) || isOp((Player) sender);
        }
        else if (sender instanceof ConsoleCommandSender) result = true;
        return result;
    }

    public static String unknownCommand() {
        return setColor("&cТакой команды не существует!");
    }

    public static boolean isOp(Player player) {
        return player.isOp() || player.hasPermission("guildmaster.*");
    }

    /**
     * This method need to logging some messages in console by the plugin name
     * @param message message what you can log (it can be colourful)
     * @param level level of message
     * @see #colorizeMessage(String, Color)
     */
    public static void log(Level level, String message) {
        GuildMaster.getPlugin(GuildMaster.class).getLogger().log(level, message);
    }

    /**
     * This method needs to colorize the console message.
     * For example: if you can print an error message that it must be red color, right?
     * Use Color enum for give the red color and printing a done message to console!
     *
     * @param message message what you can to colorize
     * @param color color which will be colorized you message
     * @return colorized string
     */
    public static String colorizeMessage(String message, Color color) {
        return "%s %s %s".formatted(color.toString(), message, Color.RESET.toString());
    }

    /**
     * This method needs to convert long message to short which can be used in some menu.
     * @param mes message which need to convert
     * @return ArrayList with converted strings
     */
    @NotNull
    public static ArrayList<String> convertToMenu(@NotNull String mes) {
        ArrayList<String> result = new ArrayList<>();

        if (mes.trim().isEmpty()) {
            result.add("");
            return result;
        }

        if (mes.length() <= MAX_LINE_LENGTH) {
            result.add(mes);
            return result;
        }

        String[] words = mes.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (word.length() > MAX_LINE_LENGTH) {
                if (!currentLine.isEmpty()) {
                    result.add(currentLine.toString().trim());
                    currentLine = new StringBuilder();
                }

                int start = 0;
                while (start < word.length()) {
                    int end = Math.min(start + MAX_LINE_LENGTH, word.length());
                    String part = word.substring(start, end);
                    result.add(part);
                    start = end;
                }
                continue;
            }

            if (currentLine.length() + word.length() + 1 <= MAX_LINE_LENGTH) {
                if (!currentLine.isEmpty()) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (!currentLine.isEmpty()) {
                    result.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine.append(word);
                }
            }
        }

        if (!currentLine.isEmpty()) {
            result.add(currentLine.toString());
        }

        return result;
    }

    /**
     * This method needs to convert ArrayList<String> object to String[]
     * @param arrayList ArrayList
     * @return String[]
     */
    public static String[] ArrayToList(ArrayList<String> arrayList) {
        String[] strings = new String[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) strings[i] = arrayList.get(i);
        return strings;
    }

    private static String var1(String[] strings) {
        return "&%s%s&f - &&f%s&f\n".formatted(strings[0], strings[1], strings[0]);
    }

    /**
     * This method needs to get Player object from his UUID
     * @param uuid player's uuid
     * @return Player object or null (if player is not online)
     */
    @Nullable
    public OfflinePlayer getPlayer(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }

    /**
     * This method needs to get player name if player is not online.
     * @param uuid His uuid in string format
     * @return His in-game name
     */
    @NotNull
    public static String getPlayerName(String uuid) {
        return Objects.requireNonNull(Bukkit.getOfflinePlayer(UUID.fromString(uuid)).getName());
    }

    /**
     * This method needs to get player uuid if player is not online.
     * @param name His in-game name
     * @return His uuid
     */
    @NotNull
    public static String getPlayerUUID(String name) {
        return Objects.requireNonNull(Bukkit.getOfflinePlayer(name).getUniqueId().toString());
    }

    @NotNull
    public static String getRequestsColor() {
        return "&6";
    }

    @NotNull
    public static String removeHistory(String original) {
        return original.replaceAll("§", "&");
    }

    /**
     * This method needs to get help of color codes.
     * @return String with color codes
     */
    @NotNull
    public static String getColorsHelpMessage() {
        // colors

        String builder = "\n" +
                var1(new String[]{"b", "Aqua"}) +
                var1(new String[]{"9", "Blue"}) +
                var1(new String[]{"8", "Dark Gray"}) +
                var1(new String[]{"2", "Dark Green"}) +
                var1(new String[]{"6", "Gold"}) +
                var1(new String[]{"a", "Green"}) +
                var1(new String[]{"c", "Red"}) +
                var1(new String[]{"e", "Yellow"}) +
                var1(new String[]{"0", "Black"}) +
                var1(new String[]{"3", "Dark Aqua"}) +
                var1(new String[]{"1", "Dark Blue"}) +
                var1(new String[]{"5", "Dark Purple"}) +
                var1(new String[]{"4", "Dark Red"}) +
                var1(new String[]{"7", "Gray"}) +
                var1(new String[]{"d", "Light Purple"}) +
                var1(new String[]{"f", "White"}) +

                // fonts
                "\n" +
                var1(new String[]{"l", "Bold"}) +
                var1(new String[]{"n", "Underline"}) +
                var1(new String[]{"o", "Italic"}) +
                var1(new String[]{"k", "Random"}) +
                var1(new String[]{"m", "strikethrough"});
        return setColor(builder);
    }

    public static String errorPermission() {
        return setColor("&cВы не имеете разрешения для использования данной команды!");
    }

    /**
     * This method needs to get base plugin files directory
     * @return path
     */
    @NotNull
    public static String getPath() {
        return Objects.requireNonNull(Bukkit.getServer().getPluginManager().getPlugin(GuildMaster.getPlugin(GuildMaster.class).getName())).getDataFolder().getAbsolutePath();
    }

    /**
     * This method needs to broadcast some message to other players on server in got radius from some location.
     * If you can broadcast message to all players which on server, get in second and third argument empty objects.
     *
     * @param message Message, which will be applied to players
     * @param radius Radius in which from some location you can that this message can be sending to players
     * @param location Location from which sending message to other players
     */
    public void broadcastMessage(String message, int radius, Location location) {
        if (location != null && radius != -1) {
            for (Player player : Bukkit.getOnlinePlayers())
                if (location.distanceSquared(player.getLocation()) <= radius) player.sendMessage(setColor(message));
        }
        else for (Player player : Bukkit.getOnlinePlayers()) player.sendMessage(setColor(message));
    }

}
