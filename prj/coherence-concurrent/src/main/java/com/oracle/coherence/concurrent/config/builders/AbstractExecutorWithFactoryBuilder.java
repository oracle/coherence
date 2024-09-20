/*
 * Copyright (c) 2021, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.builders;

import com.oracle.coherence.concurrent.executor.util.NamedThreadFactory;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import java.util.concurrent.ThreadFactory;

/**
 * Base {@link ParameterizedBuilder} for named executors that support
 * {@link ThreadFactory thread factories}.
 *
 * @param <T>  the builder type
 *
 * @author rl  11.26.21
 * @since 21.12
 */
public abstract class AbstractExecutorWithFactoryBuilder<T>
        extends AbstractExecutorBuilder<T>
    {
    // ----- setters --------------------------------------------------------

    /**
     * Sets the {@link ThreadFactory} instance builder.
     *
     * @param bldr  the {@link ThreadFactory} instance builder
     */
    @SuppressWarnings("unused")
    @Injectable("thread-factory")
    public void setInstanceBuilder(ParameterizedBuilder<ThreadFactory> bldr)
        {
        m_bldr = bldr;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Creates and returns a ThreadFactory.
     *
     * @param sName           the name to use if no user-defined
     *                        ThreadFactory is defined
     * @param resolver        the {@link ParameterResolver} for resolving
     *                        named {@link Parameter}s
     * @param loader          the {@link ClassLoader} for loading any
     *                         necessary classes and if <code>null</code> the
     *                        {@link ClassLoader} used to load the builder
     *                         will be used instead
     * @param listParameters  an optional {@link ParameterList}
     *                        (may be <code>null</code>) to be used for
     *                        realizing the instance, eg: used as constructor
     *                        parameters
     *
     * @return the {@link ThreadFactory}
     */
    protected ThreadFactory instantiateThreadFactory(String sName,
            ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        return m_bldr == null
                ? instantiateNamedThreadFactory(sName)
                : m_bldr.realize(resolver, loader, listParameters);

        }

    /**
     * Creates a new {@link NamedThreadFactory}
     *
     * @param sName  the name to use if no user-defined ThreadFactory
     *               is defined
     *
     * @return the {@link NamedThreadFactory}
     */
    protected ThreadFactory instantiateNamedThreadFactory(String sName)
        {
        return new NamedThreadFactory(sName);
        }

    // ----- data members ---------------------------------------------------

    /**
     * A {@link ParameterizedBuilder} that creates a {@link ThreadFactory}.
     */
    protected ParameterizedBuilder<ThreadFactory> m_bldr;
    }
