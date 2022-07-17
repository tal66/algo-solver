package events;

public interface EventSubscriber {
    void accept(EventData eventData);
}
