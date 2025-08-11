package ru.hcc.guildmaster.tools;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import ru.hcc.guildmaster.tools.timed.EventNameKey;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.timed.Search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Guild {

    public String id;
    public String displayName;
    public byte maxMembersCount;
    public ArrayList<String> membersUUID;
    public String guildMasterUUID;
    public ArrayList<String> guildPermissions;

    public Guild(String displayName, String id, ArrayList<String> membersUUID, String guildMasterUUID) {
        this.displayName = displayName;
        this.id = id;
        this.maxMembersCount = Byte.MAX_VALUE;
        this.membersUUID = membersUUID;
        this.guildMasterUUID = guildMasterUUID;
        this.guildPermissions = new ArrayList<>();
    }

    public Guild addPermission(String permission) {
        if (!guildPermissions.contains(permission)) guildPermissions.add(permission);
        return this;
    }

    public Guild(String id, ArrayList<String> membersUUID, String guildMasterUUID) {
        this.displayName = id;
        this.id = id;
        this.maxMembersCount = Byte.MAX_VALUE;

        if (membersUUID == null) this.membersUUID = new ArrayList<>();
        else this.membersUUID = membersUUID;

        if (!Objects.equals(guildMasterUUID.replaceAll(" ", ""), "")) this.membersUUID.add(guildMasterUUID);
        this.guildMasterUUID = guildMasterUUID;
        this.guildPermissions = new ArrayList<>();
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

    public String getSuccessMemberJoinMessage() {
        return "&aПоздравляем! Вас успешно приняли в гильдию &a&o%s&a.".formatted(this.displayName);
    }

    public String getCancelMemberJoinMessage() {
        return "&cУвы, но вам отказали во вступлении в гильдию &c&o%s&c.".formatted(this.displayName);
    }

    public void addMember(Player player) {
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
        map.put("membersUUID", this.membersUUID);

        return map;
    }

    @Deprecated
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("id", this.id);
        jsonObject.put("displayName", this.displayName);
        jsonObject.put("maxMembersCount", this.maxMembersCount);
        jsonObject.put("guildMasterUUID", this.guildMasterUUID);

        JSONArray array = new JSONArray();
        for (String uuid : this.membersUUID) array.add(uuid);

        jsonObject.put("membersUUID", array);

        return jsonObject;
    }
}
