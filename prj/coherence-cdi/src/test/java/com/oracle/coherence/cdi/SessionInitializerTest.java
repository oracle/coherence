/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.inject.ConfigUri;
import com.oracle.coherence.inject.Scope;
import com.oracle.coherence.inject.SessionInitializer;
import com.oracle.coherence.common.util.Options;

import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import com.tangosol.net.options.WithConfiguration;
import com.tangosol.net.options.WithName;
import com.tangosol.net.options.WithScopeName;

import org.junit.Test;

import javax.inject.Named;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2020.11.10
 */
public class SessionInitializerTest
    {
    @Test
    public void shouldUseDefaultName()
        {
        class TestSession
                implements SessionInitializer
            {
            }

        TestSession testSession = new TestSession();
        assertThat(testSession.getName(), is(Coherence.DEFAULT_NAME));

        Options<Session.Option> options = Options.from(Session.Option.class, testSession.getOptions());
        WithName                name    = options.get(WithName.class);
        assertThat(name, is(notNullValue()));
        assertThat(name.getName(), is(Coherence.DEFAULT_NAME));
        }

    @Test
    public void shouldUseSpecifiedName()
        {
        @Named("Foo")
        class TestSession
            implements SessionInitializer
            {
            }

        TestSession testSession = new TestSession();
        assertThat(testSession.getName(), is("Foo"));

        Options<Session.Option> options = Options.from(Session.Option.class, testSession.getOptions());
        WithName                name    = options.get(WithName.class);
        assertThat(name, is(notNullValue()));
        assertThat(name.getName(), is("Foo"));
        }

    @Test
    public void shouldUseDefaultScope()
        {
        class TestSession
            implements SessionInitializer
            {
            }

        TestSession testSession = new TestSession();
        assertThat(testSession.getScopeName(), is(Coherence.DEFAULT_SCOPE));

        Options<Session.Option> options = Options.from(Session.Option.class, testSession.getOptions());
        WithScopeName           scope   = options.get(WithScopeName.class);
        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is(Coherence.DEFAULT_SCOPE));
        }

    @Test
    public void shouldUseNameAsScope()
        {
        @Named("Foo")
        class TestSession
            implements SessionInitializer
            {
            }

        TestSession testSession = new TestSession();
        assertThat(testSession.getScopeName(), is("Foo"));

        Options<Session.Option> options = Options.from(Session.Option.class, testSession.getOptions());
        WithScopeName           scope   = options.get(WithScopeName.class);
        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is("Foo"));
        }

    @Test
    public void shouldUseSpecifiedScope()
        {
        @Scope("Foo")
        class TestSession
            implements SessionInitializer
            {
            }

        TestSession testSession = new TestSession();
        assertThat(testSession.getScopeName(), is("Foo"));

        Options<Session.Option> options = Options.from(Session.Option.class, testSession.getOptions());
        WithScopeName           scope   = options.get(WithScopeName.class);
        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is("Foo"));
        }

    @Test
    public void shouldUseSpecifiedNameAndScope()
        {
        @Named("Foo")
        @Scope("Bar")
        class TestSession
            implements SessionInitializer
            {
            }

        TestSession testSession = new TestSession();
        assertThat(testSession.getName(), is("Foo"));
        assertThat(testSession.getScopeName(), is("Bar"));

        Options<Session.Option> options = Options.from(Session.Option.class, testSession.getOptions());

        WithName                name    = options.get(WithName.class);
        assertThat(name, is(notNullValue()));
        assertThat(name.getName(), is("Foo"));

        WithScopeName           scope   = options.get(WithScopeName.class);
        assertThat(scope, is(notNullValue()));
        assertThat(scope.getScopeName(), is("Bar"));
        }

    @Test
    public void shouldUseDefaultConfigURI()
        {
        class TestSession
            implements SessionInitializer
            {
            }

        TestSession             testSession       = new TestSession();
        Options<Session.Option> options           = Options.from(Session.Option.class, testSession.getOptions());
        WithConfiguration       withConfiguration = options.get(WithConfiguration.class);
        assertThat(withConfiguration, is(notNullValue()));
        assertThat(withConfiguration.getLocation(), is(WithConfiguration.autoDetect().getLocation()));
        }

    @Test
    public void shouldUseSpecifiedConfigURI()
        {
        @ConfigUri("foo.xml")
        class TestSession
            implements SessionInitializer
            {
            }

        TestSession             testSession       = new TestSession();
        Options<Session.Option> options           = Options.from(Session.Option.class, testSession.getOptions());
        WithConfiguration       withConfiguration = options.get(WithConfiguration.class);
        assertThat(withConfiguration, is(notNullValue()));
        assertThat(withConfiguration.getLocation(), is("foo.xml"));
        }
    }
