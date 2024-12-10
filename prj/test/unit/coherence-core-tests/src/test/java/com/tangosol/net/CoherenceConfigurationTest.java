/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.ArrayMatching.hasItemInArray;
import static org.hamcrest.collection.ArrayMatching.arrayContainingInAnyOrder;

import static org.mockito.Mockito.mock;

/**
 * @author Jonathan Knight  2020.11.11
 */
public class CoherenceConfigurationTest
    {
    @Test
    public void shouldCreateDefaultConfiguration()
        {
        assertDefaultConfiguration(CoherenceConfiguration.create());
        }

    @Test
    public void shouldBuildDefaultConfiguration()
        {
        assertDefaultConfiguration(CoherenceConfiguration.builder().build());
        }

    @Test
    public void shouldCreateWithName()
        {
        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                .named("Foo")
                .build();

        assertThat(cfg, is(notNullValue()));
        assertThat(cfg.getName(), is("Foo"));
        assertThat(cfg.getInterceptors(), is(emptyIterable()));
        assertThat(cfg.getSessionConfigurations(), is(notNullValue()));
        assertThat(cfg.getSessionConfigurations().size(), is(1));
        assertDefaultSession(cfg.getSessionConfigurations().get(Coherence.DEFAULT_NAME));
        }

    @Test
    public void shouldCreateWithInterceptor()
        {
        EventDispatcherAwareInterceptor<?> interceptor = mock(EventDispatcherAwareInterceptor.class);
        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                .withEventInterceptor(interceptor)
                .build();

        assertThat(cfg, is(notNullValue()));

        EventDispatcherAwareInterceptor<?>[] interceptors = StreamSupport
                .stream(cfg.getInterceptors().spliterator(), false)
                .toArray(EventDispatcherAwareInterceptor<?>[]::new);

        assertThat(interceptors, hasItemInArray(interceptor));

        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(cfg.getSessionConfigurations(), is(notNullValue()));
        assertThat(cfg.getSessionConfigurations().size(), is(1));
        assertDefaultSession(cfg.getSessionConfigurations().get(Coherence.DEFAULT_NAME));
        }

    @Test
    public void shouldCreateWithInterceptorsFromVarargs()
        {
        EventDispatcherAwareInterceptor<?> interceptor1 = mock(EventDispatcherAwareInterceptor.class);
        EventDispatcherAwareInterceptor<?> interceptor2 = mock(EventDispatcherAwareInterceptor.class);
        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                .withEventInterceptors(interceptor1, interceptor2)
                .build();

        assertThat(cfg, is(notNullValue()));

        EventDispatcherAwareInterceptor<?>[] interceptors = StreamSupport
                .stream(cfg.getInterceptors().spliterator(), false)
                .toArray(EventDispatcherAwareInterceptor<?>[]::new);

        assertThat(interceptors, arrayContainingInAnyOrder(interceptor1, interceptor2));

        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(cfg.getSessionConfigurations(), is(notNullValue()));
        assertThat(cfg.getSessionConfigurations().size(), is(1));
        assertDefaultSession(cfg.getSessionConfigurations().get(Coherence.DEFAULT_NAME));
        }

    @Test
    public void shouldCreateWithInterceptorsFromIterable()
        {
        EventDispatcherAwareInterceptor<?> interceptor1 = mock(EventDispatcherAwareInterceptor.class);
        EventDispatcherAwareInterceptor<?> interceptor2 = mock(EventDispatcherAwareInterceptor.class);
        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                .withEventInterceptors(Arrays.asList(interceptor1, interceptor2))
                .build();

        assertThat(cfg, is(notNullValue()));

        EventDispatcherAwareInterceptor<?>[] interceptors = StreamSupport
                .stream(cfg.getInterceptors().spliterator(), false)
                .toArray(EventDispatcherAwareInterceptor<?>[]::new);

        assertThat(interceptors, arrayContainingInAnyOrder(interceptor1, interceptor2));

        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(cfg.getSessionConfigurations(), is(notNullValue()));
        assertThat(cfg.getSessionConfigurations().size(), is(1));
        assertDefaultSession(cfg.getSessionConfigurations().get(Coherence.DEFAULT_NAME));
        }

    @Test
    public void shouldCreateWithSession()
        {
        SessionConfiguration   session = SessionConfiguration.builder().named("Foo").withConfigUri("foo.xml").build();
        CoherenceConfiguration cfg     = CoherenceConfiguration.builder()
                .withSession(session)
                .build();

        assertThat(cfg, is(notNullValue()));
        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(cfg.getInterceptors(), is(emptyIterable()));

        Map<String, SessionConfiguration> mapSession = cfg.getSessionConfigurations();
        assertThat(mapSession, is(notNullValue()));
        assertThat(mapSession.size(), is(1));
        assertThat(mapSession.get(session.getName()), is(sameInstance(session)));
        }

    @Test
    public void shouldNotAddDisabledSession()
        {
        SessionConfiguration   session  = SessionConfiguration.builder().named("Foo").withConfigUri("foo.xml").build();
        SessionConfiguration   disabled = new SessionConfigurationStub(SessionConfiguration.builder().named("Bar").withConfigUri("bar.xml").build(), false);
        CoherenceConfiguration cfg      = CoherenceConfiguration.builder()
                .withSession(session)
                .withSession(disabled)
                .build();

        assertThat(cfg, is(notNullValue()));
        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(cfg.getInterceptors(), is(emptyIterable()));

        Map<String, SessionConfiguration> mapSession = cfg.getSessionConfigurations();
        assertThat(mapSession, is(notNullValue()));
        assertThat(mapSession.size(), is(1));
        assertThat(mapSession.get(session.getName()), is(sameInstance(session)));
        }

    @Test
    public void shouldCreateWithSessionsVararg()
        {
        SessionConfiguration   session1 = SessionConfiguration.builder().named("Foo").withConfigUri("foo.xml").build();
        SessionConfiguration   session2 = SessionConfiguration.builder().named("Bar").withConfigUri("foo.xml").build();
        CoherenceConfiguration cfg     = CoherenceConfiguration.builder()
                .withSessions(session1, session2)
                .build();

        assertThat(cfg, is(notNullValue()));
        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(cfg.getInterceptors(), is(emptyIterable()));

        Map<String, SessionConfiguration> mapSession = cfg.getSessionConfigurations();
        assertThat(mapSession, is(notNullValue()));
        assertThat(mapSession.size(), is(2));
        assertThat(mapSession.get(session1.getName()), is(sameInstance(session1)));
        assertThat(mapSession.get(session2.getName()), is(sameInstance(session2)));
        }

    @Test
    public void shouldCreateWithSessionsIterable()
        {
        SessionConfiguration   session1 = SessionConfiguration.builder().named("Foo").withConfigUri("foo.xml").build();
        SessionConfiguration   session2 = SessionConfiguration.builder().named("Bar").withConfigUri("foo.xml").build();
        CoherenceConfiguration cfg     = CoherenceConfiguration.builder()
                .withSessions(Arrays.asList(session1, session2))
                .build();

        assertThat(cfg, is(notNullValue()));
        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(cfg.getInterceptors(), is(emptyIterable()));

        Map<String, SessionConfiguration> mapSession = cfg.getSessionConfigurations();
        assertThat(mapSession, is(notNullValue()));
        assertThat(mapSession.size(), is(2));
        assertThat(mapSession.get(session1.getName()), is(sameInstance(session1)));
        assertThat(mapSession.get(session2.getName()), is(sameInstance(session2)));
        }

    // ----- helper methods -------------------------------------------------

    public void assertDefaultConfiguration(CoherenceConfiguration cfg)
        {
        assertThat(cfg, is(notNullValue()));
        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(cfg.getInterceptors(), is(emptyIterable()));
        assertThat(cfg.getSessionConfigurations(), is(notNullValue()));
        assertThat(cfg.getSessionConfigurations().size(), is(1));
        assertDefaultSession(cfg.getSessionConfigurations().get(Coherence.DEFAULT_NAME));
        }

    public void assertDefaultSession(SessionConfiguration session)
        {
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(session.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        assertThat(session.getInterceptors(), is(emptyIterable()));
        assertThat(session.isEnabled(), is(true));
        assertThat(session.getPriority(), is(SessionConfiguration.DEFAULT_PRIORITY));
        }

    static class SessionConfigurationStub
            implements SessionConfiguration
        {
        public SessionConfigurationStub(SessionConfiguration wrapped)
            {
            this(wrapped, true);
            }

        public SessionConfigurationStub(SessionConfiguration wrapped, boolean fEnabled)
            {
            f_wrapped  = wrapped;
            f_fEnabled = fEnabled;
            }

        @Override
        public boolean isEnabled()
            {
            return f_fEnabled;
            }

        @Override
        public String getName()
            {
            return f_wrapped.getName();
            }

        @Override
        public String getScopeName()
            {
            return f_wrapped.getScopeName();
            }

        private final SessionConfiguration f_wrapped;

        private final boolean f_fEnabled;
        }
    }
