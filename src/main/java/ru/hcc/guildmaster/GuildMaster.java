package ru.hcc.guildmaster;

import org.bukkit.plugin.java.JavaPlugin;
import ru.hcc.guildmaster.commands.GuildCommand;
import ru.hcc.guildmaster.tools.Eventer;
import ru.hcc.guildmaster.tools.Reader;
import ru.hcc.guildmaster.tools.menus.GuildEditorMenu;
import ru.hcc.guildmaster.tools.menus.GuildRequestsMenu;
import ru.hcc.guildmaster.tools.menus.GuildTrackingMenu;
import ru.hcc.guildmaster.tools.timed.EventNameKey;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.timed.Search;

import java.util.Objects;

public final class GuildMaster extends JavaPlugin {

    public static final String NAME = "GuildMaster";

    @Override
    public void onEnable() {

        // commands
        Objects.requireNonNull(getServer().getPluginCommand("guild")).setExecutor(new GuildCommand());

        // tab-completer
        Objects.requireNonNull(getServer().getPluginCommand("guild")).setTabCompleter(new GuildCommand());

        // menus
        getServer().getPluginManager().registerEvents(new GuildRequestsMenu(new Search(EventNameKey.EMPTY_KEY, EventStatusKey.NOTHING)), this);
        getServer().getPluginManager().registerEvents(new GuildTrackingMenu(), this);
        getServer().getPluginManager().registerEvents(new GuildEditorMenu(), this);

        // other backend
        getServer().getPluginManager().registerEvents(new Eventer(), this);

        new Reader();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
