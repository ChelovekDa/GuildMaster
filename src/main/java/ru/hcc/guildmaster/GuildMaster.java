package ru.hcc.guildmaster;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.hcc.guildmaster.commands.GuildCommand;
import ru.hcc.guildmaster.commands.GuildMasterCommand;
import ru.hcc.guildmaster.tools.Eventer;
import ru.hcc.guildmaster.tools.Reader;
import ru.hcc.guildmaster.tools.ToolMethods;
import ru.hcc.guildmaster.tools.menus.GuildEditorMenu;
import ru.hcc.guildmaster.tools.menus.GuildRequestsMenu;
import ru.hcc.guildmaster.tools.menus.GuildTrackingMenu;
import ru.hcc.guildmaster.tools.timed.EventNameKey;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.timed.Search;

import java.util.Objects;

public final class GuildMaster extends JavaPlugin {

    @Override
    public void onEnable() {

        // commands
        Objects.requireNonNull(getServer().getPluginCommand("guild")).setExecutor(new GuildCommand());
        Objects.requireNonNull(getServer().getPluginCommand("guildmaster")).setExecutor(new GuildMasterCommand());

        // tab-completer
        Objects.requireNonNull(getServer().getPluginCommand("guild")).setTabCompleter(new GuildCommand());
        Objects.requireNonNull(getServer().getPluginCommand("guildmaster")).setTabCompleter(new GuildMasterCommand());

        // menus
        getServer().getPluginManager().registerEvents(new GuildRequestsMenu(new Search(EventNameKey.EMPTY_KEY, EventStatusKey.NOTHING)), this);
        getServer().getPluginManager().registerEvents(new GuildTrackingMenu(), this);
        getServer().getPluginManager().registerEvents(new GuildEditorMenu(), this);

        // other backend
        getServer().getPluginManager().registerEvents(new Eventer(), this);

        new Reader();
        if (Bukkit.getOnlinePlayers().isEmpty() || Bukkit.getOperators().isEmpty()) return;

        int var1 = new Search(EventNameKey.ATTEMPT_TO_RELOAD, EventStatusKey.WAITING).search().size();
        if (var1 != 0 && !Bukkit.getOperators().isEmpty()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOp())
                    player.sendMessage(ToolMethods.setColor("&6Была произведена перезагрузка плагина %s!".formatted(GuildMaster.getPlugin(GuildMaster.class).getName())));
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
