package app;

import events.Event;
import events.Publisher;
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
        AlgoInputHandler algoInputHandler = new AlgoInputHandler();
        publisher.subscribe(Event.FILE_CREATED, algoInputHandler);

        // init
        new Thread(() -> algoInputHandler.start()).start();
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
            logger.error("Directory {} doesn't exist", dirPath);
            System.exit(1);
        } else if(!Files.isDirectory(dirPath)){
            logger.error("<{}> not a directory", dirPath);
            System.exit(1);
        }
    }
}
