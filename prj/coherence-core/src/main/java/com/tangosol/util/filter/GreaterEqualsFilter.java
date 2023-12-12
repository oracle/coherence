/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.ConditionalIndex;
import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.SafeSortedMap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;


/**
* Filter which compares the result of a method invocation with a value for
* "Greater or Equal" condition. In a case when either result of a method
* invocation or a value to compare are equal to null, the <tt>evaluate</tt>
* test yields <tt>false</tt>. This approach is equivalent to the way
* the NULL values are handled by SQL.
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the value to use for comparison
*
* @author cp/gg 2002.10.29
*/
public class GreaterEqualsFilter<T, E extends Comparable<? super E>>
        extends    GreaterFilter<T, E>
        implements IndexAwareFilter<Object, T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public GreaterEqualsFilter()
        {
        }

    /**
    * Construct a GreaterEqualFilter for testing "Greater or Equal"
    * condition.
    *
    * @param extractor the ValueExtractor to use by this filter
    * @param value     the object to compare the result with
    */
    public GreaterEqualsFilter(ValueExtractor<? super T, ? extends E> extractor, E value)
        {
        super(extractor, value);
        }

    /**
    * Construct a GreaterEqualFilter for testing "Greater or Equal"
    * condition.
    *
    * @param sMethod  the name of the method to invoke via reflection
    * @param value    the object to compare the result with
    */
    public GreaterEqualsFilter(String sMethod, E value)
        {
        super(sMethod, value);
        }

    // ----- Filter interface -----------------------------------------------

    protected String getOperator()
        {
        return ">=";
        }

    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        E value = getValue();

        return extracted != null && value != null &&
            extracted.compareTo(value) >= 0;
        }

    // ---- helpers ---------------------------------------------------------

    @Override
    protected int subtractEqualKeys(SortedMap<E, Set<?>> mapContents, int cMatch)
        {
        // equal keys should be included; simply return current match count
        return cMatch;
        }

    @Override
    protected void removeEqualKeys(SortedMap<E, Set<?>> mapContents, Set setKeys)
        {
        // equal keys should be included; do nothing
        }
    }
