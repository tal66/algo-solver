package app;

import algo.AlgoSolver;
import algo.Knapsack;
import algo.SequenceAlignment;
import events.Event;
import events.EventData;
import events.EventSubscriber;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class AlgoInputHandler implements EventSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(AlgoInputHandler.class);

    private static final int FILE_SIZE_LIMIT = 10*1024;
    private static final HashMap<String, Supplier<AlgoSolver>> algorithmSolvers = new HashMap<>(){{
        put("knapsack", Knapsack::new);
        put("sequencealignment", SequenceAlignment::new);
    }};

    private static final int QUEUE_LIMIT = 50;
    private static final int TASK_TIMEOUT = 15*1000;

    private final BlockingQueue<EventData> eventQueue = new LinkedBlockingQueue<>(QUEUE_LIMIT);
    private final BlockingQueue<TaskInfo> futuresQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<Runnable> algoExecutorInnerQueue = new LinkedBlockingQueue<>(QUEUE_LIMIT);
    private final ExecutorService algoExecutorService = new ThreadPoolExecutor(
            3, 3, 0L, TimeUnit.MILLISECONDS, algoExecutorInnerQueue);
    private boolean STOPPED = false;

    @Getter
    @AllArgsConstructor
    class TaskInfo{
        Future<?> future;
        EventData eventData;
    }

    public AlgoInputHandler() {
    }

    public void start() throws InterruptedException {
        logger.info("started");

        startCancellationService();

        while (true){
            EventData eventData = null;

            eventData = eventQueue.take();
            long startTime = System.currentTimeMillis();
            String filePath = eventData.getData();
            Event event = eventData.getEvent();

            if (Path.of(filePath).getFileName().toString().equals(EventData.STOP_MESSAGE)){
                logger.warn("stopping service (reason: STOP_MESSAGE)");
                STOPPED = true;
                futuresQueue.add(new TaskInfo(null, eventData));
                break;
            }

            Future<?> future = algoExecutorService.submit(() -> {
                handleEvent(filePath, event);
                logger.info("{} ms (receive event->done). Task <{} {}>)", System.currentTimeMillis() - startTime, filePath, event);
            });
            futuresQueue.add(new TaskInfo(future, eventData));
        }
    }

    private void startCancellationService() {
        // get() blocks, so another thread. maybe there's a better way to cancel
        new Thread(()-> {
            while (true) {
                TaskInfo taskInfo = null;
                try {
                    taskInfo = futuresQueue.take();
                } catch (InterruptedException e) {
                    logger.error("stopping task cancellation service (reason: interrupted)", e);
                    break;
                }

                EventData eventData = taskInfo.eventData;
                String filename = Path.of(eventData.getData()).getFileName().toString();
                if (filename.equals(EventData.STOP_MESSAGE)){
                    logger.warn("stopping task cancellation service (reason: STOP_MESSAGE)");
                    break;
                }
                try {
                    taskInfo.future.get(TASK_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | TimeoutException e) {
                    taskInfo.future.cancel(true); // *Attempts* to cancel
                    logger.warn("canceling task for [{} {}] (reason: timeout).", eventData.getEvent(), eventData.getData());
                } catch (ExecutionException e) {
                    logger.error("", e);
                }
            }
        }).start();
    }

    @Override
    public void accept(EventData eventData) {
        if (STOPPED){
            logger.warn("ignoring event (service is stopped). {}", eventData);
            return;
        }

        logger.info("received {}", eventData);
        if (eventQueue.size() >= QUEUE_LIMIT){
            logger.error("dropping {} (reason: queue max capacity) ", eventData);
            return;
        }

        eventQueue.add(eventData);
    }

    public void handleEvent(String filePath, Event event) {
        try{
            logger.info("handling event [{} {}]", event, filePath);
            if (!validateFile(filePath)) {
                return;
            }

            List<String> content = Files.readAllLines(Path.of(filePath), StandardCharsets.UTF_8);
            AlgoSolver solver = getConcreteAlgoSolver(content.get(0));

            if (solver == null){
                logger.error("unknown algorithm {}.", content.get(0));
                return;
            }

            String result = solver.solve(content, filePath);
            appendResultToFile(filePath, result);

        } catch (IOException e){
            logger.error("filename <{}>: ", filePath, e);
        }
    }

    private void appendResultToFile(String filePath, String result) {
        logger.info("insert result to file <{}>", filePath);
        try {
            Files.write(Path.of(filePath), ("\nSolution:\n"+result).getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("filename <{}>: {}", filePath, e);
        }
    }

    public AlgoSolver getConcreteAlgoSolver(String name){
        String algoName = name.strip().toLowerCase().replace("-","");
        Supplier<AlgoSolver> algoSolverSupplier = algorithmSolvers.get(algoName);
        if (algoSolverSupplier == null){
            return null;
        }
        return algoSolverSupplier.get();
    }

    private boolean validateFile(String filename) throws IOException {
        if (!filename.endsWith(".txt")){
            logger.error("skipping {}. can only handle .txt files", filename);
            return false;
        } else if (Files.size(Path.of(filename)) > FILE_SIZE_LIMIT){
            logger.error("skipping {}. can only handle file size < {}", filename, FILE_SIZE_LIMIT);
            return false;
        }
        return true;
    }
}
