package ru.hcc.guildmaster.tools;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.logging.Level;

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

    public void unSetPermission(String perm, UUID uuid) {
        LuckPerms luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);
        if (luckPerms != null) luckPerms.getUserManager().modifyUser(uuid,
                user -> user.data().remove(Node.builder(perm).build()));
        else log(Level.WARNING, colorizeMessage("Permission '%s' can't be cleared on %s player because LuckPerms API returned a null source!".formatted(perm, getPlayerName(uuid.toString())), Color.RED));
    }

    public void setPermission(String perm, UUID uuid) {
        LuckPerms luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);
        if (luckPerms != null) luckPerms.getUserManager().modifyUser(uuid,
                user -> user.data().add(Node.builder(perm).build()));
        else log(Level.WARNING, colorizeMessage("Permission '%s' can't be cleared on %s player because LuckPerms API returned a null source!".formatted(perm, getPlayerName(uuid.toString())), Color.RED));

    }

    protected void removeGuildMasterPerms(UUID masterUUID) {
        for (String permission : GUILD_MASTER_PERMS) unSetPermission(permission, masterUUID);
    }

    protected void setGuildMasterPerms(UUID masterUUID) {
        for (String permission : GUILD_MASTER_PERMS) setPermission(permission, masterUUID);
    }

}
