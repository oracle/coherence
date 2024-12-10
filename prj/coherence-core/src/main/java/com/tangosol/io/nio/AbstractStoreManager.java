/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.tangosol.io.BinaryStore;
import com.tangosol.io.BinaryStoreManager;

import com.tangosol.util.Base;


/**
* A base implementation of BinaryStoreManager interface that uses
* {@link BinaryMap} objects built on a {@link ByteBufferManager} to provide
* {@link BinaryStore} objects.
*
* @author gg 2004.06.09
* @since Coherence 2.4
*/
public abstract class AbstractStoreManager
        extends    Base
        implements BinaryStoreManager
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a AbstractStoreManager that uses BinaryMap objests built on
    * the ByteBufferManager(s) with certain initial and maximum size.
    *
    * @param cbInitial  the initial size of the managed buffers
    * @param cbMaximum  the maximum size of the managed buffers
    */
    public AbstractStoreManager(int cbInitial, int cbMaximum)
        {
        setMinCapacity(cbInitial);
        setMaxCapacity(cbMaximum);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the minimum size that the managed buffers can reach.
    *
    * @return minimum size for the managed buffers
    */
    public int getMinCapacity()
        {
        return m_cbInitial;
        }

    /**
    * Specify the minimum size that the managed buffers can reach.
    *
    * @param cb  minimum size for the managed buffers
    */
    public void setMinCapacity(int cb)
        {
        m_cbInitial = cb;
        }

    /**
    * Determine the maximum size that the managed buffers can reach.
    *
    * @return maximum size for the managed buffers
    */
    public int getMaxCapacity()
        {
        return m_cbMaximum;
        }

    /**
    * Specify the maximum size that the managed buffers can reach.
    *
    * @param cb  maximum size for the managed buffers
    */
    public void setMaxCapacity(int cb)
        {
        m_cbMaximum = cb;
        }


    // ----- BinaryStoreManager interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public BinaryStore createBinaryStore()
        {
        return new BinaryMapStore(new BinaryMap(createBufferManager()));
        }

    /**
    * Lifecycle method: Destroy a BinaryStore previously created by this
    * manager.
    *
    * @param store  a BinaryStore object previously created by this
    *               manager
    */
    public void destroyBinaryStore(BinaryStore store)
        {
        ((BinaryMapStore) store).close();
        }


    // ----- internal ------------------------------------------------------

    /**
    * Create a ByteBufferManager to be used by a BinaryStore.
    */
    abstract protected ByteBufferManager createBufferManager();


    // ----- data members ---------------------------------------------------

    /**
    * Initial (and minimum) size of the managed buffers.
    */
    private int m_cbInitial;

    /**
    * Maximum size of the managed buffers.
    */
    private int m_cbMaximum;
    }