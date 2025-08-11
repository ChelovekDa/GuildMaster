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
import ru.hcc.guildmaster.tools.Guild;
import ru.hcc.guildmaster.tools.Color;
import ru.hcc.guildmaster.tools.Reader;
import ru.hcc.guildmaster.tools.ToolMethods;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.timed.TimedMessage;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class GuildRequestJoinMenu extends ToolMethods implements Menu {

    private final TimedMessage message;

    public GuildRequestJoinMenu(TimedMessage message) {
        this.message = message;
    }

    @EventHandler
    public void onPlayerClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase(getMenuTitle())) {
            event.setCancelled(true);

            ItemStack currentItem = event.getCurrentItem();
            Player player = Bukkit.getPlayer(UUID.fromString(String.valueOf(message.customValues.get("uuid"))));
            Player admin = (Player) event.getWhoClicked();
            Guild guild = Objects.requireNonNull(new Reader().getGuilds()).get(String.valueOf(message.customValues.get("guild")));

            if (currentItem == null) return;
            else if (currentItem.equals(getAgreeItem()) && admin.hasPermission("guildmaster.guild.accept")) {

                if (guild.maxMembersCount <= guild.membersUUID.size()) {
                    admin.closeInventory();
                    admin.sendMessage(setColor("&cНевозможно принять игрока в гильдию: нет свободных мест."));
                    System.out.println(colorizeMessage("Player %s can't be join to guild '%s' because it's no have empty slots!".formatted(player.displayName(), guild.id), Color.RED));
                    return;
                }

                guild.addMember(Objects.requireNonNull(player));
                admin.sendMessage(setColor("&aИгрок %s успешно зачислен в гильдию &a&o%s&a!".formatted(player.displayName(), guild.displayName)));

                if (player.isOnline()) player.sendMessage(setColor(guild.getSuccessMemberJoinMessage()));

            }
            else if (currentItem.equals(getCancelItem()) && admin.hasPermission("guildmaster.guild.deny")) {
                admin.sendMessage(setColor("&aВы отказали игроку %s в принятии в гильдию &a&o%s&a."));

                if (Objects.requireNonNull(player).isOnline()) player.sendMessage(setColor(guild.getCancelMemberJoinMessage()));
            }

            new Reader().saveTimedMessage(message.setStatus(EventStatusKey.READ));

        }
    }

    @Override
    public @NotNull Inventory getMenu() {
        Inventory inventory = Bukkit.createInventory(null, 27, getMenuTitle());

        HashMap<Integer, ItemStack> items = getItemStacks();
        for (int key : items.keySet()) inventory.setItem(key, items.get(key));

        return inventory;
    }

    @Override
    public @NotNull String getMenuTitle() {
        return "Принятие или отклонение заявки";
    }

    @NotNull
    private ItemStack getCancelItem() {
        ItemStack itemStack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor("&c&lОтклонить"));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.BREACH, 1, true);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    @NotNull
    private ItemStack getAgreeItem() {
        ItemStack itemStack = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor("&a&lПринять"));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.CHANNELING, 1, true);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    @NotNull
    private HashMap<Integer, ItemStack> getItemStacks() {
        HashMap<Integer, ItemStack> result = new HashMap<>();

        result.put(12, getAgreeItem());
        result.put(14, getCancelItem());

        return result;
    }

}
