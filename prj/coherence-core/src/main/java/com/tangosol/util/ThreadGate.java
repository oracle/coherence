/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
 * A thin extension of {@link com.oracle.coherence.common.util.ThreadGate}.
 */
public class ThreadGate<R>
        extends com.oracle.coherence.common.util.ThreadGate<R>
        implements Gate
    {
    /**
    * Default constructor.
    */
    public ThreadGate()
        {
        this (null);
        }

    /**
     * Construct a gate protecting the specified resource.
     *
     * @param resource  the resource, or null
     */
    public ThreadGate(R resource)
        {
        super (resource);
        }
    }