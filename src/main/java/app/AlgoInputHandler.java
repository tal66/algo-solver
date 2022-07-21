package app;

import algo.AlgoSolver;
import algo.Knapsack;
import algo.SequenceAlignment;
import com.google.gson.Gson;
import events.*;
import lombok.AllArgsConstructor;
import monitor.TaskStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AlgoInputHandler implements EventSubscriber, EventEmitter {
    private static final Logger logger = LoggerFactory.getLogger(AlgoInputHandler.class);

    private static final int FILE_SIZE_LIMIT = 10 * 1024;
    private static final HashMap<String, Supplier<AlgoSolver>> algorithmSolvers = new HashMap<>() {{
        put("knapsack", Knapsack::new);
        put("sequencealignment", SequenceAlignment::new);
    }};
    private static final Gson gson = new Gson();

    private static final int QUEUE_LIMIT = 50;
    private static final int TASK_TIMEOUT = 15 * 1000;

    private final BlockingQueue<EventData> eventQueue = new LinkedBlockingQueue<>(QUEUE_LIMIT);
    private final BlockingQueue<TaskInfo> futuresQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<Runnable> algoExecutorInnerQueue = new LinkedBlockingQueue<>(QUEUE_LIMIT);
    private final ExecutorService algoExecutorService = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, algoExecutorInnerQueue);
    private boolean STOPPED = false;
    private final Publisher publisher;

    @AllArgsConstructor
    static class TaskInfo {
        Future<?> future;
        EventData eventData;
    }

    public AlgoInputHandler(Publisher publisher) {
        this.publisher = publisher;
    }

    public void start() throws InterruptedException {
        logger.info("started");

        startCancellationService();

        while (true) {
            EventData eventData = eventQueue.take();
            long startTime = System.currentTimeMillis();
            String filePath = eventData.getData();
            Event event = eventData.getEvent();

            if (Path.of(filePath).getFileName().toString().equals(EventData.STOP_MESSAGE)) {
                logger.warn("stopping service (reason: STOP_MESSAGE)");
                STOPPED = true;
                futuresQueue.add(new TaskInfo(null, eventData));
                break;
            }

            if (algoExecutorInnerQueue.size() >= QUEUE_LIMIT) {
                logger.error("dropping {} (reason: inner queue max capacity) ", eventData);
                return;
            }

            Future<?> future = algoExecutorService.submit(() -> {
                String algoName = handleEvent(filePath, event);
                if (algoName.length() == 0) {
                    algoName = "error";
                }
                long totalTime = System.currentTimeMillis() - startTime;
                logger.info("{} ms (receive event->done). Task <{} {}>)", totalTime, filePath, event);
                emit(new EventData(Event.TASK_STATS, gson.toJson(new TaskStats(algoName, totalTime))));
            });

            futuresQueue.add(new TaskInfo(future, eventData));
        }
    }

    private void startCancellationService() {
        // get() blocks, so another thread. maybe there's a better way to cancel
        new Thread(() -> {
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
                if (filename.equals(EventData.STOP_MESSAGE)) {
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
        if (STOPPED) {
            logger.warn("ignoring event (service is stopped). {}", eventData);
            return;
        }

        logger.info("received {}", eventData);
        if (eventQueue.size() >= QUEUE_LIMIT) {
            logger.error("dropping {} (reason: queue max capacity) ", eventData);
            return;
        }

        eventQueue.add(eventData);
    }

    @Override
    public void emit(EventData eventData) {
        publisher.notifyListeners(eventData);
    }

    public String handleEvent(String filePath, Event event) {
        String algoName = "";
        logger.info("handling event [{} {}]", event, filePath);
        try (FileWriter fw = new FileWriter(filePath, true);
             FileReader fr = new FileReader(filePath);
             BufferedReader bufferedReader = new BufferedReader(fr)) {

            if (!validateFile(filePath)) {
                return algoName;
            }

            List<String> content = readFile(bufferedReader, filePath);

            algoName = content.get(0);
            AlgoSolver solver = getConcreteAlgoSolver(algoName);
            if (solver == null) {
                logger.error("unknown algorithm {}.", algoName);
                return "unknown";
            }

            String result = solver.solve(content, filePath);
            appendResultToFile(result, fw, filePath);

        } catch (IOException | IndexOutOfBoundsException e) {
            logger.error("filename <{}>: ", filePath, e);
            return "";
        }
        return algoName;
    }

    private List<String> readFile(BufferedReader br, String filePath) {
        List<String> content = br.lines().collect(Collectors.toList());
        if (content.size() == 0) {
            logger.warn("retry: reading file {}", filePath);
            content = br.lines().collect(Collectors.toList());
        }

        return content;
    }

    private void appendResultToFile(String result, FileWriter fw, String filePath) {
        logger.info("insert result to file <{}>", filePath);
        try {
            fw.write("\nSolution:\n" + result);
        } catch (IOException e) {
            logger.error("filename <{}>: {}", filePath, e);
        }
    }

    private AlgoSolver getConcreteAlgoSolver(String name) {
        String algoName = name.strip().toLowerCase().replace("-", "");
        Supplier<AlgoSolver> algoSolverSupplier = algorithmSolvers.get(algoName);
        if (algoSolverSupplier == null) {
            return null;
        }
        return algoSolverSupplier.get();
    }

    private boolean validateFile(String filename) throws IOException {
        if (!filename.endsWith(".txt")) {
            logger.error("skipping {}. can only handle .txt files", filename);
            return false;
        } else if (Files.size(Path.of(filename)) > FILE_SIZE_LIMIT) {
            logger.error("skipping {}. can only handle file size < {}", filename, FILE_SIZE_LIMIT);
            return false;
        }
        return true;
    }
}
