/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import com.tangosol.coherence.config.unit.Megabytes;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.io.BinaryStore;
import com.tangosol.io.nio.AbstractStoreManager;

import com.tangosol.util.Base;

import static org.junit.Assert.assertEquals;

/**
 * Base class for NIO manager builder tests.
 *
 * @author pfm  2012.05.29
 */
public abstract class AbstractNioTestSupport<T extends AbstractStoreManager>
    {
    /**
     * Test the default settings.
     *
     * @param bldr  the manager builder
     *
     * @return the manager
     */
    public T testDefaults(AbstractNioManagerBuilder<T> bldr)
        {
        return validate(new Megabytes(1).getByteCount(), new Megabytes(1024).getByteCount(), bldr);
        }

    /**
     * Test a custom settings.
     *
     * @param bldr  the manager builder
     *
     * @return the manager
     */
    public T testCustom(AbstractNioManagerBuilder<T> bldr)
        {
        final Megabytes cMinSize = new Megabytes(2);
        final Megabytes cMaxSize = new Megabytes(2000);

        bldr.setInitialSize(new LiteralExpression<Megabytes>(cMinSize));
        bldr.setMaximumSize(new LiteralExpression<Megabytes>(cMaxSize));
        return validate(cMinSize.getByteCount(), cMaxSize.getByteCount(), bldr);
        }

    /**
     * Validate the settings.
     *
     * @param cMinSize  the minimum size
     * @param cMaxSize  the maximum size
     * @param bldr      the manager builder
     *
     * @return the manager
     */
    @SuppressWarnings("unchecked")
    protected T validate(long cMinSize, long cMaxSize, AbstractNioManagerBuilder<T> bldr)
        {
        assertEquals(cMinSize, bldr.getInitialSize(null));
        assertEquals(cMaxSize, bldr.getMaximumSize(null));

        T mgr = (T) bldr.realize(new NullParameterResolver(),
                Base.getContextClassLoader(), false);

        assertEquals(cMinSize, mgr.getMinCapacity());
        assertEquals(cMaxSize, mgr.getMaxCapacity());

        BinaryStore store = mgr.createBinaryStore();
        mgr.destroyBinaryStore(store);

        return mgr;
        }
    }
