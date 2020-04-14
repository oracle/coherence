/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.extractor.ConstantExtractor;

import com.tangosol.util.ValueExtractor;

import java.util.Date;


/**
* Global column to include report start time as a column.  Including time
* is helpful when doing time series analysis.
*
* @since Coherence 3.4
* @author ew 2008.01.28
*/
public class DateTimeLocator
        extends BaseLocator
    {
    /**
    * @inheritDoc
    */
    public ValueExtractor getExtractor()
        {
        super.getExtractor();
        if (m_veExtractor == null)
            {
            m_veExtractor = new ConstantExtractor(
                    new Date(m_queryHandler.getStartTime()));
            }
        return m_veExtractor;
        }
    }
