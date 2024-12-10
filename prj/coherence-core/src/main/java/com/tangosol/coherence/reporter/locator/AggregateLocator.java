/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.extractor.AggregateExtractor;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;

import java.util.Set;


/**
* A super class for single column aggregations.
*
* @author ew 2008.01.28
* @since Coherence 3.4
*/
public class AggregateLocator
        extends BaseLocator
    {
    /**
    * @inheritDoc
    */
    public void configure(XmlElement xml)
        {
        super.configure(xml);
        String sColumnRef = xml.getSafeElement(TAG_COLUMNREF).getString();
        m_veColumn = Base.checkNotNull(m_queryHandler.ensureExtractor(sColumnRef), "Column extractor");
        }

    /**
    * @inheritDoc
    */
    public ValueExtractor getExtractor()
        {
        if (m_veAggregate == null)
            {
            m_veAggregate = new AggregateExtractor(m_source, m_iAggregatePos);
            }
        return m_veAggregate;
        }

    /**
    * @inheritDoc
    */
    public void reset(Set setResults)
        {
        super.reset(setResults);
        m_veAggregate = null; // must be nulled out for repeated calls.
        m_veColumn    = null; // must be nulled out for repeated calls.
        }

    /**
    * @inheritDoc
    */
    public boolean isAggregate()
        {
        return true;
        }


    // ----- data members ----------------------------------------------------

    /**
    * The Extractor to be aggregated.
    */
    protected ValueExtractor m_veColumn;

    /**
    * The Extractor to be aggregated.
    */
    protected ValueExtractor m_veAggregate;

    }
