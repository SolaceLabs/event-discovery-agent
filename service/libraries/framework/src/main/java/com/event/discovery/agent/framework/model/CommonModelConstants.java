package com.event.discovery.agent.framework.model;

public class CommonModelConstants {
    public static class Channel {
        public static final String TOPIC = "topic";
        public static final String TYPE = "type";
    }

    public static class Client {
        public static final String TYPE_PRODUCER = "producer";
        public static final String TYPE_CONSUMER = "consumer";
        public static final String CLIENT_TYPE_KEY = "clientType";
        public static final String CLIENT_TYPE_CONNECTOR = "connector";
        public static final String CLIENT_TYPE_APPLICATION = "clientApplication";
        public static final String CONNECTOR_CLASS_KEY = "connectorClass";
        public static final String MAX_THREADS = "maxThreads";
        public static final String SIMPLE_CONSUMER = "simpleConsumer";
        public static final String TOPICS = "topics";
    }

    public static class Schema {
        public static final String CONTENT_TYPE = "contentType";
        public static final String SUBJECT_NAME = "subjectName";
        public static final String SUBJECT_VERSION = "subjectVersion";
        public static final String SCHEMA_REG = "schemaReg";
        public static final String SCHEMA_REG_ID = "schemaRegId";
    }

    public static class ChannelToSchemaRelationship {
        public static final String KEY_SCHEMA_ID = "keySchemaId";
    }
}
