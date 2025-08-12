package ru.hcc.guildmaster.tools;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * This class needs to comfortable work with plugin's permissions.
 */
public class PermissionTools extends ToolMethods {

    private static final String[] GUILD_MASTER_PERMS = new String[] {
            "guildmaster.guild.accept",
            "guildmaster.guild.deny",
            "guildmaster.guild.kick",
            "guildmaster.guild.track",
            "guildmaster.guild.edit",
            "guildmaster.guild.menu"
    };

    public void unSetPermission(String perm, Player player) {
        LuckPerms luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);
        if (luckPerms != null) luckPerms.getUserManager().modifyUser(player.getUniqueId(),
                user -> user.data().remove(Node.builder(perm).build()));
        else
            System.out.println(colorizeMessage("Permission '%s' can't be cleared on %s player because LuckPerms API returned a null source!".formatted(perm, player.getDisplayName()), Color.RED));
    }

    public void removeGuildMasterPerms(Player player) {
        for (String permission : GUILD_MASTER_PERMS) unSetPermission(permission, player);
    }

}
