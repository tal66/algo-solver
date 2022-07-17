package events;

public class EventData {
    public static final String STOP_MESSAGE = "BYE";
    private Event event;
    private String data;

    public EventData(Event event, String data) {
        this.event = event;
        this.data = data;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
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