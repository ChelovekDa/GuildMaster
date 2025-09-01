package ru.hcc.guildmaster.tools.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import ru.hcc.guildmaster.GuildMaster;
import ru.hcc.guildmaster.tools.Color;
import ru.hcc.guildmaster.tools.Guild;
import ru.hcc.guildmaster.tools.Reader;
import ru.hcc.guildmaster.tools.ToolMethods;
import ru.hcc.guildmaster.tools.menus.patterns.ConfirmMenu;
import ru.hcc.guildmaster.tools.timed.EventNameKey;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.timed.Search;
import ru.hcc.guildmaster.tools.timed.TimedMessage;

import java.util.*;
import java.util.logging.Level;

@SuppressWarnings("deprecation")
public class GuildEditorMenu extends ToolMethods implements Menu {

    private final Reader reader = new Reader();
    private Guild guild;

    @EventHandler
    public void onAdminPermissionAdding(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String mes = event.getMessage().replaceAll(" ", "");

        HashMap<String, Object> map = new HashMap<>();
        map.put("uuid", player.getUniqueId().toString());
        Search search = new Search(EventNameKey.GUILD_ADD_PERMISSION, EventStatusKey.NOTHING, map);
        ArrayList<TimedMessage> messages = search.search();
        if (messages.isEmpty()) return;
        else event.setCancelled(true);

        TimedMessage timedMessage = messages.getFirst();

        String guildId = String.valueOf(timedMessage.customValues.get("guild"));
        if (timedMessage.customValues.get("guild") == null) return;
        Guild guild = Objects.requireNonNull(reader.getGuilds()).get(guildId);

        for (String guildPermission : guild.getPermissions()) {
            if (guildPermission.equals(mes)) {
                for (TimedMessage message : messages) reader.saveTimedMessage(message.setStatus(EventStatusKey.READ));
                player.sendMessage(setColor("&cУстановить это разрешение невозможно, поскольку оно уже установленно для этой гильдии."));
                return;
            }
        }

        guild.addPermission(mes);
        reader.writeGuild(guild);
        for (TimedMessage message : messages) reader.saveTimedMessage(message.setStatus(EventStatusKey.WAITING));
        player.sendMessage(setColor("&aРазрешение &6&o%s&a успешно установленно для гильдии %s&a!".formatted(mes, guild.displayName)));
        player.sendMessage(setColor("&aДля успешной выдачи разрешения, всем участникам этой гильдии необходимо перезайти на сервер."));
    }

    @EventHandler
    public void onMemberCountChange(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String mes = event.getMessage().replaceAll(" ", "");

        HashMap<String, Object> map = new HashMap<>();
        map.put("uuid", player.getUniqueId().toString());
        Search search = new Search(EventNameKey.GUILD_CHANGE_COUNT_MEMBERS, EventStatusKey.NOTHING, map);
        var messages = search.search();
        if (messages.isEmpty()) return;
        else event.setCancelled(true);

        TimedMessage timedMessage = messages.getFirst();

        String guildId = String.valueOf(timedMessage.customValues.get("guild"));
        if (timedMessage.customValues.get("guild") == null) return;
        Guild guild = Objects.requireNonNull(reader.getGuilds()).get(guildId);

        byte newCount;
        try {
            newCount = Byte.parseByte(mes);
        } catch (Exception e) {
            log(Level.INFO, colorizeMessage("Can't setting new members count in guild '%s' because was appeared an error:\n%s".formatted(guild.id, e.getMessage()), Color.RED));
            player.sendMessage(setColor("&cНе удалось получить числовое значение с диапазоне между 1 и 127."));
            return;
        }

        if (guild.membersUUID.size() > newCount) {
            player.sendMessage(setColor("&cНевозможно установить такое количество участников, поскольку в гильдии состоит больше людей, чем вы хотите!"));
            log(Level.INFO, colorizeMessage("Can't setting new count of members in guild '%s' because now count of members more than need to set.".formatted(guild.id), Color.RED));
            return;
        }

        guild.maxMembersCount = newCount;
        reader.writeGuild(guild);
        reader.saveTimedMessage(timedMessage.setStatus(EventStatusKey.WAITING));
        player.sendMessage(setColor("&aМаксимальное количество участников гильдии %s&a изменено на %s&a!".formatted(guild.displayName, String.valueOf(newCount))));
    }

    @EventHandler
    public void onGuildNameChange(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String mes = event.getMessage();

        HashMap<String, Object> map = new HashMap<>();
        map.put("uuid", player.getUniqueId().toString());
        Search search = new Search(EventNameKey.GUILD_CHANGE_NAME, EventStatusKey.NOTHING, map);
        var messages = search.search();
        if (messages.isEmpty()) return;
        else event.setCancelled(true);

        TimedMessage timedMessage = messages.getFirst();

        String guildID = String.valueOf(timedMessage.customValues.get("guild"));
        if (guildID == null) return;
        Guild guild = Objects.requireNonNull(reader.getGuilds()).get(guildID);

        ConfirmMenu menu = new ConfirmMenu() {
            @Override
            protected void onConfirm() {
                guild.displayName = mes;

                TimedMessage timedMessage = messages.getFirst();
                timedMessage.setStatus(EventStatusKey.WAITING);
                reader.saveTimedMessage(timedMessage);

                reader.writeGuild(guild);

                player.sendMessage(setColor("&aНазвание гильдии успешно изменено!"));
                player.closeInventory();
            }

            @Override
            protected @NotNull String[] getConfirmLore() {
                return new String[] {"", "&fВы уверены, что хотите изменить название гильдии с '%s'&f на '%s'&f?".formatted(guild.displayName, mes), ""};
            }

            @Override
            protected void onCancel() {
                TimedMessage timedMessage = messages.getFirst();
                timedMessage.setStatus(EventStatusKey.READ);
                reader.saveTimedMessage(timedMessage);

                player.sendMessage(setColor("&aДействие отменено."));
                player.closeInventory();
            }
        };
        Bukkit.getScheduler().runTask(GuildMaster.getPlugin(GuildMaster.class), () -> {
            player.closeInventory();
            player.openInventory(menu.getMenu());
        });

    }

    @EventHandler
    public void onGuildMasterSetting(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String mes = event.getMessage().replaceAll(" ", "");

        HashMap<String, Object> map = new HashMap<>();
        map.put("uuid", player.getUniqueId().toString());
        ArrayList<TimedMessage> messages = new Search(EventNameKey.GUILD_CHANGE_MASTER, EventStatusKey.NOTHING, map).search();
        if (messages.isEmpty()) return;
        else event.setCancelled(true);

        TimedMessage timedMessage = messages.getFirst();

        String guildID = String.valueOf(timedMessage.customValues.get("guild"));
        if (guildID == null) return;
        var guilds = reader.getGuilds();
        Guild guild = Objects.requireNonNull(guilds).get(guildID);

        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (pl.getName().equals(mes)) {
                for (String id : guilds.keySet()) {
                    Guild g = guilds.get(id);
                    if (g.membersUUID.contains(pl.getUniqueId().toString()) && !g.id.equals(guild.id)) {
                        timedMessage.setStatus(EventStatusKey.READ);
                        reader.saveTimedMessage(timedMessage);
                        player.sendMessage(setColor("&cДанный игрок уже состоит в другой гильдии."));
                        return;
                    }
                }
                ConfirmMenu menu = new ConfirmMenu() {
                    @Override
                    protected void onConfirm() {
                        String masterUUID = guild.getGuildMasterUUID();
                        reader.writeGuild(guild.setGuildMasterUUID(pl.getUniqueId().toString()));

                        TimedMessage timedMessage = messages.getFirst();
                        reader.saveTimedMessage(timedMessage.setStatus(EventStatusKey.WAITING));

                        if (!masterUUID.isEmpty()) {
                            OfflinePlayer master = getPlayer(UUID.fromString(masterUUID));
                            if (master != null && master.isOnline()) {
                                Objects.requireNonNull(master.getPlayer()).sendMessage(setColor("&cВы были понижены до рядового участника гильдии!"));
                            }
                        }

                        broadcastMessage("&6Игрок %s&6 стал главой гильдии %s&6!".formatted(pl.getName(), guild.displayName), -1, null);
                        player.closeInventory();
                    }

                    @Override
                    protected @NotNull String[] getConfirmLore() {
                        return new String[] {"", "&aВы подтверждаете, что хотите установить в качестве нового главы гильдии игрока %s?".formatted(pl.getName()), ""};
                    }

                    @Override
                    protected void onCancel() {
                        TimedMessage timedMessage = messages.getFirst();
                        timedMessage.setStatus(EventStatusKey.READ);
                        reader.saveTimedMessage(timedMessage);
                        player.sendMessage(setColor("&aДействие отменено."));
                        player.closeInventory();
                    }
                };
                Bukkit.getScheduler().runTask(GuildMaster.getPlugin(GuildMaster.class), () -> {
                    player.closeInventory();
                    player.openInventory(menu.getMenu());
                });
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerCloseInventory(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase(getMenuTitle())) return;
        Player player = (Player) event.getViewers().getFirst();
        assert player != null;

        HashMap<String, Object> map = new HashMap<>();
        map.put("uuid", player.getUniqueId().toString());
        ArrayList<TimedMessage> messages = new Search(EventNameKey.OPEN_GUILD_EDITOR_MENU, EventStatusKey.NOTHING, map).search();

        if (messages.isEmpty()) return;

        TimedMessage timedMessage = messages.getFirst();
        reader.saveTimedMessage(timedMessage.setStatus(EventStatusKey.READ));

    }

    @EventHandler
    public void onPlayerClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase(getMenuTitle())) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack stack = event.getCurrentItem();
        if (stack == null) return;

        ItemStack idItem = Objects.requireNonNull(event.getClickedInventory()).getItem(45);
        if (idItem == null) {
            player.closeInventory();
            player.sendMessage(setColor("&cОшибка! Обратитесь к администратору!"));
            log(Level.WARNING, colorizeMessage("Editor null source!", Color.RED));
            return;
        }

        String id = idItem.getItemMeta().getDisplayName().split(" ")[1];
        if (!Objects.requireNonNull(reader.getGuildNames()).contains(id)) {
            player.closeInventory();
            player.sendMessage(setColor("&cОшибка! Обратитесь к администратору!"));
            log(Level.WARNING, colorizeMessage("Editor null source!", Color.RED));
            return;
        }

        HashMap<String, Guild> guilds = reader.getGuilds();
        guild = null;
        for (String guildId : guilds.keySet()) {
            if (guildId.equals(id)) guild = guilds.get(guildId);
        }

        if (guild == null) {
            player.closeInventory();
            player.sendMessage(setColor("&cОшибка! Обратитесь к администратору!"));
            log(Level.WARNING, colorizeMessage("Editor null source!", Color.RED));
            return;
        }

        if (stack.equals(getColorDescriptionItem())) {
            player.sendMessage(getColorsHelpMessage());
            player.closeInventory();
        }
        else if (stack.equals(getSettingGuildMasterItem())) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("guild", guild.id);
            map.put("uuid", player.getUniqueId().toString());
            TimedMessage message = new TimedMessage(EventNameKey.GUILD_CHANGE_MASTER, EventStatusKey.NOTHING, EventNameKey.GUILD_CHANGE_MASTER.getMessage(), map);

            reader.saveTimedMessage(message);
            player.closeInventory();
            player.sendMessage(setColor("&aВведите в чат игровой ник нового главы."));
        }
        else if (stack.equals(getSetDisplayNameItem())) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("guild", guild.id);
            map.put("uuid", player.getUniqueId().toString());
            TimedMessage message = new TimedMessage(EventNameKey.GUILD_CHANGE_NAME, EventStatusKey.NOTHING, EventNameKey.GUILD_CHANGE_NAME.getMessage(), map);

            reader.saveTimedMessage(message);
            player.closeInventory();
            player.sendMessage(setColor("&aВведите в чат новое название гильдии."));
        }
        else if (stack.equals(getSetMembersCountItem())) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("guild", guild.id);
            map.put("uuid", player.getUniqueId().toString());
            TimedMessage message = new TimedMessage(EventNameKey.GUILD_CHANGE_COUNT_MEMBERS, EventStatusKey.NOTHING, EventNameKey.GUILD_CHANGE_COUNT_MEMBERS.getMessage(), map);

            reader.saveTimedMessage(message);
            player.closeInventory();
            player.sendMessage(setColor("&aВведите в чат новое максимальное количество игроков в гильдии."));
        }
        else if (stack.equals(getDeleteItem())) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("guild", guild.id);
            map.put("uuid", player.getUniqueId().toString());
            TimedMessage message = new TimedMessage(EventNameKey.GUILD_DELETE, EventStatusKey.NOTHING, EventNameKey.GUILD_DELETE.getMessage(), map);

            reader.saveTimedMessage(message);

            ConfirmMenu menu = new ConfirmMenu() {
                @Override
                protected void onConfirm() {
                    message.message = "Guild deleted";
                    reader.saveTimedMessage(message.setStatus(EventStatusKey.WAITING));
                    reader.deleteGuild(guild);
                    player.sendMessage(setColor("&aГильдия %s&a успешно удалена!".formatted(guild.displayName)));
                    player.closeInventory();
                }

                @Override
                protected @NotNull String[] getConfirmLore() {
                    return new String[] {"", "&cВы уверены, что хотите безвозвратно удалить эту гильдию?", ""};
                }

                @Override
                protected void onCancel() {
                    reader.saveTimedMessage(message.setStatus(EventStatusKey.READ));
                    player.sendMessage(setColor("&aДействие отменено."));
                    player.closeInventory();
                }
            };
            player.closeInventory();
            player.openInventory(menu.getMenu());
        }
        else if (stack.equals(getPermissionAddItem())) {

            if (!isOp(player)) {
                player.sendMessage(setColor("&cВам не доступна эта опция!"));
                player.closeInventory();
                return;
            }

            HashMap<String, Object> map = new HashMap<>();
            map.put("guild", guild.id);
            map.put("uuid", player.getUniqueId().toString());
            TimedMessage message = new TimedMessage(EventNameKey.GUILD_ADD_PERMISSION, EventStatusKey.NOTHING, EventNameKey.GUILD_ADD_PERMISSION.getMessage(), map);

            reader.saveTimedMessage(message);
            player.closeInventory();
            player.sendMessage(setColor("&aВведите в чат новое разрешение для гильдии."));
        }
    }

    private static final byte[] cords = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 49, 50, 51, 52, 53};

    @NotNull
    private ItemStack getMenuDecorationGlass() {
        ItemStack itemStack = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName("");

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @NotNull
    private ItemStack getMenuGuildIDItem() {
        ItemStack itemStack = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor("&f&lID: %s".formatted(guild.id)));

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @NotNull
    private ItemStack getPermissionAddItem() {
        ItemStack itemStack = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor("&2&lДобавить разрешение"));

        ArrayList<String> lore = new ArrayList<>();
        lore.addFirst("");
        lore.add("&fПозволяет добавить разрешение (пермишен) для всех участников гильдии.");
        lore.add("&cМожно использовать только администратору.&f");
        lore.add("&fРазрешения этой гильдии:");

        ArrayList<String> perms = guild.getPermissions();
        if (perms.isEmpty()) {
            lore.add("");
            lore.add(" &cУ этой гильдии нет уникальных разрешений.");
        }
        else for (String perm : perms) lore.add(" &6- %s".formatted(perm));
        lore.addLast("");

        meta.setLore(List.of(setColors(ArrayToList(lore))));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.KNOCKBACK, 2, false);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @NotNull
    private ItemStack getColorDescriptionItem() {
        ItemStack itemStack = new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor("&bСправка по цветам"));
        meta.setLore(List.of(setColors(new String[] {"", "&fВыводит в чат справку по цветовым кодам.", ""})));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.LOYALTY, 1, false);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @NotNull
    private ItemStack getSettingGuildMasterItem() {
        ItemStack itemStack = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor("&a&lНазначить главу гильдии"));
        meta.setLore(List.of(setColors(new String[] {"", "&fПозволяет назначить нового главу гильдии.", "&fТекущий глава гильдии: &b%s&f".formatted(guild.getGuildMasterName()), "&cПредупреждение!&f Информацию о назначении нового главы увидят все игроки!", "&fНовый глава гильдии должен быть &f&nонлайн&f.", ""})));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.LOOTING, 1, false);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @NotNull
    private ItemStack getSetDisplayNameItem() {
        ItemStack itemStack = new ItemStack(Material.BOOK);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor("&d&lИзменить название"));
        meta.setLore(List.of(setColors(new String[] {"", "&fПозволяет изменить&f&o визуальное&f название гильдии, с использованием цветовых кодов.", "&fТекущее визуальное название: %s".formatted(guild.displayName), "&8Примечание: справку по использованию цветовых кодов вы можете найти справа внизу.", ""})));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.POWER, 2, false);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @NotNull
    private ItemStack getMembersManipulationItem() {
        ItemStack itemStack = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor("&3&l&kУправление участниками"));
        meta.setLore(List.of(setColors(new String[] {"", "&c&lВ РАЗРАБОТКЕ..", ""})));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.EFFICIENCY, 4, false);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @NotNull
    private ItemStack getSetMembersCountItem() {
        ItemStack itemStack = new ItemStack(Material.REDSTONE);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor("&6&lУстановить количество участников"));
        meta.setLore(List.of(setColors(new String[] {"", "&fПозволяет изменить максимально возможное количество участников в гильдии ", "&cв диапазоне от 1 до 127 человек.", "&fТекущее максимальное количество участников: &b%s".formatted(String.valueOf(guild.maxMembersCount)), ""})));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.KNOCKBACK, 2, false);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @NotNull
    private ItemStack getDeleteItem() {
        ItemStack itemStack = new ItemStack(Material.TNT);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor("&c&lУдалить гильдию"));
        meta.setLore(List.of(setColors(new String[] {"", "&fПолностью удаляет гильдию, но сохранит все данные в логах плагина.", ""})));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 1, false);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    @NotNull
    private HashMap<Integer, ItemStack> getMenuDecorations() {
        HashMap<Integer, ItemStack> map = new HashMap<>();
        for (byte cord : cords) map.put((int) cord, getMenuDecorationGlass());
        return map;
    }

    @Override
    public @NotNull Inventory getMenu() {
        Inventory inventory = Bukkit.createInventory(null, 54, getMenuTitle());

        ArrayList<TimedMessage> messages = new Search(EventNameKey.OPEN_GUILD_EDITOR_MENU, EventStatusKey.NOTHING).search();

        if (messages.isEmpty()) return getNullInventory();
        TimedMessage timedMessage = messages.getFirst();
        Player player = Bukkit.getPlayer(UUID.fromString(String.valueOf(timedMessage.customValues.get("uuid"))));

        if (player != null) {
            guild = Objects.requireNonNull(reader.getGuilds()).get(String.valueOf(timedMessage.customValues.get("guild")));

            var decors = getMenuDecorations();
            for (int key : decors.keySet()) inventory.setItem(key, decors.get(key));

            inventory.setItem(20, getMembersManipulationItem());
            inventory.setItem(21, getSettingGuildMasterItem());
            inventory.setItem(22, getDeleteItem());
            inventory.setItem(23, getSetMembersCountItem());
            inventory.setItem(24, getSetDisplayNameItem());

            inventory.setItem(31, getPermissionAddItem());

            inventory.setItem(43, getColorDescriptionItem());
            inventory.setItem(45, getMenuGuildIDItem());

            reader.saveTimedMessage(timedMessage.setStatus(EventStatusKey.READ));

            return inventory;
        }

        reader.saveTimedMessage(timedMessage.setStatus(EventStatusKey.READ));
        return getNullInventory();
    }

    @Override
    public @NotNull String getMenuTitle() {
        return "Управление гильдией";
    }
}
