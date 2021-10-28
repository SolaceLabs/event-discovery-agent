package com.event.discovery.agent.google;

import java.nio.file.Path;
import java.nio.file.Paths;

public class GooglePubSubUtils {
    public static String getResourceNameFromResourcePath(String fullResourcePath) {
        Path path = Paths.get(fullResourcePath);
        Path resourceName = path.getFileName();
        return resourceName.toString();
    }
}
