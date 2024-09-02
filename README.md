# Zookeeper-Leader-Election
A leader election implementation using Zookeeper

## How to run?
1. Start Zookeeper server by running the following command:
```bash
./bin/zkServer.sh start
```
2. Create the election node in Zookeeper by running the following command:
```bash
./bin/zkCli.sh create /election ""
```
3. Build the project by running the following command:
```bash
mvn clean package
```
4. Run the leader election program by running the following command:
```bash
java -jar target/Zookeeper-Leader-Election-1.0-SNAPSHOT-jar-with-dependencies.jar
```