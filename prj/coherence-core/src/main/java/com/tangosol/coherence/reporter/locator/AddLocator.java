/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.extractor.AddExtractor;

import com.tangosol.util.ValueExtractor;

/**
* Instantiate an AddExtractor.
*
* @author ew 2008.02.20
* @since Coherence 3.4
*/
public class AddLocator
        extends FunctionLocator
    {
    /**
    * @inheritDoc
    */
    public ValueExtractor getExtractor()
        {
        if (m_veExtractor == null)
            {
            ValueExtractor[] aVE = new ValueExtractor[2];

            buildExtractors();

            aVE[0]       = m_veColumn1;
            aVE[1]       = m_veColumn2;

            m_veExtractor = new AddExtractor(aVE);
            }
        return m_veExtractor;
        }
    }
