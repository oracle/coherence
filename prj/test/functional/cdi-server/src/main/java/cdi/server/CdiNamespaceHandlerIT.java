/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cdi.server;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.ConfigUri;
import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SessionInitializer;
import com.oracle.coherence.cdi.events.Activated;
import com.oracle.coherence.cdi.events.Activating;
import com.oracle.coherence.cdi.events.CacheName;
import com.oracle.coherence.cdi.events.Created;
import com.oracle.coherence.cdi.events.Destroyed;
import com.oracle.coherence.cdi.events.Disposing;
import com.oracle.coherence.cdi.events.ServiceName;

import com.oracle.coherence.cdi.server.CdiNamespaceHandler;
import com.oracle.coherence.cdi.server.CoherenceServerExtension;
import com.oracle.coherence.common.base.Blocking;

import com.tangosol.net.MemberEvent;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.events.Event;
import com.tangosol.net.events.EventDispatcher.InterceptorRegistrationEvent;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.CacheLifecycleEvents;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.LifecycleEvents;

import com.tangosol.net.events.application.LifecycleEvent;

import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEventDispatcher;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.net.partition.PartitionEvent;

import com.tangosol.util.Base;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.event.Observes;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.tangosol.net.events.partition.cache.CacheLifecycleEvent.Type.CREATED;
import static com.tangosol.net.events.partition.cache.CacheLifecycleEvent.Type.DESTROYED;
import static com.tangosol.net.events.partition.cache.EntryEvent.Type.INSERTING;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the {@link CdiNamespaceHandler}
 * using the Weld JUnit extension.
 *
 * @author Aleks Seovic  2020.03.31
*/
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CdiNamespaceHandlerIT
    {

    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addExtension(new CoherenceServerExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(CdiBeansSession.class)
                                                          .addBeanClass(CacheStore.class)
                                                          .addBeanClass(PartitionListener.class)
                                                          .addBeanClass(MemberListener.class)
                                                          .addBeanClass(CacheListener.class)
                                                          .addBeanClass(StorageListener.class)
                                                          .addBeanClass(RegistrationListener.class)
                                                          .addBeanClass(ActivationListener.class));

    @ApplicationScoped
    @Named("cdi-beans")
    @Scope("CDI")
    @ConfigUri("cdi-beans-config.xml")
    private static class CdiBeansSession
            implements SessionInitializer
        {}

    @Inject
    @Name("cdi-beans")
    private Session m_session;

    @Inject
    private ActivationListener activationListener;

    @Inject
    private StorageListener serviceListener;

    @Inject
    private MemberListener memberListener;

    @Inject
    private PartitionListener partitionListener;

    @Inject
    private CacheStore cacheStore;

    @Test
    @Order(10)
    void shouldNotifyActivationListener()
        {
        assertThat(activationListener.isActivated(), is(true));
        }

    @Test
    @Order(20)
    void shouldConvertValuesToUppercase()
        {
        NamedCache<Long, String> numbers = m_session.getCache("numbers");
        numbers.put(1L, "one");
        numbers.put(2L, "two");
        assertThat(numbers.get(1L), is("ONE"));
        assertThat(numbers.get(2L), is("TWO"));
        }

    @Test
    @Order(25)
    void shouldUpdateCacheStore()
        {
        assertThat(cacheStore.getStoreMap().get(1L), is("ONE"));
        assertThat(cacheStore.getStoreMap().get(2L), is("TWO"));
        }

    @Test
    @Order(25)
    void shouldLoadFromCacheStore()
        {
        NamedCache<Long, String> numbers = m_session.getCache("numbers");
        assertThat(numbers.get(10L), is("10"));
        assertThat(numbers.get(20L), is("20"));
        }

    @Test
    @Order(30)
    void shouldUpdateCacheNames()
        {
        assertThat(serviceListener.hasCache("apples"), is(false));
        NamedCache<?, ?> apples = m_session.getCache("apples");
        Eventually.assertDeferred(() -> serviceListener.hasCache("apples"), is(true));

        // COH-22954 - give a little time for all async initialization tasks to complete before
        //             destroying the cache
        try
            {
            Blocking.sleep(500);
            }
        catch (InterruptedException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        apples.destroy();
        Eventually.assertDeferred(() -> serviceListener.hasCache("apples"), is(false));
        }

    @Test
    @Order(40)
    void shouldJoin()
        {
        assertThat(memberListener.hasJoined(), is(true));
        }

    @Test
    @Order(50)
    void shouldAssignPartitions()
        {
        assertThat(partitionListener.getId(), is(PartitionEvent.PARTITION_ASSIGNED));
        }

    // ---- helper classes --------------------------------------------------

    @ApplicationScoped
    private static class CdiObservers
        {
        // will be called for all events
        @SuppressWarnings("rawtypes")
        private void onEvent(@Observes Event event)
            {
            System.out.println("onEvent: " + event);
            }

        // will be called for all lifecycle events
        private void onLifecycleEvent(@Observes LifecycleEvent event)
            {
            System.out.println("onLifecycleEvent: " + event);
            }

        // will be called for specific lifecycle events
        private void onActivating(@Observes @Activating LifecycleEvent event)
            {
            System.out.println("onActivating: " + event);
            }

        private void onActivated(@Observes @Activated LifecycleEvent event)
            {
            System.out.println("onActivated: " + event);
            }

        private void onDisposing(@Observes @Disposing LifecycleEvent event)
            {
            System.out.println("onDisposing: " + event);
            }

        // will be called for all cache lifecycle events
        private void onCacheLifecycleEvent(@Observes CacheLifecycleEvent event)
            {
            System.out.println("onCacheLifecycleEvent: " + event);
            }

        private void onCreated(@Observes @Created CacheLifecycleEvent event)
            {
            System.out.println("onCreated: " + event);
            }

        private void onCreatedCache(@Observes @Created @ServiceName("PartitionedCache") CacheLifecycleEvent event)
            {
            System.out.println("onCreatedCache: " + event);
            }

        private void onCreatedApples(@Observes @Created @CacheName("apples") CacheLifecycleEvent event)
            {
            System.out.println("onCreatedApples: " + event);
            }
        }

    @ApplicationScoped
    @Named("registrationListener")
    public static class RegistrationListener
            implements EventInterceptor<InterceptorRegistrationEvent<?>>
        {
        @Override
        public synchronized void onEvent(InterceptorRegistrationEvent<?> e)
            {
            if (e.getType() == InterceptorRegistrationEvent.Type.INSERTED)
                {
                System.out.println("REGISTERED: " + e.getIdentifier() + ", EVENTS: " + e.getEventTypes());
                }
            }
        }

    @ApplicationScoped
    @Named("activationListener")
    @LifecycleEvents
    public static class ActivationListener
            implements EventInterceptor<LifecycleEvent>
        {
        private boolean activated = false;

        @Inject
        private javax.enterprise.event.Event<LifecycleEvent> lifecycleEvent;

        synchronized boolean isActivated()
            {
            return activated;
            }

        @Override
        public synchronized void onEvent(LifecycleEvent e)
            {
            if (e.getType() == LifecycleEvent.Type.ACTIVATING)
                {
                lifecycleEvent.select(Activating.Literal.INSTANCE).fire(e);
                }
            else if (e.getType() == LifecycleEvent.Type.ACTIVATED)
                {
                lifecycleEvent.select(Activated.Literal.INSTANCE).fire(e);
                activated = true;
                }
            else if (e.getType() == LifecycleEvent.Type.DISPOSING)
                {
                lifecycleEvent.select(Disposing.Literal.INSTANCE).fire(e);
                }
            }
        }

    @ApplicationScoped
    @Named("storageListener")
    @CacheLifecycleEvents({CREATED, DESTROYED})
    public static class StorageListener
            implements EventInterceptor<CacheLifecycleEvent>
        {
        private final Set<String> caches = new HashSet<>();

        @Inject
        private javax.enterprise.event.Event<CacheLifecycleEvent> cacheLifecycleEvent;

        synchronized boolean hasCache(String cacheName)
            {
            return caches.contains(cacheName);
            }

        @Override
        public synchronized void onEvent(CacheLifecycleEvent e)
            {
            System.out.println(e);

            CacheName                     cache      = CacheName.Literal.of(e.getCacheName());
            CacheLifecycleEventDispatcher dispatcher = e.getEventDispatcher();
            ServiceName                   service    = ServiceName.Literal.of(dispatcher.getServiceName());

            if (e.getType() == CREATED)
                {
                cacheLifecycleEvent.select(cache, service, Created.Literal.INSTANCE).fire(e);
                caches.add(e.getCacheName());
                }
            if (e.getType() == DESTROYED)
                {
                cacheLifecycleEvent.select(cache, service, Destroyed.Literal.INSTANCE).fire(e);
                caches.remove(e.getCacheName());
                }
            }
        }

    @ApplicationScoped
    @Named("cacheListener")
    @EntryEvents(INSERTING)
    public static class CacheListener
            implements EventInterceptor<EntryEvent<Long, String>>
        {
        @Override
        public synchronized void onEvent(EntryEvent<Long, String> e)
            {
            e.getEntrySet().stream().filter(entry -> !entry.isValueLoaded()).
                    forEach(entry -> entry.setValue(entry.getValue().toUpperCase()));
            }
        }

    @ApplicationScoped
    @Named("memberListener")
    public static class MemberListener
            implements com.tangosol.net.MemberListener
        {
        private volatile boolean fJoined;

        public synchronized boolean hasJoined()
            {
            return fJoined;
            }

        @Override
        public synchronized void memberJoined(MemberEvent memberEvent)
            {
            fJoined = true;
            System.out.println(memberEvent);
            }

        @Override
        public void memberLeaving(MemberEvent memberEvent)
            {
            System.out.println(memberEvent);
            }

        @Override
        public void memberLeft(MemberEvent memberEvent)
            {
            System.out.println(memberEvent);
            }
        }

    @ApplicationScoped
    @Named("partitionListener")
    public static class PartitionListener
            implements com.tangosol.net.partition.PartitionListener
        {
        private int id;

        public synchronized int getId()
            {
            return id;
            }

        @Override
        public synchronized void onPartitionEvent(PartitionEvent partitionEvent)
            {
            id = partitionEvent.getId();
            System.out.println(partitionEvent);
            }
        }

    @ApplicationScoped
    @Named("cacheStore")
    public static class CacheStore
            implements com.tangosol.net.cache.CacheStore<Long, String>
        {
        private final Map<Long, String> storeMap = new HashMap<>();

        public synchronized Map<Long, String> getStoreMap()
            {
            return storeMap;
            }

        @Override
        public synchronized void store(Long key, String value)
            {
            storeMap.put(key, value);
            }

        @Override
        public synchronized void storeAll(Map<? extends Long, ? extends String> map)
            {
            storeMap.putAll(map);
            }

        @Override
        public synchronized void erase(Long key)
            {
            storeMap.remove(key);
            }

        @Override
        public synchronized void eraseAll(Collection<? extends Long> keys)
            {
            keys.forEach(storeMap::remove);
            }

        @Override
        public synchronized String load(Long key)
            {
            return key.toString();
            }

        @Override
        public synchronized Map<Long, String> loadAll(Collection<? extends Long> keys)
            {
            return keys.stream().collect(Collectors.toMap(k -> k, Object::toString));
            }
        }
    }
