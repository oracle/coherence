/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

import com.tangosol.coherence.config.scheme.PagedTopicScheme;
import com.tangosol.coherence.config.scheme.LocalScheme;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
import com.tangosol.internal.net.topic.impl.paged.model.ContentKey;

import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.Session;

import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;

import com.tangosol.net.partition.ObservableSplittingBackingCache;
import com.tangosol.net.topic.Position;
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

import com.oracle.coherence.testing.SystemPropertyIsolation;

import org.hamcrest.Matchers;

import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;

import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.tangosol.net.cache.TypeAssertion.withoutTypeChecking;

import static com.tangosol.net.topic.Subscriber.inGroup;
import static org.hamcrest.CoreMatchers.is;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.junit.Assert.fail;

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
        System.setProperty(Logging.PROPERTY_LEVEL, "9");
        System.setProperty("coherence.topic.publisher.close.timeout", "2s");

        String      sHost  = LocalPlatform.get().getLoopbackAddress().getHostAddress();
        ClassLoader loader = Base.getContextClassLoader();

        System.setProperty("coherence.wka", sHost);
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

        try
            {
            DefaultCacheServer.getInstance();
            }
        catch (IllegalStateException ignored)
            {
            ExtensibleConfigurableCacheFactory eccf = new ExtensibleConfigurableCacheFactory(deps);

            CacheFactory.getCacheFactoryBuilder().setConfigurableCacheFactory(eccf, "$Default$", loader, true);
            DefaultCacheServer.start(eccf);
            m_session = new ConfigurableCacheFactorySession(eccf, loader);
            }

        }


    @Parameterized.Parameters(name = "serializer={0}")
    public static Collection<Object[]> parameters()
        {
        return Arrays.asList(new Object[][]
            {
            {"java"}, {"pof"}
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

    public static void cleanupAfterAll()
        {
        DefaultCacheServer.shutdown();
        CacheFactory.shutdown();
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
    public void shouldNotBeAbleToFlushClosedPublisher()
        {
        NamedTopic<String> topic     = ensureTopic();
        try (Publisher<String>  publisher = topic.createPublisher())
            {
            publisher.publish("foo").join();
            publisher.close();

            try
                {
                publisher.flush().join();
                fail("Expected IllegalStateException, publisher is closed");
                }
            catch (IllegalStateException e)
                {
                // expected
                }
            }
        }

    //@Test
    public void shouldRepeatTest() throws Exception
        {
        for (int i = 0; i < 1000; i++)
            {
            shouldHandleErrorWhenPublishing();

            cleanupTests();
            cleanup();
            beforeEach();
            }
        }

    @Test
    public void shouldHandleErrorWhenPublishing() throws Exception
        {
        Assume.assumeThat(m_sSerializer, is("pof"));

        NamedTopic<String>     topic       = ensureTopic();
        int                    nErrorAfter = 1;

        List<CompletableFuture<Publisher.Status>> listFutures = new ArrayList<>();

        AtomicBoolean fPublisherClosed  = new AtomicBoolean(false);

        // introduce an error after first asynchronous publish succeeds
        addErrorInterceptor(topic, nErrorAfter);

        String sPrefix = "Element-";

        try (Publisher<String>  publisher = topic.createPublisher(Publisher.OrderBy.id(0));
             Subscriber<String> subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled()))
            {
            publisher.onClose(() -> fPublisherClosed.set(true));

            int cMessage = 0;
            while (!fPublisherClosed.get())
                {
                try
                    {
                    // validate that exception introduced by addErrorInterceptor call is not thrown at site of async send call
                    String sMsg = sPrefix + cMessage;
                    CompletableFuture<Publisher.Status> future = publisher.publish(sMsg);
                    listFutures.add(future);
                    cMessage++;
                    }
                catch (IllegalStateException e)
                    {
                    // Ignore exception, The publisher is no longer active
                    // all it means is that second asynchronous send occurred before loop completed and resulted in publisher closing
                    assertThat("should not fail on first publish", cMessage, Matchers.greaterThan(0));
                    assertThat(e.getMessage().contains("This publisher is no longer active"), is(true));
                    System.err.println("handled IllegalStateException: " + e.getMessage());
                    System.err.println("Publisher Closed: " + fPublisherClosed.get());
                    break;
                    }
                }

            // topic should contain just the first value
            assertThat(subscriber.receive().get(2, TimeUnit.MINUTES).getValue(), is(sPrefix + 0));
            assertThat(subscriber.receive().get(2, TimeUnit.MINUTES), is(nullValue()));
            }

        for (int i = 0; i < listFutures.size(); i++)
            {
            if (i < nErrorAfter)
                {
                // First nErrorAfter publishes should complete as normal
                assertCompletedNormally(listFutures.get(i), i);
                }
            else
                {
                // All other publishes should have completed exceptionally
                assertCompletedExceptionally(listFutures.get(i), i);
                }
            }
        }


    private void assertCompletedNormally(CompletableFuture<?> future, int n)
        {
        assertThat("Future (" + n + ") should be done",  future.isDone(), is(true));
        assertThat("Future (" + n + ") should not have completed exceptionally",  future.isCompletedExceptionally(), is(false));
        assertThat("Future (" + n + ") should not be cancelled",  future.isCancelled(), is(false));
        }

    private void assertCompletedExceptionally(CompletableFuture<?> future, int n)
        {
        assertThat("Future (" + n + ") should be done",  future.isDone(), is(true));
        assertThat("Future (" + n + ") should have completed exceptionally",  future.isCompletedExceptionally(), is(true));
        assertThat("Future (" + n + ") should not be cancelled", future.isCancelled(), is(false));
        }

    @Test
    public void shouldCleanUpSubscribers()
        {
        Assume.assumeThat(m_sSerializer, is("pof"));

        NamedTopic<String> topic = ensureTopic();

        Subscriber<String> subscriber1 = topic.createSubscriber(inGroup("Foo"));
        Subscriber<String> subscriber2 = topic.createSubscriber(inGroup("Foo"));
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

            assertThat(map.getClass().getCanonicalName(), is(ObservableSplittingBackingCache.class.getCanonicalName()));
            }
        }

    @Test
    public void shouldSeekGroupSubscriberForwardsUsingTimestamp() throws Exception
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-small-rewindable");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot of messages, so we have multiple pages spread over all the partitions
        String sSuffix  = "abcdefghijklmnopqrstuvwxyz";
        int    nChannel = 1;

        try (Publisher<String> publisher = topic.createPublisher(Publisher.OrderBy.id(nChannel)))
            {
            for (int i = 0; i < 500; i++)
                {
                String                              sMsg   = "element-" + i + sSuffix;
                CompletableFuture<Publisher.Status> future = publisher.publish(sMsg);
                Publisher.Status                    status = future.get(1, TimeUnit.MINUTES);

                assertThat(status.getChannel(), is(nChannel));
                // sleep to ensure that every message has a different millisecond timestamp
                Thread.sleep(3L);
                }
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        String sGroupPrefix = ensureGroupName();
        try (NamedTopicSubscriber<String> subscriberOne = (NamedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "-one"), Subscriber.CompleteOnEmpty.enabled());
             NamedTopicSubscriber<String> subscriberTwo = (NamedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "-two")))
            {
            // move subscriber two on by receiving pages
            // (we'll then seek subscriber one to the same place)
            CompletableFuture<Subscriber.Element<String>> future = null;
            for (int i = 0; i < 250; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two
            Subscriber.Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition              pagedPosition = (PagedPosition) element.getPosition();
            int                        nOffset       = pagedPosition.getOffset();

            // ensure the position is not a head or tail of a page
            PagedTopicCaches caches = new PagedTopicCaches(topic.getName(), (PagedTopicService) topic.getService());
            Page             page   = caches.Pages.get(new Page.Key(nChannel, pagedPosition.getPage()));
            while (nOffset == 0 || nOffset == page.getTail())
                {
                // we're at a head or tail so read another
                future        = subscriberTwo.receive();
                element       = future.get(2, TimeUnit.MINUTES);
                pagedPosition = (PagedPosition) element.getPosition();
                nOffset       = pagedPosition.getOffset();
                page          = caches.Pages.get(new Page.Key(nChannel, pagedPosition.getPage()));
                }

            Instant       seekTimestamp        = element.getTimestamp();
            PagedPosition expectedSeekPosition = (PagedPosition) element.getPosition();

            // Poll the next element from subscriber two
            Subscriber.Element<String> elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            while (elementTwo.getTimestamp().equals(seekTimestamp))
                {
                // the next element is the same timestamp, so we need to read more
                expectedSeekPosition = (PagedPosition) elementTwo.getPosition();
                elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                assertThat(elementTwo, is(notNullValue()));
                }

            // we know we're now not at a head or tail of a page
            // Seek subscriber one to the next message with a timestamp higher than seekTimestamp
            Position result = subscriberOne.seek(element.getChannel(), seekTimestamp);
            // should have the correct seek result
            assertThat(result, is(expectedSeekPosition));

            // Poll the next element for each subscriber, they should match
            Subscriber.Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));

            // now move subscriber two some way ahead
            for (int i = 0; i < 100; i++)
                {
                subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                }

            element              = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            pagedPosition        = (PagedPosition) element.getPosition();
            nOffset              = pagedPosition.getOffset();
            page                 = caches.Pages.get(new Page.Key(nChannel, pagedPosition.getPage()));

            // keep reading until subscriber two has read the tail of the page
            while (nOffset != page.getTail())
                {
                // we're not at the tail so read another
                future               = subscriberTwo.receive();
                element              = future.get(2, TimeUnit.MINUTES);
                pagedPosition        = (PagedPosition) element.getPosition();
                nOffset              = pagedPosition.getOffset();
                }

            // we're now at the tail of a page
            // Seek subscriber one to the last timestamp read by subscription two
            System.err.println(">>>> Seeking subscriber one to timestamp from element: " + element + " ");
            seekTimestamp = element.getTimestamp();
            result = subscriberOne.seek(element.getChannel(), seekTimestamp);
            // should have a seeked result
            Optional<Subscriber.Element<String>> peek = subscriberOne.peek(element.getChannel());
            if (peek.isPresent())
                {
                Subscriber.Element<String> elementPeek = peek.get();
                System.err.println(">>>> Head element timestamp is " + elementPeek.getTimestamp());
                System.err.println(">>>> Head element is " + elementPeek);
                }
            else
                {
                System.err.println(">>>> Head element is MISSING!!!!");
                }
            assertThat(result, is(notNullValue()));
            System.err.println(">>>> Seeked subscriber one to timestamp from element: " + element + " result: " + result);

            // Poll the next element for each subscriber, they should match
            elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            // ensure that we have read a later timestamp than the seeked to position
            while (!elementTwo.getTimestamp().isAfter(seekTimestamp))
                {
                elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                assertThat(elementTwo, is(notNullValue()));
                }

            elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));

            System.err.println(">>>> ElementOne: " + elementOne);
            System.err.println(">>>> ElementTwo: " + elementTwo);

            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));

            // now move subscriber two some way ahead
            for (int i = 0; i < 100; i++)
                {
                subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                }

            // Poll the next element from subscriber two
            element = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(element, is(notNullValue()));
            while (element.getTimestamp().equals(seekTimestamp))
                {
                // the next element is the same timestamp, so we need to read more
                element = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                }

            pagedPosition        = (PagedPosition) element.getPosition();
            nOffset              = pagedPosition.getOffset();
            expectedSeekPosition = pagedPosition;

            // keep reading until subscriber two has read the head of the page
            while (nOffset != 0)
                {
                // we're not at the head so read another
                future               = subscriberTwo.receive();
                element              = future.get(2, TimeUnit.MINUTES);
                pagedPosition        = (PagedPosition) element.getPosition();
                nOffset              = pagedPosition.getOffset();
                expectedSeekPosition = pagedPosition;
                }

            // we're now at the head of a page
            // Seek subscriber one to the last timestamp read by subscription two
            System.err.println(">>>> Seeking subscriber one to timestamp from element: " + element + " ");
            seekTimestamp = element.getTimestamp();
            result = subscriberOne.seek(element.getChannel(), seekTimestamp);
            System.err.println(">>>> Seeked subscriber one to timestamp from element: " + element + " result: " + result);
            peek = subscriberOne.peek(element.getChannel());
            if (peek.isPresent())
                {
                Subscriber.Element<String> elementPeek = peek.get();
                System.err.println(">>>> Head element timestamp is " + elementPeek.getTimestamp());
                System.err.println(">>>> Head element is " + elementPeek);
                }
            else
                {
                System.err.println(">>>> Head element is MISSING!!!!");
                }
            // should have correct seeked result
            assertThat(result, is(expectedSeekPosition));

            // Poll the next element for each subscriber, they should match
            elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            // ensure that we have read a later timestamp than the seeked to position
            while (!elementTwo.getTimestamp().isAfter(seekTimestamp))
                {
                elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                assertThat(elementTwo, is(notNullValue()));
                }

            elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            System.err.println(">>>> ElementOne: " + elementOne);
            System.err.println(">>>> ElementTwo: " + elementTwo);
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));
            }
        }

    // ----- helper methods -------------------------------------------------

    protected PagedTopicDependencies getDependencies(NamedTopic<?> topic)
        {
        CacheService                service      = (CacheService) topic.getService();
        PagedTopicBackingMapManager mgr          = (PagedTopicBackingMapManager) service.getBackingMapManager();
        return mgr.getTopicDependencies(topic.getName());
        }

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
