package com.event.discovery.agent.integrationTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.event.discovery.agent.plugins.PluginConfigurationMap;
import com.event.discovery.agent.plugins.broker.BrokerAuthenticationDeserializer;
import com.event.discovery.agent.plugins.broker.BrokerIdentityDeserializer;
import com.event.discovery.agent.springboot.properties.DiscoveryProperties;
import com.event.discovery.agent.framework.model.broker.BrokerAuthentication;
import com.event.discovery.agent.framework.model.broker.BrokerIdentity;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;

public abstract class BaseIT {

    @Autowired
    protected ObjectMapper objectMapper;


    @Autowired
    protected DiscoveryProperties discoveryProperties;

    protected RestAssuredConfig restAssuredConfig;

    @Autowired
    protected PluginConfigurationMap pluginConfigurationMap;

    @Before
    public void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule identityModule = new SimpleModule();
        identityModule.addDeserializer(BrokerIdentity.class, new BrokerIdentityDeserializer(pluginConfigurationMap));
        SimpleModule authenticationModule = new SimpleModule();
        authenticationModule.addDeserializer(BrokerAuthentication.class, new BrokerAuthenticationDeserializer(pluginConfigurationMap));
        objectMapper.registerModule(identityModule);
        objectMapper.registerModule(authenticationModule);
        restAssuredConfig = config().objectMapperConfig(objectMapperConfig().jackson2ObjectMapperFactory((cls, charset) -> objectMapper));

        RestAssured.port = getPort();
        RestAssured.baseURI = "http://localhost";
    }

    protected abstract int getPort();

    protected ValidatableResponse getAndThen(String url, String... pathParams) {
        return givenInternal().config(restAssuredConfig).when().get(url, pathParams).prettyPeek().then();
    }

    protected ValidatableResponse getWithTokenAndThen(String token, String url, String... pathParams) {
        return givenInternal(token).config(restAssuredConfig).when().get(url, pathParams).prettyPeek().then();
    }

    protected ValidatableResponse getAndThen(Map<String, Object> params, String url, String... pathParams) {
        return givenInternal().config(restAssuredConfig).params(params).when().get(url, pathParams).prettyPeek().then();
    }

    protected ValidatableResponse putAndThen(Object body, String url, String... pathParams) {
        return givenInternal().config(restAssuredConfig).body(body).when().put(url, pathParams).prettyPeek().then();
    }

    protected ValidatableResponse putWithTokenAndThen(String token, Object body, String url, String... pathParams) {
        return givenInternal(token).config(restAssuredConfig).body(body).when().put(url, pathParams).prettyPeek().then();
    }

    protected ValidatableResponse deleteAndThen(String url, String... pathParams) {
        return givenInternal().config(restAssuredConfig).when().delete(url, pathParams).prettyPeek().then();
    }

    protected ValidatableResponse deleteWithTokenAndThen(String token, String url, String... pathParams) {
        return givenInternal(token).config(restAssuredConfig).when().delete(url, pathParams).prettyPeek().then();
    }

    protected ValidatableResponse postAndThen(Object body, String url, String... pathParams) {
        return givenInternal().config(restAssuredConfig).body(body).when().post(url, pathParams).prettyPeek().then();
    }

    protected ValidatableResponse postWithTokenAndThen(String token, Object body, String url, String... pathParams) {
        return givenInternal(token).config(restAssuredConfig).body(body).when().post(url, pathParams).prettyPeek().then();
    }

    private RequestSpecification givenInternal() {
        return given().config(restAssuredConfig).header("X-Internal-Authorization", getToken()).contentType(ContentType.JSON);
    }

    private RequestSpecification givenInternal(String token) {
        return given().config(restAssuredConfig).header("X-Internal-Authorization", token).contentType(ContentType.JSON);
    }

    private String getToken() {
        return "token";
    }

    public String getToken(String org) {
        return getToken();
    }

}