/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.SafeHashMap;

import java.util.Map;


/**
* An AsyncBinaryStoreManager is a wrapper BinaryStoreManager that creates
* wrapper AsyncBinaryStore objects. An AsyncBinaryStore is a BinaryStore
* wrapper that performs the "O" portion of its I/O asynchronously (on a
* daemon thread.) Since the "O" portion is passed along to the wrapped
* BinaryStore on a separate thread, only read operations are blocking, thus
* the BinaryStore operations on a whole appear much faster. (It is somewhat
* analogous to a write-behind cache.)
* <p>
* In order for the AsyncBinaryStore objects to be used in situations that
* require a BinaryStoreManager, this class provides a manager for the
* AsyncBinaryStore objects, but to create the BinaryStore objects that will
* be wrapped by the AsyncBinaryStore objects, this AsyncBinaryStoreManager
* class has to be provided with an underlying BinaryStoreManager to wrap.
*
* @since Coherence 2.5
* @author cp 2004.06.18
*/
public class AsyncBinaryStoreManager
        implements BinaryStoreManager
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an AsyncBinaryStoreManager. An AsyncBinaryStoreManager
    * creates and destroys AsyncBinaryStore objects, which wrap other
    * BinaryStore objects to provide asynchronous write operations.
    *
    * @param manager  the BinaryStoreManager to wrap
    */
    public AsyncBinaryStoreManager(BinaryStoreManager manager)
        {
        m_manager = manager;
        }

    /**
    * Construct an AsyncBinaryStoreManager. An AsyncBinaryStoreManager
    * creates and destroys AsyncBinaryStore objects, which wrap other
    * BinaryStore objects to provide asynchronous write operations.
    *
    * @param manager  the BinaryStoreManager to wrap
    * @param cbMax    the maximum number of bytes to queue before blocking
    */
    public AsyncBinaryStoreManager(BinaryStoreManager manager, int cbMax)
        {
        this(manager);
        m_cbMaxPend = cbMax;
        }


    // ----- BinaryStoreManager interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public BinaryStore createBinaryStore()
        {
        BinaryStore      storeWrapped = getBinaryStoreManager().createBinaryStore();
        AsyncBinaryStore storeAsync   = instantiateAsyncBinaryStore(storeWrapped);
        getBinaryStoreMap().put(storeAsync, storeWrapped);
        return storeAsync;
        }

    /**
    * Lifecycle method: Destroy a BinaryStore previously created by this
    * manager.
    *
    * @param store  a BinaryStore object previously created by this
    *               manager
    */
    public synchronized void destroyBinaryStore(BinaryStore store)
        {
        AsyncBinaryStore storeAsync   = (AsyncBinaryStore) store;
        BinaryStore      storeWrapped = (BinaryStore) getBinaryStoreMap().remove(storeAsync);
        if (storeWrapped != null)
            {
            // the concrete implementations of BinaryStore will be disposed as part of destroyBinaryStore
            storeAsync.internalClose(s -> getBinaryStoreManager().destroyBinaryStore(s));
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * @return the wrapped BinaryStoreManager
    */
    protected BinaryStoreManager getBinaryStoreManager()
        {
        return m_manager;
        }

    /**
    * @return the map of created AsyncBinaryStore objects and their
    *         corresponding wrapped BinaryStore objects
    */
    protected Map getBinaryStoreMap()
        {
        return m_mapStores;
        }

    /**
    * @return the maximum number of pending bytes to allow to accumulate for
    *         async I/O in each of the AsyncBinaryStore objects created by
    *         this manager
    */
    protected int getQueuedLimit()
        {
        return m_cbMaxPend;
        }


    // ----- internal -------------------------------------------------------

    /**
    * Factory method: Instantiate an AsyncBinaryStore.
    *
    * @return a new AsyncBinaryStore
    */
    protected AsyncBinaryStore instantiateAsyncBinaryStore(BinaryStore store)
        {
        int cbMax = getQueuedLimit();
        return cbMax > 0 ? new AsyncBinaryStore(store, cbMax)
                         : new AsyncBinaryStore(store);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The wrapped BinaryStoreManager.
    */
    private BinaryStoreManager m_manager;

    /**
    * The map of created AsyncBinaryStore objects and their corresponding
    * wrapped BinaryStore objects.
    */
    private Map m_mapStores = new SafeHashMap();

    /**
    * The maximum number of pending bytes to allow to accumulate for async
    * I/O in each of the AsyncBinaryStore objects created by this manager.
    */
    private int m_cbMaxPend;
    }