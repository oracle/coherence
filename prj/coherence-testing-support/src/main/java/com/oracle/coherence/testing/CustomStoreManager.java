/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import com.tangosol.io.nio.AbstractStoreManager;
import com.tangosol.io.nio.ByteBufferManager;
import com.tangosol.io.nio.MappedBufferManager;

/**
 * The CustomStoreManager is a custom storage manager used to test the BinaryStoreManagerBuilder.
 *
 * @author pfm  2011.11.28
 */
public class CustomStoreManager
        extends AbstractStoreManager
    {
    public CustomStoreManager()
        {
        this(1, Integer.MAX_VALUE-1023);
        }

    public CustomStoreManager(int cbInitial, int cbMaximum)
        {
        super(cbInitial, cbMaximum);
        }

    @Override
    protected ByteBufferManager createBufferManager()
        {
        return new MappedBufferManager(1000000, 2000000, null);
        }
    }
