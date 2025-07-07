/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.extractor;


import com.tangosol.util.ValueExtractor;

/**
* ValueExtractor to extract a fixed key value extractor in a query.
*
* @author ew 2008.03.17
* @since Coherence 3.4
*/
public class CorrelatedExtractor
        implements ValueExtractor
    {
    //----- Constructors -----------------------------------------------------
    /**
    * Construct an extractor that can have the target object "fixed" for the
    * duration of a query.
    *
    * @param veSource  the source extractor.
    */
    public CorrelatedExtractor(ValueExtractor veSource)
        {
        m_veSource = veSource;
        }

    //----- ValueExtractor interface -----------------------------------------
    /**
    * @inheritDoc
    */
    public Object extract(Object oTarget)
        {
        if (m_oTarget == null)
            {
            return m_veSource.extract(oTarget);
            }
        else
            {
            return m_veSource.extract(m_oTarget);
            }
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
    //----- accessors --------------------------------------------------------
    /**
    * Set the fixed target for the accessor.
    *
    * @param oTarget the fixed target for the wapped Extractor
    */
    public void setTarget(Object oTarget)
        {
        m_oTarget = oTarget;
        }

    //----- data members -----------------------------------------------------
    /**
    * the fixed target key.
    */
    protected Object m_oTarget;

    /**
    * the related ValueExtractor
    */
    protected ValueExtractor m_veSource;

    }
