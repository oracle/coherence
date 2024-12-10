/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.tangosol.io.BinaryStore;


/**
* An implementation of BinaryStoreManager interface that uses {@link BinaryMap}
* objects built on the {@link DirectBufferManager} to provide {@link BinaryStore}
* objects.
*
* @author gg 2004.06.09
* @since Coherence 2.4
*
* @deprecated use {@link com.tangosol.io.journal.JournalBinaryStore JournalBinaryStore}
*             instead
*/
@Deprecated
public class DirectStoreManager
        extends AbstractStoreManager
    {
    /**
    * Construct a DirectStoreManager that uses BinaryMap objests built on
    * the DirectBufferManager(s) with certain initial and maximum size.
    *
    * @param cbInitial  the initial size of the managed buffers
    * @param cbMaximum  the maximum size of the managed buffers
    */
    public DirectStoreManager(int cbInitial, int cbMaximum)
        {
        super(cbInitial, cbMaximum);
        }

    /**
    * Create a ByteBufferManager to be used by a BinaryStore.
    */
    protected ByteBufferManager createBufferManager()
        {
        return new DirectBufferManager(getMinCapacity(), getMaxCapacity());
        }
    }