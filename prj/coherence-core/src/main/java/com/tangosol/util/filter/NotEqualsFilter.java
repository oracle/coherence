/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.ChainedCollection;
import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
* Filter which compares the result of a method invocation with a value for
* inequality.
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the value to use for comparison
*
* @author cp/gg 2002.10.27
*/
@SuppressWarnings({"rawtypes", "unchecked"})
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

    // ----- Filter interface -----------------------------------------------

    protected String getOperator()
        {
        return "!=";
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
        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        if (index == null)
            {
            // there is no relevant index
            return -1;
            }
        else
            {
            Set setEquals = (Set) index.getIndexContents().get(getValue());
            return setEquals == null ? setKeys.size() : setKeys.size() - setEquals.size();
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
            Map    mapContents = index.getIndexContents();
            Object oValue      = getValue();

            if (index.isPartial())
                {
                List<Set<?>> listNE = new ArrayList<>(mapContents.size());
                for (Object o : mapContents.entrySet())
                    {
                    Map.Entry entry = (Map.Entry) o;
                    if (!entry.getKey().equals(oValue))
                        {
                        listNE.add(ensureSafeSet((Set) entry.getValue()));
                        }
                    }
                setKeys.retainAll(new ChainedCollection<>(listNE.toArray(Set[]::new)));
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
