/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
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

    /*
    * the key part to extract.
    */
    protected String m_sKey;
    }