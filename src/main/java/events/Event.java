package events;

public enum Event {
    UNKNOWN(0),
    FILE_CREATED(1),
    FILE_CHANGED(2);

    private final int value;

    Event(int i) {
        this.value = i;
    }

    public int getValue() { return value; }
}
