/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Filter;


/**
* Filter which returns the logical "and" of two other filters.
*
* @author cp/gg 2002.10.26
*/
public class AndFilter
        extends AllFilter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public AndFilter()
        {
        }

    /**
    * Construct an "and" filter. The result is defined as:
    * <blockquote>
    *   filterLeft &amp;&amp; filterRight
    * </blockquote>
    *
    * @param filterLeft   the "left" filter
    * @param filterRight  the "right" filter
    */
    public AndFilter(Filter filterLeft, Filter filterRight)
        {
        super(new Filter[] {filterLeft, filterRight});
        }
    }
