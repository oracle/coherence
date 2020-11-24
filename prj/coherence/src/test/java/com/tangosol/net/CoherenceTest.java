/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.net.events.CoherenceLifecycleEvent;
import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.events.InterceptorRegistry;

import com.tangosol.util.RegistrationBehavior;

import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
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

        Coherence coherence = Coherence.builder(config).build();
        assertThat(coherence.isStarted(), is(false));
        assertThat(coherence.getName(), is("foo"));
        assertThat(coherence.getConfiguration(), is(sameInstance(config)));
        }

    @Test
    public void shouldRegisterNewCoherenceInstance()
        {
        CoherenceConfiguration config = mock(CoherenceConfiguration.class);

        when(config.getName()).thenReturn("foo");

        Coherence             coherence = Coherence.create(config);
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

        Coherence             coherenceFoo = Coherence.builder(configFoo).build();
        Coherence             coherenceBar = Coherence.builder(configBar).build();
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
        Coherence coherence = Coherence.builder(EMPTY_CONFIG).build();
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
        Coherence                       coherence  = Coherence.create(EMPTY_CONFIG);
        CompletableFuture<Void>         future     = coherence.whenStarted();

        when(sessionSys.getConfigurableCacheFactory()).thenReturn(ccf);
        when(ccf.getInterceptorRegistry()).thenReturn(registry);

        Coherence.setSystemSession(sessionSys);
        coherence.start();

        future.toCompletableFuture().get(1, TimeUnit.MINUTES);

        assertThat(future.toCompletableFuture().isDone(), is(true));
        assertThat(future.toCompletableFuture().isCancelled(), is(false));
        assertThat(future.toCompletableFuture().isCompletedExceptionally(), is(false));
        }

    @Test
    public void shouldCompleteShutdownFutureOnShutdown()
        {
        Coherence               coherence = Coherence.create(EMPTY_CONFIG);
        CompletableFuture<Void> future    = coherence.whenClosed();
        coherence.close();

        assertThat(future.toCompletableFuture().isDone(), is(true));
        assertThat(future.toCompletableFuture().isCancelled(), is(false));
        assertThat(future.toCompletableFuture().isCompletedExceptionally(), is(false));
        }

    @Test
    public void shouldEnsureSystemSession()
        {
        ConfigurableCacheFactorySession    session      = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccf          = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registry     = mock(InterceptorRegistry.class);
        SessionProvider                    provider     = mock(SessionProvider.class);
        Session.Option[]                   options      = new Session.Option[0];
        EventInterceptor<?>                interceptor  = mock(EventInterceptor.class);
        Iterable<EventInterceptor<?>>      interceptors = Collections.singletonList(interceptor);
        SessionConfiguration               cfg          = new SessionConfigStub("SYS", options, interceptors, provider);

        when(provider.createSession(any(Session.Option.class))).thenReturn(session);
        when(session.getConfigurableCacheFactory()).thenReturn(ccf);
        when(session.getInterceptorRegistry()).thenReturn(registry);
        when(ccf.getServiceMap()).thenReturn(Collections.emptyMap());


        Coherence.setSystemSessionConfiguration(cfg);
        Session systemSession = Coherence.getSystemSession();

        assertThat(systemSession, is(sameInstance(session)));

        verify(provider).createSession(any(Session.Option.class));
        verify(session).getConfigurableCacheFactory();
        verify(ccf).activate();
        verify(registry).registerEventInterceptor(same(interceptor), eq(RegistrationBehavior.FAIL));
        }

    @Test
    public void shouldStartConfiguredSessions() throws Exception
        {
        ConfigurableCacheFactorySession    sessionSys      = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfSys          = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registrySys     = mock(InterceptorRegistry.class);

        ConfigurableCacheFactorySession    sessionOne      = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfOne          = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryOne     = mock(InterceptorRegistry.class);
        SessionProvider                    providerOne     = mock(SessionProvider.class);
        Session.Option[]                   optionsOne      = new Session.Option[0];
        EventInterceptor<?>                interceptorOne  = mock(EventInterceptor.class);
        Iterable<EventInterceptor<?>>      interceptorsOne = Collections.singletonList(interceptorOne);
        SessionConfiguration               cfgOne          = new SessionConfigStub("One", optionsOne, interceptorsOne, providerOne);

        ConfigurableCacheFactorySession    sessionTwo      = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfTwo          = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryTwo     = mock(InterceptorRegistry.class);
        SessionProvider                    providerTwo     = mock(SessionProvider.class);
        Session.Option[]                   optionsTwo      = new Session.Option[0];
        EventInterceptor<?>                interceptorTwo  = mock(EventInterceptor.class);
        Iterable<EventInterceptor<?>>      interceptorsTwo = Collections.singletonList(interceptorTwo);
        SessionConfiguration               cfgTwo          = new SessionConfigStub("Two", optionsTwo, interceptorsTwo, providerTwo);

        CoherenceConfiguration             configuration   = CoherenceConfiguration.builder()
                                                                    .withSession(cfgOne)
                                                                    .withSessions(cfgTwo)
                                                                    .build();
        Coherence                          coherence       = Coherence.create(configuration);
        CompletableFuture<Void>            future          = coherence.whenStarted();

        when(sessionSys.getConfigurableCacheFactory()).thenReturn(ccfSys);
        when(sessionSys.getInterceptorRegistry()).thenReturn(registrySys);

        when(providerOne.createSession(any(Session.Option.class))).thenReturn(sessionOne);
        when(sessionOne.getConfigurableCacheFactory()).thenReturn(ccfOne);
        when(sessionOne.getInterceptorRegistry()).thenReturn(registryOne);
        when(ccfOne.getServiceMap()).thenReturn(Collections.emptyMap());

        when(providerTwo.createSession(any(Session.Option.class))).thenReturn(sessionTwo);
        when(sessionTwo.getConfigurableCacheFactory()).thenReturn(ccfTwo);
        when(sessionTwo.getInterceptorRegistry()).thenReturn(registryTwo);
        when(ccfTwo.getServiceMap()).thenReturn(Collections.emptyMap());

        Coherence.setSystemSession(sessionSys);
        coherence.start();

        future.toCompletableFuture().get(1, TimeUnit.MINUTES);

        assertThat(coherence.getSession("One"), is(sameInstance(sessionOne)));
        assertThat(coherence.getSession("Two"), is(sameInstance(sessionTwo)));

        verify(providerOne, times(2)).createSession(any(Session.Option.class));
        verify(providerTwo, times(2)).createSession(any(Session.Option.class));
        
        verify(registryOne).registerEventInterceptor(interceptorOne, RegistrationBehavior.FAIL);
        verify(registryTwo).registerEventInterceptor(interceptorTwo, RegistrationBehavior.FAIL);

        verify(ccfOne).activate();
        verify(ccfTwo).activate();
        }


    @Test
    public void shouldRegisterLifecycleInterceptor()
        {
        ConfigurableCacheFactorySession    sessionSys = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfSys     = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registry   = mock(InterceptorRegistry.class);
        Session                            session    = mock(Session.class);

        when(sessionSys.getConfigurableCacheFactory()).thenReturn(ccfSys);
        when(sessionSys.getInterceptorRegistry()).thenReturn(registry);
        when(session.getInterceptorRegistry()).thenReturn(registry);

        Coherence.setSystemSession(sessionSys);

        SessionConfigStub cfgSession = new SessionConfigStub("Foo");

        cfgSession.setProvider(new SessionBuilder(session));

        List<CoherenceLifecycleEvent> lifecycleEvents      = new ArrayList<>();
        Coherence.LifecycleListener   lifecycleInterceptor = lifecycleEvents::add;

        CoherenceConfiguration config = CoherenceConfiguration.builder()
                .withSession(cfgSession)
                .withEventInterceptor(lifecycleInterceptor)
                .build();

        Coherence coherence = Coherence.builder(config).build();

        coherence.start().join();
        Eventually.assertDeferred(lifecycleEvents::size, is(2));

        coherence.close();
        assertThat(lifecycleEvents.size(), is(4));

        assertThat(lifecycleEvents.get(0).getCoherence(), is(sameInstance(coherence)));
        assertThat(lifecycleEvents.get(0).getType(), is(CoherenceLifecycleEvent.Type.STARTING));
        assertThat(lifecycleEvents.get(1).getCoherence(), is(sameInstance(coherence)));
        assertThat(lifecycleEvents.get(1).getType(), is(CoherenceLifecycleEvent.Type.STARTED));
        assertThat(lifecycleEvents.get(2).getCoherence(), is(sameInstance(coherence)));
        assertThat(lifecycleEvents.get(2).getType(), is(CoherenceLifecycleEvent.Type.STOPPING));
        assertThat(lifecycleEvents.get(3).getCoherence(), is(sameInstance(coherence)));
        assertThat(lifecycleEvents.get(3).getType(), is(CoherenceLifecycleEvent.Type.STOPPED));
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
        SessionProvider                    providerOne   = mock(SessionProvider.class);
        SessionConfiguration               cfgOne        = new SessionConfigStub(Coherence.DEFAULT_NAME, new Session.Option[0], Collections.emptyList(), providerOne);

        ConfigurableCacheFactorySession    sessionTwo    = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfTwo        = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryTwo   = mock(InterceptorRegistry.class);
        SessionProvider                    providerTwo   = mock(SessionProvider.class);
        SessionConfiguration               cfgTwo        = new SessionConfigStub("Two", new Session.Option[0], Collections.emptyList(), providerTwo);

        CoherenceConfiguration             configuration = CoherenceConfiguration.builder()
                                                                    .withSession(cfgOne)
                                                                    .withSessions(cfgTwo)
                                                                    .build();
        Coherence                          coherence     = Coherence.create(configuration);
        CompletableFuture<Void>            future        = coherence.whenStarted();

        when(sessionSys.getConfigurableCacheFactory()).thenReturn(ccfSys);
        when(sessionSys.getInterceptorRegistry()).thenReturn(registrySys);

        when(providerOne.createSession(any(Session.Option.class))).thenReturn(sessionOne);
        when(sessionOne.getConfigurableCacheFactory()).thenReturn(ccfOne);
        when(sessionOne.getInterceptorRegistry()).thenReturn(registryOne);
        when(ccfOne.getServiceMap()).thenReturn(Collections.emptyMap());

        when(providerTwo.createSession(any(Session.Option.class))).thenReturn(sessionTwo);
        when(sessionTwo.getConfigurableCacheFactory()).thenReturn(ccfTwo);
        when(sessionTwo.getInterceptorRegistry()).thenReturn(registryTwo);
        when(ccfTwo.getServiceMap()).thenReturn(Collections.emptyMap());

        Coherence.setSystemSession(sessionSys);
        coherence.start();

        future.toCompletableFuture().get(1, TimeUnit.MINUTES);

        Session session = coherence.getSession();
        assertThat(session, is(sameInstance(sessionOne)));
        }

    @Test
    public void shouldThrowGettingDefaultSessionIfNotExists() throws Exception
        {
        SessionProvider        providerOne   = mock(SessionProvider.class);
        SessionConfiguration   cfgOne        = new SessionConfigStub("Foo", new Session.Option[0], Collections.emptyList(), providerOne);

        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                                                                    .withSession(cfgOne)
                                                                    .build();
        Coherence              coherence     = Coherence.create(configuration);

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
        SessionConfiguration               cfgOne        = new SessionConfigStub(Coherence.DEFAULT_NAME, new Session.Option[0], Collections.emptyList(), providerOne);

        ConfigurableCacheFactorySession    sessionTwo    = mock(ConfigurableCacheFactorySession.class);
        ExtensibleConfigurableCacheFactory ccfTwo        = mock(ExtensibleConfigurableCacheFactory.class);
        InterceptorRegistry                registryTwo   = mock(InterceptorRegistry.class);
        SessionProvider                    providerTwo   = mock(SessionProvider.class);
        SessionConfiguration               cfgTwo        = new SessionConfigStub("Two", new Session.Option[0], Collections.emptyList(), providerTwo);

        CoherenceConfiguration             configuration = CoherenceConfiguration.builder()
                                                                    .withSession(cfgOne)
                                                                    .withSessions(cfgTwo)
                                                                    .build();
        Coherence                          coherence     = Coherence.create(configuration);
        CompletableFuture<Void>            future        = coherence.whenStarted();

        when(sessionSys.getConfigurableCacheFactory()).thenReturn(ccfSys);
        when(sessionSys.getInterceptorRegistry()).thenReturn(registrySys);

        when(providerOne.createSession(any(Session.Option.class))).thenReturn(sessionOne);
        when(sessionOne.getConfigurableCacheFactory()).thenReturn(ccfOne);
        when(sessionOne.getInterceptorRegistry()).thenReturn(registryOne);
        when(ccfOne.getServiceMap()).thenReturn(Collections.emptyMap());

        when(providerTwo.createSession(any(Session.Option.class))).thenReturn(sessionTwo);
        when(sessionTwo.getConfigurableCacheFactory()).thenReturn(ccfTwo);
        when(sessionTwo.getInterceptorRegistry()).thenReturn(registryTwo);
        when(ccfTwo.getServiceMap()).thenReturn(Collections.emptyMap());

        Coherence.setSystemSession(sessionSys);
        coherence.start();

        future.toCompletableFuture().get(1, TimeUnit.MINUTES);

        Session session = coherence.getSession(cfgTwo.getName());
        assertThat(session, is(sameInstance(sessionTwo)));
        }


    @Test
    public void shouldThrowGettingNamedSessionIfNotExists() throws Exception
        {
        SessionProvider        providerOne   = mock(SessionProvider.class);
        SessionConfiguration   cfgOne        = new SessionConfigStub("Foo", new Session.Option[0], Collections.emptyList(), providerOne);

        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                                                                     .withSession(cfgOne)
                                                                     .build();
        Coherence              coherence     = Coherence.create(configuration);

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
        
        private final String f_sName;
        private final Map<String, SessionConfiguration> f_mapConfig;
        private final Iterable<EventInterceptor<?>> f_listInterceptor;
        }

    // ----- inner class: SessionConfigStub ---------------------------------
    
    static class SessionConfigStub
            implements SessionConfiguration
        {
        public SessionConfigStub(String sName)
            {
            this(sName, new Session.Option[0], Collections.emptyList(), null);
            }

        public SessionConfigStub(String sName,
                                 Session.Option[] options,
                                 Iterable<EventInterceptor<?>> interceptors,
                                 SessionProvider provider)
            {
            f_sName        = sName;
            f_options      = options;
            f_interceptors = interceptors;
            m_provider = provider;
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
        public Session.Option[] getOptions()
            {
            return f_options;
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

        private final String f_sName;
        private final Session.Option[] f_options;
        private final Iterable<EventInterceptor<?>> f_interceptors;
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
        public Session createSession(Session.Option... options)
            {
            return f_session;
            }

        private final Session f_session;
        }

    // ----- data members ---------------------------------------------------

    private static final CoherenceConfiguration EMPTY_CONFIG = new ConfigStub();
    }
