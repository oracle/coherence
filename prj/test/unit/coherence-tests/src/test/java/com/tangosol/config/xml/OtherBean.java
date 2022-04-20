/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.annotation.Injectable;

/**
 * The {@link com.tangosol.config.xml.OtherBean} that we can use to test injection.
 *
 * @author bo
 */
public class OtherBean
    {
    /**
     * Sets the Name property.
     *
     * @param sName  the name
     */
    @Injectable
    public void setName(String sName)
        {
        m_sName = sName;
        }

    /**
     * Obtains the Name property.
     *
     * @return  the name
     */
    public String getName()
        {
        return m_sName;
        }

    // ----- data fields ----------------------------------------------------

    /**
     * A {@link String} property.
     */
    private String m_sName = "unknown";
    }
