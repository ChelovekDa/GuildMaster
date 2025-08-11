package ru.hcc.guildmaster.tools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.hcc.guildmaster.tools.timed.EventNameKey;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.timed.Search;

import java.util.logging.Level;

public class Eventer extends ToolMethods implements Listener {

    @EventHandler
    public void onGuildMasterJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.isOp() || player.hasPermission("guildmaster.*")) {
            var timedMessages = new Search(null, null).search();
            if (!timedMessages.isEmpty())
                player.sendMessage(setColor("&6Есть непрочитанные заявки в гильдии (&6%s&6).".formatted(String.valueOf(timedMessages.size()))));
        }
        else {
            var guilds = new Reader().getGuilds();
            if (guilds != null) {
                for (String id : guilds.keySet()) {
                    Guild guild = guilds.get(id);
                    if (guild.guildMasterUUID.equals(event.getPlayer().getUniqueId().toString())) {
                        var timedMessages = new Search(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD, EventStatusKey.WAITING).search();
                        if (!timedMessages.isEmpty())
                            player.sendMessage(setColor("&6Есть непрочитанные заявки в гильдию (&6%s&6).".formatted(String.valueOf(timedMessages.size()))));
                    }
                }
            }
            else Bukkit.getLogger().log(Level.INFO, "Plugin not found that anybody created guild.");
        }
    }

}
