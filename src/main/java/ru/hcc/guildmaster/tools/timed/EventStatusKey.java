package ru.hcc.guildmaster.tools.timed;

public enum EventStatusKey {

    NOTHING((byte) 0),
    READ((byte) 1),
    WAITING((byte) 2);

    private final byte id;

    EventStatusKey(final byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static EventStatusKey getById(byte id) {
        for (EventStatusKey value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        throw new IllegalArgumentException("No enum constant with id " + id);
    }

}
