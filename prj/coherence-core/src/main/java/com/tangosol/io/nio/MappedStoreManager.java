/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.tangosol.io.BinaryStore;

import java.io.File;


/**
* An implementation of BinaryStoreManager interface that uses {@link BinaryMap}
* objects built on the {@link MappedBufferManager} to provide {@link BinaryStore}
* objects.
*
* @author gg 2004.06.09
* @since Coherence 2.4
*/
public class MappedStoreManager
        extends AbstractStoreManager
    {
    /**
    * Construct a DirectStoreManager that uses BinaryMap objests built on
    * the MappedBufferManager(s) with certain initial and maximum size.
    *
    * @param cbInitial  the initial size of the managed buffers
    * @param cbMaximum  the maximum size of the managed buffers
    */
    public MappedStoreManager(int cbInitial, int cbMaximum, File dir)
        {
        super(cbInitial, cbMaximum);
        setDirectory(dir);
        }

    /**
    * Obtain the directory to use for MappedBufferManager(s).
    *
    * @return the File object, or null if the default location is used
    */
    public File getDirectory()
        {
        return m_dir;
        }

    /**
    * Specify the directory to use for MappedBufferManager(s).
    *
    * @param dir  the File object representing the directory, or null
    *             to use the default location
    */
    public void setDirectory(File dir)
        {
        if (dir != null && !dir.isDirectory())
            {
            throw new IllegalArgumentException("Not a directory: " + dir);
            }
        m_dir = dir;
        }

    
    // ----- internal -------------------------------------------------------

    /**
    * Create a ByteBufferManager to be used by a BinaryStore.
    */
    protected ByteBufferManager createBufferManager()
        {
        return new MappedBufferManager(
            getMinCapacity(), getMaxCapacity(), getDirectory());
        }
    
    
    // ----- data members ---------------------------------------------------

    /**
    * The directory to use.
    */
    private File m_dir;
    }