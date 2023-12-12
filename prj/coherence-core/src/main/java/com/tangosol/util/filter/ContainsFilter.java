/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


/**
* Filter which tests a {@link Collection} or Object array value returned from
* a method invocation for containment of a given value.
* <p>
* More formally, if the specified extractor returns a Collection,
* {@link #evaluate evaluate(o)} is functionally equivalent to the following
* code:
* <pre>
* return ((Collection) extract(o)).contains(getValue());
* </pre>
* If the specified method returns an Object array, {@link #evaluate
* evaluate(o)} is functionally equivalent to the following code:
* <pre>
* return Collections.asList((Object[]) extract(o)).contains(getValue());
* </pre>
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the extracted attribute to use for comparison
*
* @author jh 2005.06.06
*/
public class ContainsFilter<T, E>
        extends    ComparisonFilter<T, E, E>
        implements IndexAwareFilter<Object, T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ContainsFilter()
        {
        }

    /**
    * Construct an ContainsFilter for testing containment of the given
    * object.
    *
    * @param extractor the ValueExtractor used by this filter
    * @param value     the object that a Collection or Object array is tested
    *                  to contain
    */
    public ContainsFilter(ValueExtractor<? super T, ? extends E> extractor, E value)
        {
        super(extractor, value);
        }

    /**
    * Construct an ContainsFilter for testing containment of the given
    * object.
    *
    * @param sMethod  the name of the method to invoke via reflection
    * @param value    the object that a Collection or Object array is tested
    *                 to contain
    */
    public ContainsFilter(String sMethod, E value)
        {
        super(sMethod, value);
        }

    // ----- Filter interface -----------------------------------------------

    protected String getOperator()
        {
        return "CONTAINS";
        }

    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        E value = getValue();

        if (extracted instanceof Collection)
            {
            return ((Collection) extracted).contains(value);
            }
        else if (extracted instanceof Object[])
            {
            Object[] aoExtracted = (Object[]) extracted;
            for (int i = 0, c = aoExtracted.length; i < c; ++i)
                {
                if (equals(aoExtracted[i], value))
                    {
                    return true;
                    }
                }
            }

        return false;
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        if (index == null)
            {
            // there is no relevant index
            return -1;
            }
        else
            {
            Set setEQ = (Set) index.getIndexContents().get(getValue());
            return setEQ == null ? 0 : setEQ.size();
            }
        }

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        if (index == null)
            {
            // there is no relevant index
            return this;
            }
        else
            {
            Set setEquals = (Set) index.getIndexContents().get(getValue());
            if (setEquals == null || setEquals.isEmpty())
                {
                setKeys.clear();
                }
            else
                {
                setKeys.retainAll(setEquals);
                }
            return null;
            }
        }
    }
