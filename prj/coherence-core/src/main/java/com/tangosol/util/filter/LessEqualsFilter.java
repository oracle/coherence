/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;

import com.tangosol.util.ValueExtractor;

/**
* Filter which compares the result of a method invocation with a value for
* "Less or Equals" condition. In a case when either result of a method
* invocation or a value to compare are equal to null, the <tt>evaluate</tt>
* test yields <tt>false</tt>. This approach is equivalent to the way
* the NULL values are handled by SQL.
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of value to use for comparison
*
* @author cp/gg 2002.10.29
*/
public class LessEqualsFilter<T, E extends Comparable<? super E>>
        extends    LessFilter<T, E>
        implements IndexAwareFilter<Object, T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public LessEqualsFilter()
        {
        }

    /**
    * Construct a LessEqualsFilter for testing "Less or Equals" condition.
    *
    * @param extractor the ValueExtractor to use by this filter
    * @param value     the object to compare the result with
    */
    public LessEqualsFilter(ValueExtractor<? super T, ? extends E> extractor, E value)
        {
        super(extractor, value);
        }

    /**
    * Construct a LessEqualsFilter for testing "Less or Equals" condition.
    *
    * @param sMethod  the name of the method to invoke via reflection
    * @param value    the object to compare the result with
    */
    public LessEqualsFilter(String sMethod, E value)
        {
        super(sMethod, value);
        }

    // ----- Filter interface -----------------------------------------------

    protected String getOperator()
        {
        return "<=";
        }

    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        E value = getValue();

        return extracted != null && value != null &&
               extracted.compareTo(value) <= 0;
        }

    // ---- helpers ---------------------------------------------------------

    @Override
    protected boolean includeEquals()
        {
        return true;
        }
    }
