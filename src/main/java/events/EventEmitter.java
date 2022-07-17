package events;

public interface EventEmitter {
    void emit(Event event, String data);
}
