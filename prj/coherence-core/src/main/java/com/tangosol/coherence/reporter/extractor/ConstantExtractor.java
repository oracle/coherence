/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;


import com.tangosol.util.ValueExtractor;

/**
* Generic extractor for constant values.
*
* @author ew 2008.03.17
* @since Coherence 3.4
*/
public class ConstantExtractor
        implements ValueExtractor
    {
    // ----- constructors ----------------------------------------------------

    /**
    * Construct an Extractor for the Value.
    *
    * @param oValue  the value to be extracted.
    */
    public ConstantExtractor(Object oValue)
        {
        m_oConstant = oValue;
        }

    // ----- ValueExtractor interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object extract(Object oTarget)
        {
        return m_oConstant;
        }

    //----- data members -----------------------------------------------------

    /**
    * The constant value of the extractor
    */
    protected Object m_oConstant;
    }