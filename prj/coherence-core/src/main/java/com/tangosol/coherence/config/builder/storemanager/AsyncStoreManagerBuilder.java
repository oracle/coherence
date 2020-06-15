/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import com.oracle.coherence.common.util.MemorySize;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.unit.Bytes;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.AsyncBinaryStoreManager;
import com.tangosol.io.BinaryStoreManager;

import com.tangosol.util.Base;

/**
 * The AsyncStoreManagerBuilder class builds and instance of an
 * AsyncBinaryStoreManager.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class AsyncStoreManagerBuilder
        extends AbstractStoreManagerBuilder<AsyncBinaryStoreManager>
        implements BinaryStoreManagerBuilderCustomization
    {
    // ----- StoreManagerBuilder interface ----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public BinaryStoreManager realize(ParameterResolver resolver, ClassLoader loader, boolean fPaged)
        {
        validate(resolver);

        int                                           cbMaxAsync   = (int) getAsyncLimit(resolver);
        BinaryStoreManager                            asyncManager = null;
        BinaryStoreManager storeManager = getBinaryStoreManagerBuilder().realize(resolver, loader, fPaged);

        ParameterizedBuilder<AsyncBinaryStoreManager> bldrCustom   = getCustomBuilder();

        if (bldrCustom == null)
            {
            // create the default manager
            asyncManager = cbMaxAsync <= 0
                           ? new AsyncBinaryStoreManager(storeManager)
                           : new AsyncBinaryStoreManager(storeManager, cbMaxAsync);
            }
        else
            {
            // create the custom object that is implementing AsyncBinaryStoreManager.
            ParameterList listArgs = new ResolvableParameterList();

            listArgs.add(new Parameter("store-manager", storeManager));

            if (cbMaxAsync <= 0)
                {
                listArgs.add(new Parameter("async-limit", cbMaxAsync));
                }

            asyncManager = bldrCustom.realize(resolver, loader, listArgs);
            }

        return asyncManager;
        }

    // ----- BinaryStoreManagerBuilderCustomization interface ---------------

    /**
     * {@inheritDoc}
     */
    public BinaryStoreManagerBuilder getBinaryStoreManagerBuilder()
        {
        return m_bldrStoreManager;
        }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStoreManagerBuilder(BinaryStoreManagerBuilder bldr)
        {
        m_bldrStoreManager = bldr;
        }

    // ----- AsyncStoreManagerBuilder methods -------------------------------

    /**
     * Return the maximum number of bytes that are queued to be written
     * asynchronously.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the memory limit
     */
    public long getAsyncLimit(ParameterResolver resolver)
        {
        return m_exprAsyncLimit.evaluate(resolver).getByteCount();
        }

    /**
     * Set the maximum number of bytes that are queued to be written
     * asynchronously.
     *
     * @param expr  the  memory limit
     */
    @Injectable
    public void setAsyncLimit(Expression<Bytes> expr)
        {
        m_exprAsyncLimit = expr;
        }

    // ----- internal -------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(ParameterResolver resolver)
        {
        super.validate(resolver);

        Base.checkNotNull(getBinaryStoreManagerBuilder(), "StoreMangerBuilder");
        }

    // ----- data members ---------------------------------------------------

    /**
     * The store manager builder being wrapped by this manager.
     */
    private BinaryStoreManagerBuilder m_bldrStoreManager;

    /**
     * The async limit.
     */
    private Expression<Bytes> m_exprAsyncLimit = new LiteralExpression<Bytes>(new Bytes(new MemorySize("4MB")));
    }
