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
import ru.hcc.guildmaster.tools.Color;
import ru.hcc.guildmaster.tools.Reader;
import ru.hcc.guildmaster.tools.ToolMethods;
import ru.hcc.guildmaster.tools.menus.patterns.ConfirmMenu;

import java.util.List;
import java.util.logging.Level;

public class GuildTrackingMenu extends ToolMethods implements Menu {

    @EventHandler
    public void onPlayerInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase(getMenuTitle())) {
            event.setCancelled(true);
            ItemStack stack = event.getCurrentItem();
            if (stack == null) return;

            Player player = (Player) event.getWhoClicked();

            if (!player.hasPermission("guildmaster.guild.track")) player.sendMessage(getErrorPermissionMessage());

            if (stack.equals(getLeaveButton())) {
                ConfirmMenu menu = new ConfirmMenu() {
                    @SuppressWarnings("deprecation")
                    @Override
                    protected void onConfirm() {
                        Reader reader = new Reader();
                        if (!reader.restoreData(player)) {
                            player.sendMessage(setColor("&cОшибка восстановления данных! Обратитесь к администратору за помощью!"));
                            System.out.println(colorizeMessage("Error tracking data restore!", Color.RED_BACKGROUND_BRIGHT));
                            Bukkit.getLogger().log(Level.WARNING, "Error tracking data restore!");
                            return;
                        }

                        player.setInvisible(false);
                        player.setInvulnerable(false);
                        player.setAllowFlight(false);
                        player.setCanPickupItems(true);
                        player.setCollidable(true);

                        Bukkit.getLogger().log(Level.INFO, "Player %s left the tracking mode.".formatted(player.getDisplayName()));
                        player.sendMessage(setColor("&aВы вышли из режима слежки!"));
                        player.closeInventory();

                    }

                    @Override
                    protected void onCancel() {
                        player.closeInventory();
                        player.openInventory(new GuildTrackingMenu().getMenu());
                    }
                };
                player.closeInventory();
                player.openInventory(menu.getMenu());
            }
        }
    }

    @NotNull
    private ItemStack getLeaveButton() {
        ItemStack itemStack = new ItemStack(Material.IRON_DOOR);
        ItemMeta meta = itemStack.getItemMeta();

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.CHANNELING, 1, true);
        meta.setDisplayName(setColor("&c&lВыйти"));
        meta.setLore(List.of(setColors(new String[] {"", "&fВыйти из режима слежки", ""})));

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    @Override
    public @NotNull Inventory getMenu() {
        Inventory inventory = Bukkit.createInventory(null, 27, getMenuTitle());

        inventory.setItem(13, getLeaveButton());

        return inventory;
    }

    @Override
    public @NotNull String getMenuTitle() {
        return "Меню слежки";
    }
}
