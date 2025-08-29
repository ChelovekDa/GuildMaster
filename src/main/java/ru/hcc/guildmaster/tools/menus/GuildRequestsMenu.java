package ru.hcc.guildmaster.tools.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hcc.guildmaster.tools.Guild;
import ru.hcc.guildmaster.tools.Color;
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
public class GuildRequestsMenu extends ToolMethods implements Menu {

    @EventHandler
    public void onPlayerClickMenu(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase(getMenuTitle())) return;
        event.setCancelled(true);

        ItemStack currentItem = event.getCurrentItem();

        if (currentItem == null || currentItem.equals(getEmptyItem())) return;

        TimedMessage message = getMessage(getAdditionalValues(currentItem));
        if (message == null) {
            event.getWhoClicked().sendMessage(setColor("&cВ процессе выполнения возникла ошибка! Обратитесь к администратору!"));
            log(Level.WARNING, colorizeMessage("Request null source!", Color.RED_BACKGROUND));
            return;
        }

        if (message.eventNameKey.equals(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD)) {
            Player player = Bukkit.getPlayer(UUID.fromString(String.valueOf(message.customValues.get("uuid"))));
            Player admin = (Player) event.getWhoClicked();
            Guild guild = Objects.requireNonNull(new Reader().getGuilds()).get(String.valueOf(message.customValues.get("guild")));
            ConfirmMenu confirmMenu = new ConfirmMenu() {
                @Override
                protected void onConfirm() {
                    if (admin.hasPermission("guildmaster.guild.accept") || admin.hasPermission("guildmaster.*") || admin.isOp()) {
                        if (guild.maxMembersCount <= guild.membersUUID.size()) {
                            admin.closeInventory();
                            admin.sendMessage(setColor("&cНевозможно принять игрока в гильдию: нет свободных мест."));
                            assert player != null;
                            log(Level.WARNING, colorizeMessage("Player %s can't be join to guild '%s' because it's no have empty slots!".formatted(player.getName(), guild.id), Color.RED));
                            new Reader().saveTimedMessage(message.setStatus(EventStatusKey.READ));
                        }
                        else {
                            guild.addMember(Objects.requireNonNull(player));
                            admin.closeInventory();
                            admin.sendMessage(setColor("&aИгрок %s успешно зачислен в гильдию %s&a!".formatted(player.getName(), guild.displayName)));
                            if (player.isOnline()) player.sendMessage(setColor(guild.getSuccessMemberJoinMessage()));

                            for (TimedMessage mes : message.getSearchInstance().search())
                                new Reader().saveTimedMessage(mes.setStatus(EventStatusKey.READ));
                        }
                    }
                    else {
                        admin.closeInventory();
                        admin.sendMessage(errorPermission());
                    }
                }

                @Override
                protected @NotNull String[] getCancelLore() {
                    assert player != null;
                    return new String[] {"", "&fВы подтверждаете, что хотите отклонить заявку в гильдию %s&f игрока %s&f?".formatted(guild.displayName, player.getName()), ""};
                }

                @Override
                protected @NotNull String[] getConfirmLore() {
                    assert player != null;
                    return new String[] {"", "&fВы подтверждаете, что хотите принять заявку в гильдию %s&f игрока %s&f?".formatted(guild.displayName, player.getName()), ""};
                }

                @Override
                protected void onCancel() {
                    if (admin.hasPermission("guildmaster.guild.deny")) {
                        assert player != null;
                        if (Objects.requireNonNull(player).isOnline()) player.sendMessage(setColor(guild.getCancelMemberJoinMessage()));
                        new Reader().saveTimedMessage(message.setStatus(EventStatusKey.READ));
                        admin.sendMessage(setColor("&aВы отказали игроку %s в принятии в гильдию %s&a.".formatted(player.getName(), guild.displayName)));
                        admin.closeInventory();
                    }
                    else {
                        admin.closeInventory();
                        admin.sendMessage(errorPermission());
                    }
                }
            };
            admin.openInventory(confirmMenu.getMenu());
        }
        else {
            Player admin = (Player) event.getWhoClicked();
            ConfirmMenu menu = new ConfirmMenu() {
                @Override
                protected void onConfirm() {
                    new Reader().saveTimedMessage(message.setStatus(EventStatusKey.READ));
                    admin.closeInventory();
                    admin.sendMessage(setColor("&aУведомление помечено как прочитанное!"));
                }

                @Override
                protected @NotNull String[] getCancelLore() {
                    return new String[] {"", "&aПометить это уведомление как прочитанное?", ""};
                }

                @Override
                protected void onCancel() {
                    new Reader().saveTimedMessage(message.setStatus(EventStatusKey.WAITING));
                    admin.closeInventory();
                }
            };
            admin.openInventory(menu.getMenu());
        }
    }

    private final Search searchObject;

    public GuildRequestsMenu(@NotNull Search searchObject) {
        this.searchObject = searchObject;
    }

    @Override
    public @NotNull String getMenuTitle() {
        return "Сообщения гильдий";
    }

    @Override
    public @NotNull Inventory getMenu() {
        Inventory inventory = Bukkit.createInventory(null, 54, getMenuTitle());

        ArrayList<TimedMessage> messages = searchObject.search();
        if (!messages.isEmpty()) {
            for (TimedMessage mes : messages) {
                ItemStack itemStack = getItem(mes);
                if (itemStack == null) continue;
                inventory.addItem(itemStack);
            }
        }
        else inventory.setItem(22, getEmptyItem());

        return inventory;
    }

    @Nullable
    private ItemStack getItem(TimedMessage message) {
        ItemStack itemStack = ItemStack.of(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();

        Player player;
        try {
            player = Bukkit.getPlayer(UUID.fromString(String.valueOf(message.customValues.get("uuid"))));
        }
        catch (IllegalArgumentException exception) {
            player = null;
        }

        if (player == null && !message.eventNameKey.equals(EventNameKey.ATTEMPT_TO_RELOAD)) return null;

        meta.setDisplayName(setColor("&e%s".formatted(message.date)));

        if (message.eventNameKey.equals(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD)) {
            assert player != null;
            ArrayList<String> arrayList = convertToMenu(message.message);
            arrayList.add(0, "");
            arrayList.add(1, "&fЗаявка на вступление в гильдию:");
            arrayList.add(2, "&fАйди гильдии: %s%s".formatted(getRequestsColor(), message.customValues.get("guild")));
            arrayList.add(3, "&fИгрок: %s%s".formatted(getRequestsColor(), player.getName()));
            arrayList.add(4,"&fСообщение игрока:");
            arrayList.add(5, "");

            meta.setLore(List.of(setColors(ArrayToList(arrayList))));
        }
        else if (message.eventNameKey.equals(EventNameKey.ATTEMPT_TO_RELOAD)) {
            var res = String.valueOf(message.customValues.get("uuid"));
            String name;
            if (res != null && res.equals("ConsoleSender")) name = "&c&lКонсоль&f";
            else {
                assert player != null;
                name = player.getName();
            }

            ArrayList<String> arrayList = convertToMenu(message.message);
            arrayList.add(0, "");
            arrayList.add(1, "&fДействие: %s%s".formatted(getRequestsColor(), message.eventNameKey.getMessage()));
            arrayList.add(2, "&fАктивировал: %s%s".formatted(getRequestsColor(), name));
            arrayList.add(3, "");

            meta.setLore(List.of(setColors(ArrayToList(arrayList))));
        }
        else {
            assert player != null;
            ArrayList<String> arrayList = convertToMenu(message.message);
            arrayList.add(0, "");
            arrayList.add(1, "&fДействие: %s%s".formatted(getRequestsColor(), message.eventNameKey.getMessage()));
            arrayList.add(2, "&fАйди гильдии: %s%s".formatted(getRequestsColor(), message.customValues.get("guild")));
            arrayList.add(3, "&fАктивировал: %s%s".formatted(getRequestsColor(), player.getName()));
            arrayList.add(4, "");

            meta.setLore(List.of(setColors(ArrayToList(arrayList))));
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    @Nullable
    private TimedMessage getMessage(HashMap<String, Object> additionalValues) {
        Search search;

        if (additionalValues.containsKey("action")) {
            EventNameKey key = EventNameKey.getById(Integer.parseInt(additionalValues.get("action").toString()));
            additionalValues.remove("action");

            search = new Search(key, EventStatusKey.WAITING, additionalValues);
        }
        else {
            search = new Search(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD, EventStatusKey.WAITING, additionalValues);
        }

        try {
            return search.search().getFirst();
        } catch (Exception e) {
            return null;
        }
    }

    private HashMap<String, Object> getAdditionalValues(ItemStack stack) {
        List<String> lore = stack.getItemMeta().getLore();
        HashMap<String, Object> map = new HashMap<>();
        assert lore != null;

        String nickname;
        Player player;

        if (lore.size() == 5) {
            nickname = removeHistory(lore.get(2).split(" ")[1]).replace(getRequestsColor(), "");
            if (nickname.contains("&")) nickname = "ConsoleSender";
            else {
                player = Bukkit.getPlayer(nickname);
                assert player != null;
                nickname = player.getUniqueId().toString();
            }
            map.put("uuid", nickname);
        }
        else {
            nickname = removeHistory(lore.get(3).split(" ")[1]).replace(getRequestsColor(), "");
            player = Bukkit.getPlayer(nickname);
            assert player != null;

            map.put("guild", removeHistory(lore.get(2).split(" ")[2]).replace(getRequestsColor(), ""));
            map.put("uuid", player.getUniqueId().toString());
        }

        String line = removeHistory(lore.get(1));
        if (line.contains(getRequestsColor())) {
            map.put("action", EventNameKey.getByMessage(line.replaceAll(getRequestsColor(), "").split(": ")[1]).getId());
        }
        return map;
    }

}
