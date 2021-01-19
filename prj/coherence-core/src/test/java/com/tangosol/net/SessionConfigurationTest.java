/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.mockito.Mockito.mock;

/**
 * @author Jonathan Knight  2020.11.10
 */
public class SessionConfigurationTest
    {
    @Test
    public void shouldCreateDefaultSessionConfiguration()
        {
        SessionConfiguration session = SessionConfiguration.defaultSession();
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(session.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        assertThat(session.getConfigUri().isPresent(), is(true));
        assertThat(session.getConfigUri().get(), is(CacheFactoryBuilder.URI_DEFAULT));
        assertThat(session.getInterceptors(), is(emptyIterable()));
        assertThat(session.isEnabled(), is(true));
        assertThat(session.getPriority(), is(SessionConfiguration.DEFAULT_PRIORITY));
        }

    @Test
    public void shouldCreateDefaultSessionConfigurationWithConfigUri()
        {
        SessionConfiguration session = SessionConfiguration.create("foo.xml");
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(session.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        assertThat(session.getConfigUri().isPresent(), is(true));
        assertThat(session.getConfigUri().get(), is("foo.xml"));
        assertThat(session.getInterceptors(), is(emptyIterable()));
        assertThat(session.isEnabled(), is(true));
        assertThat(session.getPriority(), is(SessionConfiguration.DEFAULT_PRIORITY));
        }

    @Test
    public void shouldCreateSessionConfigurationWithNameAndConfigUri()
        {
        SessionConfiguration session = SessionConfiguration.create("Foo", "foo.xml");
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is("Foo"));
        assertThat(session.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        assertThat(session.getConfigUri().isPresent(), is(true));
        assertThat(session.getConfigUri().get(), is("foo.xml"));
        assertThat(session.getInterceptors(), is(emptyIterable()));
        assertThat(session.isEnabled(), is(true));
        assertThat(session.getPriority(), is(SessionConfiguration.DEFAULT_PRIORITY));
        }

    @Test
    public void shouldCreateSessionConfigurationWithNameConfigUriAndScope()
        {
        SessionConfiguration session = SessionConfiguration.create("Foo", "foo.xml", "Bar");
        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is("Foo"));
        assertThat(session.getScopeName(), is("Bar"));
        assertThat(session.getConfigUri().isPresent(), is(true));
        assertThat(session.getConfigUri().get(), is("foo.xml"));
        assertThat(session.getInterceptors(), is(emptyIterable()));
        assertThat(session.isEnabled(), is(true));
        assertThat(session.getPriority(), is(SessionConfiguration.DEFAULT_PRIORITY));
        }

    @Test
    public void shouldBuildWithDefaultName()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        }

    @Test
    public void shouldBuildWithSpecifiedName()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .named("Foo")
                .build();

        assertThat(cfg.getName(), is("Foo"));
        }

    @Test
    public void shouldBuildWithDefaultScope()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        assertThat(cfg.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        }

    @Test
    public void shouldBuildWithSpecifiedNameAndScope()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .named("Foo")
                .withScopeName("Bar")
                .build();

        assertThat(cfg.getName(), is("Foo"));
        assertThat(cfg.getScopeName(), is("Bar"));
        }

    @Test
    public void shouldBuildWithDefaultClassLoader()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        Optional<ClassLoader> optional = cfg.getClassLoader();
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(false));
        }

    @Test
    public void shouldBuildWithSpecifiedClassLoader()
        {
        ClassLoader          loader = mock(ClassLoader.class);
        SessionConfiguration cfg    = SessionConfiguration
                .builder()
                .withClassLoader(loader)
                .build();

        Optional<ClassLoader> optional = cfg.getClassLoader();
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(true));
        assertThat(optional.get(), is(sameInstance(loader)));
        }

    @Test
    public void shouldBuildWithDefaultConfigURI()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        Optional<String> option = cfg.getConfigUri();
        assertThat(option, is(notNullValue()));
        assertThat(option.isPresent(), is(false));
        }

    @Test
    public void shouldBuildWithSpecifiedConfigURI()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .withConfigUri("foo.xml")
                .build();

        Optional<String> option = cfg.getConfigUri();
        assertThat(option, is(notNullValue()));
        assertThat(option.isPresent(), is(true));
        assertThat(option.get(), is("foo.xml"));
        }
    }
