/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.extractor.MultiplyExtractor;

import com.tangosol.util.ValueExtractor;

/**
* FunctionLocator extension to multiply two locators
*
* @since Coherence 3.4
* @author ew 2008.01.28
*/
public class MultiplyLocator
    extends FunctionLocator
    {
    /**
    * @inheritDoc
    */
    public ValueExtractor getExtractor()
        {
        super.getExtractor();
        if (m_veExtractor == null)
            {
            ValueExtractor[] aVE = new ValueExtractor[2];

            if (m_veColumn1 == null || m_veColumn2 == null)
                {
                buildExtractors();
                }
            aVE[0] = m_veColumn1;
            aVE[1] = m_veColumn2;

            return new MultiplyExtractor(aVE);
            }
        return m_veExtractor;
        }
    }
