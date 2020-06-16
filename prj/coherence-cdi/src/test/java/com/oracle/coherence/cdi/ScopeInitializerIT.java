/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;


import com.oracle.coherence.cdi.data.Account;
import com.tangosol.net.NamedCache;
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
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Integration test for the {@link ScopeInitializer} using the Weld JUnit
 * extension.
 *
 * @author Aleks Seovic  2020.04.03
*/
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScopeInitializerIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addBeanClass(CacheFactoryUriResolver.Default.class)
                                                          .addBeanClass(ConfigurableCacheFactoryProducer.class)
                                                          .addBeanClass(NamedCacheProducer.class)
                                                          .addBeanClass(FilterProducer.class)
                                                          .addBeanClass(ExtractorProducer.class)
                                                          .addBeanClass(InjectableScope.class)
                                                          .addBeanClass(TestScope.class));

    @ApplicationScoped
    @Named("injectable")
    @ConfigUri("injectable-config.xml")
    private static class InjectableScope
            implements ScopeInitializer
        {}

    @ApplicationScoped
    @Named("test")
    private static class TestScope
            implements ScopeInitializer
        {}

    @Inject
    @Scope("injectable")
    private NamedCache<String, Account> accounts;

    @Inject
    @Scope("test")
    private NamedCache<String, String> test;

    @Test
    void shouldUseInjectableConfig()
        {
        assertThat(accounts.getCacheService().getBackingMapManager().getCacheFactory().getScopeName(), is("injectable"));
        assertThat(accounts.getCacheService().getInfo().getServiceName(), is("injectable:Accounts"));
        }

    @Test
    void shouldUseDefaultConfig()
        {
        assertThat(test.getCacheService().getBackingMapManager().getCacheFactory().getScopeName(), is("test"));
        assertThat(test.getCacheService().getInfo().getServiceName(), is("test:StorageService"));
        }
    }
