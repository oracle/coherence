/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.util.MemorySize;
import com.oracle.coherence.common.util.MemorySize.Magnitude;

import com.tangosol.coherence.config.unit.Megabytes;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.CacheFactory;


/**
 * The AbstractNioManagerBuilder class is an abstract class used to build
 * an NIO file manager or an NIO memory manager.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public abstract class AbstractNioManagerBuilder<T>
        extends AbstractStoreManagerBuilder<T>
    {
    // ----- AbstractNioManagerBuilder methods ------------------------------

    /**
     * Return the initial buffer size in bytes.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the initial buffer size in bytes
     */
    public long getInitialSize(ParameterResolver resolver)
        {
        return m_exprInitialSize.evaluate(resolver).getByteCount();
        }

    /**
     * Set the initial buffer size.
     *
     * @param expr  the initial buffer size
     */
    @Injectable
    public void setInitialSize(Expression<Megabytes> expr)
        {
        m_exprInitialSize = expr;
        }

    /**
     * Return the maximum buffer size in bytes.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the maximum buffer size in bytes
     */
    public long getMaximumSize(ParameterResolver resolver)
        {
        return m_exprMaxSize.evaluate(resolver).getByteCount();
        }

    /**
     * Set the maximum buffer size.
     *
     * @param expr  the maximum buffer size
     */
    @Injectable
    public void setMaximumSize(Expression<Megabytes> expr)
        {
        m_exprMaxSize = expr;
        }

    // ----- internal -------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(ParameterResolver resolver)
        {
        super.validate(resolver);

        long cbMax  = getMaximumSize(resolver);
        long cbInit = getInitialSize(resolver);

        // bounds check:
        // 1 <= cbInitSize <= cbMaxSize <= Integer.MAX_VALUE - 1023
        // (Integer.MAX_VALUE - 1023 is the largest integer multiple of 1024)
        int cbMaxSize  = (int) Math.min(Math.max(cbMax, 1L), (long) Integer.MAX_VALUE - 1023);
        int cbInitSize = (int) Math.min(Math.max(cbInit, 1L), cbMaxSize);

        // warn about changes to configured values
        if (cbInitSize != cbInit)
            {
            Logger.warn("Invalid NIO manager initial-size changed to: " + cbInitSize + " bytes");
            }
        if (cbMaxSize != cbMax)
            {
            Logger.warn("Invalid NIO manager maximum-size changed to: " + cbMaxSize + " bytes");
            }

        m_exprMaxSize     = new LiteralExpression<Megabytes>(new Megabytes(new MemorySize(cbMaxSize, Magnitude.BYTES)));
        m_exprInitialSize = new LiteralExpression<Megabytes>(new Megabytes(new MemorySize(cbInitSize, Magnitude.BYTES)));
        }

    // ----- data members ---------------------------------------------------

    /**
     * The initial buffer size.
     */
    private Expression<Megabytes> m_exprInitialSize = new LiteralExpression<Megabytes>(new Megabytes(1));

    /**
     * The maximum buffer size.
     */
    private Expression<Megabytes> m_exprMaxSize = new LiteralExpression<Megabytes>(new Megabytes(1024));
    }
