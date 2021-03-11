/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.HashSet;


/**
* Filter which compares the result of a method invocation with a value for
* inequality.
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the value to use for comparison
*
* @author cp/gg 2002.10.27
*/
public class NotEqualsFilter<T, E>
        extends    ComparisonFilter<T, E, E>
        implements IndexAwareFilter<Object, T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public NotEqualsFilter()
        {
        }

    /**
    * Construct a NotEqualsFilter for testing inequality.
    *
    * @param extractor the ValueExtractor to use by this filter
    * @param value     the object to compare the result with
    */
    public NotEqualsFilter(ValueExtractor<? super T, ? extends E> extractor, E value)
        {
        super(extractor, value);
        }

    /**
    * Construct a NotEqualsFilter for testing inequality.
    *
    * @param sMethod  the name of the method to invoke via reflection
    * @param value    the object to compare the result with
    */
    public NotEqualsFilter(String sMethod, E value)
        {
        super(sMethod, value);
        }


    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        return !equals(extracted, getValue());
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        return calculateMatchEffectiveness(mapIndexes, setKeys);
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
            Map    mapContents = index.getIndexContents();
            Object oValue      = getValue();

            if (index.isPartial())
                {
                Set setNE = new HashSet();
                for (Iterator iter = mapContents.entrySet().iterator();
                     iter.hasNext();)
                    {
                    Map.Entry entry = (Map.Entry) iter.next();
                    if (!entry.getKey().equals(oValue))
                        {
                        setNE.addAll(ensureSafeSet((Set) entry.getValue()));
                        }
                    }
                setKeys.retainAll(setNE);
                }
            else
                {
                Set setEquals = (Set) mapContents.get(oValue);

                setKeys.removeAll(ensureSafeSet(setEquals));
                }
            return null;
            }
        }
    }