package ru.hcc.guildmaster.tools.menus.patterns;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import ru.hcc.guildmaster.GuildMaster;
import ru.hcc.guildmaster.tools.ToolMethods;
import ru.hcc.guildmaster.tools.menus.Menu;

import java.util.List;

/**
 * This class need to confirm or cancel something actions.
 * It's basic menu-class for needs other menus.
 */
public abstract class ConfirmMenu extends ToolMethods implements Menu {

    public ConfirmMenu() {
        register();
    }

    @EventHandler
    public void onPlayerClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equalsIgnoreCase(getMenuTitle())) return;
        event.setCancelled(true);
        ItemStack itemStack = event.getCurrentItem();
        if (itemStack == null) return;

        if (itemStack.equals(getConfirmButton())) {
            onConfirm();
        }
        else if (itemStack.equals(getCancelButton())) {
            onCancel();
        }
        unregister();
    }

    @NotNull
    protected ItemStack getConfirmButton() {
        ItemStack itemStack = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor(getConfirmTitle()));
        meta.setLore(List.of(setColors(getConfirmLore())));

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        meta.addEnchant(Enchantment.EFFICIENCY, 1, false);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    @NotNull
    protected ItemStack getCancelButton() {
        ItemStack itemStack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(setColor(getCancelTitle()));
        meta.setLore(List.of(setColors(getCancelLore())));

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        meta.addEnchant(Enchantment.FIRE_PROTECTION, 1, false);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    @NotNull
    protected String getConfirmTitle() {
        return "&a&lПодтвердить";
    }

    @NotNull
    protected String getCancelTitle() {
        return "&c&lОтклонить";
    }

    @NotNull
    protected String[] getCancelLore() {
        return new String[] {"", "&fОтклонить действие", ""};
    }

    @NotNull
    protected String[] getConfirmLore() {
        return new String[] {"", "&fПодтвердить действие", ""};
    }

    private void register() {
        JavaPlugin plugin = GuildMaster.getPlugin(GuildMaster.class);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void unregister() {
        JavaPlugin plugin = GuildMaster.getPlugin(GuildMaster.class);
        InventoryClickEvent.getHandlerList().unregister(this);
    }

    protected abstract void onConfirm();

    protected abstract void onCancel();

    @Override
    public @NotNull Inventory getMenu() {
        Inventory inventory = Bukkit.createInventory(null, 27, getMenuTitle());

        inventory.setItem(12, getConfirmButton());
        inventory.setItem(14, getCancelButton());

        return inventory;
    }

    @Override
    public @NotNull String getMenuTitle() {
        return "Меню подтверждения действия";
    }

}
