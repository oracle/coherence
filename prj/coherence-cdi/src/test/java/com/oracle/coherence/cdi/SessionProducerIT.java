/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.inject.ConfigUri;
import com.oracle.coherence.inject.Name;
import com.oracle.coherence.inject.Scope;
import com.oracle.coherence.inject.SessionInitializer;
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the {@link SessionProducer}
 * using the Weld JUnit extension.
 *
 * @author Jonathan Knight  2019.10.19
*/
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionProducerIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addBeanClass(SessionOne.class)
                                                          .addBeanClass(SessionProducer.class));

    @ApplicationScoped
    @Named("TestSession")
    @Scope("Test")
    @ConfigUri("test-config.xml")
    private static class SessionOne
            implements SessionInitializer
        {
        }

    /**
     * Should inject the test Session.
     */
    @Inject
    @Name("TestSession")
    private Session testSession;

    /**
     * Should inject the default Session.
     */
    @Inject
    private Session defaultSession;

    /**
     * Should inject the the default Session.
     */
    @Inject
    @Name(Coherence.DEFAULT_NAME)
    private Session qualifiedDefaultSession;

    /**
     * Should inject the the default Session.
     */
    @Inject
    @Name(" ")
    private Session namedDefaultSession;

    /**
     * Should inject the the system Session.
     */
    @Inject
    @Name(Coherence.SYSTEM_SESSION)
    private Session systemSession;

    @Test
    void shouldInjectDefaultSession()
        {
        assertThat(defaultSession, is(notNullValue()));
        }

    @Test
    void shouldInjectSystemSession()
        {
        assertThat(systemSession, is(notNullValue()));
        }

    @Test
    void shouldInjectTestSession()
        {
        assertThat(testSession, is(notNullValue()));
        }

    @Test
    void shouldInjectQualifiedDefaultSession()
        {
        assertThat(qualifiedDefaultSession, is(notNullValue()));
        }

    @Test
    void shouldInjectNamedDefaultSession()
        {
        assertThat(namedDefaultSession, is(notNullValue()));
        }
    }
