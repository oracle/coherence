/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.google.protobuf.Message;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.grpc.proxy.common.GrpcConnectionMetrics;
import com.oracle.coherence.grpc.proxy.common.GrpcMetricsInterceptor;
import com.oracle.coherence.grpc.proxy.common.GrpcProxyMetrics;
import com.oracle.coherence.grpc.proxy.common.GrpcProxyMetricsMBean;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.management.AnnotatedStandardMBean;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;
import com.tangosol.util.Filters;

import grpc.proxy.routeguide.Feature;
import grpc.proxy.routeguide.RouteGuideClient;
import grpc.proxy.routeguide.RouteGuideServer;
import grpc.proxy.routeguide.RouteGuideUtil;


import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ManagedChannelProvider;
import io.grpc.ServerBuilder;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test gRPC metrics.
 * <p>
 * This test uses classes from the gRPC metrics Route Guide
 * example as this is a simple service with all four request
 * types.
 *
 * @author Jonathan Knight  2020.10.16
 */
public class GrpcMetricsInterceptorIT
    {
    @BeforeAll
    static void setup() throws Exception
        {
        // Start DCS so that we have the Coherence management and MBean registry running
        DefaultCacheServer.startServerDaemon()
                .waitForServiceStart();

        URL           urlFeatures = RouteGuideUtil.getDefaultFeaturesFile();
        List<Feature> listFeature = RouteGuideUtil.parseFeatures(urlFeatures);
        int           port        = LocalPlatform.get().getAvailablePorts().next();

        s_metrics = new GrpcProxyMetrics("type=TestGrpcService", null);
        GrpcMetricsInterceptor s_interceptor = new GrpcMetricsInterceptor(s_metrics);

        Registry registry = CacheFactory.getCluster().getManagement();
        String   sName    = registry.ensureGlobalName(MBEAN_NAME);
        registry.register(sName, new AnnotatedStandardMBean(s_metrics, GrpcProxyMetricsMBean.class));

        try
            {
            ServerBuilder<?> serverBuilder = ServerBuilder.forPort(port).intercept(s_interceptor);

            s_server = new RouteGuideServer(serverBuilder, port, listFeature);
            s_server.start();

            s_channel = ManagedChannelBuilder.forAddress("localhost", port)
                    .usePlaintext()
                    .build();
            }
        catch (ManagedChannelProvider.ProviderNotFoundException e)
            {
            throw new TestAbortedException("Test aborted, cannot load gRPC provider", e);
            }
        }

    @AfterAll
    static void tearDown() throws InterruptedException
        {
        Registry         registry = CacheFactory.getCluster().getManagement();
        MBeanServerProxy proxy    = registry.getMBeanServerProxy();

        System.out.println();
        System.out.println("gRPC Metrics");
        dumpMBeanAttributes(proxy, MBEAN_NAME + ",*");
        System.out.println();
        System.out.println("gRPC Connection Metrics:");
        dumpMBeanAttributes(proxy, GrpcConnectionMetrics.MBEAN_TYPE + ",*");

        if (s_channel != null)
            {
            s_channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            }
        if (s_server != null)
            {
            s_server.stop();
            }
        }

    private static void dumpMBeanAttributes(MBeanServerProxy proxy, String sPattern)
        {
        Set<String> setNames = proxy.queryNames(sPattern, Filters.always());

        for (String sName : setNames)
            {
            Map<String, Object> map = proxy.getAttributes(sName, Filters.always());
            new TreeMap<>(map).forEach((k, v) -> System.out.println(k + " " + v));
            }
        }

    @Test
    public void shouldCallUnary() throws Exception
        {
        RouteGuideClient client      = new RouteGuideClient(s_channel);
        long             cReqBefore  = s_metrics.getSuccessfulRequestCount();
        long             cMsgBefore  = s_metrics.getMessagesReceivedCount();
        long             cRespBefore = s_metrics.getResponsesSentCount();

        // Unary method - Looking for a valid feature
        client.getFeature(409146138, -746188906);

        Eventually.assertDeferred(() -> s_metrics.getSuccessfulRequestCount(), is(cReqBefore + 1));
        Eventually.assertDeferred(() -> s_metrics.getMessagesReceivedCount(), is(cMsgBefore + 1));
        Eventually.assertDeferred(() -> s_metrics.getResponsesSentCount(), is(cRespBefore + 1));
        }

    @Test
    public void shouldServerStreaming() throws Exception
        {
        RouteGuideClient client      = new RouteGuideClient(s_channel);
        long             cReqBefore  = s_metrics.getSuccessfulRequestCount();
        long             cMsgBefore  = s_metrics.getMessagesReceivedCount();
        long             cRespBefore = s_metrics.getResponsesSentCount();
        MessageCounter   counter     = new MessageCounter();

        client.setTestHelper(counter);

        // Server Streaming method - Looking for features between 40, -75 and 42, -73.
        client.listFeatures(400000000, -750000000, 420000000, -730000000);

        Eventually.assertDeferred(() -> s_metrics.getSuccessfulRequestCount(), is(cReqBefore + 1));
        Eventually.assertDeferred(() -> s_metrics.getMessagesReceivedCount(), is(cMsgBefore + 1));
        Eventually.assertDeferred(() -> s_metrics.getResponsesSentCount(), is(cRespBefore + counter.getCount()));
        }

    @Test
    public void shouldClientStreaming() throws Exception
        {
        RouteGuideClient client      = new RouteGuideClient(s_channel);
        long             cReqBefore  = s_metrics.getSuccessfulRequestCount();
        long             cMsgBefore  = s_metrics.getMessagesReceivedCount();
        long             cRespBefore = s_metrics.getResponsesSentCount();
        MessageCounter   counter     = new MessageCounter();

        // Client Streaming method - Record a few randomly selected points from the features file.
        List<Feature> features = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        int           cPoints  = 10;

        client.setTestHelper(counter);
        client.recordRoute(features, cPoints);

        Eventually.assertDeferred(() -> s_metrics.getSuccessfulRequestCount(), is(cReqBefore + 1));
        Eventually.assertDeferred(() -> s_metrics.getMessagesReceivedCount(), is(cMsgBefore + cPoints));
        Eventually.assertDeferred(() -> s_metrics.getResponsesSentCount(), is(cRespBefore + counter.getCount()));
        }

    @Test
    public void shouldCallBidirectionalStreaming() throws Exception
        {
        RouteGuideClient client      = new RouteGuideClient(s_channel);
        long             cReqBefore  = s_metrics.getSuccessfulRequestCount();
        long             cMsgBefore  = s_metrics.getMessagesReceivedCount();
        long             cRespBefore = s_metrics.getResponsesSentCount();
        MessageCounter   counter     = new MessageCounter();

        // Bidirectional Streaming method - Send and receive some notes.
        client.setTestHelper(counter);
        CountDownLatch chatOne = client.routeChat();
        assertThat(chatOne.await(5, TimeUnit.MINUTES), is(true));
        CountDownLatch chatTwo = client.routeChat();
        assertThat(chatTwo.await(5, TimeUnit.MINUTES), is(true));

        Eventually.assertDeferred(() -> s_metrics.getSuccessfulRequestCount(), is(cReqBefore + 2));
        Eventually.assertDeferred(() -> s_metrics.getMessagesReceivedCount(), is(cMsgBefore + 8));
        Eventually.assertDeferred(() -> s_metrics.getResponsesSentCount(), is(cRespBefore + 4));
        }


    public static class MessageCounter
            implements RouteGuideClient.TestHelper
        {
        int getCount()
            {
            return f_cMessages.intValue();
            }

        @Override
        public void onMessage(Message message)
            {
            f_cMessages.increment();
            }

        @Override
        public void onRpcError(Throwable exception)
            {
            }

        private final LongAdder f_cMessages = new LongAdder();
        }

    public static final String MBEAN_NAME = "type=TestMetrics";

    private static GrpcProxyMetrics s_metrics;
    private static RouteGuideServer s_server;
    private static ManagedChannel s_channel;
    }

