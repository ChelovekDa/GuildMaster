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
import ru.hcc.guildmaster.tools.timed.Search;
import ru.hcc.guildmaster.tools.timed.TimedMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class GuildRequestsMenu extends ToolMethods implements Menu {

    @EventHandler
    public void onPlayerClickMenu(InventoryClickEvent event) {
        if (event.getView().getTitle().equalsIgnoreCase(getMenuTitle())) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null) return;

            TimedMessage message = fromItemStack(event.getCurrentItem());
            if (message == null) {
                event.getWhoClicked().sendMessage(setColor("&cВ процессе выполнения возникла ошибка! Обратитесь к администратору!"));
                System.out.println(colorizeMessage("In proccess of running the code was appeared an error!", Color.RED_BOLD));
                Bukkit.getLogger().log(Level.WARNING, "Error: message in GuildRequestJoinMenu.java is the null source! ItemStack string: %s".formatted(Objects.requireNonNull(event.getCurrentItem()).toString()));
            }
            else event.getWhoClicked().openInventory(new GuildRequestJoinMenu(message).getMenu());
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
            for (TimedMessage mes : messages) inventory.addItem(getItem(mes));
        }
        else inventory.setItem(22, getEmptyItem());

        return inventory;
    }

    @NotNull
    private ItemStack getItem(TimedMessage message) {
        ItemStack itemStack = ItemStack.of(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = itemStack.getItemMeta();

        Player player = Bukkit.getPlayer(UUID.fromString(String.valueOf(message.customValues.get("uuid"))));
        Guild guild = Objects.requireNonNull(new Reader().getGuilds()).get(String.valueOf(message.customValues.get("guild")));

        meta.setDisplayName(setColor("&e%s".formatted(message.date)));

        ArrayList<String> arrayList = convertToMenu(message.message);
        arrayList.add(0, "&fЯ, &f&o%s&f, прошу принять меня в гильдию '%s' в качестве нового участника!".formatted(player.getDisplayName(), guild.displayName));
        arrayList.add("");
        arrayList.add(1, "&eОбо мне:");

        meta.setLore(List.of(setColors(ArrayToList(arrayList))));

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.AQUA_AFFINITY, 1, true);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    @Nullable
    private TimedMessage fromItemStack(ItemStack itemStack) {
        ArrayList<TimedMessage> messages = searchObject.search();

        for (TimedMessage mes : messages) {
            ItemStack stack = getItem(mes);
            if (Objects.equals(stack, itemStack)) return mes;
        }

        return null;
    }

}
