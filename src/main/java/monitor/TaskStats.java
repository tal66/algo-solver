package monitor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class TaskStats {
    private String algorithm;
    private long time_ms;
}
