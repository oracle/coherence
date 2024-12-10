/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter.locator;


import com.tangosol.coherence.reporter.Constants;
import com.tangosol.coherence.reporter.extractor.AttributeExtractor;
import com.tangosol.coherence.reporter.extractor.JoinExtractor;
import com.tangosol.coherence.reporter.JMXQueryHandler;
import com.tangosol.coherence.reporter.Reporter;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;

import javax.management.MBeanServer;

import java.util.Iterator;
import java.util.Set;

/**
* Locator for configuring an AttributeExtractor or JoinExtractor
*
* @since Coherence 3.4
* @author ew 2008.01.28
*
*/
public class AttributeLocator
        extends BaseLocator
        implements Constants
    {
    /**
    * @inheritDoc
    */
    public void configure(XmlElement xmlConfig)
        {
        super.configure(xmlConfig);
        m_fReturnNeg = xmlConfig.getSafeElement("return-neg").getBoolean(false);
        setJoinTemplate(xmlConfig.getSafeElement(Reporter.TAG_QUERY)
                           .getSafeElement(Reporter.TAG_PATTERN)
                           .getString(""));
        }

    /**
    * @inheritDoc
    */
    public ValueExtractor getExtractor()
        {
        if (m_veExtractor == null)
            {
            String      sTemplate = m_sJoinTemplate;
            MBeanServer server    = m_source.getMBeanServer();

            if(sTemplate == null || sTemplate.length() == 0)
                {
                m_veExtractor = new AttributeExtractor(m_sName, m_cDelim, m_fReturnNeg, server);
                }
            else
                {
                // the attribute is a join.
                Set setMacros = Reporter.getMacros(sTemplate);
                if (setMacros.size() > 0)
                    {
                    int              nSize = setMacros.size();
                    ValueExtractor[] aVE   = new ValueExtractor[nSize];
                    int              c     = 0;
                    JMXQueryHandler  qh    = m_queryHandler;
                    for (Iterator i = setMacros.iterator(); i.hasNext();)
                        {
                        String sExtractorName = (String)i.next();
                        aVE[c] = Base.checkNotNull(qh.ensureExtractor(sExtractorName), "Column extractor");
                        c++;
                        }
                    return new JoinExtractor(aVE, m_sJoinTemplate,
                            new AttributeExtractor(m_sName, ',', m_fReturnNeg, server),
                            server);
                    }
                return null; // not sure if this is a valid case.
                }
            }
        return m_veExtractor;
        }

    /**
    * Obtain the join template string for joined attributes
    *
    * @return a JMX query string containing value replacement {macros}.
    */
    public String getJoinTemplate()
        {
        return m_sJoinTemplate;
        }

    /**
    * Set the join template string for joined attributes
    *
    * @param sTemplate a JMX query string containing v  alue replacement {macros}.
    */
    protected void setJoinTemplate(String sTemplate)
        {
        m_sJoinTemplate = sTemplate;
        }

    /**
    * @inheritDoc
    */
    public boolean isRowDetail()
        {
        return true;
        }

    //----- data members -----------------------------------------------------

    /**
    * A JMX query string containing value replacement macros  The macros will
    * be replaced with data from column information during runtime.
    */
    protected String m_sJoinTemplate;

    /**
    * flag allow the return of a negative number
    */
    protected boolean m_fReturnNeg = false;
    }
