/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jmx;

/**
 * Test that notification supporting mbeans work
 *
 * @author narliss 2012.01.11
 */
public interface TestEmitterMBean
    {
    /**
     * An attribute.
     *
     * @return the attribute
     */
    public int getCacheSize();

    /**
     * Set the cache size.
     *
     * @param cacheSize  the cache size
     */
    public void setCacheSize(int cacheSize);

    /**
     * An attribute.
     *
     * @return the attribute
     */
    public void EmitNotification();
    }
