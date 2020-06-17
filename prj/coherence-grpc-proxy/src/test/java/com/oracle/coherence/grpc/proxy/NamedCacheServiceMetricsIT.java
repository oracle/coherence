/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.ByteString;

import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.EntrySetRequest;
import com.oracle.coherence.grpc.InvokeAllRequest;
import com.oracle.coherence.grpc.KeySetRequest;
import com.oracle.coherence.grpc.MapListenerRequest;
import com.oracle.coherence.grpc.PageRequest;
import com.oracle.coherence.grpc.Requests;
import com.oracle.coherence.grpc.ValuesRequest;

import com.tangosol.internal.util.processor.BinaryProcessors;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.util.Extractors;
import com.tangosol.util.Filters;

import com.tangosol.util.aggregator.Count;

import io.grpc.stub.StreamObserver;

import io.helidon.microprofile.grpc.client.GrpcProxy;

import io.helidon.microprofile.grpc.core.InProcessGrpcChannel;

import io.helidon.microprofile.server.Server;

import java.io.InputStream;

import java.util.Collections;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.Instance;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.logging.LoggingFeature;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration tests for {@link com.oracle.coherence.grpc.proxy.NamedCacheService}
 * that assert metrics are created for the various service methods.
 *
 * @author Jonathan Knight  2020.01.07
 * @since 20.06
 */
class NamedCacheServiceMetricsIT
    {
    // ----- test lifecycle methods -----------------------------------------

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl",        "0");
        System.setProperty("coherence.cluster",    "NamedCacheServiceIT");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.override",   "test-coherence-override.xml");

        Server server = Server.create();
        server.start();
        s_nHttpPort = server.port();

        s_client = ClientBuilder.newBuilder()
                .register(new LoggingFeature(LOGGER, Level.WARNING, LoggingFeature.Verbosity.PAYLOAD_ANY,
                                             500))
                .property(ClientProperties.FOLLOW_REDIRECTS, true)
                .build();

        s_serializer = new ConfigurablePofContext();
        s_service    = createService();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldHaveAddIndexMetrics() throws Exception
        {
        String sMetricName = "addIndex";
        int    cBefore     = getMetric(sMetricName);

        s_service.addIndex(Requests.addIndex(Scope.DEFAULT, CACHE_NAME,
                                             SERIALIZER_NAME,
                                             BinaryHelper.toByteString(Extractors.identity(), s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveAggregateMetrics() throws Exception
        {
        String sMetricName = "aggregate";
        int    cBefore     = getMetric(sMetricName);

        s_service.aggregate(Requests.aggregate(Scope.DEFAULT, CACHE_NAME,
                                               SERIALIZER_NAME,
                                               BinaryHelper.toByteString(Filters.equal(Extractors.identity(),
                                                                                       100), s_serializer),
                                               BinaryHelper.toByteString(new Count<>(), s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveClearMetrics() throws Exception
        {
        String sMetricName = "clear";
        int    cBefore     = getMetric(sMetricName);

        s_service.clear(Requests.clear(Scope.DEFAULT, CACHE_NAME));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveContainsEntryMetrics() throws Exception
        {
        String sMetricName = "containsEntry";
        int    cBefore     = getMetric(sMetricName);

        s_service.containsEntry(Requests.containsEntry(Scope.DEFAULT, CACHE_NAME,
                                                       SERIALIZER_NAME,
                                                       BinaryHelper.toByteString("foo", s_serializer),
                                                       BinaryHelper.toByteString("bar", s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveContainsKeyMetrics() throws Exception
        {
        String sMetricName = "containsKey";
        int    cBefore     = getMetric(sMetricName);

        s_service.containsKey(Requests.containsKey(Scope.DEFAULT, CACHE_NAME,
                                                   SERIALIZER_NAME,
                                                   BinaryHelper.toByteString("foo", s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveContainsValueMetrics() throws Exception
        {
        String sMetricName = "containsValue";
        int    cBefore     = getMetric(sMetricName);

        s_service.containsValue(Requests.containsValue(Scope.DEFAULT, CACHE_NAME,
                                                       SERIALIZER_NAME,
                                                       BinaryHelper.toByteString("foo", s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveDestroyMetrics() throws Exception
        {
        String sMetricName = "destroy";
        int    cBefore     = getMetric(sMetricName);

        s_service.destroy(Requests.destroy(Scope.DEFAULT, CACHE_NAME));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveEntrySetMetrics() throws Exception
        {
        String sMetricName = "entrySet";
        int    cBefore     = getMetric(sMetricName);

        EntrySetRequest request = Requests.entrySet(Scope.DEFAULT, CACHE_NAME,
                                                    SERIALIZER_NAME,
                                                    BinaryHelper.toByteString(Filters.equal(Extractors.identity(),
                                                                                            100), s_serializer));

        s_service.entrySet(request, NullStreamObserver.instance());

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveEventsMetrics() throws Exception
        {
        String sMetricName = "events";
        int    cBefore     = getMetric(sMetricName);

        StreamObserver<MapListenerRequest> response = s_service.events(NullStreamObserver.instance());
        response.onCompleted();

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveGetMetrics() throws Exception
        {
        String sMetricName = "get";
        int    cBefore     = getMetric(sMetricName);

        s_service.get(Requests.get(Scope.DEFAULT, CACHE_NAME, SERIALIZER_NAME, BinaryHelper.toByteString("foo", s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveGetAllMetrics() throws Exception
        {
        String sMetricName = "getAll";
        int    cBefore     = getMetric(sMetricName);

        Iterable<ByteString> colKeys = Collections.singletonList(BinaryHelper.toByteString("foo", s_serializer));
        s_service.getAll(Requests.getAll(Scope.DEFAULT, CACHE_NAME, SERIALIZER_NAME, colKeys), NullStreamObserver.instance());

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveInvokeMetrics() throws Exception
        {
        String sMetricName = "invoke";
        int    cBefore     = getMetric(sMetricName);

        s_service.invoke(Requests.invoke(Scope.DEFAULT, CACHE_NAME,
                                         SERIALIZER_NAME,
                                         BinaryHelper.toByteString("foo", s_serializer),
                                         BinaryHelper.toByteString(BinaryProcessors.get(), s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveInvokeAllMetrics() throws Exception
        {
        String sMetricName = "invokeAll";
        int    cBefore     = getMetric(sMetricName);

        Iterable<ByteString> colKeys = Collections.singletonList(BinaryHelper.toByteString("foo", s_serializer));
        InvokeAllRequest     request = Requests.invokeAll(Scope.DEFAULT, CACHE_NAME,
                                                          SERIALIZER_NAME,
                                                          colKeys,
                                                          BinaryHelper.toByteString(BinaryProcessors.get(), s_serializer));

        s_service.invokeAll(request, NullStreamObserver.instance());

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveIsEmptyMetrics() throws Exception
        {
        String sMetricName = "isEmpty";
        int    cBefore     = getMetric(sMetricName);

        s_service.isEmpty(Requests.isEmpty(Scope.DEFAULT, CACHE_NAME));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveKeySetMetrics() throws Exception
        {
        String sMetricName = "keySet";
        int    cBefore     = getMetric(sMetricName);

        KeySetRequest request = Requests.keySet(Scope.DEFAULT, CACHE_NAME,
                                                SERIALIZER_NAME,
                                                BinaryHelper.toByteString(Filters.equal(Extractors.identity(),
                                                                                        100), s_serializer));

        s_service.keySet(request, NullStreamObserver.instance());

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveNextEntrySetPageMetrics() throws Exception
        {
        String sMetricName = "nextEntrySetPage";
        int    cBefore     = getMetric(sMetricName);

        PageRequest request = Requests.page(Scope.DEFAULT, CACHE_NAME,
                                            SERIALIZER_NAME,
                                            ByteString.EMPTY);

        s_service.nextEntrySetPage(request, NullStreamObserver.instance());

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveNextKeySetPageMetrics() throws Exception
        {
        String sMetricName = "nextKeySetPage";
        int    cBefore     = getMetric(sMetricName);

        PageRequest request = Requests.page(Scope.DEFAULT, CACHE_NAME,
                                            SERIALIZER_NAME,
                                            ByteString.EMPTY);

        s_service.nextKeySetPage(request, NullStreamObserver.instance());

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHavePutMetrics() throws Exception
        {
        String sMetricName = "put";
        int    cBefore     = getMetric(sMetricName);

        s_service.put(Requests.put(Scope.DEFAULT, CACHE_NAME,
                                   SERIALIZER_NAME,
                                   BinaryHelper.toByteString("foo", s_serializer),
                                   BinaryHelper.toByteString("bar", s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHavePutAllMetrics() throws Exception
        {
        String sMetricName = "putAll";
        int    cBefore     = getMetric(sMetricName);

        s_service.putAll(Requests.putAll(Scope.DEFAULT, CACHE_NAME, SERIALIZER_NAME, Collections.emptyList()));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHavePutIfAbsentMetrics() throws Exception
        {
        String sMetricName = "putIfAbsent";
        int    cBefore     = getMetric(sMetricName);

        s_service.putIfAbsent(Requests.putIfAbsent(Scope.DEFAULT, CACHE_NAME,
                                                   SERIALIZER_NAME,
                                                   BinaryHelper.toByteString("foo", s_serializer),
                                                   BinaryHelper.toByteString("bar", s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveRemoveMetrics() throws Exception
        {
        String sMetricName = "remove";
        int    cBefore     = getMetric(sMetricName);

        s_service.remove(Requests.remove(Scope.DEFAULT, CACHE_NAME,
                                         SERIALIZER_NAME,
                                         BinaryHelper.toByteString("foo", s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveRemoveIndexMetrics() throws Exception
        {
        String sMetricName = "removeIndex";
        int    cBefore     = getMetric(sMetricName);

        s_service.removeIndex(Requests.removeIndex(Scope.DEFAULT, CACHE_NAME,
                                                   SERIALIZER_NAME,
                                                   BinaryHelper.toByteString(Extractors.identity(), s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveRemoveMappingMetrics() throws Exception
        {
        String sMetricName = "removeMapping";
        int    cBefore     = getMetric(sMetricName);

        s_service.removeMapping(Requests.remove(Scope.DEFAULT, CACHE_NAME,
                                                SERIALIZER_NAME,
                                                BinaryHelper.toByteString("foo", s_serializer),
                                                BinaryHelper.toByteString("bar", s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveReplaceMetrics() throws Exception
        {
        String sMetricName = "replace";
        int    cBefore     = getMetric(sMetricName);

        s_service.replace(Requests.replace(Scope.DEFAULT, CACHE_NAME,
                                           SERIALIZER_NAME,
                                           BinaryHelper.toByteString("foo", s_serializer),
                                           BinaryHelper.toByteString("bar", s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveReplaceMappingMetrics() throws Exception
        {
        String sMetricName = "replaceMapping";
        int    cBefore     = getMetric(sMetricName);

        s_service.replaceMapping(Requests.replace(Scope.DEFAULT, CACHE_NAME,
                                                  SERIALIZER_NAME,
                                                  BinaryHelper.toByteString("foo", s_serializer),
                                                  BinaryHelper.toByteString("old", s_serializer),
                                                  BinaryHelper.toByteString("new", s_serializer)));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveSizeMetrics() throws Exception
        {
        String sMetricName = "size";
        int    cBefore     = getMetric(sMetricName);

        s_service.size(Requests.size(Scope.DEFAULT, CACHE_NAME));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveTruncateMetrics() throws Exception
        {
        String sMetricName = "truncate";
        int    cBefore     = getMetric(sMetricName);

        s_service.truncate(Requests.truncate(Scope.DEFAULT, CACHE_NAME));

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    @Test
    public void shouldHaveValuesMetrics() throws Exception
        {
        String sMetricName = "values";
        int    cBefore     = getMetric(sMetricName);

        ValuesRequest request = Requests.values(Scope.DEFAULT, CACHE_NAME,
                                                SERIALIZER_NAME,
                                                BinaryHelper.toByteString(Filters.equal(Extractors.identity(),
                                                                                        100), s_serializer));

        s_service.values(request, NullStreamObserver.instance());

        eventuallyAssertMetric(sMetricName, cBefore);
        }

    // ----- helper methods -------------------------------------------------

    protected static NamedCacheClient createService()
        {
        BeanManager                beanManager = CDI.current().getBeanManager();
        Instance<NamedCacheClient> instance    = beanManager.createInstance()
                .select(NamedCacheClient.class, GrpcProxy.Literal.INSTANCE, InProcessGrpcChannel.Literal.INSTANCE);

        assertThat(instance.isResolvable(), is(true));
        return instance.get();
        }

    protected void eventuallyAssertMetric(String sMetricName, int beforeCount) throws Exception
        {
        int  cExpected = beforeCount + 1;
        long ldtTime   = 0L;
        long ldtStart  = System.currentTimeMillis();
        int  cCount    = 0;

        while (ldtTime < 10000L)
            {
            cCount = getMetric(sMetricName);
            if (cCount == cExpected)
                {
                break;
                }
            Thread.sleep(100L);
            ldtTime = System.currentTimeMillis() - ldtStart;
            }

        assertThat(cCount, is(cExpected));
        }

    protected int getMetric(String sMetricName)
        {
        Response response = queryMetric(sMetricName);

        if (response.getStatus() == 200)
            {
            JsonObject json = (JsonObject) Json.createReader((InputStream) response.getEntity()).read();
            JsonObject jsonMetric = json.getJsonObject(METRIC_PREFIX + sMetricName);
            String sKey = jsonMetric.keySet().stream()
                    .filter(k -> k.startsWith("count"))
                    .findFirst()
                    .orElse(null);
            if (sKey != null)
                {
                JsonNumber jsonNumber = jsonMetric.getJsonNumber(sKey);
                return jsonNumber == null ? 0 : jsonNumber.intValue();
                }
            }

        return 0;
        }

    protected Response queryMetric(String sName)
        {
        return s_client.target("http://localhost:" + s_nHttpPort)
                .path("metrics/application/" + METRIC_PREFIX + sName)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        }

    // ----- constants ------------------------------------------------------

    protected static final String METRIC_PREFIX = NamedCacheService.class.getName() + '.';

    protected static final Logger LOGGER = Logger.getLogger(NamedCacheServiceMetricsIT.class.getName());

    protected static final String CACHE_NAME = "test-cache";

    protected static final String SERIALIZER_NAME = "pof";

    // ----- data members ---------------------------------------------------

    protected static Serializer s_serializer;

    protected static Client s_client;

    protected static int s_nHttpPort;

    protected static NamedCacheClient s_service;
    }
