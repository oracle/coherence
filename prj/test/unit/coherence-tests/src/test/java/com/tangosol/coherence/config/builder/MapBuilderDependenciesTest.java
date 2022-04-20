/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.component.util.BackingMapManagerContext;

import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import com.tangosol.util.Base;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link MapBuilderDependenciesTest} contains unit tests for {@link MapBuilder}s
 * dependencies.
 *
 * @author pfm  2012.06.07
 * @since Coherence 12.1.2
 */
public class MapBuilderDependenciesTest
    {
    /**
     * Ensure correct access to {@link MapBuilder} dependencies.
     */
    @Test
    public void testAccess()
        {
        ConfigurableCacheFactory ccf          = Mockito.mock(ExtensibleConfigurableCacheFactory.class);
        BackingMapManagerContext contextBmm   = Mockito.mock(BackingMapManagerContext.class);
        ClassLoader              loader       = Base.getContextClassLoader();
        String                   sCacheName   = "test";
        String                   sServiceType = CacheService.TYPE_DISTRIBUTED;

        MapBuilder.Dependencies dependencies = new MapBuilder.Dependencies(ccf, contextBmm, loader, sCacheName,
                                                   sServiceType);

        assertEquals(ccf, dependencies.getConfigurableCacheFactory());
        assertEquals(contextBmm, dependencies.getBackingMapManagerContext());
        assertEquals(loader, dependencies.getClassLoader());
        assertEquals(sCacheName, dependencies.getCacheName());
        assertEquals(sServiceType, dependencies.getServiceType());
        assertTrue(dependencies.isBinary());
        assertFalse(dependencies.isBackup());

        dependencies.setBackup(true);
        assertTrue(dependencies.isBackup());

        // check that binary is set to false for non-distributed service
        dependencies = new MapBuilder.Dependencies(ccf, contextBmm, loader, sCacheName, "local");
        assertFalse(dependencies.isBinary());
        }
    }
