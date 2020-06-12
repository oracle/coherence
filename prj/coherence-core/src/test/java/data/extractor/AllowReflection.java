/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.extractor;


import data.extractor.TestInterface;

/**
 * Test class for CoherenceReflectFilterTests.
 *
 * @author jf 2020.05.19
 */
public class AllowReflection
    implements TestInterface
    {
    // ----- constructors ---------------------------------------------------

    public AllowReflection(String sValue)
        {
        m_sProperty = sValue;
        }

    // ----- TestInterface methods ------------------------------------------

    @Override
    public String getProperty()
        {
        return m_sProperty;
        }

    @Override
    public void setProperty(String sValue)
        {
        m_sProperty = sValue;
        }

    // ----- data members ---------------------------------------------------

    private String m_sProperty;
    }
