/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;


/**
 * The Disposable interface is used for life-cycle management of resources.
 *
 * Disposable is also AutoCloseable and thus is compatible with the try-with-resources pattern.
 *
 * @author ch  2010.01.11
 */
public interface Disposable
    extends AutoCloseable
    {
    /**
     * Invoked when all resources owned by the implementer can safely be
     * released.
     * <p>
     * Once disposed of the object should no longer be considered to be
     * usable.
     * <p>
     * Note the Disposable interface is compatible with try-with-resources which will automatically
     * invoke this method.
     */
    public void dispose();

    /**
     * Default implementation invokes {@link #dispose}, it is not recommended that this be overridden.
     */
    @Override
    public default void close()
        {
        dispose();
        }
    }
