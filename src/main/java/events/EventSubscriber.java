package events;

public interface EventSubscriber {
    void handle(Event event, String data);
}
