package ru.hcc.guildmaster.tools.timed;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.hcc.guildmaster.tools.Reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class Search {
    /**
     * This class needs to search definite TimedMessages.
     * This class search objects on gave search-args in all TimedMessages.
     *
     * By default, this search-args it's EventNameKey or EventStatusKey values, but you can give additional values if you want to use advanced search.
     * For example, it's using for search definite TimedMessages related to the requests to join the guild.
     *
     * @see EventNameKey
     * @see EventStatusKey
     * @see TimedMessage
     */
    private EventNameKey eventNameKey;
    private EventStatusKey eventStatusKey;
    private HashMap<String, Object> additionalValues;

    public Search(EventNameKey eventNameKey, EventStatusKey eventStatusKey, HashMap<String, Object> additionalValues) {
        this.eventNameKey = eventNameKey;
        this.eventStatusKey = eventStatusKey;
        this.additionalValues = additionalValues;
    }

    public Search(@Nullable EventNameKey eventNameKey, @Nullable EventStatusKey eventStatusKey) {
        this.eventNameKey = eventNameKey;
        this.eventStatusKey = eventStatusKey;
        this.additionalValues = new HashMap<>();
    }

    @NotNull
    public ArrayList<TimedMessage> search() {
        ArrayList<TimedMessage> result = new ArrayList<>();

        ArrayList<TimedMessage> allMessages = new Reader().getTimedMessages();

        if (this.eventNameKey == null && this.eventStatusKey == null) {
            for (TimedMessage message : allMessages) {
                if (message.eventStatusKey.equals(EventStatusKey.WAITING)) result.add(message);
            }
        }
        else if (!allMessages.isEmpty()) {
            for (TimedMessage mes : allMessages) {
                if (Objects.equals(this.eventNameKey, mes.eventNameKey) && Objects.equals(this.eventStatusKey, mes.eventStatusKey)) {
                    byte res = 1;
                    for (String key : this.additionalValues.keySet()) {
                        if (mes.customValues.containsKey(key) && !Objects.equals(mes.customValues.get(key), additionalValues.get(key))) {
                            res--;
                            break;
                        }
                    }
                    if (res != 0) result.add(mes);
                }
            }
        }
        return result;
    }

}
