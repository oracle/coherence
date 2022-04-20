/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

/**
 * SystemPropertyResource automates isolation of SystemProperty values
 * between try with resource blocks. Enables system property isolation
 * between individual test cases in a junit file.
 *
 * @author jf  2015.05.19
 */
public class SystemPropertyResource
        implements AutoCloseable
    {
    // ----- constructors -----------------------------------------------

    /**
     * Set system property sName to value sValue.
     *
     * @param sName  system property
     * @param sValue system property value
     */
    public SystemPropertyResource(String sName, String sValue)
        {
        m_sName          = sName;
        m_sValueOriginal = System.getProperty(sName);
        System.setProperty(sName, sValue);
        }

    // ----- AutoCloseable methods --------------------------------------

    /**
     * Clear system property value.
     */
    @Override
    public void close()
        {
        // restore original system property
        if (m_sValueOriginal == null)
            {
            System.clearProperty(m_sName);
            }
        else
            {
            System.setProperty(m_sName, m_sValueOriginal);
            }
        }

    // ----- data members -----------------------------------------------
    final private String m_sName;
    final private String m_sValueOriginal;
    }
