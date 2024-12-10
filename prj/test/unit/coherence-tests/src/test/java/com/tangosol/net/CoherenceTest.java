/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.internal.net.DefaultSessionProvider;

import org.junit.After;
import org.junit.Test;

import org.mockito.InOrder;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.junit.Assert.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2020.11.05
 */
public class CoherenceTest
    {
    @After
    public void cleanup()
        {
        Coherence.closeAll();
        }

    @Test
    public void shouldBuildCoherenceInstance()
        {
        CoherenceConfiguration config = mock(CoherenceConfiguration.class);

        when(config.getName()).thenReturn("foo");

        Coherence coherence = Coherence.clusterMemberBuilder(config).build();
        assertThat(coherence.isStarted(), is(false));
        assertThat(coherence.getName(), is("foo"));
        assertThat(coherence.getConfiguration(), is(sameInstance(config)));
        }

    @Test
    public void shouldRegisterNewCoherenceInstance()
        {
        CoherenceConfiguration config = mock(CoherenceConfiguration.class);

        when(config.getName()).thenReturn("foo");

        Coherence             coherence = Coherence.clusterMember(config);
        Coherence             instance  = Coherence.getInstance();
        Coherence             named     = Coherence.getInstance("foo");
        Collection<Coherence> instances = Coherence.getInstances();

        assertThat(instance, is(sameInstance(coherence)));
        assertThat(named, is(sameInstance(coherence)));
        assertThat(instances, containsInAnyOrder(coherence));
        }

    @Test
    public void shouldRegisterMultipleNewCoherenceInstances()
        {
        CoherenceConfiguration configFoo = mock(CoherenceConfiguration.class);
        CoherenceConfiguration configBar = mock(CoherenceConfiguration.class);

        when(configFoo.getName()).thenReturn("foo");
        when(configBar.getName()).thenReturn("bar");

        Coherence             coherenceFoo = Coherence.clusterMemberBuilder(configFoo).build();
        Coherence             coherenceBar = Coherence.clusterMemberBuilder(configBar).build();
        Coherence             instance     = Coherence.getInstance();
        Coherence             namedFoo     = Coherence.getInstance("foo");
        Coherence             namedBar     = Coherence.getInstance("bar");
        Collection<Coherence> instances    = Coherence.getInstances();

        assertThat(instance, is(anyOf(sameInstance(coherenceFoo), sameInstance(coherenceBar))));
        assertThat(namedFoo, is(sameInstance(coherenceFoo)));
        assertThat(namedBar, is(sameInstance(coherenceBar)));
        assertThat(instances, containsInAnyOrder(coherenceFoo, coherenceBar));
        }

    @Test
    public void shouldUnregisterCoherenceInstanceOnShutdown()
        {
        Coherence coherence = Coherence.clusterMemberBuilder(EMPTY_CONFIG).build();
        coherence.close();

        assertThat(coherence.isStarted(), is(false));
        assertThat(coherence.isClosed(), is(true));

        Coherence             instance  = Coherence.getInstance();
        Coherence             named     = Coherence.getInstance(EMPTY_CONFIG.getName());
        Collection<Coherence> instances = Coherence.getInstances();

        assertThat(instance, is(nullValue()));
        assertThat(named, is(nullValue()));
        assertThat(instances, empty());
        }

    @Test
    public void shouldCompleteStartedFutureOnStart() throws Exception
        {
        ConfigurableCacheFactorySession sessionSys = mock(ConfigurableCacheFactorySession.class);
        ConfigurableCacheFactory        ccf        = mock(ConfigurableCacheFactory.class);
        InterceptorRegistry             registry   = mock(InterceptorRegistry.class);
        Coherence                       coherence  = Coherence.clusterMember(EMPTY_CONFIG);
        CompletableFuture<Coherence>    future     = coherence.whenStarted();

        when(sessionSys.getConfigurableCacheFactory()).thenReturn(ccf);
        when(ccf.getInterceptorRegistry()).thenReturn(registry);

        Coherence.setSystemSession(Optional.of(sessionSys));
        coherence.start();

        future.toCompletableFuture().get(1, TimeUnit.MINUTES);

        assertThat(future.toCompletableFuture().isDone(), is(true));
        assertThat(future.toCompletableFuture().isCancelled(), is(false));
        assertThat(future.toCompletableFuture().isCompletedExceptionally(), is(false));
        }

    @Test
    public void shouldCompleteShutdownFutureOnShutdown()
        {
        Coherence               coherence = Coherence.clusterMember(EMPTY_CONFIG);
        CompletableFuture<Void> future    = coherence.whenClosed();
        coherence.close();

        assertThat(future.toCompletableFuture().isDone(), is(true));
        assertThat(future.toCompletableFuture().isCancelled(), is(false));
        assertThat(future.toCompletableFuture().isCompletedExceptionally(), is(false));
        }

    @Test
    public void shouldStartConfiguredSessionsInPriorityOrder() throws Exception
        {
        ConfigurableCacheFactorySession    sessionSys      = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfSys          = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registrySys     = mock(InterceptorRegistry.class);

        ConfigurableCacheFactorySession    sessionOne      = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfOne          = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryOne     = mock(InterceptorRegistry.class);
        SessionProvider                    providerOne     = mock(SessionProvider.class);
        EventInterceptor<?>                interceptorOne  = mock(EventInterceptor.class);
        Iterable<EventInterceptor<?>>      interceptorsOne = Collections.singletonList(interceptorOne);
        SessionConfiguration               cfgOne          = new SessionConfigStub("One", interceptorsOne, 1, providerOne);

        ConfigurableCacheFactorySession    sessionTwo      = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfTwo          = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryTwo     = mock(InterceptorRegistry.class);
        SessionProvider                    providerTwo     = mock(SessionProvider.class);
        EventInterceptor<?>                interceptorTwo  = mock(EventInterceptor.class);
        Iterable<EventInterceptor<?>>      interceptorsTwo = Collections.singletonList(interceptorTwo);
        SessionConfiguration               cfgTwo          = new SessionConfigStub("Two", interceptorsTwo, 0, providerTwo);

        ConfigurableCacheFactorySession    sessionThree      = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfThree          = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryThree     = mock(InterceptorRegistry.class);
        SessionProvider                    providerThree     = mock(SessionProvider.class);
        EventInterceptor<?>                interceptorThree  = mock(EventInterceptor.class);
        Iterable<EventInterceptor<?>>      interceptorsThree = Collections.singletonList(interceptorThree);
        SessionConfiguration               cfgThree          = new SessionConfigStub("Three", interceptorsThree, 10, providerThree);

        CoherenceConfiguration             configuration   = CoherenceConfiguration.builder()
                                                                    .withSession(cfgOne)
                                                                    .withSessions(cfgTwo)
                                                                    .withSessions(cfgThree)
                                                                    .build();
        Coherence                          coherence       = Coherence.clusterMember(configuration);
        CompletableFuture<Coherence>       future     = coherence.whenStarted();

        when(sessionSys.getConfigurableCacheFactory()).thenReturn(ccfSys);
        when(sessionSys.getInterceptorRegistry()).thenReturn(registrySys);

        when(providerOne.createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class))).thenReturn(new ContextStub(sessionOne));
        when(sessionOne.getConfigurableCacheFactory()).thenReturn(ccfOne);
        when(sessionOne.getInterceptorRegistry()).thenReturn(registryOne);
        when(ccfOne.getInterceptorRegistry()).thenReturn(registryOne);
        when(ccfOne.getServiceMap()).thenReturn(Collections.emptyMap());

        when(providerTwo.createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class))).thenReturn(new ContextStub(sessionTwo));
        when(sessionTwo.getConfigurableCacheFactory()).thenReturn(ccfTwo);
        when(sessionTwo.getInterceptorRegistry()).thenReturn(registryTwo);
        when(ccfTwo.getInterceptorRegistry()).thenReturn(registryTwo);
        when(ccfTwo.getServiceMap()).thenReturn(Collections.emptyMap());

        when(providerThree.createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class))).thenReturn(new ContextStub(sessionThree));
        when(sessionThree.getConfigurableCacheFactory()).thenReturn(ccfThree);
        when(sessionThree.getInterceptorRegistry()).thenReturn(registryThree);
        when(ccfThree.getInterceptorRegistry()).thenReturn(registryThree);
        when(ccfThree.getServiceMap()).thenReturn(Collections.emptyMap());

        Coherence.setSystemSession(Optional.of(sessionSys));
        coherence.start();

        future.toCompletableFuture().get(1, TimeUnit.MINUTES);

        assertThat(coherence.getSession("One"), is(sameInstance(sessionOne)));
        assertThat(coherence.getSession("Two"), is(sameInstance(sessionTwo)));
        assertThat(coherence.getSession("Three"), is(sameInstance(sessionThree)));

        verify(providerOne, times(1)).createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class));
        verify(providerTwo, times(1)).createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class));
        verify(providerThree, times(1)).createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class));

        // should start in the highest priority order
        InOrder inOrder = inOrder(ccfOne, ccfTwo, ccfThree);
        inOrder.verify(ccfThree).activate();
        inOrder.verify(ccfOne).activate();
        inOrder.verify(ccfTwo).activate();
        }

    @Test
    public void shouldStopConfiguredSessionsInOrder() throws Exception
        {
        ConfigurableCacheFactorySession    sessionSys      = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfSys          = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registrySys     = mock(InterceptorRegistry.class);

        ConfigurableCacheFactorySession    sessionOne      = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfOne          = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryOne     = mock(InterceptorRegistry.class);
        SessionProvider                    providerOne     = mock(SessionProvider.class);
        EventInterceptor<?>                interceptorOne  = mock(EventInterceptor.class);
        Iterable<EventInterceptor<?>>      interceptorsOne = Collections.singletonList(interceptorOne);
        SessionConfiguration               cfgOne          = new SessionConfigStub("One", interceptorsOne, 1, providerOne);

        ConfigurableCacheFactorySession    sessionTwo      = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfTwo          = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryTwo     = mock(InterceptorRegistry.class);
        SessionProvider                    providerTwo     = mock(SessionProvider.class);
        EventInterceptor<?>                interceptorTwo  = mock(EventInterceptor.class);
        Iterable<EventInterceptor<?>>      interceptorsTwo = Collections.singletonList(interceptorTwo);
        SessionConfiguration               cfgTwo          = new SessionConfigStub("Two", interceptorsTwo, 0, providerTwo);

        CoherenceConfiguration             configuration   = CoherenceConfiguration.builder()
                                                                    .withSession(cfgOne)
                                                                    .withSessions(cfgTwo)
                                                                    .build();
        Coherence                          coherence       = Coherence.clusterMember(configuration);
        CompletableFuture<Coherence>       future     = coherence.whenStarted();

        when(sessionSys.getConfigurableCacheFactory()).thenReturn(ccfSys);
        when(sessionSys.getInterceptorRegistry()).thenReturn(registrySys);

        when(providerOne.createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class))).thenReturn(new ContextStub(sessionOne));
        when(sessionOne.getConfigurableCacheFactory()).thenReturn(ccfOne);
        when(sessionOne.getInterceptorRegistry()).thenReturn(registryOne);
        when(ccfOne.getServiceMap()).thenReturn(Collections.emptyMap());

        when(providerTwo.createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class))).thenReturn(new ContextStub(sessionTwo));
        when(sessionTwo.getConfigurableCacheFactory()).thenReturn(ccfTwo);
        when(sessionTwo.getInterceptorRegistry()).thenReturn(registryTwo);
        when(ccfTwo.getServiceMap()).thenReturn(Collections.emptyMap());

        Coherence.setSystemSession(Optional.of(sessionSys));
        coherence.start();

        future.toCompletableFuture().get(1, TimeUnit.MINUTES);

        coherence.close();

        // should start in highest priority order
        InOrder inOrder = inOrder(ccfOne, ccfTwo);
        inOrder.verify(ccfOne).dispose();
        inOrder.verify(ccfTwo).dispose();
        }

    @Test
    public void shouldGetDefaultSession() throws Exception
        {
        ConfigurableCacheFactorySession    sessionSys    = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfSys        = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registrySys   = mock(InterceptorRegistry.class);

        ConfigurableCacheFactorySession    sessionOne    = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfOne        = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryOne   = mock(InterceptorRegistry.class);
        SessionProvider                    providerOne   = mock(SessionProvider.class, "ProviderOne");
        SessionConfiguration               cfgOne        = new SessionConfigStub(Coherence.DEFAULT_NAME, Collections.emptyList(), 0, providerOne);

        ConfigurableCacheFactorySession    sessionTwo    = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfTwo        = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryTwo   = mock(InterceptorRegistry.class);
        SessionProvider                    providerTwo   = mock(SessionProvider.class, "ProviderTwo");
        SessionConfiguration               cfgTwo        = new SessionConfigStub("Two", Collections.emptyList(), 0, providerTwo);

        CoherenceConfiguration             configuration = CoherenceConfiguration.builder()
                                                                    .withSession(cfgOne)
                                                                    .withSessions(cfgTwo)
                                                                    .build();
        Coherence                          coherence     = Coherence.clusterMember(configuration);
        CompletableFuture<Coherence>       future     = coherence.whenStarted();

        when(sessionSys.getConfigurableCacheFactory()).thenReturn(ccfSys);
        when(sessionSys.getInterceptorRegistry()).thenReturn(registrySys);

        when(providerOne.createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class))).thenReturn(new ContextStub(sessionOne));
        when(sessionOne.getConfigurableCacheFactory()).thenReturn(ccfOne);
        when(sessionOne.getInterceptorRegistry()).thenReturn(registryOne);
        when(ccfOne.getServiceMap()).thenReturn(Collections.emptyMap());

        when(providerTwo.createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class))).thenReturn(new ContextStub(sessionTwo));
        when(sessionTwo.getConfigurableCacheFactory()).thenReturn(ccfTwo);
        when(sessionTwo.getInterceptorRegistry()).thenReturn(registryTwo);
        when(ccfTwo.getServiceMap()).thenReturn(Collections.emptyMap());

        Coherence.setSystemSession(Optional.of(sessionSys));
        coherence.start();

        future.toCompletableFuture().get(1, TimeUnit.MINUTES);

        Session session = coherence.getSession();
        assertThat(session, is(sameInstance(sessionOne)));
        }

    @Test
    public void shouldThrowGettingDefaultSessionIfNotExists()
        {
        SessionProvider        providerOne   = mock(SessionProvider.class);
        SessionConfiguration   cfgOne        = new SessionConfigStub("Foo", Collections.emptyList(), 0, providerOne);

        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                                                                    .withSession(cfgOne)
                                                                    .build();
        Coherence              coherence     = Coherence.clusterMember(configuration);

        try
            {
            coherence.getSession();
            fail("Expected IllegalArgumentException");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    @Test
    public void shouldGetNamedSession() throws Exception
        {
        ConfigurableCacheFactorySession    sessionSys    = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfSys        = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registrySys   = mock(InterceptorRegistry.class);

        ConfigurableCacheFactorySession    sessionOne    = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfOne        = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryOne   = mock(InterceptorRegistry.class);
        SessionProvider                    providerOne   = mock(SessionProvider.class);
        SessionConfiguration               cfgOne        = new SessionConfigStub(Coherence.DEFAULT_NAME, Collections.emptyList(), 0, providerOne);

        ConfigurableCacheFactorySession    sessionTwo    = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfTwo        = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryTwo   = mock(InterceptorRegistry.class);
        SessionProvider                    providerTwo   = mock(SessionProvider.class);
        SessionConfiguration               cfgTwo        = new SessionConfigStub("Two", Collections.emptyList(), 0, providerTwo);

        CoherenceConfiguration             configuration = CoherenceConfiguration.builder()
                                                                    .withSession(cfgOne)
                                                                    .withSessions(cfgTwo)
                                                                    .build();
        Coherence                          coherence     = Coherence.clusterMember(configuration);
        CompletableFuture<Coherence>       future        = coherence.whenStarted();

        when(sessionSys.getConfigurableCacheFactory()).thenReturn(ccfSys);
        when(sessionSys.getInterceptorRegistry()).thenReturn(registrySys);

        when(providerOne.createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class))).thenReturn(new ContextStub(sessionOne));
        when(sessionOne.getConfigurableCacheFactory()).thenReturn(ccfOne);
        when(sessionOne.getInterceptorRegistry()).thenReturn(registryOne);
        when(ccfOne.getServiceMap()).thenReturn(Collections.emptyMap());

        when(providerTwo.createSession(any(SessionConfiguration.class), any(SessionProvider.Context.class))).thenReturn(new ContextStub(sessionTwo));
        when(sessionTwo.getConfigurableCacheFactory()).thenReturn(ccfTwo);
        when(sessionTwo.getInterceptorRegistry()).thenReturn(registryTwo);
        when(ccfTwo.getServiceMap()).thenReturn(Collections.emptyMap());

        Coherence.setSystemSession(Optional.of(sessionSys));
        coherence.start();

        future.toCompletableFuture().get(1, TimeUnit.MINUTES);

        Session session = coherence.getSession(cfgTwo.getName());
        assertThat(session, is(sameInstance(sessionTwo)));
        }


    @Test
    public void shouldThrowGettingNamedSessionIfNotExists()
        {
        SessionProvider        providerOne   = mock(SessionProvider.class);
        SessionConfiguration   cfgOne        = new SessionConfigStub("Foo", Collections.emptyList(), 0, providerOne);

        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                                                                     .withSession(cfgOne)
                                                                     .build();
        Coherence              coherence     = Coherence.clusterMember(configuration);

        try
            {
            coherence.getSession("Bar");
            fail("Expected IllegalArgumentException");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    // ----- inner class: ConfigStub ----------------------------------------

    static class ConfigStub
            implements CoherenceConfiguration
        {
        ConfigStub()
            {
            this("foo", Collections.emptyMap(), Collections.emptyList());
            }

        public ConfigStub(String                            sName,
                          Map<String, SessionConfiguration> mapConfig, 
                          Iterable<EventInterceptor<?>>     listInterceptor)
            {
            f_sName           = sName;
            f_mapConfig       = mapConfig;
            f_listInterceptor = listInterceptor;
            }

        @Override
        public String getName()
            {
            return f_sName;
            }

        @Override
        public Map<String, SessionConfiguration> getSessionConfigurations()
            {
            return f_mapConfig;
            }

        @Override
        public Iterable<EventInterceptor<?>> getInterceptors()
            {
            return f_listInterceptor;
            }

        private final String                            f_sName;
        private final Map<String, SessionConfiguration> f_mapConfig;
        private final Iterable<EventInterceptor<?>>     f_listInterceptor;
        }

    // ----- inner class: SessionConfigStub ---------------------------------
    
    static class SessionConfigStub
            implements SessionConfiguration, SessionProvider.Provider
        {
        public SessionConfigStub(String sName)
            {
            this(sName, Collections.emptyList(), 0, null);
            }

        public SessionConfigStub(String sName,
                                 Iterable<EventInterceptor<?>> interceptors,
                                 int nPriority,
                                 SessionProvider provider)
            {
            f_sName        = sName;
            f_interceptors = interceptors;
            f_nPriority    = nPriority;
            m_provider     = provider;
            }

        @Override
        public String getName()
            {
            return f_sName;
            }

        @Override
        public String getScopeName()
            {
            return f_sName;
            }

        @Override
        public Iterable<EventInterceptor<?>> getInterceptors()
            {
            return f_interceptors;
            }

        @Override
        public boolean isEnabled()
            {
            return m_fEnabled;
            }

        public void setEnabled(boolean fEnabled)
            {
            m_fEnabled = fEnabled;
            }

        @Override
        public Optional<SessionProvider> getSessionProvider()
            {
            return Optional.ofNullable(m_provider);
            }

        public void setProvider(SessionProvider provider)
            {
            m_provider = provider;
            }

        @Override
        public int getPriority()
            {
            return f_nPriority;
            }

        private final String f_sName;
        private final Iterable<EventInterceptor<?>> f_interceptors;
        private final int f_nPriority;
        private SessionProvider m_provider;
        private boolean m_fEnabled = true;
        }

    // ----- inner class SessionBuilder -------------------------------------

    static class SessionBuilder
            implements SessionProvider
        {
        SessionBuilder(Session session)
            {
            f_session = session;
            }

        @Override
        public Context createSession(SessionConfiguration configuration, Context context)
            {
            return context.complete(f_session);
            }

        private final Session f_session;
        }

    // ----- inner class: ContextStub ---------------------------------------

    public static class ContextStub
            extends SessionProvider.DefaultContext
        {
        public ContextStub(Session session)
            {
            super(Coherence.Mode.ClusterMember, DefaultSessionProvider.INSTANCE, Collections.emptyList(), null);
            complete(session);
            }
        }

    // ----- data members ---------------------------------------------------

    private static final CoherenceConfiguration EMPTY_CONFIG = new ConfigStub();
    }
