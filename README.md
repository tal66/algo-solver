### AlgoSolver

An event-driven Java app that monitors a directory, solves the problems found in newly created files, 
and tracks statistics related to the processing tasks:

The **DirectoryWatcher** monitors the *input files* directory for file creation events, using the Java WatchService. 
It emits events using the **Publisher** to notify subscribers about file creation events.

The **AlgoInputHandler** handles incoming events related to file creation (implements the EventSubscriber interface). 
It parses the contents of created files, determines the appropriate algorithm to use, and appends the solution to each file. 

A thread pool is used for concurrent processing of algorithm tasks.

Addition of new algorithms can be done by simply extending the **AlgoSolver** interface, and updating the algorithmSolvers map.

Statistics events emmited by the AlgoInputHandler are published to a **Node.js UDP server** 
that periodically displays updated statistics in the console.

The main app can be run from the IDE, while the monitor is run from the *node* folder using `node index.js`.

Written 07/2022

***

Demo shows what happens when files are added to the watched folder.
<br><br>
![Alt demo](pics/demo.gif)
