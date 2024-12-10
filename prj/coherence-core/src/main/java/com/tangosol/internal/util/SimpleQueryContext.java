/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;


import com.tangosol.net.BackingMapContext;
import com.tangosol.util.QueryContext;


/**
 * Simple QueryContext implementation.
 *
 * @since Coherence 3.7.1
 *
 * @author tb 2011.05.26
 */
public class SimpleQueryContext
        implements QueryContext
    {
    // ----- Constructors ---------------------------------------------------

    /**
     * Construct a SimpleQueryContext.
     *
     * @param ctx  the backing map context
     */
    public SimpleQueryContext(BackingMapContext ctx)
        {
        m_ctx = ctx;
        }


    // ----- QueryContext interface -----------------------------------------

    /**
     * {@inheritDoc}
     */
    public BackingMapContext getBackingMapContext()
        {
        return m_ctx;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The backing map context.
     */
    private final BackingMapContext m_ctx;
    }

