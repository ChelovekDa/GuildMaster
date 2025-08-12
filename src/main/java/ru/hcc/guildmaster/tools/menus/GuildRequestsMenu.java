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
            System.out.println(colorizeMessage("In proccess of running the code was appeared an error!", Color.RED_BACKGROUND));
            Bukkit.getLogger().log(Level.WARNING, "Error: message in GuildRequestMenu.java is the null source! ItemStack string: %s".formatted(Objects.requireNonNull(event.getCurrentItem()).toString()));
        }
        else {
            Player player = Bukkit.getPlayer(UUID.fromString(String.valueOf(message.customValues.get("uuid"))));
            Player admin = (Player) event.getWhoClicked();
            Guild guild = Objects.requireNonNull(new Reader().getGuilds()).get(String.valueOf(message.customValues.get("guild")));
            ConfirmMenu confirmMenu = new ConfirmMenu() {
                @Override
                protected void onConfirm() {
                    if (admin.hasPermission("guildmaster.guild.accept")) {
                        if (guild.maxMembersCount <= guild.membersUUID.size()) {
                            admin.closeInventory();
                            admin.sendMessage(setColor("&cНевозможно принять игрока в гильдию: нет свободных мест."));
                            assert player != null;
                            System.out.println(colorizeMessage("Player %s can't be join to guild '%s' because it's no have empty slots!".formatted(player.getName(), guild.id), Color.RED));
                            new Reader().saveTimedMessage(message.setStatus(EventStatusKey.READ));
                        }
                        else {
                            guild.addMember(Objects.requireNonNull(player));
                            admin.sendMessage(setColor("&aИгрок %s успешно зачислен в гильдию %s&a!".formatted(player.displayName(), guild.displayName)));
                            if (player.isOnline()) player.sendMessage(setColor(guild.getSuccessMemberJoinMessage()));
                        }
                    }
                    else {
                        admin.closeInventory();
                        admin.sendMessage(getErrorPermissionMessage());
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
                        admin.sendMessage(setColor("&aВы отказали игроку %s в принятии в гильдию %s&a.".formatted(player.getName(), guild.displayName)));
                        if (Objects.requireNonNull(player).isOnline()) player.sendMessage(setColor(guild.getCancelMemberJoinMessage()));
                        new Reader().saveTimedMessage(message.setStatus(EventStatusKey.READ));
                    }
                    else {
                        admin.closeInventory();
                        admin.sendMessage(getErrorPermissionMessage());
                    }
                }
            };
            event.getWhoClicked().openInventory(confirmMenu.getMenu());
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

        Player player = Bukkit.getPlayer(UUID.fromString(String.valueOf(message.customValues.get("uuid"))));

        if (player == null) return null;

        meta.setDisplayName(setColor("&e%s".formatted(message.date)));

        ArrayList<String> arrayList = convertToMenu(message.message);
        arrayList.add(0, "");
        arrayList.add(1, "&fЗаявка на вступление в гильдию:");
        arrayList.add(2, "&fАйди гильдии: %s".formatted(message.customValues.get("guild")));
        arrayList.add(3, "&fИгрок: %s".formatted(player.getName()));
        arrayList.add(4,"&fСообщение игрока:");

        meta.setLore(List.of(setColors(ArrayToList(arrayList))));

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    @Nullable
    private TimedMessage getMessage(HashMap<String, Object> additionalValues) {
        Search search = new Search(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD, EventStatusKey.WAITING, additionalValues);
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

        Player player = Bukkit.getPlayer(lore.get(3).split(" ")[1]);
        assert player != null;

        map.put("id", lore.get(2).split(" ")[2]);
        map.put("uuid", player.getUniqueId().toString());

        return map;
    }

}
