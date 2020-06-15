/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.reporter;


import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.ValueExtractor;
import com.tangosol.util.Base;
import com.tangosol.util.extractor.ComparisonValueExtractor;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.OrFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.LessFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.Filter;

import java.util.Iterator;
import java.util.List;
import java.util.Comparator;


/**
* Factory to create Filter instances from XML defintion.
*
* @author ew 2008.02.27
* @since Coherence 3.4
*/
public class FilterFactory
        implements Constants
    {
    /**
    * Create a new FilterFactory for the specified query handler and XML
    * definition.
    *
    * @param query      The query context for the filter.
    *
    * @param xmlFilters The filter and subfilter XML definition.
    */
    public FilterFactory(JMXQueryHandler query, XmlElement xmlFilters)
        {
        m_query = query;
        m_xmlFilters = xmlFilters;
        }

    /**
    * Instantiates a Filter as defined by the XML
    *
    * @param xmlConfig  The Filter configuation XML.
    *
    * @return a Filter instance
    */
    public Filter getFilter(XmlElement xmlConfig)
        {
        String sType = xmlConfig.getSafeElement(TAG_FILTERTYPE).getString();

        if (sType.equals(VALUE_AND) || sType.equals(VALUE_OR)
         || sType.equals(VALUE_NOT))
            {
            List listRef = xmlConfig.getSafeElement(TAG_PARAMS).getElementList();
            if (listRef.size() == 2)
                {
                Iterator   i           = listRef.iterator();
                XmlElement xmlRef      = (XmlElement) i.next();
                String     sId         = xmlRef.getString();
                Filter     filterRight = getFilter(sId);

                xmlRef = (XmlElement) i.next();
                sId    = xmlRef.getString();

                Filter filterLeft = getFilter(sId);
                if (sType.equals(VALUE_AND))
                    {
                    return new AndFilter(filterLeft, filterRight);
                    }
                else
                    {
                    return new OrFilter(filterLeft, filterRight);
                    }
                }
            else
                {
                if (listRef.size() == 1)
                    {
                    Iterator   i      = listRef.iterator();
                    XmlElement xmlRef = (XmlElement) i.next();
                    String     sId    = xmlRef.getString();
                    Filter     filter = getFilter(sId);
                    return new NotFilter(filter);
                    }
                else
                    {
                    Base.log("FilterFactory: Invalid Filter Definition:" +
                            xmlConfig.toString());
                    return null;
                    }
                }
            }
        else
            {
            List listRef    = xmlConfig.getSafeElement(TAG_PARAMS).getElementList();
            if (listRef.size() == 2)
                {

                Iterator       iter   = listRef.iterator();
                XmlElement     xmlRef = (XmlElement) iter.next();
                String         sId    = xmlRef.getString();
                ValueExtractor ve1    = m_query.ensureExtractor(sId);

                xmlRef = (XmlElement) iter.next();
                sId    = xmlRef.getString();
                ValueExtractor ve2    = m_query.ensureExtractor(sId);

                if (sType.equals(VALUE_GREATER))
                    {
                    return new GreaterFilter(new ComparisonValueExtractor(ve1,ve2,
                                        new NumericComparator()),
                                        Integer.valueOf(0));
                    }
                else if (sType.equals(VALUE_LESS))
                    {
                    return new LessFilter(new ComparisonValueExtractor(ve1,ve2,
                                        new NumericComparator()),
                                        Integer.valueOf(0));
                    }
                else if (sType.equals(VALUE_EQUALS))
                    {
                    return new EqualsFilter(new ComparisonValueExtractor(ve1,ve2,
                                        new NumericComparator()),
                                        Integer.valueOf(0));
                    }
                Base.log("FilterFactory: Invalid Filter Definition:" +
                        xmlConfig.toString());
                return null;
                }
            else
                {
                Base.log("FilterFactory: Invalid Filter Definition:" + xmlConfig.toString());
                return null;
                }
            }
        }

    /**
    * Create a Filter as defined by the filter reference Identifier
    *
    * @param sId a string identifier for a filter.
    *
    * @return a Filter instance.
    */
    public Filter getFilter(String sId)
        {
        return getFilter(getFilterXmlByRef(sId));
        }

    /**
    * Determine the configuration XML for a filter based on the filter
    * reference.
    *
    * @param sRef   the reference identifier to locate
    *
    * @return the configuration XML for the filter reference
    */
    public XmlElement getFilterXmlByRef(String sRef)
        {
        XmlElement xmlFilters = m_xmlFilters;

        if (xmlFilters != null)
            {
            List listXml = xmlFilters.getElementList();
            for (Iterator i = listXml.iterator(); i.hasNext();)
                {
                XmlElement xmlSub = (XmlElement) i.next();
                String sTemp = xmlSub.getSafeAttribute("id").getString();
                if (sTemp.equals(sRef) && xmlSub.getElementList() != null)
                    {
                    return xmlSub;
                    }
                }
            }
            return null;
        }

    /**
    * Determine the report configuration xml for a given XmlElement.
    *
    * @param xml   the xml configuration component to determine the report
    *
    * @return The report configuration XML
    */
     protected static XmlElement getReportXml(XmlElement xml)
        {
        XmlElement xmlTemp = xml;
        while (xmlTemp.getElement(Reporter.TAG_REPORT) == null)
            {
            xmlTemp = xmlTemp.getParent();
            if (xmlTemp == null)
                {
                return null;
                }
            }
        return xmlTemp.getElement(Reporter.TAG_REPORT);
        }

    /**
    * A numeric comparator class providing type independent numeric comparison.
    *
    * @author ew 2008.02.27
    * @since Coherence 3.4
    */
    protected static class NumericComparator
        implements Comparator
        {
        /**
        * @inheritDoc
        */
        public int compare(Object o1, Object o2)
            {
            if (o1 instanceof Number && o2 instanceof Number)
                {
                double d1 = ((Number)o1).doubleValue();
                double d2 = ((Number)o2).doubleValue();
                if (d1 == d2)
                    {
                    return 0;
                    }
                if (d1 < d2)
                    {
                    return -1;
                    }
                return 1;
                }
            if (o1 instanceof Comparable && o2 instanceof Comparable
                    && o1.getClass().getName().equals(o2.getClass().getName()))
                {
                return ((Comparable)o1).compareTo(o2);
                }
            throw new IllegalArgumentException("NumericComparator only " +
                    "accepts Numeric Objects");
            }
        }



    // ----- data members ---------------------------------------------------
    /**
    * The XML definition for the Query and related sub filters.
    */
    protected XmlElement m_xmlFilters;

    /**
    * The query context for filter creation.  The query creates ValueExtractors
    * for comparison filters.
    */
    protected JMXQueryHandler m_query;
    }
