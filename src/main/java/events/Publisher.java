package events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Publisher {
    private static final Logger logger = LoggerFactory.getLogger(Publisher.class);
    private final HashMap<Event, List<EventSubscriber>> listenersByEvent;

    public Publisher() {
        this.listenersByEvent = new HashMap<>();
        for (Event event: Event.values()){
            this.listenersByEvent.put(event, new ArrayList<>());
        }
    }

    public void subscribe(Event event, EventSubscriber listener) {
        List<EventSubscriber> listeners = listenersByEvent.get(event);
        if (listeners == null){
            logger.error("unknown event {}", event);
            return;
        }
        listeners.add(listener);
    }

    public void unsubscribe(Event event, EventSubscriber listener) {
        List<EventSubscriber> listeners = listenersByEvent.get(event);
        if (listeners == null){
            logger.error("unknown event {}", event);
            return;
        }
        listeners.remove(listener);
    }

    public void notifyListeners(Event event, String data) {
        List<EventSubscriber> listeners = listenersByEvent.get(event);
        if (listeners == null){
            logger.error("unknown event {}", event);
            return;
        }
        for (EventSubscriber listener : listeners) {
            listener.handle(event, data);
        }
    }
}
