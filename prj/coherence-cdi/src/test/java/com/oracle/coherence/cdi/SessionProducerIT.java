/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi;

import java.lang.annotation.Annotation;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import com.tangosol.net.Session;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the {@link com.oracle.coherence.cdi.SessionProducer}
 * using the Weld JUnit extension.
 *
 * @author jk  2019.10.19
 */
@ExtendWith(WeldJunit5Extension.class)
class SessionProducerIT
    {

    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addBeanClass(CacheFactoryUriResolver.Default.class)
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
    @CacheFactory("test-cache-config.xml")
    private Session testSession;

    /**
     * Should inject the the default Session.
     */
    @Inject
    @CacheFactory("")
    private Session qualifiedDefaultSession;

    /**
     * Should inject the the default Session.
     */
    @Inject
    @CacheFactory(" ")
    private Session namedDefaultSession;

    @Test
    void shouldInjectDefaultSession()
        {
        assertThat(defaultSession, is(notNullValue()));
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
        assertThat(qualifiedDefaultSession, is(sameInstance(defaultSession)));
        }

    @Test
    void shouldInjectNamedDefaultSession()
        {
        assertThat(namedDefaultSession, is(notNullValue()));
        assertThat(namedDefaultSession, is(sameInstance(defaultSession)));
        }

    @Test
    void shouldGetDynamicCCF()
        {
        Annotation qualifier = CacheFactory.Literal.of("test-cache-config.xml");
        Instance<Session> instance = weld.select(Session.class, qualifier);

        assertThat(instance.isResolvable(), is(true));
        assertThat(instance.get(), is(sameInstance(testSession)));
        }
    }
