/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Filter;


/**
* Filter which returns the logical "or" of two other filters.
*
* @author cp/gg 2002.10.27
*/
public class OrFilter
        extends AnyFilter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public OrFilter()
        {
        }

    /**
    * Construct an "or" filter. The result is defined as:
    * <blockquote>
    *   filterLeft || filterRight
    * </blockquote>
    *
    * @param filterLeft   the "left" filter
    * @param filterRight  the "right" filter
    */
    public OrFilter(Filter filterLeft, Filter filterRight)
        {
        super(new Filter[] {filterLeft, filterRight});
        }
    }
