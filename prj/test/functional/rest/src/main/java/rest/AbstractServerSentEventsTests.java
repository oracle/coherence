/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.rest.events.SimpleMapEvent;

import com.tangosol.net.NamedCache;

import javax.ws.rs.client.Client;

import rest.data.Persona;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.media.sse.EventSource;
import org.glassfish.jersey.media.sse.InboundEvent;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Aleksandar Seovic  2015.06.26
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractServerSentEventsTests
        extends AbstractRestTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     *
     * @param sPath the configuration resource name or file path
     */
    public AbstractServerSentEventsTests(String sPath)
        {
        super(sPath);
        }

    // ---- tests -----------------------------------------------------------

    @Test
    public void testCacheSSE()
        {
        final Map<String, Integer> mapCounts = new HashMap<>();

        WebTarget   target = getWebTarget("dist-test-named-query");
        EventSource source = createEventSource(target, mapCounts);

        NamedCache cache = getNamedCache("dist-test-named-query");
        cache.remove(2);
        cache.put(2, new Persona("Aleks", 40));
        cache.put(1, new Persona("Ivan", 36));
        cache.remove(3);

        Eventually.assertDeferred(mapCounts::size, is(3));
        Eventually.assertDeferred(() -> mapCounts.get("insert"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("update"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("delete"), is(2));

        source.close();
        }

    @Test
    public void testFilterSSE()
        {
        final Map<String, Integer> mapCounts = new HashMap<>();

        WebTarget   target = getWebTarget("dist-test-named-query?q=age=37");
        EventSource source = createEventSource(target, mapCounts);

        NamedCache cache = getNamedCache("dist-test-named-query");
        cache.put(1, new Persona("Ivan Cikic", 33));
        cache.put(1, new Persona("Ivan", 37));
        cache.put(2, new Persona("Aleks", 40));
        cache.put(3, new Persona("Vaso Putica", 37));
        cache.remove(2);
        cache.remove(3);

        Eventually.assertDeferred(mapCounts::size, is(3));
        Eventually.assertDeferred(() -> mapCounts.get("insert"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("update"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("delete"), is(2));

        source.close();
        }

    @Test
    public void testNamedQuerySSE()
        {
        final Map<String, Integer> mapCounts = new HashMap<>();

        WebTarget   target = getWebTarget("dist-test-named-query/age-37-query");
        EventSource source = createEventSource(target, mapCounts);

        NamedCache cache = getNamedCache("dist-test-named-query");
        cache.put(1, new Persona("Ivan Cikic", 33));
        cache.put(1, new Persona("Ivan", 37));
        cache.put(2, new Persona("Aleks", 40));
        cache.put(3, new Persona("Vaso Putica", 37));
        cache.remove(2);
        cache.remove(3);

        Eventually.assertDeferred(mapCounts::size, is(3));
        Eventually.assertDeferred(() -> mapCounts.get("insert"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("update"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("delete"), is(2));

        source.close();
        }

    @Test
    public void testKeySSE()
        {
        final Map<String, Integer> mapCounts = new HashMap<>();

        WebTarget   target = getWebTarget("dist-test-named-query/1");
        EventSource source = createEventSource(target, mapCounts);

        NamedCache cache = getNamedCache("dist-test-named-query");
        cache.put(1, new Persona("Ivan Cikic", 33));
        cache.put(1, new Persona("Ivan Cikic", 37));
        cache.put(2, new Persona("Aleks", 40));
        cache.put(3, new Persona("Vaso Putica", 37));
        cache.remove(1);
        cache.remove(2);
        cache.remove(3);
        cache.put(1, new Persona("Ivan", 37));

        Eventually.assertDeferred(mapCounts::size, is(3));
        Eventually.assertDeferred(() -> mapCounts.get("insert"), is(1));
        Eventually.assertDeferred(() -> mapCounts.get("update"), is(2));
        Eventually.assertDeferred(() -> mapCounts.get("delete"), is(1));

        source.close();
        }

    protected EventSource createEventSource(WebTarget target, Map<String, Integer> mapCounts)
        {
        // implementation note:  getClient() isn't used here as the LoggingFeature doesn't
        //                       appear to work well with SSE
        Client    sseClient = createClient().build();
        WebTarget sseTarget = sseClient.target(target.getUri());

        int i = 0;
        while (i++ < 3)
            {
            EventSource source = new EventSource(sseTarget)
                {
                @Override
                public void onEvent(InboundEvent inboundEvent)
                    {
                    mapCounts.merge(inboundEvent.getName(), 1, (v1, v2) -> v1 + 1);
                    System.out.println("received " + inboundEvent.getName() + " event: "
                            + inboundEvent.readData(SimpleMapEvent.class));
                    }
                };

            try
                {
                assertThat(source.isOpen(), is(true));
                return source;
                }
            catch (AssertionError e)
                {
                System.out.println("createEventSource() got an AssertionError: " + e);
                source.close();
                }

            Logger.info(String.format("Connection attempt %s failed.  Retrying in 10 seconds.", i + 1));
            try
                {
                Blocking.sleep(10000);
                }
            catch (InterruptedException e)
                {
                throw new RuntimeException(e);
                }
            }

        return null;
        }
    }
