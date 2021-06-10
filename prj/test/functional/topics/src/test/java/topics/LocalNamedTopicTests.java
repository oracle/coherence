/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

import com.tangosol.coherence.config.scheme.BackingMapScheme;
import com.tangosol.coherence.config.scheme.ClusteredCachingScheme;
import com.tangosol.coherence.config.scheme.PagedTopicScheme;
import com.tangosol.coherence.config.scheme.FlashJournalScheme;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.coherence.config.scheme.RamJournalScheme;

import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.model.ContentKey;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;
import com.tangosol.net.partition.ObservableSplittingBackingCache;

import com.tangosol.net.topic.Subscriber.CompleteOnEmpty;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.Resources;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import common.SystemPropertyIsolation;

import org.hamcrest.Matchers;

import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;
import static com.tangosol.net.topic.Subscriber.Name.of;

import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk 2015.06.30
 */
@SuppressWarnings("unchecked")
@RunWith(Parameterized.class)
public class LocalNamedTopicTests
        extends AbstractNamedTopicTests
    {
    // ----- constructors ---------------------------------------------------

    public LocalNamedTopicTests(String sSerializer)
        {
        super(sSerializer);
        }

    // ----- test lifecycle methods -----------------------------------------

    /**
     *
     */
    @BeforeClass
    public static void setup() throws Exception
        {
        System.setProperty("coherence.topic.publisher.close.timeout", "2s");

        String      sHost  = LocalPlatform.get().getLoopbackAddress().getHostAddress();
        ClassLoader loader = Base.getContextClassLoader();

        System.setProperty("coherence.localhost", sHost);

        URL urlCacheConfig = Resources.findFileOrResource("topic-cache-config.xml", LocalNamedTopicTests.class.getClassLoader());

        if (urlCacheConfig == null)
            {
            throw new IllegalStateException("unable to find cache config file: " + "topic-cache-config.xml");
            }

        XmlElement xml = XmlHelper.loadXml(urlCacheConfig.openStream());

        CacheFactory.getCacheFactoryBuilder().setCacheConfiguration("topic-cache-config.xml", loader, xml);
        ExtensibleConfigurableCacheFactory.Dependencies deps =
            ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("topic-cache-config.xml");

        ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(deps);

        CacheFactory.getCacheFactoryBuilder().setConfigurableCacheFactory(eccf, "$Default$", loader, true);
        DefaultCacheServer.start(eccf);

        m_session = new ConfigurableCacheFactorySession(eccf, loader);
        }


    @Parameterized.Parameters(name = "serializer={0}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]
            {
            {"pof"}, {"java"}
            });
        }

    @After
    public void cleanupTests()
        {
        if (m_topic != null)
            {
            unregisterErrorInterceptor(m_topic);
            }
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testSubscriberIsActive()
        {
        NamedTopic<String>  topic    = ensureTopic();

        Subscriber<String> subscriber = topic.createSubscriber();
        assertThat(subscriber.isActive(), is(true));
        subscriber.close();
        assertThat(subscriber.isActive(), is(false));
        }


    @Test(expected = IllegalStateException.class)
    public void testClosedSubscriberReceive()
        {
        NamedTopic<String>  topic    = ensureTopic();

        Subscriber<String> subscriber = topic.createSubscriber(CompleteOnEmpty.enabled());
        subscriber.close();
        assertThat(subscriber.isActive(), is(false));
        subscriber.receive();
        }

    @Test(expected = IllegalStateException.class)
    public void testClosedPublisherSend()
        {
        NamedTopic<String>  topic    = ensureTopic();

        Publisher<String> publisher = topic.createPublisher();
        publisher.close();
        publisher.publish("never sent");
        }

    @Test
    public void shouldHandleExceptionSubscriberOnClose()
        {
        final AtomicBoolean fRan = new AtomicBoolean(false);

        NamedTopic<String> topic      = ensureTopic();
        Subscriber<String> subscriber = topic.createSubscriber();

        subscriber.onClose(() -> {fRan.set(true); throw new IllegalStateException("validate onClose throwing unhandled exception is handled");});
        subscriber.close();
        assertThat(fRan.get(), is(true));
        }

    @Test
    public void shouldHandleExceptionPublisherOnClose()
        {
        final AtomicBoolean fRan = new AtomicBoolean(false);

        NamedTopic<String> topic     = ensureTopic();
        Publisher<String>  publisher = topic.createPublisher();

        publisher.onClose(() -> {fRan.set(true); throw new IllegalStateException("validate onClose throwing unhandled exception is handled");});
        publisher.close();
        assertThat(fRan.get(), is(true));
        }

    @Test
    public void shouldHandleErrorWhenPublishing() throws Exception
        {
        Assume.assumeThat(m_sSerializer, is("pof"));

        NamedTopic<String>     topic    = ensureTopic();
        int                    cValues  = 20;
        int                    nError   = 1;
        CompletableFuture<?>[] aFutures = new CompletableFuture[cValues];

        final AtomicBoolean fPublisherClosed  = new AtomicBoolean(false);

        // introduce an error after first asynchronous send succeeds
        addErrorInterceptor(topic, nError);

        String sPrefix = "Element-";

        try (Publisher<String>  publisher = topic.createPublisher(Publisher.OrderBy.id(0));
             Subscriber<String> subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled()))
            {
            publisher.onClose(() -> fPublisherClosed.set(true));

            for (int i=0; i<cValues && !fPublisherClosed.get(); i++)
                {
                try
                    {
                    // validate that exception introduced by addErrorInterceptor call is not thrown at site of async send call
                    aFutures[i] = publisher.publish(sPrefix + i);
                    }
                catch (IllegalStateException e)
                    {
                    // ignore exception This publisher is no longer active
                    // all it means is that second asynchronous send occurred before loop completed and resulted in publisher closing
                    assertThat("should not fail on first publish", i, Matchers.greaterThan(0));
                    assertThat(e.getMessage().contains("This publisher is no longer active"), is(true));
                    System.err.println("handled IllegalStateException: " + e.getMessage());
                    System.err.println("Publisher Closed: " + fPublisherClosed.get());
                    break;
                    }
                }

            CompletableFuture<Void> futureFlush = publisher.flush();
            futureFlush.join();

            // topic should contain just the first value
            assertThat(subscriber.receive().get(10, TimeUnit.MINUTES).getValue(), is(sPrefix + 0));
            assertThat(subscriber.receive().get(10, TimeUnit.MINUTES) == null, is(true));
            }

        // First add completes as normal
        assertCompletedNormally(aFutures[0]);

        // All other adds should have not occurred at all or be cancelled
        for (int i=1; i<cValues; i++)
            {
            if (aFutures[i] != null)
                {
                assertCompletedExceptionally(aFutures[i]);
                }
            }
        }


    private void assertCompletedNormally(CompletableFuture<?> future)
        {
        assertThat(future.isDone(), is(true));
        assertThat(future.isCompletedExceptionally(), is(false));
        assertThat(future.isCancelled(), is(false));
        }

    private void assertCancelled(CompletableFuture<?> future)
        {
        assertThat(future.isDone(), is(true));
        assertThat(future.isCompletedExceptionally(), is(true));
        assertThat(future.isCancelled(), is(true));
        }

    private void assertCompletedExceptionally(CompletableFuture<?> future)
        {
        assertThat(future.isDone(), is(true));
        assertThat(future.isCompletedExceptionally(), is(true));
        assertThat(future.isCancelled(), is(false));
        }

    @Test
    public void shouldCleanUpSubscribers()
        {
        Assume.assumeThat(m_sSerializer, is("pof"));

        NamedTopic<String> topic = ensureTopic();

        Subscriber<String> subscriber1 = topic.createSubscriber(of("Foo"));
        Subscriber<String> subscriber2 = topic.createSubscriber(of("Foo"));
        Subscriber<String> subscriber3 = topic.createSubscriber();

        subscriber1.close();
        subscriber2.close();
        subscriber3.close();

        topic.destroySubscriberGroup("Foo");

        CacheService     service = (CacheService) topic.getService();
        NamedCache<?, ?> cacheSubscribers = service.ensureCache(PagedTopicCaches.Names.SUBSCRIPTIONS.cacheNameForTopicName(m_sSerializer), null);

        Eventually.assertThat(cacheSubscribers.getCacheName() + " should be empty",
                              invoking(cacheSubscribers).isEmpty(), is(true));
        }

    @Test
    public void shouldUseLocalScheme()
        {
        ensureTopic();

        Session session = getSession();

        PagedTopicScheme scheme = (PagedTopicScheme)
                ((ExtensibleConfigurableCacheFactory)CacheFactory.getConfigurableCacheFactory()).getCacheConfig().findSchemeByTopicName(m_sSerializer);
        assertThat(scheme.getStorageScheme().getClass().getName(), is(LocalScheme.class.getName()));


        for (PagedTopicCaches.Names<?, ?> names : PagedTopicCaches.Names.values())
            {
            String              cacheName = names.cacheNameForTopicName(m_sSerializer);
            NamedCache<?, ?>    cache     = session.getCache(cacheName, withoutTypeChecking());
            BackingMapManager   manager   = cache.getCacheService().getBackingMapManager();
            BackingMapContext   context   = manager.getContext().getBackingMapContext(cacheName);
            ObservableMap<?, ?> map       = context.getBackingMap();

            assertThat(map.getClass().getCanonicalName(), is(LocalCache.class.getCanonicalName()));
            }
        }

    // ----- helper methods -------------------------------------------------


    @Override
    protected Session getSession()
        {
        return m_session;
        }

    @Override
    protected void runInCluster(RemoteRunnable runnable)
        {
        runnable.run();
        }

    @Override
    protected int getStorageMemberCount()
        {
        return 1;
        }

    @Override
    protected String getCoherenceCacheConfig()
        {
        return CACHE_CONFIG_FILE;
        }

    public void addErrorInterceptor(NamedTopic<?> topic, int cMax)
        {
        String              sTopicName = topic.getName();
        String              sCacheName = PagedTopicCaches.Names.CONTENT.cacheNameForTopicName(sTopicName);
        NamedCache<?, ?>    cache      = getSession().getCache(sCacheName, withoutTypeChecking());
        CacheService        service    = cache.getCacheService();
        InterceptorRegistry registry   = service.getBackingMapManager().getCacheFactory().getInterceptorRegistry();

        registry.registerEventInterceptor("ErrorTests",
                new ErrorCausingInterceptor(sCacheName, cMax), RegistrationBehavior.IGNORE);
        }

    public void unregisterErrorInterceptor(NamedTopic<?> topic)
        {
        String              sTopicName = topic.getName();
        String              sCacheName = PagedTopicCaches.Names.CONTENT.cacheNameForTopicName(sTopicName);
        NamedCache<?, ?>    cache      = getSession().getCache(sCacheName, withoutTypeChecking());
        CacheService        service    = cache.getCacheService();
        InterceptorRegistry registry   = service.getBackingMapManager().getCacheFactory().getInterceptorRegistry();

        registry.unregisterEventInterceptor("ErrorTests");
        }

    // ----- inner class: ErrorCausingInterceptor ---------------------------

    /**
     * This interceptor will allow at most a set number of elements
     * to be added to the Elements cache for a topic but only once
     * so if the same operation is retried it will succeed
     */
    public static class ErrorCausingInterceptor
            implements EventDispatcherAwareInterceptor<EntryEvent<ContentKey,?>>
        {
        public ErrorCausingInterceptor(String sCacheName, int cMax)
            {
            m_sCacheName = sCacheName;
            m_cMax       = cMax;
            }

        @Override
        public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
            {
            if (dispatcher instanceof PartitionedCacheDispatcher)
                {
                String sName = ((PartitionedCacheDispatcher) dispatcher).getBackingMapContext().getCacheName();

                if (sName.equals(m_sCacheName))
                    {
                    dispatcher.addEventInterceptor(sIdentifier, this,
                            Collections.singleton(EntryEvent.Type.INSERTING), false);
                    }
                }
            }

        @Override
        public void onEvent(EntryEvent<ContentKey,?> event)
            {
            Set<? extends BinaryEntry<ContentKey, ?>> entries = event.getEntrySet();

            for (BinaryEntry<ContentKey, ?> entry : entries)
                {
                if (ContentKey.fromBinary(entry.getBinaryKey(), true).getElement() >= m_cMax && m_fDone.compareAndSet(false, true))
                    {
                    throw new RuntimeException("No!!!");
                    }
                }
            }

        private final String m_sCacheName;

        private final int m_cMax;

        private final AtomicBoolean m_fDone = new AtomicBoolean(false);
        }

    // ----- constants ------------------------------------------------------

    public static final String CACHE_CONFIG_FILE = "topic-cache-config.xml";


    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();

    // ----- data members ---------------------------------------------------

    private static Session m_session;
    }
