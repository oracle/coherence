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

import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
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
                                                         .addBeanClass(CacheFactoryUriResolver.Default.class)
                                                         .addBeanClass(ConfigurableCacheFactoryProducer.class));

    /**
     * Should inject the default CCF.
     */
    @Inject
    private ConfigurableCacheFactory defaultCacheFactory;

    /**
     * Should inject the test CCF.
     */
    @Inject
    @Name("test-cache-config.xml")
    private ConfigurableCacheFactory testCacheFactory;

    /**
     * Should inject the the default CCF.
     */
    @Inject
    @Name("")
    private ConfigurableCacheFactory qualifiedDefaultCacheFactory;

    /**
     * Should inject the the default CCF.
     */
    @Inject
    @Name(" ")
    private ConfigurableCacheFactory namedDefaultCacheFactory;

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
    void shouldGetDynamicCCF()
        {
        Annotation qualifier = Name.Literal.of("test-cache-config.xml");
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
