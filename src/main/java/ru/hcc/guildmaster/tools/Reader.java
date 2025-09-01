package ru.hcc.guildmaster.tools;

import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Reader extends PlayerTrackingLogger {

    private static final String GUILDS_FILE_NAME = "guilds.json";
    private static final String README_FILE = "README.txt";

    private static final String[] README_TEXT = new String[] {"Уважаемый Администратор! Большая просьба не трогать ни единый файл в этом месте, кроме конфига, иначе всё нахер сломается!!!"};

    public Reader() {
        //readmeCheck();
    }

    /**
     * This method needs to checking readme.txt file
     * @see #README_TEXT
     * @see #README_FILE
     */
    private static void readmeCheck() {
        try {
            String path = getPath();
            if (!Files.exists(Path.of(path))) {
                new File(path).mkdirs();
                new File("%s/%s".formatted(path, README_FILE)).createNewFile();

                FileWriter writer = new FileWriter("%s/%s".formatted(path, README_FILE), false);
                for (String line : README_TEXT) writer.write("%s\n".formatted(line));

                writer.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    /**
     * This method needs to get all guild names.
     */
    @NotNull
    public ArrayList<String> getGuildNames() {
        var guilds = getGuilds();
        if (guilds.isEmpty()) return new ArrayList<>();

        ArrayList<String> result = new ArrayList<>();
        for (String guildID : guilds.keySet()) result.add(guilds.get(guildID).id);

        return result;
    }

    public void deleteGuild(@NotNull Guild guild) {
        String path = "%s/%s".formatted(getPath(), GUILDS_FILE_NAME);

        HashMap<String, Guild> guildsHashMap = getGuilds();

        if (!guildsHashMap.isEmpty()) {
            for (String guildId : guildsHashMap.keySet()) {
                if (guildId.equals(guild.id)) guildsHashMap.remove(guildId);
            }
        }
        else return;

        try {
            Map<String, Map<String, Object>> map = new HashMap<>();
            for (String guildID : guildsHashMap.keySet()) map.put(guildID, guildsHashMap.get(guildID).toMap());
            Files.write(Paths.get(path), new JSONObject(map).toJSONString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull String getTrackDataDir(@NotNull UUID uuid) {
        return super.getTrackDataDir(uuid);
    }

    /**
     * This method needs to write changed guild's object
     * @param guild changed guild object
     * @see #getGuilds()
     * @see #deleteGuild(Guild)
     */
    public void writeGuild(@NotNull Guild guild) {
        String path = "%s/%s".formatted(getPath(), GUILDS_FILE_NAME);

        HashMap<String, Guild> guildsHashMap = getGuilds();

        if (!guildsHashMap.isEmpty()) {
            HashMap<String, Guild> copied = (HashMap<String, Guild>) guildsHashMap.clone();
            for (String guildId : copied.keySet()) {
                if (guildId.equals(guild.id)) {
                    guildsHashMap.remove(guildId);
                    guildsHashMap.put(guildId, guild);
                }
            }
            if (!guildsHashMap.containsKey(guild.id)) guildsHashMap.put(guild.id, guild);
        }
        else {
            guildsHashMap = new HashMap<>();
            guildsHashMap.put(guild.id, guild);
        }

        try {
            Map<String, Map<String, Object>> map = new HashMap<>();
            for (String guildID : guildsHashMap.keySet()) map.put(guildID, guildsHashMap.get(guildID).toMap());
            Files.write(Paths.get(path), new JSONObject(map).toJSONString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method needs to get all guilds from file
     * @return HashMap with all guilds, where key - guild id
     * @see #writeGuild(Guild)
     * @see #GUILDS_FILE_NAME
     */
    @NotNull
    public HashMap<String, Guild> getGuilds() {
        JSONObject jsonObject = readFile(GUILDS_FILE_NAME);

        HashMap<String, Guild> map = new HashMap<>();

        if (jsonObject != null) {
            for (Object guildKey : jsonObject.keySet()) {
                JSONObject guild_object = (JSONObject) jsonObject.get(guildKey);
                Guild guild = new Guild("", null, "");

                for (Object key : guild_object.keySet()) {

                    if (Objects.equals(key, "id"))
                        guild.id = String.valueOf(guild_object.get(key));

                    else if (Objects.equals(key, "displayName"))
                        guild.displayName = String.valueOf(guild_object.get(key));

                    else if (Objects.equals(key, "maxMembersCount"))
                        guild.maxMembersCount = ((Number) guild_object.get(key)).byteValue();

                    else if (Objects.equals(key, "guildMasterUUID"))
                        guild.setGuildMasterUUID(String.valueOf(guild_object.get(key)));

                    else if (Objects.equals(key, "membersUUID")) {
                        JSONArray array = (JSONArray) guild_object.get(key);
                        if (!array.isEmpty()) {
                            HashSet<String> uuids = new HashSet<>();
                            for (Object value : array) uuids.add(String.valueOf(value));
                            guild.membersUUID = uuids;
                        }
                        else guild.membersUUID = new HashSet<>();
                    }

                    else if (Objects.equals(key, "permissions")) {
                        JSONArray array = (JSONArray) guild_object.get(key);
                        if (!array.isEmpty()) for (Object value : array) guild.addPermission(String.valueOf(value));
                    }
                }

                map.put(guild.id, guild);
            }
        }

        return map;
    }

}
