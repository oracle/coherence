/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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

    /**
    * Return the canonical name for this extractor.
    * Override {@link ValueExtractor#getCanonicalName()} method as an optimization since that
    * method assumes it is only working with lambdas.
    *
    * @return null
    */
    @Override
    public String getCanonicalName()
        {
        return null;
        }

    //----- data members -----------------------------------------------------

    /**
    * The constant value of the extractor
    */
    protected Object m_oConstant;
    }