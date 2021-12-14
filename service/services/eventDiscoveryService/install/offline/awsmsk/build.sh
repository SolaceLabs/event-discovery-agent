cp ../../../target/event-discovery-agent-app-1.0-SNAPSHOT.jar .
docker build --build-arg JAR_FILE=event-discovery-agent-app-1.0-SNAPSHOT.jar --build-arg AGENT_VERSION=1.0.0 --tag async-api-tool:1.0 .
rm ./event-discovery-agent-app-1.0-SNAPSHOT.jar
