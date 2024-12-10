/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import java.io.File;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.LiteralExpression;

import com.tangosol.io.nio.MappedStoreManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for NioFileManagerBuilder.
 *
 * @author pfm  2012.05.29
 */
public class NioFileManagerBuilderTest
        extends AbstractNioTestSupport<MappedStoreManager>
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        MappedStoreManager mgr = super.testDefaults(new NioFileManagerBuilder());
        assertNull(mgr.getDirectory());
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testCustom()
        {
        final String sDir = "tempDir";

        ParameterizedBuilder<MappedStoreManager> bldrCustom
            = new InstanceBuilder<MappedStoreManager>(CustomManager.class);

        NioFileManagerBuilder bldr = new NioFileManagerBuilder();
        bldr.setCustomBuilder(bldrCustom);
        bldr.setDirectory(new LiteralExpression<String>(sDir));

        MappedStoreManager mgr = super.testCustom(bldr);
        assertEquals(sDir, mgr.getDirectory().getName());
        }

    // ----- inner classes --------------------------------------------------

    /**
     * Custom BinaryStoreManager.
     */
    public static class CustomManager
            extends MappedStoreManager
        {
        // ----- constructors -----------------------------------------------

        public CustomManager(int cbInitial, int cbMaximum)
            {
            super(cbInitial, cbMaximum, null);
            }

        public CustomManager(int cbInitial, int cbMaximum, File dir)
            {
            // Don't pass directory to base class since it checks for existence.
            super(cbInitial, cbMaximum, null);
            m_dir = dir;
            }

        // ----- CustomManager methods --------------------------------------

        public void setDirectory(String s)
            {
            m_dir = new File(s);
            }

        public File getDirectory()
            {
            return m_dir;
            }

        // ----- data members -----------------------------------------------

        private File m_dir;
        }
    }
