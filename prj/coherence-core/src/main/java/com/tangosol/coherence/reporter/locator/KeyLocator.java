/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.extractor.KeyExtractor;

import com.tangosol.util.ValueExtractor;

/**
* Column returning a JMX ObjectName part.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class KeyLocator
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
            m_veExtractor = new KeyExtractor(m_sName);
            }
        return m_veExtractor;
        }

    /**
    * @inheritDoc
    */
    public boolean isRowDetail()
        {
        return true;
        }
    }
