/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.responsecaching;

import com.oracle.coherence.cdi.CacheAdd;
import com.oracle.coherence.cdi.CacheGet;
import com.oracle.coherence.cdi.CacheKey;
import com.oracle.coherence.cdi.CachePut;
import com.oracle.coherence.cdi.CacheRemove;
import com.oracle.coherence.cdi.CacheValue;
import com.oracle.coherence.cdi.events.CacheName;

import jakarta.enterprise.context.RequestScoped;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import jakarta.ws.rs.core.MediaType;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Root resource used to demonstrate CDI Response Caching.
 */
// tag::resource[]
@Path("/")
@RequestScoped
@CacheName("messages-cache")
public class GreetResource {
    /**
     * This is used to track the count of invocations of a method annotated with {@link CacheGet}.
     */
    public static final AtomicInteger GET_CALLS = new AtomicInteger();

    /**
     * This is used to track the count of invocations of a method annotated with {@link CacheAdd}.
     */
    public static final AtomicInteger ADD_CALLS = new AtomicInteger();

    /**
     * This is used to track the count of invocations of a method annotated with {@link CachePut}.
     */
    public static final AtomicInteger PUT_CALLS = new AtomicInteger();

    /**
     * This is used to track the count of invocations of a method annotated with {@link CacheRemove}.
     */
    public static final AtomicInteger REMOVE_CALLS = new AtomicInteger();

    /**
     * This is used to track the count of invocations of a method with multiple parameters that
     * are used to build cache key.
     */
    public static final AtomicInteger MULTI_PARAM_CALLS = new AtomicInteger();
    // # end::resource[]

    // tag::cacheget[]
    @Path("greet/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @CacheGet // <1>
    public Message getMessage(@PathParam("name") String name) { // <2>
        GET_CALLS.incrementAndGet(); // <3>
        return new Message("Hello " + name); // <4>
    }
    // # end::cacheget[]

    // tag::cacheadd[]
    @Path("greet/{name}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @CacheAdd // <1>
    public Message addMessage(@PathParam("name") String name) { // <2>
        ADD_CALLS.incrementAndGet(); // <3>
        return new Message("ADD executed"); // <4>
    }
    // end::cacheadd[]

    // tag::cacheput[]
    @Path("greet/{name}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @CachePut // <1>
    public Message putMessage(@CacheKey @PathParam("name") String name, @CacheValue Message message) { // <2>
        PUT_CALLS.incrementAndGet(); // <3>
        return new Message("PUT executed"); // <4>
    }
    // end::cacheput[]

    // tag::cacheremove[]
    @Path("greet/{name}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @CacheRemove // <1>
    public Message removeMessage(@PathParam("name") String name) { // <2>
        REMOVE_CALLS.incrementAndGet(); // <3>
        return new Message("Deleted cached value for " + name); // <4>
    }
    // end::cacheremove[]

    // tag::cachename[]
    @Path("another")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @CacheGet
    @CacheName("another-cache") // <1>
    public Message getFromAnotherCache(@QueryParam("name") @CacheKey String name) {
        return new Message("Another " + name + "?");
    }
    // end::cachename[]

    // tag::cacheparams[]
    @Path("parameters")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @CacheGet
    public Message get(@QueryParam("firstName") String firstName, @QueryParam("lastName") String lastName) { // <1>
        MULTI_PARAM_CALLS.incrementAndGet();
        return new Message("Message for " + firstName + " " + lastName);
    }
    // end::cacheparams[]
}