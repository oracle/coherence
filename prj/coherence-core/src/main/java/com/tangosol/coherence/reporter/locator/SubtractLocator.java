/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.util.ValueExtractor;
import com.tangosol.coherence.reporter.extractor.SubtractExtractor;


/**
* FunctionLocator extension that subtracts the second value from the first
* value.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class SubtractLocator
        extends FunctionLocator
    {
    /**
    * @inhertiDoc
    */
    public ValueExtractor getExtractor()
        {
        ValueExtractor[] aVE = new ValueExtractor[2];

        if (m_veColumn1 == null || m_veColumn2 == null)
            {
            buildExtractors();
            }

        aVE[0] = m_veColumn1;
        aVE[1] = m_veColumn2;

        return new SubtractExtractor(aVE);
        }
    }
