/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.extractor.ConstantExtractor;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ValueExtractor;

/**
* Class to include a constant value as a report column.   Constants are most
* frequently used in functions and are not visible.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class ConstantLocator
        extends BaseLocator
    {
    /**
    * @inheritDoc
    */
    public void configure(XmlElement xml)
        {
        super.configure(xml);

        String sType  = xml.getSafeElement(TAG_TYPE).getString("double");
        Object oValue = null;
        if (sType.equals("string"))
            {
            oValue = xml.getSafeElement(TAG_VALUE).getString();
            }
        else if (sType.equals("double"))
            {
            oValue = xml.getSafeElement(TAG_VALUE).getDouble();
            }
        m_Ret = oValue;
        }

    /**
    * @inheritDoc
    */
    public ValueExtractor getExtractor()
        {
        super.getExtractor();
        if (m_veExtractor == null)
            {
            m_veExtractor = new ConstantExtractor(m_Ret);
            }
        return m_veExtractor;
        }

    /**
    * @inheritDoc
    */
    public Object getValue(Object oKey)
        {
        return m_Ret;
        }

    // ----- constants -------------------------------------------------------

    /**
    * the constant data type XML tag
    */
    public static String TAG_TYPE = "data-type";

    /**
    * the constant value XML tag
    */
    public static String TAG_VALUE = "value";

    // ----- data members ----------------------------------------------------

    /**
    * the value of the constant
    */
    protected Object m_Ret;
    }
