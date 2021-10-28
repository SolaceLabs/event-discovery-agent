FROM adoptopenjdk/openjdk11

ARG JAR_FILE
ADD ${JAR_FILE} app.jar

# Setup the discovery agent version
ARG AGENT_VERSION
ENV EVENT_DISCOVERY_VERSION=$AGENT_VERSION

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]