package ru.hcc.guildmaster.tools;

import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
     * This method needs to write all changed guild's objects
     * @param guilds changed guild's objects
     * @see #writeGuild(Guild)
     * @see #getGuilds()
     */
    public void writeGuilds(@NotNull Guild[] guilds) {
        for (Guild value : guilds) writeGuild(value);
    }

    /**
     * This method needs to get all guild names.
     */
    @Nullable
    public ArrayList<String> getGuildNames() {
        var guilds = getGuilds();
        if (guilds == null) return null;

        ArrayList<String> result = new ArrayList<>();
        for (String guildID : guilds.keySet()) result.add(guilds.get(guildID).id);

        return result;
    }

    public void deleteGuild(@NotNull Guild guild) {
        String path = "%s/%s".formatted(getPath(), GUILDS_FILE_NAME);

        HashMap<String, Guild> guildsHashMap = getGuilds();

        if (guildsHashMap != null) {
            for (String guildId : ((HashMap<String, Guild>) guildsHashMap.clone()).keySet()) {
                if (guildId.equals(guild.id)) guildsHashMap.remove(guildId);
            }
        }
        else return;

        try {
            for (Guild g : guildsHashMap.values()) Files.write(Paths.get(path), g.toJson().toJSONString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method needs to write changed guild's object
     * @param guild changed guild object
     * @see #getGuilds()
     * @see #writeGuilds(Guild[])
     */
    private void writeGuild(@NotNull Guild guild) {
        String path = "%s/%s".formatted(getPath(), GUILDS_FILE_NAME);

        HashMap<String, Guild> guildsHashMap = getGuilds();

        if (guildsHashMap != null) {
            for (String guildId : ((HashMap<String, Guild>) guildsHashMap.clone()).keySet()) {
                if (guildId.equals(guild.id)) {
                    guildsHashMap.remove(guildId);
                    guildsHashMap.put(guildId, guild);
                }
            }
        }
        else {
            guildsHashMap = new HashMap<>();
            guildsHashMap.put(guild.id, guild);
        }

        try {
            for (Guild g : guildsHashMap.values()) Files.write(Paths.get(path), g.toJson().toJSONString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method needs to get all guilds from file
     * @return HashMap with all guilds, where key - guild id
     * @see #writeGuilds(Guild[])
     * @see #writeGuild(Guild)
     * @see #GUILDS_FILE_NAME
     */
    @Nullable
    public HashMap<String, Guild> getGuilds() {
        JSONObject jsonObject = readFile(GUILDS_FILE_NAME);

        HashMap<String, Guild> map = new HashMap<>();

        if (jsonObject != null) {
            for (Object guildKey : jsonObject.keySet()) {
                JSONObject guild_object = (JSONObject) jsonObject.get(guildKey);
                Guild guild = new Guild("", "", null, "");

                for (Object key : guild_object.keySet()) {

                    if (Objects.equals(key, "id"))
                        guild.id = String.valueOf(guild_object.get(key));

                    else if (Objects.equals(key, "displayName"))
                        guild.displayName = String.valueOf(guild_object.get(key));

                    else if (Objects.equals(key, "maxMembersCount"))
                        guild.maxMembersCount = ((Number) guild_object.get(key)).byteValue();

                    else if (Objects.equals(key, "guildMasterUUID"))
                        guild.guildMasterUUID = String.valueOf(guild_object.get(key));

                    else if (Objects.equals(key, "membersUUID")) {
                        JSONArray array = (JSONArray) guild_object.get(key);
                        if (!array.isEmpty()) {
                            ArrayList<String> uuids = new ArrayList<>();
                            for (int i = 0; i < array.size(); i++) uuids.add(i, String.valueOf(array.get(i)));
                            guild.membersUUID = uuids;
                        }
                        else guild.membersUUID = new ArrayList<>();
                    }
                }

                map.put(guild.id, guild);
            }
        }
        else map = null;

        return map;
    }

}
