/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.io.nio.DirectStoreManager;

import org.junit.Test;

/**
 * Unit tests for NioMemoryManagerBuilder.
 *
 * @author pfm  2012.05.29
 */
public class NioMemoryManagerBuilderTest
        extends AbstractNioTestSupport<DirectStoreManager>
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        super.testDefaults(new NioMemoryManagerBuilder());
        }

    /**
     * Test a custom manager.
     */
    @Test
    public void testCustom()
        {
        // create the custom builder
        ParameterizedBuilder<DirectStoreManager> bldrCustom
            = new InstanceBuilder<DirectStoreManager>(CustomManager.class);

        NioMemoryManagerBuilder bldr = new NioMemoryManagerBuilder();
        bldr.setCustomBuilder(bldrCustom);
        super.testCustom(bldr);
        }

    // ----- inner classes --------------------------------------------------

    /**
     * Custom BinaryStoreManager.
     */
    public static class CustomManager
            extends DirectStoreManager
        {
        // ----- constructors -----------------------------------------------

        public CustomManager()
            {
            super(0,0);
            }

        public CustomManager(int cbInitial, int cbMaximum)
           {
           super(cbInitial, cbMaximum);
           }
        }
    }
