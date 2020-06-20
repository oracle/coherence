/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.Scope;

import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Integration test for the {@link ConfigurableCacheFactoryProducer} using the
 * Weld JUnit extension.
 *
 * @author Jonathan Knight  2019.10.19
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConfigurableCacheFactoryProducerIT
    {

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                         .addExtension(new CoherenceExtension())
                                                         .addExtension(new CoherenceServerExtension())
                                                         .addBeanClass(CacheFactoryUriResolver.Default.class)
                                                         .addBeanClass(ConfigurableCacheFactoryProducer.class));

    @Inject
    private CoherenceServerExtension extension;

    /**
     * Should inject the default CCF.
     */
    @Inject
    private ConfigurableCacheFactory defaultCacheFactory;

    /**
     * Should inject the test CCF.
     */
    @Inject
    @Scope("test-config.xml")
    private ConfigurableCacheFactory testCacheFactory;

    /**
     * Should inject the the default CCF.
     */
    @Inject
    @Scope("")
    private ConfigurableCacheFactory qualifiedDefaultCacheFactory;

    /**
     * Should inject the the default CCF.
     */
    @Inject
    @Scope(" ")
    private ConfigurableCacheFactory namedDefaultCacheFactory;

    /**
     * Should inject the the system CCF.
     */
    @Inject
    @Scope(Scope.SYSTEM)
    private ConfigurableCacheFactory systemCacheFactory;

    /**
     * Should inject cache factory builder.
     */
    @Inject
    private CacheFactoryBuilder builder;

    @Test
    void shouldInjectDefaultConfigurableCacheFactory()
        {
        assertThat(defaultCacheFactory, is(notNullValue()));
        assertThat(((ExtensibleConfigurableCacheFactory) defaultCacheFactory).getScopeName(), is(""));
        }

    @Test
    void shouldInjectTestConfigurableCacheFactory()
        {
        assertThat(testCacheFactory, is(notNullValue()));
        assertThat(testCacheFactory, is(not(sameInstance(defaultCacheFactory))));
        assertThat(((ExtensibleConfigurableCacheFactory) testCacheFactory).getScopeName(), is("test"));
        }

    @Test
    void shouldInjectQualifiedDefaultConfigurableCacheFactory()
        {
        assertThat(qualifiedDefaultCacheFactory, is(notNullValue()));
        assertThat(qualifiedDefaultCacheFactory, is(sameInstance(defaultCacheFactory)));
        }

    @Test
    void shouldInjectNamedDefaultConfigurableCacheFactory()
        {
        assertThat(namedDefaultCacheFactory, is(notNullValue()));
        assertThat(namedDefaultCacheFactory, is(sameInstance(defaultCacheFactory)));
        }

    @Test
    void shouldInjectSystemConfigurableCacheFactory()
        {
        assertThat(systemCacheFactory, is(notNullValue()));
        assertThat(systemCacheFactory, is(sameInstance(extension.getSystemCacheFactory())));
        }

    @Test
    void shouldGetDynamicCCF()
        {
        Annotation qualifier = Scope.Literal.of("test-config.xml");
        Instance<ConfigurableCacheFactory> instance = weld.select(ConfigurableCacheFactory.class, qualifier);

        assertThat(instance.isResolvable(), is(true));
        assertThat(instance.get(), is(sameInstance(testCacheFactory)));
        }

    @Test
    void shouldInjectCacheFactoryBuilder()
        {
        assertThat(builder, is(notNullValue()));
        }
    }
