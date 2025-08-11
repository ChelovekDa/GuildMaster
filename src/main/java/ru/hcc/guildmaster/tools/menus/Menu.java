package ru.hcc.guildmaster.tools.menus;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Menu extends Listener {

    @NotNull
    Inventory getMenu();

    @NotNull
    String getMenuTitle();

    @NotNull
    default String getEmptyItemDescription() {
        return "\nЕще никто ничего не подал :(\n";
    }

    @NotNull
    default String getEmptyItemTitle() {
        return "Грустный квадратик";
    }

    @NotNull
    default ItemStack getEmptyItem() {
        ItemStack itemStack = ItemStack.of(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();

        meta.setDisplayName(getEmptyItemTitle());
        meta.setLore(List.of(getEmptyItemDescription()));
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

}
