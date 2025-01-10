/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.proxy;

import com.tangosol.coherence.component.net.extend.proxy.serviceProxy.TopicServiceProxy;
import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.net.topic.ConverterNamedTopic;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Session;
import com.tangosol.net.TopicService;
import com.tangosol.net.topic.NamedTopic;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("resource")
public class TopicServiceProxyTests
    {
    @BeforeClass
    public static void setup() throws Exception
        {
        System.setProperty("coherence.cluster", "TopicServiceProxyTests");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.serializer", "pof");

        s_coherence = Coherence.clusterMember()
                .start()
                .get(5, TimeUnit.MINUTES);

        Session session = s_coherence.getSession();
        s_ccf = ((ConfigurableCacheFactorySession) session).getConfigurableCacheFactory();
        }

    @Test
    public void shouldEnsureTopicWithCompatibleSerializer()
        {
        TopicService      service = (TopicService) s_ccf.ensureService("PartitionedTopic");
        TopicServiceProxy proxy   = new TopicServiceProxy();
        proxy.setCacheFactory(s_ccf);
        proxy.setTopicService(service);
        proxy.setSerializer(service.getSerializer());
        proxy.setTopicBackingMapManager(service.getTopicBackingMapManager());

        String             sTopic = getTopicName();
        NamedTopic<String> topic  = proxy.ensureTopic(sTopic, null);
        assertThat(topic, is(not(instanceOf(ConverterNamedTopic.class))));

        int cChannel = topic.getChannelCount();
        assertThat(cChannel, is(17));
        }

    @Test
    public void shouldEnsureTopicWithIncompatibleSerializer()
        {
        Serializer        serializer = new DefaultSerializer();
        TopicService      service    = (TopicService) s_ccf.ensureService("PartitionedTopic");
        TopicServiceProxy proxy      = new TopicServiceProxy();
        proxy.setCacheFactory(s_ccf);
        proxy.setTopicService(service);
        proxy.setSerializer(serializer);
        proxy.setTopicBackingMapManager(service.getTopicBackingMapManager());

        String             sTopic = getTopicName();
        NamedTopic<String> topic  = proxy.ensureTopic(sTopic, null);
        assertThat(topic, is(instanceOf(ConverterNamedTopic.class)));

        int cChannel = topic.getChannelCount();
        assertThat(cChannel, is(17));
        }

    @Test
    public void shouldEnsureChannelCount()
        {
        String             sTopic  = getTopicName();
        NamedTopic<String> topic   = s_ccf.ensureTopic(sTopic, null);
        TopicService       service = topic.getTopicService();

        TopicServiceProxy proxy = new TopicServiceProxy();
        proxy.setCacheFactory(s_ccf);
        proxy.setTopicService(service);
        proxy.setSerializer(service.getSerializer());
        proxy.setTopicBackingMapManager(service.getTopicBackingMapManager());

        int cChannel = proxy.ensureChannelCount(sTopic, 25);
        assertThat(cChannel, is(25));
        assertThat(topic.getChannelCount(), is(25));
        }

    @Test
    public void shouldGetChannelCount()
        {
        String             sTopic  = getTopicName();
        NamedTopic<String> topic   = s_ccf.ensureTopic(sTopic, null);
        TopicService       service = topic.getTopicService();

        int cChannel = service.ensureChannelCount(sTopic, 33);
        assertThat(cChannel, is(33));

        TopicServiceProxy proxy = new TopicServiceProxy();
        proxy.setCacheFactory(s_ccf);
        proxy.setTopicService(service);
        proxy.setSerializer(service.getSerializer());
        proxy.setTopicBackingMapManager(service.getTopicBackingMapManager());

        assertThat(proxy.getChannelCount(sTopic), is(33));
        }

    // ----- helper methods -------------------------------------------------

    protected String getTopicName()
        {
        return "test-" + s_counter.incrementAndGet();
        }

    // ----- data members ---------------------------------------------------

    private static Coherence s_coherence;

    private static ConfigurableCacheFactory s_ccf;

    private static AtomicInteger s_counter = new AtomicInteger(0);
    }
