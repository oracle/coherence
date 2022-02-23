/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.oracle.coherence.common.base.Disposable;

import com.tangosol.io.BinaryStore;

import com.tangosol.util.Binary;
import com.tangosol.util.SimpleEnumerator;

import java.util.Collections;
import java.util.Iterator;


/**
* An implementation of BinaryStore backed by a BinaryMap.
*
* @since Coherence 2.4
* @author cp 2004.03.31
*/
public class BinaryMapStore
        implements BinaryStore, BinaryStore.SizeAware, Disposable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a BinaryMapStore object, which is an implementation of
    * the BinaryStore interface backed by a BinaryMap.
    *
    * @param map  the BinaryMap to use for storage for this BinaryStore
    *             implementation
    */
    public BinaryMapStore(BinaryMap map)
        {
        m_map = map;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying BinaryMap.
    *
    * @return the BinaryMap that this BinaryStore sits on top of
    */
    public BinaryMap getBinaryMap()
        {
        return m_map;
        }


    // ----- BinaryStore interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Binary load(Binary binKey)
        {
        return (Binary) m_map.get(binKey);
        }

    /**
    * {@inheritDoc}
    */
    public void store(Binary binKey, Binary binValue)
        {
        m_map.putAll(Collections.singletonMap(binKey, binValue));
        }

    /**
    * {@inheritDoc}
    */
    public void erase(Binary binKey)
        {
        m_map.keySet().remove(binKey);
        }

    /**
    * {@inheritDoc}
    */
    public void eraseAll()
        {
        m_map.clear();
        }

    /**
    * {@inheritDoc}
    */
    public Iterator keys()
        {
        return new SimpleEnumerator(m_map.keySet().toArray());
        }


    // ----- BinaryStore.SizeAware interface ---------------------------------

    /**
    * {@inheritDoc}
    */
    public int size()
        {
        return m_map.size();
        }


    // ----- Disposable interface --------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void dispose()
        {
        BinaryMap map = m_map;

        m_map = null;

        // only close MappedBufferManager once
        if (map != null)
            {
            try
                {
                ByteBufferManager mgr = map.getBufferManager();
                if (mgr instanceof MappedBufferManager)
                    {
                    ((MappedBufferManager) mgr).close();
                    }
                }
            catch (Exception e)
                {
                }
            }
        }


    // ----- life-cycle support ---------------------------------------------

    /**
    * Release underlying resources.
    *
    * @deprecated use the Disposable interface instead
    */
    public void close()
        {
        dispose();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The BinaryMap that provides the storage for this BinaryStore
    * implementation.
    */
    private volatile BinaryMap m_map;
    }