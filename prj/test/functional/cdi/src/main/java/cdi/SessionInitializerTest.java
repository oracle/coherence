/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cdi;

import com.oracle.coherence.cdi.ConfigUri;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SessionInitializer;
import com.tangosol.net.Coherence;

import com.tangosol.net.options.WithConfiguration;

import org.junit.Test;

import javax.inject.Named;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
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
        }

    @Test
    public void shouldUseDefaultScopeIfNoneSet()
        {
        @Named("Foo")
        class TestSession
            implements SessionInitializer
            {
            }

        TestSession testSession = new TestSession();
        assertThat(testSession.getScopeName(), is(Coherence.DEFAULT_SCOPE));
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
        }

    @Test
    public void shouldUseDefaultConfigURI()
        {
        class TestSession
            implements SessionInitializer
            {
            }

        TestSession testSession = new TestSession();

        assertThat(testSession.getConfigUri(), is(notNullValue()));
        assertThat(testSession.getConfigUri().isPresent(), is(true));
        assertThat(testSession.getConfigUri().get(), is(WithConfiguration.autoDetect().getLocation()));
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
        assertThat(testSession.getConfigUri(), is(notNullValue()));
        assertThat(testSession.getConfigUri().isPresent(), is(true));
        assertThat(testSession.getConfigUri().get(), is("foo.xml"));
        }
    }
