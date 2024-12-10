/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.responsecaching;

import com.oracle.coherence.cdi.Name;

import com.tangosol.net.NamedMap;

import io.helidon.microprofile.tests.junit5.HelidonTest;

import javax.inject.Inject;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * A test class showing the usage of CDI Response Caching.
 */
// tag::testclass[]
@HelidonTest
public class GreetResourceTest {

    @Inject
    private WebTarget target;

    @Inject
    @Name("messages-cache")
    private NamedMap cache; // <1>

    @Inject
    @Name("another-cache")
    private NamedMap anotherCache;

    @BeforeAll
    static void boot() {
        System.setProperty("coherence.wka", "127.0.0.1");
    }

    @BeforeEach
    void setup() { // <2>
        cache.clear();
        anotherCache.clear();
        GreetResource.GET_CALLS.set(0);
        GreetResource.ADD_CALLS.set(0);
        GreetResource.PUT_CALLS.set(0);
        GreetResource.REMOVE_CALLS.set(0);
    }
    // end::testclass[]

    // tag::testget[]
    @Test
    void testGet() {
        Message getResponse = target.path("/greet/John") // <1>
                .request()
                .acceptEncoding(MediaType.APPLICATION_JSON)
                .get(Message.class);
        final Message expected = new Message("Hello John");
        assertThat(getResponse, is(expected)); // <2>
        assertThat(GreetResource.GET_CALLS.get(), is(1)); // <3>
        assertThat(cache.get("John"), is(expected)); // <4>

        getResponse = target.path("/greet/John") // <5>
                .request()
                .acceptEncoding(MediaType.APPLICATION_JSON)
                .get(Message.class);
        assertThat(getResponse, is(expected));
        assertThat(GreetResource.GET_CALLS.get(), is(1));
    }
    // end::testget[]

    // tag::testadd[]
    @Test
    void testAdd() {
        Message getResponse = target.path("/greet/John") // <1>
                .request()
                .get(Message.class);
        final Message expectedGetResponse = new Message("Hello John");
        assertThat(getResponse, is(expectedGetResponse));
        assertThat(GreetResource.GET_CALLS.get(), is(1));
        assertThat(cache.get("John"), is(expectedGetResponse));

        final Message addedMessage = new Message("ADD executed");
        Message addResponse = target.path("/greet/John") // <2>
                .request()
                .acceptEncoding(MediaType.APPLICATION_JSON)
                .post(null, Message.class);
        assertThat(addResponse, is(addedMessage));
        assertThat(GreetResource.ADD_CALLS.get(), is(1));
        assertThat(cache.get("John"), is(addedMessage)); // <3>

        addResponse = target.path("/greet/John") // <4>
                .request()
                .acceptEncoding(MediaType.APPLICATION_JSON)
                .post(null, Message.class);
        assertThat(addResponse, is(addedMessage));
        assertThat(GreetResource.ADD_CALLS.get(), is(2)); // <5>
        assertThat(cache.get("John"), is(addedMessage));
    }
    // end::testadd[]

    // tag::testput[]
    @Test
    void testPut() {
        final Message messageToCache = new Message("Hola");
        final Message expectedPutResponse = new Message("PUT executed");
        Message putResponse = target.path("/greet/John") // <1>
                .request() // <1>
                .acceptEncoding(MediaType.APPLICATION_JSON)
                .put(Entity.entity(messageToCache, MediaType.APPLICATION_JSON_TYPE), Message.class);
        assertThat(putResponse, is(expectedPutResponse));
        assertThat(GreetResource.PUT_CALLS.get(), is(1));
        assertThat(cache.get("John"), is(messageToCache)); // <2>

        putResponse = target.path("/greet/John") // <3>
                .request() // <3>
                .acceptEncoding(MediaType.APPLICATION_JSON)
                .put(Entity.entity(messageToCache, MediaType.APPLICATION_JSON_TYPE), Message.class);
        assertThat(putResponse, is(expectedPutResponse));
        assertThat(GreetResource.PUT_CALLS.get(), is(2)); // <4>
    }
    // end::testput[]

    // tag::testremove[]
    @Test
    void testRemove() {
        final Message hola = new Message("Hola");
        Message putResponse = target.path("/greet/John") // <1>
                .request()
                .acceptEncoding(MediaType.APPLICATION_JSON)
                .put(Entity.entity(hola, MediaType.APPLICATION_JSON_TYPE), Message.class);
        assertThat(putResponse, is(new Message("PUT executed")));
        assertThat(GreetResource.PUT_CALLS.get(), is(1));
        assertThat(cache.get("John"), is(hola)); // <2>

        Message deleteResponse = target.path("/greet/John") // <3>
                .request()
                .delete(Message.class);
        assertThat(deleteResponse, is(new Message("Deleted cached value for John")));
        assertThat(GreetResource.REMOVE_CALLS.get(), is(1));
        assertThat(cache.get("John"), is(nullValue())); // <4>
        assertThat(cache.size(), is(0));
    }
    // end::testremove[]

    // tag::testname[]
    @Test
    void testCacheName() {
        Message anotherGetResponse = target.path("/another") // <1>
                .queryParam("name", "John")
                .request()
                .acceptEncoding(MediaType.APPLICATION_JSON)
                .get(Message.class);
        assertThat(anotherGetResponse, is(new Message("Another John?")));
        assertThat(cache.size(), is(0)); // <2>
        assertThat(anotherCache.size(), is(1));
        assertThat(anotherCache.get("John"), is(new Message("Another John?"))); // <3>
    }
    // end::testname[]

    @Test
    void testMultiParameters() {
        Message response = target.path("/parameters") // <1>
                .queryParam("firstName", "John")
                .queryParam("lastName", "Doe")
                .request()
                .acceptEncoding(MediaType.APPLICATION_JSON)
                .get(Message.class);
        final Message expected = new Message("Message for John Doe");
        assertThat(response, is(expected));
        assertThat(GreetResource.MULTI_PARAM_CALLS.get(), is(1));
        assertThat(cache.size(), is(1));
        assertThat(cache.values().stream().findFirst().get(), is(expected)); // <2>

        response = target.path("/parameters")
                .queryParam("firstName", "John")
                .queryParam("lastName", "Doe")
                .request()
                .acceptEncoding(MediaType.APPLICATION_JSON)
                .get(Message.class);
        assertThat(response, is(expected));
        assertThat(GreetResource.MULTI_PARAM_CALLS.get(), is(1)); // <3>
    }
}