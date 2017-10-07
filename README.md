## Corda trade finance API

### Build

Open a terminal window in the cordapp directory
Build the test nodes with our CorDapp using the following command:

- Unix/Mac OSX: ```./gradlew deployNodes```
- Windows: ```gradlew.bat deployNodes```

This will automatically build four pre-configured nodes with our CorDapp installed.

After the build process has finished, you will see the newly-build nodes in the kotlin-source/build/nodes folder. There will be one folder generated for each node you built, plus a runnodes shell script (or batch file on Windows) to run all the nodes simultaneously

### Run

Start the nodes by running the following command from the root of the cordapp folder:

- Unix/Mac OSX: ```kotlin-source/build/nodes/runnodes```
- Windows: ```call kotlin-source\build\nodes\runnodes.bat```

For each node, the runnodes script creates a node tab/window

For every node except the controller, the script also creates a webserver terminal tab/window

### API

API documentation is available in ```CordaTradeFinance.postman_collection.json``` file