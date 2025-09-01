package ru.hcc.guildmaster.tools;

import org.bukkit.OfflinePlayer;
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
    private final HashSet<String> guildPermissions;

    public Guild(String id, @Nullable HashSet<String> membersUUID, @Nullable String guildMasterUUID) {
        this.displayName = id;
        this.id = id;
        this.maxMembersCount = Byte.MAX_VALUE;

        if (membersUUID == null) this.membersUUID = new HashSet<>();
        else this.membersUUID = membersUUID;

        if (guildMasterUUID != null && !Objects.equals(guildMasterUUID.replaceAll(" ", ""), "")) this.membersUUID.add(guildMasterUUID);
        this.guildMasterUUID = "";
        this.setGuildMasterUUID(guildMasterUUID);
        this.guildPermissions = new HashSet<>();
    }

    public boolean contains(@NotNull String uuid) {
        if (!this.membersUUID.contains(uuid)) return false;
        for (String memberUUID : this.membersUUID) if (memberUUID.equals(uuid)) return true;
        return false;
    }

    public void addPermission(String permission) {
        this.guildPermissions.add(permission);
    }

    public void setPerms(UUID uuid) {
        if (this.guildPermissions.isEmpty()) return;
        for (String perm : this.guildPermissions) setPermission(perm, uuid);
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
        if (!this.guildMasterUUID.isEmpty()) removeGuildMasterPerms(UUID.fromString(this.guildMasterUUID));

        if (guildMasterUUID == null || guildMasterUUID.isEmpty()) this.guildMasterUUID = "";
        else {
            if (this.membersUUID.size() < this.maxMembersCount || this.membersUUID.contains(guildMasterUUID)) {
                this.guildMasterUUID = guildMasterUUID;
                this.membersUUID.add(this.guildMasterUUID);

                setGuildMasterPerms(UUID.fromString(this.guildMasterUUID));
            }
        }
        return this;
    }

    private void removePlayer(@NotNull UUID uuid) {
        if (this.membersUUID.contains(uuid.toString())) {
            this.membersUUID.remove(uuid.toString());

            if (uuid.toString().equals(this.guildMasterUUID)) this.setGuildMasterUUID(null);

            for (String permission : this.guildPermissions) unSetPermission(permission, uuid);
            new Reader().writeGuild(this);
        }
    }

    public void kickPlayer(@NotNull Player player) {
        this.removePlayer(player.getUniqueId());
    }

    public void kickPlayer(@NotNull UUID uuid) {
        this.removePlayer(uuid);
    }

    @NotNull
    public String getGuildMasterName() {
        if (!Objects.equals(this.guildMasterUUID, "")) return getPlayerName(this.guildMasterUUID);
        else return "&cГлава гильдии не назначен!";
    }

    @NotNull
    public String getInfo() {
        return "&6Информация о гильдии:\n&6- Название: %s&6\n&6- Айди: %s\n&6- Глава гильдии: %s\n&6- Количество участников: %s".formatted(this.displayName, this.id, this.getGuildMasterName(), this.membersUUID.size());
    }

    public void addMember(OfflinePlayer player) {
        this.membersUUID.add(player.getUniqueId().toString());
        new Reader().writeGuild(this);
    }

    @NotNull
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("id", this.id);
        map.put("displayName", this.displayName);
        map.put("maxMembersCount", this.maxMembersCount);
        map.put("guildMasterUUID", this.guildMasterUUID);
        map.put("membersUUID", new ArrayList<>(this.membersUUID));
        map.put("permissions", new ArrayList<>(this.guildPermissions));

        return map;
    }
}
