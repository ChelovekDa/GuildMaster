package ru.hcc.guildmaster.tools;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hcc.guildmaster.tools.timed.EventNameKey;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.timed.Search;

import java.util.*;

public class Guild extends PermissionTools {

    public String id;
    public String displayName;
    public byte maxMembersCount;
    public HashSet<String> membersUUID;
    private String guildMasterUUID;
    private ArrayList<String> guildPermissions;

    public Guild(String id, @Nullable HashSet<String> membersUUID, String guildMasterUUID) {
        this.displayName = id;
        this.id = id;
        this.maxMembersCount = Byte.MAX_VALUE;

        if (membersUUID == null) this.membersUUID = new HashSet<>();
        else this.membersUUID = membersUUID;

        if (!Objects.equals(guildMasterUUID.replaceAll(" ", ""), "")) this.membersUUID.add(guildMasterUUID);
        this.guildMasterUUID = guildMasterUUID;
        this.guildPermissions = new ArrayList<>();
    }

    public boolean contains(@NotNull String uuid) {
        if (!this.membersUUID.contains(uuid)) return false;
        for (String memberUUID : this.membersUUID) if (memberUUID.equals(uuid)) return true;
        return false;
    }

    @NotNull
    public Guild addPermission(String permission) {
        if (!guildPermissions.contains(permission)) guildPermissions.add(permission);
        return this;
    }

    @NotNull
    public String getGuildMasterUUID() {
        if (this.guildMasterUUID != null && !this.guildMasterUUID.isEmpty()) return this.guildMasterUUID;
        else return "";
    }

    @NotNull
    private HashMap<String, Object> getThisAdditionalValues() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("guild", this.id);
        return map;
    }

    @NotNull
    public Search getSearchObject() {
        return new Search(EventNameKey.PLAYER_CALL_REQUEST_TO_JOIN_GUILD, EventStatusKey.WAITING, getThisAdditionalValues());
    }

    @NotNull
    public String getSuccessMemberJoinMessage() {
        return "&aПоздравляем! Вас успешно приняли в гильдию &a&o%s&a.".formatted(this.displayName);
    }

    @NotNull
    public String getCancelMemberJoinMessage() {
        return "&cУвы, но вам отказали во вступлении в гильдию &c&o%s&c.".formatted(this.displayName);
    }

    @NotNull
    public Guild setGuildMasterUUID(@Nullable String guildMasterUUID) {
        if (!this.guildMasterUUID.isEmpty()) {
            Player master = getPlayer(UUID.fromString(this.guildMasterUUID));
            assert master != null;
            removeGuildMasterPerms(master);
        }

        if (guildMasterUUID == null || guildMasterUUID.isEmpty()) this.guildMasterUUID = "";
        else {
            if (this.membersUUID.size() < this.maxMembersCount || this.membersUUID.contains(guildMasterUUID)) {
                this.guildMasterUUID = guildMasterUUID;
                Player master = getPlayer(UUID.fromString(this.guildMasterUUID));
                if (master != null) setGuildMasterPerms(master);
                else this.guildMasterUUID = "";
            }
        }
        return this;
    }

    private void removePlayer(@NotNull Player player) {
        if (this.membersUUID.contains(player.getUniqueId().toString())) {
            this.membersUUID.remove(player.getUniqueId().toString());

            if (player.getUniqueId().toString().equals(this.guildMasterUUID)) this.setGuildMasterUUID(null);

            for (String permission : this.guildPermissions) unSetPermission(permission, player);
            new Reader().writeGuild(this);
        }
    }

    public void kickPlayer(@NotNull Player player) {
        this.removePlayer(player);
    }

    @NotNull
    public String getGuildMasterName() {
        if (!Objects.equals(this.guildMasterUUID, "")) {
            Player player = getPlayer(UUID.fromString(this.guildMasterUUID));
            assert player != null;
            return player.getName();
        }
        else return "&cГлава гильдии не назначен!";
    }

    @NotNull
    public Guild addMember(Player player) {
        this.membersUUID.add(player.getUniqueId().toString());
        new Reader().writeGuild(this);

        for (String permission : this.guildPermissions) setPermission(permission, player);

        return this;
    }

    @NotNull
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("id", this.id);
        map.put("displayName", this.displayName);
        map.put("maxMembersCount", this.maxMembersCount);
        map.put("guildMasterUUID", this.guildMasterUUID);
        map.put("membersUUID", new ArrayList<>(this.membersUUID));

        return map;
    }
}
