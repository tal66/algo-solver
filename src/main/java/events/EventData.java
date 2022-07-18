package events;

import lombok.Getter;

@Getter
public class EventData {
    public static final String STOP_MESSAGE = "BYE";
    private Event event;
    private String data;

    public EventData(Event event, String data) {
        this.event = event;
        this.data = data;
    }

    @Override
    public String toString() {
        return "EventData{" +
                "event=" + event +
                ", data='" + data + '\'' +
                '}';
    }
}
