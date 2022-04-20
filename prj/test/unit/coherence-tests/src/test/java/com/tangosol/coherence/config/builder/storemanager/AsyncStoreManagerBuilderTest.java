/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.unit.Bytes;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.io.AsyncBinaryStoreManager;
import com.tangosol.io.BinaryStore;
import com.tangosol.io.BinaryStoreManager;

import com.tangosol.util.Base;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for AsyncStoreManagerBuilder.
 *
 * @author pfm  2012.05.29
 */
public class AsyncStoreManagerBuilderTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        AsyncStoreManagerBuilder bldrAsync = new AsyncStoreManagerBuilder();
        bldrAsync.setBinaryStoreManagerBuilder(new BdbStoreManagerBuilder());

        BinaryStoreManager mgr = bldrAsync.realize(new NullParameterResolver(),
                Base.getContextClassLoader(), false);

        assertNotNull(mgr);
        assertEquals(cAsyncLimitDefault.getByteCount(), bldrAsync.getAsyncLimit(new NullParameterResolver()));

        BinaryStore store = mgr.createBinaryStore();
        mgr.destroyBinaryStore(store);

        bldrAsync.setAsyncLimit(new LiteralExpression<Bytes>(cAsyncLimit));
        assertEquals(cAsyncLimit.getByteCount(), bldrAsync.getAsyncLimit(new NullParameterResolver()));
        }

    /**
     * Test the custom settings.
     */
    @Test
    public void testCustom()
        {
        AsyncStoreManagerBuilder bldrAsync = new AsyncStoreManagerBuilder();
        bldrAsync.setBinaryStoreManagerBuilder(new BdbStoreManagerBuilder());

        // set the custom AsyncBinaryStoreManager builder
        ParameterizedBuilder<AsyncBinaryStoreManager> bldrCustom
            = new InstanceBuilder<AsyncBinaryStoreManager>(CustomManager.class);
        bldrAsync.setCustomBuilder(bldrCustom);

        BinaryStoreManager mgr = bldrAsync.realize(new NullParameterResolver(),
                Base.getContextClassLoader(), false);

        assertNotNull(mgr);

        BinaryStore store = mgr.createBinaryStore();
        mgr.destroyBinaryStore(store);

        bldrAsync.setAsyncLimit(new LiteralExpression<Bytes>(cAsyncLimit));
        assertEquals(cAsyncLimit.getByteCount(), bldrAsync.getAsyncLimit(new NullParameterResolver()));
        }

    // ----- inner classes --------------------------------------------------

    /**
     * Custom BinaryStoreManager.
     */
    public static class CustomManager
            extends AsyncBinaryStoreManager
        {
        // ----- constructors -----------------------------------------------

        public CustomManager(BinaryStoreManager manager)
            {
            super(manager);
            }

        public CustomManager(BinaryStoreManager manager, int cbMax)
            {
            super(manager, cbMax);
            }
        }

    // ----- data members ---------------------------------------------------

    private final Bytes cAsyncLimit        = new Bytes("10MB");

    private final Bytes cAsyncLimitDefault = new Bytes("4MB");
    }
