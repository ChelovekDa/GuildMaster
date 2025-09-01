package ru.hcc.guildmaster.tools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.hcc.guildmaster.tools.timed.EventNameKey;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.timed.Search;
import ru.hcc.guildmaster.tools.timed.TimedMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

public class Eventer extends ToolMethods implements Listener {

    @EventHandler
    public void onGuildMasterJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (isOp(player)) {
            ArrayList<TimedMessage> timedMessages = new Search(null, null).search();
            if (!timedMessages.isEmpty())
                player.sendMessage(setColor("&6Есть непрочитанные уведомления (&6%s&6).".formatted(String.valueOf(timedMessages.size()))));
        }
        else {
            HashMap<String, Guild> guilds = new Reader().getGuilds();
            if (!guilds.isEmpty()) {
                for (String id : guilds.keySet()) {
                    Guild guild = guilds.get(id);
                    if (guild.getGuildMasterUUID().equals(event.getPlayer().getUniqueId().toString())) {
                        HashMap<String, Object> additionalValues = new HashMap<>();
                        additionalValues.put("guild", guild.id);
                        ArrayList<TimedMessage> timedMessages = new Search(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD, EventStatusKey.WAITING, additionalValues).search();
                        if (!timedMessages.isEmpty())
                            player.sendMessage(setColor("&6Есть непрочитанные заявки в гильдию (&6%s&6).".formatted(String.valueOf(timedMessages.size()))));
                    }
                }
            }
            else Bukkit.getLogger().log(Level.INFO, "Plugin not found that anybody created guild.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        HashMap<String, Guild> guilds = new Reader().getGuilds();
        if (guilds.isEmpty()) return;

        for (String id : guilds.keySet()) {
            Guild guild = guilds.get(id);
            if (guild.membersUUID.contains(player.getUniqueId().toString())) {
                guild.setPerms(player.getUniqueId());
                return;
            }
        }
    }

}
