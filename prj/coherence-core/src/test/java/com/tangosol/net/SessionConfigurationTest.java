/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.util.Options;

import com.tangosol.net.options.WithClassLoader;
import com.tangosol.net.options.WithConfiguration;
import com.tangosol.net.options.WithName;
import com.tangosol.net.options.WithScopeName;

import org.junit.Test;

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
        assertThat(session.getInterceptors(), is(emptyIterable()));
        assertThat(session.isEnabled(), is(true));
        assertThat(session.getSessionProvider(), is(notNullValue()));
        assertThat(session.getSessionProvider().isPresent(), is(false));
        assertThat(session.getPriority(), is(SessionConfiguration.DEFAULT_PRIORITY));
        }

    @Test
    public void shouldBuildWithDefaultName()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));

        Options<Session.Option> options = Options.from(Session.Option.class, cfg.getOptions());
        WithName                name    = options.get(WithName.class);
        assertThat(name, is(notNullValue()));
        assertThat(name.getName(), is(Coherence.DEFAULT_NAME));
        }

    @Test
    public void shouldBuildWithSpecifiedName()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .named("Foo")
                .build();

        assertThat(cfg.getName(), is("Foo"));

        Options<Session.Option> options = Options.from(Session.Option.class, cfg.getOptions());
        WithName                name    = options.get(WithName.class);
        assertThat(name, is(notNullValue()));
        assertThat(name.getName(), is("Foo"));
        }

    @Test
    public void shouldBuildWithDefaultScope()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        assertThat(cfg.getScopeName(), is(Coherence.DEFAULT_SCOPE));

        Options<Session.Option> options = Options.from(Session.Option.class, cfg.getOptions());
        WithScopeName           scope   = options.get(WithScopeName.class);
        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        }

    @Test
    public void shouldBuildWithNameAsScope()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .named("Foo")
                .build();

        assertThat(cfg.getScopeName(), is("Foo"));

        Options<Session.Option> options = Options.from(Session.Option.class, cfg.getOptions());
        WithScopeName           scope   = options.get(WithScopeName.class);
        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is("Foo"));
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

        Options<Session.Option> options = Options.from(Session.Option.class, cfg.getOptions());
        WithName                name    = options.get(WithName.class);
        WithScopeName           scope   = options.get(WithScopeName.class);

        assertThat(name, is(notNullValue()));
        assertThat(name.getName(), is("Foo"));

        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is("Bar"));
        }

    @Test
    public void shouldBuildWithDefaultClassLoader()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        Options<Session.Option> options    = Options.from(Session.Option.class, cfg.getOptions());
        WithClassLoader         withLoader = options.get(WithClassLoader.class);

        assertThat(withLoader, is(notNullValue()));
        assertThat(withLoader.getClassLoader(), is(sameInstance(WithClassLoader.autoDetect().getClassLoader())));
        }

    @Test
    public void shouldBuildWithSpecifiedClassLoader()
        {
        ClassLoader          loader = mock(ClassLoader.class);
        SessionConfiguration cfg    = SessionConfiguration
                .builder()
                .withClassLoader(loader)
                .build();

        Options<Session.Option> options    = Options.from(Session.Option.class, cfg.getOptions());
        WithClassLoader         withLoader = options.get(WithClassLoader.class);

        assertThat(withLoader, is(notNullValue()));
        assertThat(withLoader.getClassLoader(), is(sameInstance(loader)));
        }

    @Test
    public void shouldBuildWithDefaultConfigURI()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .build();

        Options<Session.Option> options    = Options.from(Session.Option.class, cfg.getOptions());
        WithConfiguration       withConfig = options.get(WithConfiguration.class);

        assertThat(withConfig, is(notNullValue()));
        assertThat(withConfig.getLocation(), is(WithConfiguration.autoDetect().getLocation()));
        }

    @Test
    public void shouldBuildWithSpecifiedConfigURI()
        {
        SessionConfiguration cfg = SessionConfiguration
                .builder()
                .withConfigUri("foo.xml")
                .build();

        Options<Session.Option> options    = Options.from(Session.Option.class, cfg.getOptions());
        WithConfiguration       withConfig = options.get(WithConfiguration.class);

        assertThat(withConfig, is(notNullValue()));
        assertThat(withConfig.getLocation(), is("foo.xml"));
        }

    @Test
    public void shouldCreateWithURI()
        {
        SessionConfiguration cfg = SessionConfiguration.create("foo.xml");

        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(cfg.getScopeName(), is(Coherence.DEFAULT_SCOPE));

        Options<Session.Option> options    = Options.from(Session.Option.class, cfg.getOptions());
        WithName                name       = options.get(WithName.class);
        WithScopeName           scope      = options.get(WithScopeName.class);
        WithClassLoader         withLoader = options.get(WithClassLoader.class);
        WithConfiguration       withConfig = options.get(WithConfiguration.class);

        assertThat(name, is(notNullValue()));
        assertThat(name.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        assertThat(withLoader, is(notNullValue()));
        assertThat(withLoader.getClassLoader(), is(sameInstance(WithClassLoader.autoDetect().getClassLoader())));
        assertThat(withConfig, is(notNullValue()));
        assertThat(withConfig.getLocation(), is("foo.xml"));
        }

    @Test
    public void shouldCreateWithNameURI()
        {
        SessionConfiguration cfg = SessionConfiguration.create("Foo", "foo.xml");

        assertThat(cfg.getName(), is("Foo"));
        assertThat(cfg.getScopeName(), is("Foo"));

        Options<Session.Option> options    = Options.from(Session.Option.class, cfg.getOptions());
        WithName                name       = options.get(WithName.class);
        WithScopeName           scope      = options.get(WithScopeName.class);
        WithClassLoader         withLoader = options.get(WithClassLoader.class);
        WithConfiguration       withConfig = options.get(WithConfiguration.class);

        assertThat(name, is(notNullValue()));
        assertThat(name.getName(), is("Foo"));
        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is("Foo"));
        assertThat(withLoader, is(notNullValue()));
        assertThat(withLoader.getClassLoader(), is(sameInstance(WithClassLoader.autoDetect().getClassLoader())));
        assertThat(withConfig, is(notNullValue()));
        assertThat(withConfig.getLocation(), is("foo.xml"));
        }

    @Test
    public void shouldCreateWithURIAndClassLoader()
        {
        ClassLoader          loader = mock(ClassLoader.class);
        SessionConfiguration cfg    = SessionConfiguration.create("foo.xml", loader);

        assertThat(cfg.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(cfg.getScopeName(), is(Coherence.DEFAULT_SCOPE));

        Options<Session.Option> options    = Options.from(Session.Option.class, cfg.getOptions());
        WithName                name       = options.get(WithName.class);
        WithScopeName           scope      = options.get(WithScopeName.class);
        WithClassLoader         withLoader = options.get(WithClassLoader.class);
        WithConfiguration       withConfig = options.get(WithConfiguration.class);

        assertThat(name, is(notNullValue()));
        assertThat(name.getName(), is(Coherence.DEFAULT_NAME));
        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        assertThat(withLoader, is(notNullValue()));
        assertThat(withLoader.getClassLoader(), is(sameInstance(loader)));
        assertThat(withConfig, is(notNullValue()));
        assertThat(withConfig.getLocation(), is("foo.xml"));
        }

    @Test
    public void shouldCreateWithNameURIAndClassLoader()
        {
        ClassLoader          loader = mock(ClassLoader.class);
        SessionConfiguration cfg    = SessionConfiguration.create("Foo", "foo.xml", loader);

        assertThat(cfg.getName(), is("Foo"));
        assertThat(cfg.getScopeName(), is("Foo"));

        Options<Session.Option> options    = Options.from(Session.Option.class, cfg.getOptions());
        WithName                name       = options.get(WithName.class);
        WithScopeName           scope      = options.get(WithScopeName.class);
        WithClassLoader         withLoader = options.get(WithClassLoader.class);
        WithConfiguration       withConfig = options.get(WithConfiguration.class);

        assertThat(name, is(notNullValue()));
        assertThat(name.getName(), is("Foo"));
        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is("Foo"));
        assertThat(withLoader, is(notNullValue()));
        assertThat(withLoader.getClassLoader(), is(sameInstance(loader)));
        assertThat(withConfig, is(notNullValue()));
        assertThat(withConfig.getLocation(), is("foo.xml"));
        }

    @Test
    public void shouldCreateWithNameURIAndScope()
        {
        ClassLoader          loader = mock(ClassLoader.class);
        SessionConfiguration cfg    = SessionConfiguration.create("Foo", "foo.xml", "Bar");

        assertThat(cfg.getName(), is("Foo"));
        assertThat(cfg.getScopeName(), is("Bar"));

        Options<Session.Option> options    = Options.from(Session.Option.class, cfg.getOptions());
        WithName                name       = options.get(WithName.class);
        WithScopeName           scope      = options.get(WithScopeName.class);
        WithClassLoader         withLoader = options.get(WithClassLoader.class);
        WithConfiguration       withConfig = options.get(WithConfiguration.class);

        assertThat(name, is(notNullValue()));
        assertThat(name.getName(), is("Foo"));
        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is("Bar"));
        assertThat(withLoader, is(notNullValue()));
        assertThat(withLoader.getClassLoader(), is(sameInstance(WithClassLoader.autoDetect().getClassLoader())));
        assertThat(withConfig, is(notNullValue()));
        assertThat(withConfig.getLocation(), is("foo.xml"));
        }
    }
