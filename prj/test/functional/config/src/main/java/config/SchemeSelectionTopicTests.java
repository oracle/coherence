/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.oracle.coherence.common.base.Exceptions;
import com.tangosol.coherence.config.TopicMapping;
import com.tangosol.coherence.config.builder.SubscriberGroupBuilder;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.coherence.config.scheme.NamedTopicScheme;
import com.tangosol.coherence.config.scheme.PagedTopicScheme;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.TopicService;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.net.topic.TopicBackingMapManager;
import com.tangosol.util.Base;
import com.tangosol.util.ResourceRegistry;

import org.junit.Test;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.tangosol.net.topic.Subscriber.Name;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A collection of functional tests that test the selection of topic scheme.
 *
 * @author jf 2015.10.27
 */
public class SchemeSelectionTopicTests
        extends SchemeSelectionTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public SchemeSelectionTopicTests()
        {
        super(FILE_CFG_CACHE);

        ConfigurableCacheFactory factory = getFactory();

        m_eccf = (factory instanceof ExtensibleConfigurableCacheFactory)
                 ? (ExtensibleConfigurableCacheFactory) factory : null;
        m_config = m_eccf == null ? null : m_eccf.getCacheConfig();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Validate configuration of a default distributed topic.
     */
    @Test
    public void testDistributedTopicTest()
        {
        NamedTopic<String> topic = validateNamedTopic("topic-dist-backing-local1",
                                             CacheService.TYPE_PAGED_TOPIC);

        assertNotNull(topic);

        PagedTopicDependencies config = getTopicDependencies("topic-dist-backing-local1");

        assertThat(config.getElementExpiryMillis(), is(0L));
        assertThat(config.getPageCapacity(), is(10));


        PagedTopicScheme scheme = (PagedTopicScheme) m_eccf.getCacheConfig().findSchemeByTopicName(topic.getName());
        assertThat(scheme.getStorageScheme().getClass().getSimpleName(), is(LocalScheme.class.getSimpleName()));
        assertThat(false, is(config.isRetainConsumed()));


        // avoid race condition between operations completing for topic and destroy.
        // without sleep resulted in deadlock till leaving cluster.
        sleep(1000L);
        topic.destroy();
        }

    /**
     * Validate configuration of a default distributed topic.
     */
    @Test
    public void testDistributedTopicWithDurableSubscriberTest()
        {
        String TOPIC_NAME = "topic-with-durable-subscriber42";
        NamedTopic<String> topic = validateNamedTopic(TOPIC_NAME,
                CacheService.TYPE_PAGED_TOPIC);

        assertNotNull(topic);

        PagedTopicDependencies config = getTopicDependencies(TOPIC_NAME);

        assertThat(config.getElementExpiryMillis(), is(0L));
        assertThat(config.getPageCapacity(), is(10));


        PagedTopicScheme scheme = (PagedTopicScheme) m_eccf.getCacheConfig().findSchemeByTopicName(topic.getName());
        assertThat(scheme.getStorageScheme().getClass().getSimpleName(), is(LocalScheme.class.getSimpleName()));

        TopicMapping mapping = m_eccf.getCacheConfig().getMappingRegistry().findMapping(TOPIC_NAME, TopicMapping.class);
        Collection<SubscriberGroupBuilder> colBuilders = mapping.getSubscriberGroupBuilders();
        assertThat(colBuilders.size(), is(2));

        assertThat(false, is(config.isRetainConsumed()));


        // avoid race condition between operations completing for topics and destroy.
        // without sleep resulted in deadlock till leaving cluster.
        sleep(1000L);
        topic.destroy();
        }

    /**
     * Validate configuration of a default distributed topic.
     */
    @Test
    public void testDistributedTopicTestDefaults()
        {
        NamedTopic<String> topic = validateNamedTopic("topic-dist-defaults",
                CacheService.TYPE_PAGED_TOPIC);

        assertNotNull(topic);

        PagedTopicDependencies config = getTopicDependencies("topic-dist-defaults");

        assertThat(config.getElementExpiryMillis(), is(0L));
        assertThat(config.getPageCapacity(), is(1024*1024));


        PagedTopicScheme scheme = (PagedTopicScheme) m_eccf.getCacheConfig().findSchemeByTopicName(topic.getName());
        assertThat(scheme.getStorageScheme().getClass().getSimpleName(), is(LocalScheme.class.getSimpleName()));
        assertThat(false, is(config.isRetainConsumed()));

        // avoid race condition between operations completing for topics and destroy.
        // without sleep resulted in deadlock till leaving cluster.
        sleep(1000L);
        topic.destroy();
        }

    /**
     * Validate configuration of a completely defaulted distributed topic.
     */
    @Test
    public void testTopicTestDefaults()
        {
        NamedTopic<String> topic = validateNamedTopic("all-defaulted-topic",
            CacheService.TYPE_PAGED_TOPIC);

        assertNotNull(topic);

        PagedTopicDependencies config = getTopicDependencies("all-defaulted-topic");

        assertThat(config.getElementExpiryMillis(), is(0L));
        assertThat(config.getPageCapacity(), is(1024*1024));
        assertThat(topic.getService().getInfo().getServiceName(), is(CacheService.TYPE_PAGED_TOPIC));


        PagedTopicScheme scheme = (PagedTopicScheme) m_eccf.getCacheConfig().findSchemeByTopicName(topic.getName());
        assertThat(scheme.getStorageScheme().getClass().getSimpleName(), is(LocalScheme.class.getSimpleName()));
        assertThat(false, is(config.isRetainConsumed()));


        // avoid race condition between operations completing for topics and destroy.
        // without sleep resulted in deadlock till leaving cluster.
        sleep(1000L);
        topic.destroy();
        }

    /**
     * Functional test of configuring cache and topic with same name.
     */
    @Test
    public void testSharedNameCacheAndTopicTest()
        {
        NamedCache<String, String> cache = validateNamedCache("same", CacheService.TYPE_DISTRIBUTED);
        assertNotNull(cache);


        NamedTopic<String> topic = validateNamedTopic("same",
                CacheService.TYPE_PAGED_TOPIC);
        assertNotNull(topic);

        PagedTopicDependencies config = getTopicDependencies("same");

        assertThat(config.getElementExpiryMillis(), is(0L));
        assertThat(false, is(config.isRetainConsumed()));

        // avoid race condition between operations completing for topics and destroy.
        // without sleep resulted in deadlock till leaving cluster.
        sleep(1000L);
        topic.destroy();
        cache.destroy();
        }

    /**
     * Validate configuration of a retaining element distributed topic. Verifies that parameters from topic are overriding
     * defaults in topic-scheme.
     */
    @Test
    public void testDistributedRewindableTopicTest()
        {
        final String             COLLECTION_NAME = "topic-retain-dist-backing-local1";

        NamedTopic<String> topic = validateNamedTopic(COLLECTION_NAME, CacheService.TYPE_PAGED_TOPIC);

        assertNotNull(topic);

        PagedTopicDependencies config = getTopicDependencies(COLLECTION_NAME);

        assertThat(config.getElementExpiryMillis(), is(21 * 1000L));
        assertThat(config.isRetainConsumed(), is(true));

        // avoid race condition between operations completing for topics and destroy.
        // resulted in deadlock till leaving cluster.
        sleep(1000L);
        topic.destroy();
        }

    /**
     * Validate configuration of parameter override in topic resource. Verifies that parameters from topic are overriding
     * defaults in referenced topic-scheme.
     */
    @Test
    public void testOverrideDistributedTopicTest()
        {
        final String             COLLECTION_NAME = "topic-override-dist-backing-local1";

        NamedTopic<String> topic = validateNamedTopic(COLLECTION_NAME, CacheService.TYPE_PAGED_TOPIC);

        assertNotNull(topic);

        PagedTopicDependencies config = getTopicDependencies(COLLECTION_NAME);

        assertThat(config.getPageCapacity(), is(20));


        // avoid race condition between operations completing for topics and destroy.
        // resulted in deadlock till leaving cluster.
        sleep(1000L);
        topic.destroy();
        }

    @Test
    public void validateDefaultCacheServiceDiffersFromDefaultTopicService()
        {
        NamedTopic topic = validateNamedTopic("all-defaulted-topic", CacheService.TYPE_PAGED_TOPIC);
        NamedCache cache = validateNamedCache("all-defaulted-cache", CacheService.TYPE_DISTRIBUTED);

        System.out.println("****************************** all-default-topic default service name is " + topic.getService().getInfo().getServiceName());
        System.out.println("****************************** all-default-cache default service name is " + cache.getService().getInfo().getServiceName());

        assertThat(topic.getService().getInfo().getServiceName(), is(CacheService.TYPE_PAGED_TOPIC));
        assertNotEquals("default topic service must not be same as default cache service name",
            topic.getService().getInfo().getServiceName(), cache.getService().getInfo().getServiceName());
        }

    /**
     * No category in jira for queue yet so just inlined this failure.
     *
     * Stack trace while destroying topic and consumer was just closed.
     * Probably an async issue. Close needs to be synchronous if it is not.
     *
     * (thread=DistributedCache:topic-dist-backing-local-service$Cache:EventDispatcher, member=n/a):
     * Exception caught while dispatching to "<queueConsumerstopic-retain-dist-backing-local*,DestinationConsumerInterceptor>":
     * java.lang.IllegalStateException: Service is not running: PartitionedCache{Name=topic-dist-backing-local-service$Cache, State=(SERVICE_STOPPED),
     * Not initialized}
     * at com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache.ensureCache(PartitionedCache.java:1593)
     * at PartitionedDestination.ensureCache(PartitionedDestination.java:545)
     * at PartitionedDestination.cleanupConsumers(PartitionedDestination.java:508)
     * at DestinationConsumerInterceptor.onEvent(DestinationConsumerInterceptor.java:60)
     */
    @Test
    public void testRaceConditionBetweenConsumerCloseAndCollectionDestroy()
        {
        NamedTopic<String> topic = getNamedTopic("topic-retain-dist-backing-local1", String.class);

        assertTrue(topic != null);

        Subscriber<String> subscriber = topic.createSubscriber();

        subscriber.close();

        // TODO: remove this workaround to get the above stack trace
        sleep(1000L);
        topic.destroy();
        }

    @Test
    public void testTopicStorageAuthorizerAuditing()
        {
        NamedTopic<String> topic = getNamedTopic("topic-storage-authorizer-auditing", String.class);

        assertTrue(topic != null);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Get the topic and do a simple test to ensure that it works.
     *
     * @param sName         the topic name
     * @param sServiceType  the expected service type
     *
     * @return the named topic
     */
    protected NamedTopic<String> validateNamedTopic(String sName, String sServiceType)
        {
        NamedTopic<String> topic = getNamedTopic(sName, String.class);

        assertTrue(topic != null);

        String             sPrefix    = "Element-";
        int                nCount     = 100;

        try (Subscriber<String> subscriber = topic.createSubscriber(Name.of("Foo")))
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                for (int i = 0; i < nCount; i++)
                    {
                    try
                        {
                        publisher.publish(sPrefix + i).get();
                        }
                    catch (InterruptedException | ExecutionException e)
                        {
                        fail(e.getMessage());
                        }
                    }
                }

            for (int i = 0; i < nCount; i++)
                {
                assertThat(subscriber.receive().get(1, TimeUnit.MINUTES)
                                   .getValue(), is(sPrefix + i));
                }
            }
        catch (Exception e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }

        // validate the service type
        for (PagedTopicCaches.Names names : PagedTopicCaches.Names.values())
            {
            String     cacheName = names.cacheNameForTopicName(sName);
            NamedCache cache     = m_eccf.ensureTypedCache(cacheName, null, TypeAssertion.withoutTypeChecking());

            assertTrue("cache: " + cache.getCacheName() + " expecting local, " + " got "
                       + cache.getCacheService().getInfo().getServiceType(), cache.getCacheService().getInfo()
                           .getServiceType().equals(sServiceType));

            /*
             * BackingMapManager manager = cache.getCacheService().getBackingMapManager();
             * BackingMapContext context = manager.getContext().getBackingMapContext(cacheName);
             * ObservableMap     map     = context.getBackingMap();
             *
             * assertThat(map.getClass().getCanonicalName(), is(LocalCache.class.getCanonicalName()));
             */
            }

        return topic;
        }

    private PagedTopicDependencies getTopicDependencies(String sCollectionName)
        {
        NamedTopicScheme            scheme      = m_eccf.getCacheConfig().findSchemeByTopicName(sCollectionName);
        String                      serviceName = scheme.getServiceName();
        TopicService                service     = (TopicService) m_eccf.ensureService(serviceName);
        PagedTopicBackingMapManager manager     = (PagedTopicBackingMapManager) service.getTopicBackingMapManager();
        return manager.getTopicDependencies(sCollectionName);
        }

    private <E> NamedTopic<E> getNamedTopic(String sTopicName, Class<E> clsElement)
        {
        return m_eccf.ensureTopic(sTopicName, ValueTypeAssertion.withType(clsElement));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_CFG_CACHE = "scheme-selection-topic-config.xml";

    // ----- data members ---------------------------------------------------

    /**
     * The ECCF.
     */
    private ExtensibleConfigurableCacheFactory m_eccf;
    }
