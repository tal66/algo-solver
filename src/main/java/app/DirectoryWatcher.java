package app;

import events.Event;
import events.EventEmitter;
import events.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;


public class DirectoryWatcher implements EventEmitter {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);
    private static final HashMap<WatchEvent.Kind, Event> eventsMap = new HashMap<>(){{
        put(StandardWatchEventKinds.ENTRY_CREATE, Event.FILE_CREATED);
        put(StandardWatchEventKinds.ENTRY_MODIFY, Event.FILE_CHANGED);
    }};

    private final Path dir;
    private final Publisher publisher;

    public DirectoryWatcher(Path dir, Publisher publisher) {
        this.dir = dir;
        this.publisher = publisher;
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void watchDir() throws IOException, InterruptedException {
        logger.info("started in {}", dir);
        var fs = FileSystems.getDefault();
        WatchService watchService = fs.newWatchService();
        this.dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        while (true){
            WatchKey watchKey = watchService.take();
            for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
                var file = watchEvent.context();
                WatchEvent.Kind<?> kind = watchEvent.kind();
                String filename = file.toString();
                if (new File(filename).isHidden() || filename.endsWith("~")){
                    continue;
                }

                logger.info("<{}> {}", file, kind);
                Event event = eventsMap.getOrDefault(kind, Event.UNKNOWN);
                if (event == Event.UNKNOWN){
                    logger.warn("Event kind {} is unknown", kind);
                }

                Path filePath = Path.of(dir.toString(), filename);
                emit(event, filePath.toString());
            }
            watchKey.reset();
        }
    }

    @Override
    public void emit(Event event, String data) {
        publisher.notifyListeners(event, data);
    }
}
