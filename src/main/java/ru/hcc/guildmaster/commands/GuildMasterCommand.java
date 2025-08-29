package ru.hcc.guildmaster.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hcc.guildmaster.GuildMaster;
import ru.hcc.guildmaster.tools.Reader;
import ru.hcc.guildmaster.tools.ToolMethods;
import ru.hcc.guildmaster.tools.menus.patterns.ConfirmMenu;
import ru.hcc.guildmaster.tools.timed.EventNameKey;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.timed.TimedMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class GuildMasterCommand extends ToolMethods implements CommandExecutor, TabCompleter {

    private final Reader reader = new Reader();
    private static final String[] COMMAND_VALUES = new String[] {"reload"};
    private Plugin pl;

    @Nullable
    private String reload() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin other = null;
        String message = null;

        var plugins = pluginManager.getPlugins();
        if (plugins.length >= 2) {

            for (Plugin plugin : pluginManager.getPlugins()) {
                if (plugin.isEnabled()) {
                    if (plugin.getName().equals(GuildMaster.getPlugin(GuildMaster.class).getName())) pl = plugin;
                    else other = plugin;

                    if (pl != null && other != null) break;
                }
            }
            if (pl != null && other != null) {
                Bukkit.getScheduler().runTaskLater(other, () -> pluginManager.disablePlugin(pl), 30L);
                Bukkit.getScheduler().runTaskLater(other, () -> pluginManager.enablePlugin(pl), 80L);
            }
            else message = "&cПерезагрузка невозможна: ошибка назначения вспомогательных интерфейсов!";
        }
        else message = "&cПерезагрузка невозможна: отсутствует вспомогательные интерфейсы!";

        return message;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) return false;
        else if (args[0].equals("reload")) {
            if (sender instanceof Player) {
                if (!sender.isOp()) {
                    sender.sendMessage(errorPermission());
                    return false;
                }

                HashMap<String, Object> additionalValues = new HashMap<>();
                additionalValues.put("uuid", ((Player) sender).getUniqueId().toString());
                TimedMessage message = new TimedMessage(EventNameKey.ATTEMPT_TO_RELOAD, EventStatusKey.NOTHING, EventNameKey.ATTEMPT_TO_RELOAD.getMessage(), additionalValues);
                reader.saveTimedMessage(message);

                ConfirmMenu confirmMenu = new ConfirmMenu() {
                    @Override
                    protected void onConfirm() {
                        ((Player) sender).closeInventory();
                        sender.sendMessage(setColor("&6Все операторы будут уведомлены, когда плагин будет снова функционировать."));
                        sender.sendMessage(setColor("&cПерезагрузка плагина.."));
                        reader.saveTimedMessage(message.setStatus(EventStatusKey.WAITING));

                        String mes = reload();
                        if (mes != null) {
                            sender.sendMessage(setColor(mes));
                            reader.saveTimedMessage(message.setStatus(EventStatusKey.READ));
                            return;
                        }
                        reader.saveTimedMessage(message.setStatus(EventStatusKey.WAITING));
                    }

                    @Override
                    protected @NotNull String[] getConfirmLore() {
                        return new String[] {"", "&c&lВы подтверждаете, что хотите перезагрузить плагин?", "", "&fПерезагрузка плагина может длиться до 10 секунд (в зависимости от мощности вашего оборудования).", "&fНа протяжении всего этого времени гильдии не будут функционировать!", ""};
                    }

                    @Override
                    protected void onCancel() {
                        ((Player) sender).closeInventory();
                        sender.sendMessage(setColor("&aДействие отменено!"));
                        reader.saveTimedMessage(message.setStatus(EventStatusKey.READ));
                    }
                };
                ((Player) sender).openInventory(confirmMenu.getMenu());
            }
            else {
                HashMap<String, Object> additionalValues = new HashMap<>();
                additionalValues.put("uuid", "ConsoleSender");
                TimedMessage message = new TimedMessage(EventNameKey.ATTEMPT_TO_RELOAD, EventStatusKey.NOTHING, EventNameKey.ATTEMPT_TO_RELOAD.getMessage(), additionalValues);
                reader.saveTimedMessage(message);

                sender.sendMessage(setColor("&6Все операторы будут уведомлены, когда плагин будет снова функционировать."));
                sender.sendMessage(setColor("&cПерезагрузка плагина.."));

                String mes = reload();
                if (mes != null) {
                    sender.sendMessage(setColor(mes));
                    reader.saveTimedMessage(message.setStatus(EventStatusKey.READ));
                    return false;
                }
                reader.saveTimedMessage(message.setStatus(EventStatusKey.WAITING));
            }
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

        List<String> completions = new ArrayList<>();
        if (sender.isOp()) completions.addAll(Arrays.asList(COMMAND_VALUES));
        return completions;
    }

}
