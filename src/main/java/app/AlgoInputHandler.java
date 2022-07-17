package app;

import algo.AlgoSolver;
import algo.Knapsack;
import algo.SequenceAlignment;
import events.Event;
import events.EventData;
import events.EventSubscriber;
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

    private final BlockingQueue<EventData> queue = new LinkedBlockingQueue<>();
    private static final int QUEUE_LIMIT = 50;

    private final ExecutorService algoExecutorService = Executors.newFixedThreadPool(3);
    private final ExecutorService cancelTaskExecutorService = Executors.newFixedThreadPool(1);
    private static final int TASK_TIMEOUT = 15*1000;

    public AlgoInputHandler() {
    }

    @Override
    public void accept(EventData eventData) {
        logger.info("received {}", eventData);
        if (queue.size() > QUEUE_LIMIT){
            logger.error("queue reached max capacity. dropping {}", eventData);
            return;
        }
        queue.add(eventData);
    }

    public void start()  {
        logger.info("started");
        while (true){
            EventData eventData = null;
            try {
                eventData = queue.take();
            } catch (InterruptedException e){
                logger.error("queue interrupted", e);
                System.exit(1);
            }

            String filename = eventData.getData();
            Event event = eventData.getEvent();
            if (filename.equals(EventData.STOP_MESSAGE)){
                logger.warn("stopping (reason: STOP_MESSAGE)");
                break;
            }

            Future<?> future = algoExecutorService.submit(() -> handle(filename, event));
            // get() blocks, so another thread. but maybe there's a better way to cancel
            cancelTaskExecutorService.submit(()-> {
                try {
                    future.get(TASK_TIMEOUT, TimeUnit.MILLISECONDS);
                } catch (InterruptedException | TimeoutException e) {
                    future.cancel(true); // *Attempts* to cancel
                    logger.warn("canceling task for [{} {}] (reason: timeout).", event, filename);
                } catch (ExecutionException e) {
                    logger.error("", e);
                }
            });
        }
    }

    public void handle(String filename, Event event) {
        try{
            logger.info("handling event [{} {}]", event, filename);
            if (!validateFile(filename)) {
                return;
            }

            List<String> content = Files.readAllLines(Path.of(filename), StandardCharsets.UTF_8);
            AlgoSolver solver = getConcreteAlgoSolver(content.get(0));

            if (solver == null){
                logger.error("unknown algorithm {}.", content.get(0));
                return;
            }

            String result = solver.solve(content, filename);
            appendResultToFile(filename, result);

        } catch (IOException e){
            logger.error("filename <{}>: ", filename, e);
        }
    }

    private void appendResultToFile(String filename, String result) {
        logger.info("insert result to file {}", filename);
        try {
            Files.write(Path.of(filename), ("\nSolution:\n"+result).getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("filename <{}>: {}", filename, e);
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
