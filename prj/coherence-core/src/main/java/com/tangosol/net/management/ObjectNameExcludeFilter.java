/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management;


import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.AnyFilter;
import com.tangosol.util.filter.RegexFilter;


/**
* Filter used to prevent registering MBeans that match the specified pattern.
*
* @author gg  2010.05.02
* @since Coherence 3.6
*/
public class ObjectNameExcludeFilter
        extends Base
        implements Filter
    {
    /**
    * Construct an ObjectNameExcludeFilter based on one or more regular
    * expressions. The MBeans that match any of the specified patterns
    * <b>will not</b> be registered with the Coherence JMX Framework.
    *
    * @param asPattern  an array of patterns to match
    */
    public ObjectNameExcludeFilter(String[] asPattern)
        {
        Filter filter;

        int cPatterns = asPattern == null ? 0 : asPattern.length;
        if (cPatterns == 0)
            {
            filter = AlwaysFilter.INSTANCE;
            }
        else if (cPatterns == 1)
            {
            filter = new RegexFilter(IdentityExtractor.INSTANCE, asPattern[0]);
            }
        else
            {
            Filter[] afilter = new Filter[cPatterns];
            for (int i = 0; i < cPatterns; i++)
                {
                String sPattern = asPattern[i];
                afilter[i] = new RegexFilter(IdentityExtractor.INSTANCE, sPattern);
                }
            filter = new AnyFilter(afilter);
            }
        m_filter = filter;
        }

    /**
    * Construct an ObjectNameExcludeFilter based on one or more regular
    * expressions. The MBeans that match any of the specified patterns
    * <b>will not</b> be registered with the Coherence JMX Framework.
    *
    * @param sPatterns  a {@link java.util.regex.Pattern white space} delimited
    *                   sequence of patterns to match
    */
    public ObjectNameExcludeFilter(String sPatterns)
        {
        this(sPatterns.split("\\s+"));
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(Object o)
        {
        return !m_filter.evaluate(o);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ObjectNameExcludeFilter with another object to determine
    * equality. Two ObjectNameExcludeFilter objects are considered equal iff
    * they belong to exactly the same class and their filters are equal.
    *
    * @return true iff this ObjectNameExcludeFilter and the passed object are
    *         equivalent ObjectNameExcludeFilters
    */
    public boolean equals(Object o)
        {
        if (o instanceof ObjectNameExcludeFilter)
            {
            ObjectNameExcludeFilter that = (ObjectNameExcludeFilter) o;
            return this.getClass() == that.getClass()
                && equals(this.m_filter, that.m_filter);
            }

        return false;
        }

    /**
    * Determine a hash value for the ObjectNameExcludeFilter object according
    * to the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ObjectNameExcludeFilter object
    */
    public int hashCode()
        {
        Filter filter = m_filter;
        return filter == null ? 0 : filter.hashCode();
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        String sClass = getClass().getName();

        return sClass.substring(sClass.lastIndexOf('.') + 1) +
            "(Filter=" + m_filter + ')';
        }

    
    // ----- fields ---------------------------------------------------------

    /**
    * The underlying filter.
    */
    protected Filter m_filter;
    }