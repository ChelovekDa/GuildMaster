package ru.hcc.guildmaster.tools;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import ru.hcc.guildmaster.tools.timed.EventNameKey;
import ru.hcc.guildmaster.tools.timed.EventStatusKey;
import ru.hcc.guildmaster.tools.timed.TimedMessage;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;

class EventLogger extends ToolMethods {

    private static final String TIMED_MESSAGES_FILE = "timed_messages.json";

    /**
     * This method needs to read and get object of JSONObject guild's file.
     * JSONObject - object of HashMap<Object, Object>, where first object of class Object - key (guild's id with string format) and value (second) - JSONObject.
     *
     * @param fileName name of file
     * @return JSONObject
     */
    @Nullable
    protected JSONObject readFile(@NotNull String fileName) {
        try {
            String path = getPath();
            if (!Files.exists(Path.of(path))) {
                new File(path).mkdirs();
                new File((path + "/" + fileName)).createNewFile();
            }
            return (JSONObject) new JSONParser().parse(new FileReader((path + "/" + fileName)));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * This method needs to save logging or other messages.
     * It can be useful for noticing guild's leader or administrators.
     *
     * @param message message which needs to save
     * @see #getTimedMessages()
     */
    public void saveTimedMessage(@NotNull TimedMessage message) {
        JSONObject jsonObject = new JSONObject();

        ArrayList<TimedMessage> messages = getTimedMessages();

        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).date.equals(message.date)) messages.remove(i);
        }
        messages.add(message);

        for (TimedMessage mes : messages) jsonObject.put(mes.date, mes.toJson());

        String path = "%s/%s".formatted(getPath(), TIMED_MESSAGES_FILE);
        try {
            Files.write(Paths.get(path), jsonObject.toJSONString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method needs to save a lot of timed messages for logging.
     * @param messages all messages which need to save
     * @see #saveTimedMessage(TimedMessage)
     * @see #getTimedMessages()
     */
    public void saveTimedMessages(TimedMessage[] messages) {
        for (TimedMessage message : messages) this.saveTimedMessage(message);
    }


    /**
     * This method needs to get all timed messages.
     * It can be useful for notice guild's leader or administrators about some news.
     *
     * @return ArrayList with all messages (TimedMessage objects)
     * @see #TIMED_MESSAGES_FILE
     * @see #saveTimedMessage(TimedMessage)
     * @see #saveTimedMessages(TimedMessage[]) 
     */
    @NotNull
    public ArrayList<TimedMessage> getTimedMessages() {
        JSONObject jsonObject = readFile(TIMED_MESSAGES_FILE);
        ArrayList<TimedMessage> messages = new ArrayList<>();
        if (jsonObject == null) return messages;

        for (Object dateKey : jsonObject.keySet()) {
            JSONObject record = (JSONObject) jsonObject.get(dateKey);

            TimedMessage message = new TimedMessage();
            HashMap<String, Object> map = new HashMap<>();

            for (Object key : record.keySet()) {

                if (Objects.equals(key, "date"))
                    message.date = String.valueOf(record.get(key));

                else if (Objects.equals(key, "event"))
                    message.eventNameKey = EventNameKey.getById(((Number) record.get(key)).intValue());

                else if (Objects.equals(key, "status"))
                    message.eventStatusKey = EventStatusKey.getById(((Number) record.get(key)).byteValue());

                else if (Objects.equals(key, "message"))
                    message.message = String.valueOf(record.get(key));

                else {
                    try {
                        map.put(String.valueOf(key), record.get(key));
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.WARNING, "%s".formatted(e.getMessage()));
                    }
                }
            }

            message.customValues = map;

            messages.add(message);

        }

        return messages;
    }

}
