package monitor;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import events.EventData;
import events.EventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StatsMonitor implements EventSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(StatsMonitor.class);
    private static final Gson gson = new Gson();

    private final SocketAddress clientsocketAddress;
    private final SocketAddress socketAddress;
    private final HashMap<String, UsageStats> statsByAlgo = new HashMap<>();
    private final BlockingQueue<TaskStats> queue = new LinkedBlockingQueue<>();
    private DatagramSocket socket = null;

    static class UsageStats {
        String algo;
        double avgTime;
        long maxTime;
        long minTime;
        int timesUsed;

        public UsageStats(String algo, double avgTime) {
            this.algo = algo;
            this.avgTime = avgTime;
            this.timesUsed = 1;
            this.maxTime = (long) avgTime;
            this.minTime = this.maxTime;
        }
    }

    public StatsMonitor(int port, int clientPort) {
        socketAddress = new InetSocketAddress("localhost", port);
        clientsocketAddress = new InetSocketAddress("localhost", clientPort);
    }

    public void start() throws InterruptedException {
        logger.info("started");

        boolean success = startSocket();
        if (!success){
            return;
        }

        while (true){
            TaskStats taskStats = queue.take();
            String algo = taskStats.getAlgorithm();
            long time_ms = taskStats.getTime_ms();

            UsageStats usageStats = statsByAlgo.get(algo);
            if (usageStats == null){
                usageStats = new UsageStats(algo, time_ms);
                statsByAlgo.put(algo, usageStats);
            } else {
                usageStats.timesUsed++;
                double avg = usageStats.avgTime;
                avg += (time_ms - avg) / usageStats.timesUsed;
                usageStats.avgTime = Math.round(avg * 100.0) / 100.0;
            }

            if (time_ms > usageStats.maxTime){
                usageStats.maxTime = time_ms;
            } else if (time_ms < usageStats.minTime){
                usageStats.minTime = time_ms;
            }

            sendData(gson.toJson(usageStats, UsageStats.class));
        }
    }

    public boolean startSocket(){
        try {
            socket = new DatagramSocket(socketAddress);
            return true;
        } catch (Exception e) {
            logger.error("can't bind to monitor socket ({})", e.getMessage());
            return false;
        }
    }

    private void sendData(String data){
        try {
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, clientsocketAddress);
            socket.send(packet);
        } catch (IOException e) {
            logger.error("", e);
        }
    }

    @Override
    public void accept(EventData eventData) {
        try{
            TaskStats taskStats = gson.fromJson(eventData.getData(), TaskStats.class);
            queue.add(taskStats);
        } catch (JsonSyntaxException e){
            logger.error("parse error {}", eventData);
            return;
        }
    }
}
