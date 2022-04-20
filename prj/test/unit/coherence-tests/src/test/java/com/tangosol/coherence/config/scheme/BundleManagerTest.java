/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.scheme.BundleManager.BundleConfig;
import com.tangosol.coherence.config.unit.Millis;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.cache.AbstractBundler;
import com.tangosol.net.cache.BundlingNamedCache;
import com.tangosol.net.cache.ReadWriteBackingMap.StoreWrapper;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.SafeHashMap;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit Tests for a {@link BundleManager}.
 *
 * @author pfm  2012.06.27
 */
public class BundleManagerTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test ensure.
     */
    @Test
    public void testBasic()
        {
        BundleManager mgr  = new BundleManager();
        mgr.addConfig(new BundleConfig());

        ParameterResolver resolver = new NullParameterResolver();
        mgr.ensureBundles(resolver, new BundlingNamedCache(new WrapperNamedCache(new SafeHashMap(),"Test")));
        mgr.ensureBundles(resolver, Mockito.mock(StoreWrapper.class));
        mgr.initializeBundler(resolver, Mockito.mock(AbstractBundler.class), new BundleConfig());
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testBundleConfigDefaults()
        {
        BundleManager.BundleConfig config = new BundleManager.BundleConfig();

        assertEquals("all", config.getOperationName(new NullParameterResolver()));
        assertEquals(1, config.getDelayMillis(new NullParameterResolver()));
        assertEquals(0, config.getPreferredSize(new NullParameterResolver()));
        assertEquals(4, config.getThreadThreshold(new NullParameterResolver()));
        assertFalse(config.isAutoAdjust(new NullParameterResolver()));
        }

    /**
     * Test the setters.
     */
    @Test
    public void testSetters()
        {
        BundleManager.BundleConfig config = new BundleManager.BundleConfig();

        config.setOperationName(new LiteralExpression<String>("store"));
        assertEquals("store", config.getOperationName(new NullParameterResolver()));

        config.setDelayMillis(new LiteralExpression<Millis>(new Millis("10")));
        assertEquals(10, config.getDelayMillis(new NullParameterResolver()));

        int nPreferredSize = 10;
        config.setPreferredSize(new LiteralExpression<Integer>(nPreferredSize));
        assertEquals(nPreferredSize, config.getPreferredSize(new NullParameterResolver()));

        int nThreadThreshold = 20;
        config.setThreadThreshold(new LiteralExpression<Integer>(nThreadThreshold));
        assertEquals(nThreadThreshold, config.getThreadThreshold(new NullParameterResolver()));

        config.setAutoAdjust(new LiteralExpression<Boolean>(true));
        assertTrue(config.isAutoAdjust(new NullParameterResolver()));
        }

    /**
     * Test validate.
     */
    @Test
    public void testValidate()
        {
        try
            {
            BundleManager mgr  = new BundleManager();
            BundleManager.BundleConfig config = new BundleManager.BundleConfig();
            config.setOperationName(new LiteralExpression<String>(""));

            mgr.addConfig(config);
            mgr.ensureBundles(new NullParameterResolver(),
                    new BundlingNamedCache(new WrapperNamedCache(new SafeHashMap(),"Test")));
            fail("Expected IllegalArgumentException due to invalid operation name");
            }
        catch (IllegalArgumentException e)
            {
            System.out.println("******* " + e.getMessage());
            assertTrue(e.getMessage().toLowerCase().contains("operation"));
            }
        }
    }
