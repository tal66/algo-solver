package monitor;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import events.EventData;
import events.EventSubscriber;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StatsMonitor implements EventSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(StatsMonitor.class);
    private static final Gson gson = new Gson();
    private static final int port = 8010;

    private final HashMap<String, AvgStats> avgStatsByAlgo = new HashMap<>();
    private final BlockingQueue<TaskStats> queue = new LinkedBlockingQueue<>();
    private Socket socket = null;

    @ToString
    class AvgStats{
        String algo;
        double avgTime;
        long maxTime;
        long minTime;
        int timesUsed;

        public AvgStats(String algo, double avgTime) {
            this.algo = algo;
            this.avgTime = avgTime;
            this.timesUsed = 1;
            this.maxTime = (long) avgTime;
            this.minTime = this.maxTime;
        }
    }

    public void start() throws InterruptedException {
        logger.info("started");
        startSocket();

        while (true){
            TaskStats taskStats = queue.take();
            String algo = taskStats.getAlgorithm();
            long time_ms = taskStats.getTime_ms();

            AvgStats avgStats = avgStatsByAlgo.get(algo);
            if (avgStats == null){
                avgStats = new AvgStats(algo, time_ms);
                avgStatsByAlgo.put(algo, avgStats);
            } else {
                avgStats.timesUsed++;
                double avg = avgStats.avgTime;
                avg += (time_ms - avg) / avgStats.timesUsed;
                avgStats.avgTime = Math.round(avg * 100.0) / 100.0;
            }

            if (time_ms > avgStats.maxTime){
                avgStats.maxTime = time_ms;
            } else if (time_ms < avgStats.minTime){
                avgStats.minTime = time_ms;
            }

            sendData(gson.toJson(avgStats, AvgStats.class));
        }
    }

    public boolean startSocket(){
        try {
            socket = new Socket("localhost", port);
            return true;
        } catch (Exception e) {
            logger.error("can't connect to monitor server ({})", e.getMessage());
            return false;
        }
    }

    private void sendData(String data){
        try {
            if (socket == null || !socket.isConnected()){
                boolean success = startSocket();
                if (!success){
                    return;
                }
            }
            var out = new PrintWriter(socket.getOutputStream(), true);
            out.println(data);
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    @Override
    public void accept(EventData eventData) {
        TaskStats taskStats = null;
        try{
            taskStats = gson.fromJson(eventData.getData(), TaskStats.class);
        } catch (JsonSyntaxException e){
            logger.error("parse error {}", eventData);
            return;
        }

        queue.add(taskStats);
    }
}
