/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.extractor.ConstantExtractor;

import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;

/**
* Global column to include a Batch identifier into a report.  The batch identifier
* is incremented with each execution of the Report Group.  The batch identifier
* is helpfull when needing to associate data from related reports.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class BatchLocator
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
            m_veExtractor =  new ConstantExtractor(
                    Long.valueOf(m_queryHandler.getBatch()));
            }
        return m_veExtractor;
        }

    }
