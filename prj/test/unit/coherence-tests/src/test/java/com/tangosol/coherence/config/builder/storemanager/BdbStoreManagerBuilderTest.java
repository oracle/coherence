/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder.storemanager;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.config.expression.Parameter;
import com.tangosol.io.BinaryStore;
import com.tangosol.io.bdb.BerkeleyDBBinaryStoreManager;

import com.tangosol.io.bdb.DatabaseFactory;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for BdbStoreManagerBuilder.
 *
 * @author pfm  2012.05.29
 */
public class BdbStoreManagerBuilderTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default settings.
     */
    @Test
    public void testDefaults()
        {
        BerkeleyDBBinaryStoreManager mgr = new BdbStoreManagerBuilder().realize(new NullParameterResolver(),
                Base.getContextClassLoader(), false);

        // the BerkeleyDBBinaryStoreManager doesn't expose getters for configurable fields so just
        // make sure it was created
        assertNotNull(mgr);

        BinaryStore store = mgr.createBinaryStore();
        mgr.destroyBinaryStore(store);
        }

    @Test
    public void testConfigByInitParams()
        {
        BdbStoreManagerBuilder bldr = new BdbStoreManagerBuilder();

        // simulate injection into setInitParams()
        ResolvableParameterList listParameters = new ResolvableParameterList();
        listParameters.add(new Parameter("je.test.property", true));
        bldr.setInitParams(listParameters);

        BerkeleyDBBinaryStoreManager mgr = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), false);

        // assert je.* property set on BerkeleyDBBinaryStoreManager
        XmlElement xmlParam = null;
        for (Iterator iter = mgr.getConfig().getElementList().iterator(); iter.hasNext(); )
            {
            xmlParam = (XmlElement) iter.next();
            if (xmlParam.getName().startsWith("je."))
                {
                break;
                }
            }
        assertTrue(xmlParam.getName().equals("je.test.property"));
        assertTrue(Boolean.parseBoolean((String) xmlParam.getValue()));
        }

    /**
     * Test the default settings.
     */
    @Test
    public void testCustom()
        {
        final String sDir  = "tempDir";
        final String sFile = "tempFile";

        ParameterizedBuilder<BerkeleyDBBinaryStoreManager> bldrCustom
            = new InstanceBuilder<BerkeleyDBBinaryStoreManager>(CustomManager.class);

        BdbStoreManagerBuilder bldr = new BdbStoreManagerBuilder();
        bldr.setCustomBuilder(bldrCustom);
        bldr.setDirectory(new LiteralExpression<String>(sDir));
        bldr.setStoreName(new LiteralExpression<String>(sFile));

        BerkeleyDBBinaryStoreManager mgr = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), true);
        assertEquals(sDir, ((CustomManager) mgr).getDir().getName());

        mgr = bldr.realize(new NullParameterResolver(), Base.getContextClassLoader(), false);
        assertEquals(sDir, ((CustomManager) mgr).getDir().getName());
        assertEquals(sFile, ((CustomManager) mgr).getName());

        BinaryStore store = mgr.createBinaryStore();
        mgr.destroyBinaryStore(store);
        }

    // ----- inner classes --------------------------------------------------

    /**
     * Custom BinaryStoreManager.
     */
    public static class CustomManager
            extends com.tangosol.io.bdb.BerkeleyDBBinaryStoreManager
        {
        // ----- constructors -----------------------------------------------

        public CustomManager(File dir)
            {
            m_dir = dir;
            }

        public CustomManager(File dir, String sName)
            {
            m_dir   = dir;
            m_sName = sName;
            }

        // ----- CustomManager methods --------------------------------------

        public File getDir()
            {
            return m_dir;
            }

        public String getName()
            {
            return m_sName;
            }

        // ----- data members -----------------------------------------------

        private File m_dir;

        private String m_sName;
        }
    }
