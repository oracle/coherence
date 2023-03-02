/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.executor;

import com.oracle.coherence.concurrent.executor.RemoteExecutor;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.function.Remote;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("resource")
public class ExecutorBasicTests
    {
    // # tag::bootstrap[]
    @BeforeAll
    static void boostrapCoherence()
        {
        System.setProperty("coherence.wka",         "127.0.0.1");
        System.setProperty("coherence.cacheconfig", "custom-executors.xml"); // <1>

        Coherence                    coherence = Coherence.clusterMember(); // <2>
        CompletableFuture<Coherence> future    = coherence.start(); // <3>

        future.join(); // <4>
        }
    // # end::bootstrap[]

    // # tag::cleanup[]
    @AfterAll
    static void shutdownCoherence()
        {
        Coherence coherence = Coherence.getInstance(); //<1>

        coherence.close();
        }
    // # end::cleanup[]

    // # tag::basic-runnable[]
    @Test
    void testSimpleRunnable()
            throws Exception
        {
        NamedMap<String, String> map             = getMap(); // <1>
        RemoteExecutor           defaultExecutor = RemoteExecutor.getDefault(); // <2>

        map.truncate(); //<3>
        assertTrue(map.isEmpty());

        Future<?> result = defaultExecutor.submit((Remote.Runnable) () ->
                Coherence.getInstance()
                        .getSession().getMap("data").put("key-1", "value-1")); // <4>

        result.get(); // <5>

        String sValue = map.get("key-1"); // <6>

        assertEquals(sValue, "value-1"); // <7>
        }
    // # end::basic-runnable[]

    // # tag::basic-callable[]
    @Test
    void testSimpleCallable()
            throws Exception
        {
        NamedMap<String, String> map             = getMap(); // <1>
        RemoteExecutor           defaultExecutor = RemoteExecutor.getDefault(); // <2>

        map.truncate(); //<3>
        assertTrue(map.isEmpty());

        map.put("key-1", "value-1"); // <4>

        Future<String> result = defaultExecutor.submit((Remote.Callable<String>) () ->
                (String) Coherence.getInstance().getSession().getMap("data").put("key-1", "value-2")); // <5>

        String sResult = result.get(); // <6>
        String sValue  = map.get("key-1"); // <7>

        assertEquals(sResult, "value-1"); // <8>
        assertEquals(sValue, "value-2"); // <9>
        }
    // # end::basic-callable[]

    // # tag::fixed-executor[]
    @Test
    void testCustomExecutor()
            throws Exception
        {
        RemoteExecutor fixed5 = RemoteExecutor.get("fixed-5"); // <1>

        List<Remote.Callable<String>> listCallables = new ArrayList<>(5); // <2>
        for (int i = 0; i < 10; i++)
            {
            listCallables.add(() ->
                              {
                              Thread.sleep(1000);
                              return Thread.currentThread().getName();
                              });
            }

        List<Future<String>> listFutures = fixed5.invokeAll(listCallables); // <3>

        Set<String> results = new LinkedHashSet<>(); // <4>
        for (Future<String> listFuture : listFutures)
            {
            results.add(listFuture.get());
            }

        System.out.printf("Tasks executed on threads %s", results);

        assertEquals(5, results.size()); // <5>
        }
    // # end::fixed-executor[]

    // ----- helper methods -------------------------------------------------

    // # tag::get-map[]
    NamedMap<String, String> getMap()
        {
        Coherence coherence = Coherence.getInstance(); // <1>
        Session session = coherence.getSession(); // <2>
        return session.getMap("data"); // <3>
        }
    // # end::get-map[]
    }
