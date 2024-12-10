/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.net.InvocationService;

/**
 * The DefaultInvocationServiceDependencies class provides a default
 * implementation of InvocationServiceDependencies
 *
 * @author pfm 2011.05.12
 * @since Coherence 12.1.2
 */
public class DefaultInvocationServiceDependencies
        extends DefaultGridDependencies
        implements InvocationServiceDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultInvocationServiceDependencies object.
     */
    public DefaultInvocationServiceDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultInvocationServiceDependencies object, copying the values from the
     * specified InvocationServiceDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultInvocationServiceDependencies(InvocationServiceDependencies deps)
        {
        super(deps);

        if (deps == null)
            {
            setWorkerThreadCountMin(1); // enable auto-sizing pool by default
            }
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultInvocationServiceDependencies validate()
        {
        super.validate();

        return this;
        }

    // ----- data members ---------------------------------------------------

    }
