package ru.hcc.guildmaster.tools.timed;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;

import java.util.Date;
import java.util.HashMap;

public class TimedMessage {

    public String date;
    public EventNameKey eventNameKey;
    public EventStatusKey eventStatusKey;
    public HashMap<String, Object> customValues;
    public String message;

    public TimedMessage(@NotNull EventNameKey eventNameKey, @NotNull EventStatusKey eventStatusKey, @NotNull String message, @Nullable HashMap<String, Object> customValues) {
        this.date = new Date().toString();
        this.eventNameKey = eventNameKey;
        this.eventStatusKey = eventStatusKey;
        this.message = message;

        if (customValues == null) this.customValues = new HashMap<>();
        else this.customValues = customValues;
    }

    public TimedMessage setStatus(EventStatusKey statusKey) {
        this.eventStatusKey = statusKey;
        return this;
    }

    public TimedMessage() {
        this.date = new Date().toString();
        this.eventNameKey = EventNameKey.EMPTY_KEY;
        this.eventStatusKey = EventStatusKey.NOTHING;
        this.customValues = new HashMap<>();
        this.message = "Hello!";
    }

    public Search getSearchInstance() {
        return new Search(this.eventNameKey, this.eventStatusKey, this.customValues);
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("date", this.date);
        jsonObject.put("event", this.eventNameKey.getId());
        jsonObject.put("status", this.eventStatusKey.getId());
        jsonObject.put("message", this.message);

        for (String key : customValues.keySet()) jsonObject.put(key, customValues.get(key));

        return jsonObject;
    }

}
