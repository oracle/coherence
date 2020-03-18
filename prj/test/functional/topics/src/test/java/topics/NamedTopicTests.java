/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.junit.CoherenceClusterOrchestration;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Base;

import org.junit.Assume;
import org.junit.ClassRule;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import java.util.concurrent.CompletableFuture;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.tangosol.net.topic.Subscriber.Name.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author jk 2015.05.28
 */
@RunWith(Parameterized.class)
public class NamedTopicTests
        extends AbstractNamedTopicTests
    {
    // ----- constructors ---------------------------------------------------

    public NamedTopicTests(String sSerializer, boolean fExtend)
        {
        super(sSerializer);

        m_fExtend = fExtend;
        }

    // ----- test lifecycle methods -----------------------------------------

    @Parameterized.Parameters(name = "serializer={0} extend={1}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]
            {
            {"pof", false}, {"java", false}

            // disable extend testing since not currently supported
            //, {"pof", true}, {"java", true}
            });
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldDestroy() throws Exception
        {
        NamedTopic<Object> topic = getSession().getTopic(m_sSerializer + "-topic-foo");

        Subscriber<Object> foo = topic.createSubscriber(Subscriber.Name.of("foo"));

        topic.createPublisher().send("value-1");

        CompletableFuture<Subscriber.Element<Object>> future1 = foo.receive();
        CompletableFuture<Subscriber.Element<Object>> future2 = foo.receive();

        Thread.sleep(5000);

        topic.destroySubscriberGroup("foo");

        future1.handle((v, err) ->
            {
            System.err.println("**** (1) value is " + v);
            if (err != null)
                {
                err.printStackTrace();
                }

            return null;
            });

        future2.handle((v, err) ->
            {
            System.err.println("**** (2) value is " + v);
            if (err != null)
                {
                err.printStackTrace();
                }

            return null;
            });

        System.err.println("***** sleeping...");
        Thread.sleep(60000);
        }


    //@Test
    public void shouldConsumeAfterDisconnection() throws Exception
        {
        Assume.assumeThat("Test only applies to Extend tests", m_fExtend, is(true));

        NamedTopic<String> queue    = ensureTopic(m_sSerializer + "-Extend");
        Publisher<String> publisher = queue.createPublisher();

        for (int i=0; i<1000; i++)
            {
            publisher.send("Element-" + i);
            }

        bounceProxy();

        try (Subscriber<String> subscriber = queue.createSubscriber(of("Foo")))
            {
            for (int i = 0; i < 100; i++)
                {
                assertThat(subscriber.receive().get().getValue(), is("Element-" + i));
                }
            }

        }

    // ----- helper methods -------------------------------------------------

    public void bounceProxy()
            throws Exception
        {
        CoherenceCluster       cluster      = orchestration.getCluster();
        CoherenceClusterMember memberProxy  = cluster.get("proxy-1");
        String                 sServiceName = Character.toUpperCase(m_sSerializer.charAt(0))
                + m_sSerializer.substring(1, m_sSerializer.length()) + "Proxy";


        memberProxy.submit(new ProxyBounce(sServiceName));
        Eventually.assertThat(invoking(memberProxy).isServiceRunning(sServiceName), is(true));
        Thread.sleep(2000);
        }

    @Override
    protected Session getSession()
        {
        return new ConfigurableCacheFactorySession(getECCF(), Base.getContextClassLoader());
        }

    protected ExtensibleConfigurableCacheFactory getECCF()
        {
        if (m_fExtend)
            {
            return (ExtensibleConfigurableCacheFactory) orchestration.getSessionFor(
                    SessionBuilders.extendClient("client-cache-config.xml"));
            }

        return (ExtensibleConfigurableCacheFactory) orchestration
            .getSessionFor(SessionBuilders.storageDisabledMember());
        }

    @Override
    protected void runInCluster(RemoteRunnable runnable)
        {
        orchestration.getCluster().forEach((member) -> member.submit(runnable));
        }

    @Override
    protected int getStorageMemberCount()
        {
        return STORAGE_MEMBER_COUNT;
        }

    @Override
    protected String getCoherenceCacheConfig()
        {
        return CACHE_CONFIG_FILE;
        }

    // ----- inner class: ProxyBounce -------------------------------------

    public static class ProxyBounce
            implements RemoteRunnable
        {
        public ProxyBounce(String sServiceName)
            {
            m_sServiceName = sServiceName;
            }

        @Override
        public void run()
            {
            try
                {
                Service service = CacheFactory.getService(m_sServiceName);

                ((SafeService) service).getService().shutdown();

                service = CacheFactory.getService(m_sServiceName);
                Eventually.assertThat(service.isRunning(), is(true));
                }
            catch (Throwable t)
                {
                t.printStackTrace();
                }

            }

        private String m_sServiceName;
        }

    // ----- constants ------------------------------------------------------

    public static final int STORAGE_MEMBER_COUNT = 2;

    public static final String CACHE_CONFIG_FILE = "topic-cache-config.xml";

    @ClassRule
    public static CoherenceClusterOrchestration orchestration =
        new CoherenceClusterOrchestration()
                .withOptions(ClusterName.of("TopicTests"),
                    CacheConfig.of(CACHE_CONFIG_FILE),
                        SystemProperty.of("coherence.topic.publisher.close.timeout", "2s"),
                        SystemProperty.of("coherence.management", "all"),
                        SystemProperty.of("coherence.management.remote", "true"),
                        SystemProperty.of("coherence.management.refresh.expiry", "1ms"))
                .setStorageMemberCount(STORAGE_MEMBER_COUNT);

    private boolean m_fExtend = false;
    }
