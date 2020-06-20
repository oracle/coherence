/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.Scope;

import com.tangosol.net.Session;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.Instance;

import javax.inject.Inject;

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
                                                          .addExtension(new CoherenceServerExtension())
                                                          .addBeanClass(CacheFactoryUriResolver.Default.class)
                                                          .addBeanClass(ConfigurableCacheFactoryProducer.class)
                                                          .addBeanClass(SessionProducer.class));

    /**
     * Should inject the default Session.
     */
    @Inject
    private Session defaultSession;

    /**
     * Should inject the test Session.
     */
    @Inject
    @Scope("test-config.xml")
    private Session testSession;

    /**
     * Should inject the the default Session.
     */
    @Inject
    @Scope
    private Session qualifiedDefaultSession;

    /**
     * Should inject the the default Session.
     */
    @Inject
    @Scope(" ")
    private Session namedDefaultSession;

    /**
     * Should inject the the system Session.
     */
    @Inject
    @Scope(Scope.SYSTEM)
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

    @Test
    void shouldGetDynamicSession()
        {
        Annotation qualifier = Scope.Literal.of("test-config.xml");
        Instance<Session> instance = weld.select(Session.class, qualifier);

        assertThat(instance.isResolvable(), is(true));
        }
    }
