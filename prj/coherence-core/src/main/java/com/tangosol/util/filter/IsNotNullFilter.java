/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.ValueExtractor;


/**
* Filter which tests the result of a method invocation for inequality to null.
*
* @author cp/gg 2002.10.27
*/
public class IsNotNullFilter<T, E>
        extends NotEqualsFilter<T, E>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public IsNotNullFilter()
        {
        }

    /**
    * Construct a IsNotNullFilter for testing inequality to null.
    *
    * @param extractor the ValueExtractor to use by this filter
    */
    public IsNotNullFilter(ValueExtractor<? super T, ? extends E> extractor)
        {
        super(extractor, null);
        }

    /**
    * Construct a IsNotNullFilter for testing inequality to null.
    *
    * @param sMethod  the name of the method to invoke via reflection
    */
    public IsNotNullFilter(String sMethod)
        {
        super(sMethod, null);
        }

    // ----- Filter interface -----------------------------------------------

    public String toExpression()
        {
        return "IS NOT NULL";
        }
    }
