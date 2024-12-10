/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.persistence;

import com.oracle.coherence.common.base.Collector;

/**
 * NO-OP implementation of PersistentStore.
 *
 * @author jh  2013.12.04
 */
public class NullPersistentStore
        implements PersistentStore
    {
    @Override
    public String getId()
        {
        return null;
        }

    @Override
    public boolean ensureExtent(long lExtentId)
        {
        return false;
        }

    @Override
    public void deleteExtent(long lExtentId)
        {
        }

    @Override
    public void truncateExtent(long lExtentId)
        {
        }

    @Override
    public void moveExtent(long lOldExtentId, long lNewExtentId)
        {
        }

    @Override
    public long[] extents()
        {
        return new long[0];
        }

    @Override
    public Object load(long lExtentId, Object key)
        {
        return null;
        }

    @Override
    public void store(long lExtentId, Object key, Object value, Object oToken)
        {
        }

    @Override
    public void erase(long lExtentId, Object key, Object oToken)
        {
        }

    @Override
    public void iterate(Visitor visitor)
        {
        }

    @Override
    public boolean containsExtent(long lExtentId)
        {
        return false;
        }

    @Override
    public boolean isOpen()
        {
        return false;
        }

    @Override
    public Object begin()
        {
        return null;
        }

    @Override
    public Object begin(Collector collector, Object oReceipt)
        {
        return null;
        }

    @Override
    public void commit(Object oToken)
        {
        }

    @Override
    public void abort(Object oToken)
        {
        }

    /**
     * Singleton instance.
     */
    public static final NullPersistentStore INSTANCE = new NullPersistentStore();
    }
