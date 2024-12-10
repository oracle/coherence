/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;


import com.tangosol.net.BackingMapManager;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.ConfigurableCacheFactory;

import com.tangosol.net.security.StorageAccessAuthorizer;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;


/**
 * A trivial BackingMapManager used by the KeyIndex in PartitionedCache.
 *
 * @author gg 2015.04.23
 */
public class KeyIndexManager
        implements BackingMapManager
    {
    @Override
    public void init(BackingMapManagerContext context)
        {
        m_context = context;
        }

    @Override
    public ConfigurableCacheFactory getCacheFactory()
        {
        return null;
        }

    @Override
    public BackingMapManagerContext getContext()
        {
        return m_context;
        }

    @Override
    public Map instantiateBackingMap(String sName)
        {
        return new ConcurrentHashMap();
        }

    @Override
    public boolean isBackingMapPersistent(String sName)
        {
        return false;
        }

    @Override
    public boolean isBackingMapSlidingExpiry(String sName)
        {
        return false;
        }

    @Override
    public StorageAccessAuthorizer getStorageAccessAuthorizer(String sName)
        {
        throw new UnsupportedOperationException();
        }

    @Override
    public void releaseBackingMap(String sName, Map map)
        {
        }

    /**
     * The context.
     */
    private BackingMapManagerContext m_context;
    }
