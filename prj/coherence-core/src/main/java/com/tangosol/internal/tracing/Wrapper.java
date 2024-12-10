/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing;

/**
 * Marks implementations as a wrapper for some resource.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
public interface Wrapper
    {
    /**
     * Return the underlying object this entity wraps.
     *
     * @param <T>  the type of the underlying resource
     *
     * @return the underlying object this entity wraps.
     */
    public <T> T underlying();
    }
