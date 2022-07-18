package app;

import events.Event;
import events.Publisher;
import monitor.StatsMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static String dir = "./input files";

    public static void main(String[] args) {
        if (args.length > 0) {
            dir = args[0];
        }
        Path dirPath = Path.of(dir);
        validate(dirPath);

        // di
        Publisher publisher = new Publisher();
        DirectoryWatcher watcher = new DirectoryWatcher(dirPath, publisher);
        AlgoInputHandler algoInputHandler = new AlgoInputHandler(publisher);
        StatsMonitor statsMonitor = new StatsMonitor();

        publisher.subscribe(Event.FILE_CREATED, algoInputHandler);
        publisher.subscribe(Event.TASK_STATS, statsMonitor);

        // init
        initServices(watcher, algoInputHandler, statsMonitor);
    }

    private static void initServices(DirectoryWatcher watcher, AlgoInputHandler algoInputHandler, StatsMonitor statsMonitor) {
        new Thread(() -> {
            try {
                algoInputHandler.start();
            } catch (InterruptedException e) {
                logger.error("", e);
                System.exit(1);
            }
        }).start();
        new Thread(() -> {
            try {
                statsMonitor.start();
            } catch (InterruptedException e) {
                logger.error("", e);
            }
        }).start();
        new Thread(() -> {
            try {
                watcher.watchDir();
            } catch (IOException | InterruptedException e) {
                logger.error("", e);
                System.exit(1);
            }
        }).start();
    }

    private static void validate(Path dirPath) {
        if (Files.notExists(dirPath)){
            logger.error("Directory <{}> doesn't exist", dirPath);
            System.exit(1);
        } else if(!Files.isDirectory(dirPath)){
            logger.error("<{}> not a directory", dirPath);
            System.exit(1);
        }
    }
}
