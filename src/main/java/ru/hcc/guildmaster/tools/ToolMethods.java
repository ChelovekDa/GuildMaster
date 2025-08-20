package ru.hcc.guildmaster.tools;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hcc.guildmaster.GuildMaster;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class ToolMethods {

    private static final int MAX_LINE_LENGTH = 125;

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
     * This method needs to colorize the console message.
     *
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
    public ArrayList<String> convertToMenu(@NotNull String mes) {
        ArrayList<String> result = new ArrayList<>();

        if (mes.length() > MAX_LINE_LENGTH) {
            String[] split = mes.split(" ");
            int length = 0;

            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < split.length; i++) {
                if (length + split[i].length() <= MAX_LINE_LENGTH) {
                    if (i > 0) builder.append(" ");
                    builder.append(split[i]);

                    length += split[i].length();
                }
                else {
                    result.add(builder.toString());
                    builder = new StringBuilder();
                    length = 0;
                }
            }
        }
        else result.add(mes);

        return result;
    }

    /**
     * This method needs to convert ArrayList<String> object to String[]
     * @param arrayList
     * @return List with strings
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
     * @return Player object or null
     */
    @Nullable
    public Player getPlayer(UUID uuid) {
        var players = Bukkit.getOnlinePlayers();
        if (!players.isEmpty()) for (Player pl : players) if (pl.getUniqueId().equals(uuid)) return pl;
        return Bukkit.getOfflinePlayer(uuid).getPlayer();
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
        StringBuilder builder = new StringBuilder();
        // colors
        builder.append("\n");
        builder.append(var1(new String[] {"b", "Aqua"}));
        builder.append(var1(new String[] {"9", "Blue"}));
        builder.append(var1(new String[] {"8", "Dark Gray"}));
        builder.append(var1(new String[] {"2", "Dark Green"}));
        builder.append(var1(new String[] {"6", "Gold"}));
        builder.append(var1(new String[] {"a", "Green"}));
        builder.append(var1(new String[] {"c", "Red"}));
        builder.append(var1(new String[] {"e", "Yellow"}));
        builder.append(var1(new String[] {"0", "Black"}));
        builder.append(var1(new String[] {"3", "Dark Aqua"}));
        builder.append(var1(new String[] {"1", "Dark Blue"}));
        builder.append(var1(new String[] {"5", "Dark Purple"}));
        builder.append(var1(new String[] {"4", "Dark Red"}));
        builder.append(var1(new String[] {"7", "Gray"}));
        builder.append(var1(new String[] {"d", "Light Purple"}));
        builder.append(var1(new String[] {"f", "White"}));

        // fonts
        builder.append("\n");
        builder.append(var1(new String[] {"l", "Bold"}));
        builder.append(var1(new String[] {"n", "Underline"}));
        builder.append(var1(new String[] {"o", "Italic"}));
        builder.append(var1(new String[] {"k", "Random"}));
        builder.append(var1(new String[] {"m", "strikethrough"}));
        return setColor(builder.toString());
    }

    public static String getErrorPermissionMessage() {
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
