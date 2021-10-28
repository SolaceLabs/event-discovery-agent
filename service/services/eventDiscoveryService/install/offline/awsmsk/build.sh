cp ../../../target/event-discovery-agent-1.0-SNAPSHOT.jar .
docker build --build-arg JAR_FILE=event-discovery-agent-1.0-SNAPSHOT.jar --tag event-discovery-agent:1.0 .
rm ./event-discovery-agent-1.0-SNAPSHOT.jar
