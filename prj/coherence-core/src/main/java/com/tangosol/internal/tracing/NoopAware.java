/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing;

/**
 * Provides a mechanism to determine whether or not the implementing entity can be considered a no-op.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
public interface NoopAware
    {
    /**
     * Return {@code true} if this entity can be considered a no-op.
     *
     * @return {@code true} if this entity can be considered a no-op
     */
    public boolean isNoop();
    }
