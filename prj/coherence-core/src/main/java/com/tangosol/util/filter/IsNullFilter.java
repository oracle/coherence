/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.ValueExtractor;


/**
* Filter which compares the result of a method invocation with null.
*
* @author cp/gg 2002.10.27
*/
public class IsNullFilter<T, E>
        extends EqualsFilter<T, E>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public IsNullFilter()
        {
        }

    /**
    * Construct a IsNullFilter for testing equality to null.
    *
    * @param extractor the ValueExtractor to use by this filter
    */
    public IsNullFilter(ValueExtractor<? super T, ? extends E> extractor)
        {
        super(extractor, null);
        }

    /**
    * Construct a IsNullFilter for testing equality to null.
    *
    * @param sMethod  the name of the method to invoke via reflection
    */
    public IsNullFilter(String sMethod)
        {
        super(sMethod, null);
        }
    }
