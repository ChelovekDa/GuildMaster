package ru.hcc.guildmaster.tools.timed;

import org.jetbrains.annotations.NotNull;

public enum EventNameKey {

    EMPTY_KEY(0, "Nothing"),
    PLAYER_JOIN_GUILD(1, "Игрок присоединился к гильдии"),
    PLAYER_CALL_REQUEST_TO_JOIN_GUILD(2, "Nothing"),
    PLAYER_LEAVE_GUILD(3, "Игрок покинул гильдию"),
    GUILD_MASTER_LEAVE_GUILD(4, "Глава гильдии покинул гильдию"),
    PLAYER_KICK_FROM_GUILD(5, "Игрок был выгнан из гильдии"),
    GUILD_CHANGE_MASTER(6, "В гильдии поменялся глава"),
    GUILD_CHANGE_NAME(7, "В гильдии изменилось название"),
    GUILD_CHANGE_COUNT_MEMBERS(8, "В гильдии изменилось максимальное количество участников"),
    GUILD_DELETE(9, "Гильдия была удалена"),
    OPEN_GUILD_EDITOR_MENU(10, "Open guild editor menu (Not sending to admins)"),
    ATTEMPT_TO_RELOAD(11, "Перезагрузка плагина"),
    GUILD_ADD_PERMISSION(12, "Добавление уникального разрешения в гильдию.");

    private final int id;
    private final String message;

    EventNameKey(final int id, final String message) {
        this.id = id;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public int getId() {
        return id;
    }

    public static EventNameKey getByMessage(@NotNull String message) {
        for (EventNameKey value : values()) if (value.message.equals(message)) return value;
        throw new IllegalArgumentException("No enum constant with message " + message);
    }

    public static EventNameKey getById(int id) {
        for (EventNameKey value : values()) if (value.id == id) return value;
        throw new IllegalArgumentException("No enum constant with id " + id);
    }
}
