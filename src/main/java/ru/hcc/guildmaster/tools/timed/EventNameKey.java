package ru.hcc.guildmaster.tools.timed;

public enum EventNameKey {

    EMPTY_KEY(0),
    PLAYER_JOIN_GUILD(1),
    PLAYER_CALL_REQUEST_TO_JOIN_GUILD(2),
    PLAYER_LEAVE_GUILD(3),
    GUILD_MASTER_LEAVE_GUILD(4),
    PLAYER_KICK_FROM_GUILD(5),
    GUILD_CHANGE_MASTER(6),
    GUILD_CHANGE_NAME(7),
    GUILD_CHANGE_COUNT_MEMBERS(8),
    GUILD_DELETE(9),
    OPEN_GUILD_EDITOR_MENU(10);

    private final int id;

    EventNameKey(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static EventNameKey getById(int id) {
        for (EventNameKey value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        throw new IllegalArgumentException("No enum constant with id " + id);
    }
}
