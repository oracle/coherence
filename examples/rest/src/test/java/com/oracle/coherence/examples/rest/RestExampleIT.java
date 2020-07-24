/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.rest;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static io.restassured.RestAssured.when;
import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;

/**
 * Tests for Rest example JAX-RS endpoints.
 */
public class RestExampleIT
    {

    //----- test lifecycle --------------------------------------------------

    @BeforeAll
    static void startup()
        {
        // start a single DefaultCacheServer
        LocalPlatform platform = LocalPlatform.get();

        AvailablePortIterator availablePorts = platform.getAvailablePorts();
        s_nHttpPort = availablePorts.next();
        s_server    = platform.launch(CoherenceCacheServer.class,
                        CacheConfig.of("coherence-cache-config.xml"),
                        SystemProperty.of("coherence.wka", "127.0.0.1"),
                        SystemProperty.of("coherence.ttl", "0"),
                        SystemProperty.of("coherence.examples.rest.enabled", "true"),
                        SystemProperty.of("coherence.examples.rest.port", s_nHttpPort),
                        SystemProperty.of("coherence.examples.rest.address", "127.0.0.1"),
                        ClusterPort.automatic(),
                        ClusterName.of("test-rest-cluster"));

        Eventually.assertThat(invoking(s_server).getClusterSize(), is(1));
        Eventually.assertThat(invoking(s_server).isServiceRunning("HttpProxyService"), is(true));
        
        RestAssured.baseURI = "http://localhost";
        RestAssured.port    = s_nHttpPort;
        }

    @AfterAll
    static void shutdown()
        {
        if (s_server != null)
            {
            s_server.close();
            }
        }

    //----- tests -----------------------------------------------------------

    @Test
    public void endpointsSmokeTest()
        {
        testEndpoint("/cache/countries.json");
        testEndpoint("/cache/states.json");
        testEndpoint("/cache/contacts.json");
        testEndpoint("/cache/departments.json");
        testEndpoint("/cache/products.json");
        testEndpoint("/cache/static-content/entries");
        }

    @Test
    public void testContacts()
        {
        // create a new Contact
        given().
                urlEncodingEnabled(false).
        when().
                post("/cache/contacts/Tim_Jones/random-contact-creator()").
        then().
                statusCode(OK.getStatusCode());

        // retrieve the contact
        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                get("/cache/contacts/Tim_Jones.json").
        then().
                statusCode(OK.getStatusCode()).
                body("firstName", is("Tim"),
                     "lastName", is("Jones"));

        // delete the contact
        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                delete("/cache/contacts/Tim_Jones.json").
        then().
                statusCode(OK.getStatusCode());
        }

    @Test
    public void testJson()
        {
        // create JSON
        given().
                body("{\"name\":\"Tim\", \"age\": 20 }").
                accept(ContentType.JSON).
                contentType(ContentType.JSON).
        when().
                put("/cache/json/key1.json").
        then().
                statusCode(OK.getStatusCode());

        // retrieve
        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                get("/cache/json/key1.json").
        then().
                statusCode(OK.getStatusCode()).
                body("name", is("Tim"),
                     "age", is(20));

        // increment age
        given().
                urlEncodingEnabled(false).
        when().
                post("/cache/json/key1/increment(age,1)").
        then().
                statusCode(OK.getStatusCode());


        // retrieve
        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                get("/cache/json/key1.json").
        then().
                statusCode(OK.getStatusCode()).
                body("age", is(21));

        // delete
        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                delete("/cache/json/key1.json").
        then().
                statusCode(OK.getStatusCode());

        // retrieve

        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                get("/cache/json/key1.json").
        then().
                statusCode(NOT_FOUND.getStatusCode());
        }

    @Test
    public void testDepartments()
        {
        // create JSON
        given().
                body("{\"deptCode\":\"01\", \"name\": \"Dept01\" }").
                accept(ContentType.JSON).
                contentType(ContentType.JSON).
        when().
                put("/cache/departments/01").
        then().
                statusCode(OK.getStatusCode());

        // retrieve
        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                get("/cache/departments/01").
        then().
                statusCode(OK.getStatusCode()).
                body("deptCode", is("01"),
                     "name", is("Dept01"));

        // delete
        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                delete("/cache/departments/01").
        then().
                statusCode(OK.getStatusCode());

        // retrieve

        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                get("/cache/departments/01").
        then().
                statusCode(NOT_FOUND.getStatusCode());
        }

    @Test
    public void testProducts()
        {
        // create JSON
        given().
                body("{\"productId\":\"01\", " +
                     "\"name\": \"Product01\", " +
                     "\"price\": 100, " +
                     "\"qtyOnHand\": 10, " +
                     "\"deptCode\": \"01\" " +
                     "}").
                accept(ContentType.JSON).
                contentType(ContentType.JSON).
        when().
                put("/cache/products/01").
        then().
                statusCode(OK.getStatusCode());

        // retrieve
        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                get("/cache/products/01").
        then().
                statusCode(OK.getStatusCode()).
                body("deptCode", is("01"),
                     "name", is("Product01"),
                     "deptCode", is("01"),
                     "qtyOnHand", is(10),
                     "price", is(100.0f));

        // delete
        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                delete("/cache/products/01").
        then().
                statusCode(OK.getStatusCode());

        // retrieve

        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                get("/cache/products/01").
        then().
                statusCode(NOT_FOUND.getStatusCode());
        }

    //----- helpers --------------------------------------------------------

    private void testEndpoint(String sUrl)
        {
        given().
                contentType(ContentType.JSON).
                accept(ContentType.JSON).
        when().
                get(sUrl).
        then().
                statusCode(OK.getStatusCode());
        }

    //----- data members ----------------------------------------------------

    private static CoherenceCacheServer s_server;

    private static int s_nHttpPort;
}

