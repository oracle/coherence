/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
/**
 * FilterFactory is a utility class that provides a set of
 * factory methods used for building instances of 
 *<see cref="IFilter"/> or <see cref="IValueExtractor"/>.
 *
 * @since Coherence 3.7.1.10
 * @author par 7/24/13
 */
package com.oracle.coherence.testing;

import com.tangosol.net.InvocationService;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import java.lang.String;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class FilterFactory
    {

    //----- FilterBuilder methods -------------------------------------------

    /**
     * Make a new Filter from the given string.
     * @param s  String in the Coherence Query Language representing an Filter.
     *
     * @return   The constructed Filter.
     */
    public static Filter createFilter(String s, InvocationService service)
        {
        if (service == null)
            {
            return null;
            }

        Map result = (Map) service.query(new FilterFetcher(s), null);
        if (result == null)
            {
            return null;
            }

        Iterator it = result.keySet().iterator();
        if (it.hasNext())
            {
            return (Filter) result.get(it.next());
            }
        return null;
        }

    /**
     * Make a new ValueExtracter from the given String.
     * @param s  String in the Coherence Query Language representing a
     *           ValueExtractor.
     *
     * @return  The constructed ValueExtractor.
     */
    public static ValueExtractor createExtractor(String s, InvocationService service)
        {
        if (service == null)
            {
            return null;
            }
        Collection result = (Collection) service.query(new FilterFetcher(s, true), null);
        if (result == null)
            {
            return null;
            }

        Iterator<ValueExtractor> it = result.iterator();
        if (it.hasNext())
            {
            return (ValueExtractor) it.next();
            }
        return null;
        }
    }
