/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.client.topics;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.coherence.grpc.GrpcService;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Session;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.topics.TopicLifecycleEvent;
import com.tangosol.net.events.topics.TopicLifecycleEventDispatcher;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;

public abstract class BaseLocalGrpcTests
    {
    public BaseLocalGrpcTests(String sSerializer)
        {
        f_sSerializer = sSerializer;
        }

    @BeforeClass
    public static void setup() throws Exception
        {
        String sCluster = testLogs.getTestClass().getSimpleName();

        System.setProperty("coherence.cluster", sCluster);
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.serializer", "pof");
        System.setProperty("coherence.cacheconfig", "topic-cache-config.xml");
//        System.setProperty(GrpcService.PROP_LOG_MESSAGES, "true");

        s_coherence = Coherence.clusterMember()
                .start()
                .get(5, TimeUnit.MINUTES);

        Session session = s_coherence.getSession();
        s_ccf = ((ConfigurableCacheFactorySession) session).getConfigurableCacheFactory();

        s_ccf.getInterceptorRegistry()
                .registerEventInterceptor(new TopicLifecycleInterceptor());

        s_client = LocalPlatform.get().launch(CoherenceClusterMember.class,
                WellKnownAddress.loopback(),
                ClusterName.of(sCluster),
                LocalHost.loopback(),
                SystemProperty.of("coherence.serializer", "pof"),
                SystemProperty.of("coherence.client", "grpc"),
                SystemProperty.of("coherence.cacheconfig", "grpc-topics-client-cache-config.xml"),
//                SystemProperty.of(GrpcService.PROP_LOG_MESSAGES, "true"),
                DisplayName.of("TopicClient"),
                testLogs);

        Eventually.assertDeferred(() -> s_client.isCoherenceRunning(), is(true));
        }

    // ----- helper methods -------------------------------------------------

    protected String getTopicName()
        {
        return getTopicName(f_sSerializer);
        }

    protected String getTopicName(String sSerializer)
        {
        return sSerializer + "-test-" + s_counter.incrementAndGet();
        }

    protected List<TopicLifecycleEvent> getLifecycleEvents(String sName)
        {
        return s_topicLifecycleEvents.getOrDefault(sName, Collections.emptyList());
        }

    // ----- TopicLifecycleInterceptor --------------------------------------

    protected static class TopicLifecycleInterceptor
            implements EventDispatcherAwareInterceptor<TopicLifecycleEvent>
        {
        @Override
        public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
            {
            if (dispatcher instanceof TopicLifecycleEventDispatcher)
                {
                dispatcher.addEventInterceptor(sIdentifier, this);
                }
            }

        @Override
        public void onEvent(TopicLifecycleEvent event)
            {
            String sName = event.getTopicName();
            s_topicLifecycleEvents.compute(sName, (key, events) ->
                {
                if (events == null)
                    {
                    events = new ArrayList<>();
                    }
                events.add(event);
                return events;
                });
            }
        }

    // ----- data members ---------------------------------------------------

    protected final String f_sSerializer;

    protected static Coherence s_coherence;

    protected static ConfigurableCacheFactory s_ccf;

    protected static CoherenceClusterMember s_client;

    protected static AtomicInteger s_counter = new AtomicInteger(0);

    @ClassRule
    public static final TestLogs testLogs = new TestLogs();

    protected static Map<String, List<TopicLifecycleEvent>> s_topicLifecycleEvents = new ConcurrentHashMap<>();
    }
