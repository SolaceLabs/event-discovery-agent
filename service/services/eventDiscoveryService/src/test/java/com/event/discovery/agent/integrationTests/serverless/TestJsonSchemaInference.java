package com.event.discovery.agent.integrationTests.serverless;

import com.event.discovery.agent.framework.utils.JsonSchemaGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class TestJsonSchemaInference {

    @Test
    public void testJsonSchemaInference() throws IOException, URISyntaxException {
        JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(new ObjectMapper());

        assertEquals(getOutputData("jsonSchemaOutput1.json"),
                jsonSchemaGenerator.outputAsString("test", getInputData("jsonSchemaInput1.json")));

    }

    private String getInputData(String filename) throws IOException {
        return getTestData("testInput", filename);
    }

    private String getOutputData(String filename) throws IOException {
        return getTestData("testOutput", filename);
    }

    private String getTestData(String inputOutput, String filename) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("testData/" + inputOutput + "/" + filename).getFile());
        return new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
    }

}
