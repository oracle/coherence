/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;


import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;

import javax.management.ObjectName;

/**
* ValueExtractor implmentation to extract part of the MBean ObjectName.
*
* @author ew 2008.02.20
* @since Coherence 3.4
*/
public class KeyExtractor
        implements ValueExtractor
    {
    // ----- constructors ----------------------------------------------------

    /**
    * Construct a KeyExtractor for the name "part"
    *
    * @param sName  the name of the ObjectName part to extract.
    */
    public KeyExtractor(String sName)
        {
        m_sKey = sName;
        }

    // ----- ValueExtractor interface ----------------------------------------

    /**
    * @inheritDoc
    */
    public Object extract(Object oTarget)
        {
        Base.azzert(oTarget instanceof ObjectName, "KeyExtractor only applies " +
                "to MBean ObjectName");

        // the returned value is a String
        return ((ObjectName) oTarget).getKeyPropertyList().get(m_sKey);
        }

    /**
    * Return the canonical name for this extractor.
    * Override {@link ValueExtractor#getCanonicalName()} method as an optimization since that
    * method assumes it is only working with lambdas.
    *
    * @return key name
    */
    @Override
    public String getCanonicalName()
        {
        return m_sKey;
        }

    /*
    * the key part to extract.
    */
    protected String m_sKey;
    }