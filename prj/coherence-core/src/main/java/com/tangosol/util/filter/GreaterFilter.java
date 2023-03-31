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
import com.tangosol.util.SafeSortedMap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;


/**
* Filter which compares the result of a method invocation with a value for
* "Greater" condition. In a case when either result of a method
* invocation or a value to compare are equal to null, the <tt>evaluate</tt>
* test yields <tt>false</tt>. This approach is equivalent to the way
* the NULL values are handled by SQL.
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the value to use for comparison
*
* @author cp/gg 2002.10.29
*/
@SuppressWarnings({"rawtypes", "unchecked"})
public class GreaterFilter<T, E extends Comparable<? super E>>
        extends    ComparisonFilter<T, E, E>
        implements IndexAwareFilter<Object, T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public GreaterFilter()
        {
        }

    /**
    * Construct a GreaterFilter for testing "Greater" condition.
    *
    * @param extractor the ValueExtractor to use by this filter
    * @param value     the object to compare the result with
    */
    public GreaterFilter(ValueExtractor<? super T, ? extends E> extractor, E value)
        {
        super(extractor, value);
        }

    /**
    * Construct a GreaterFilter for testing "Greater" condition.
    *
    * @param sMethod  the name of the method to invoke via reflection
    * @param value    the object to compare the result with
    */
    public GreaterFilter(String sMethod, E value)
        {
        super(sMethod, value);
        }


    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        E value = getValue();

        return extracted != null && value != null &&
            extracted.compareTo(value) > 0;
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        return calculateRangeEffectiveness(mapIndexes, setKeys);
        }

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        Object oValue = getValue();
        if (oValue == null)
            {
            // nothing could be compared to null
            setKeys.clear();
            return null;
            }

        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        if (index == null)
            {
            // there is no relevant index
            return this;
            }

        if (index.isOrdered())
            {
            SortedMap mapContents = (SortedMap) index.getIndexContents();
            Set       setEQ       = (Set) mapContents.get(oValue);
            SortedMap mapLT       = mapContents.headMap(oValue);
            SortedMap mapGE       = mapContents.tailMap(oValue);
            boolean   fHeadHeavy  = mapLT.size() > mapContents.size() / 2;

            if (setEQ != null)
                {
                setKeys.removeAll(setEQ);
                }

            if (fHeadHeavy || index.isPartial())
                {
                Set setGT = new HashSet();
                for (Object o : mapGE.values())
                    {
                    Set set = (Set) o;
                    setGT.addAll(ensureSafeSet(set));
                    }
                setKeys.retainAll(setGT);
                }
            else
                {
                for (Object o : mapLT.values())
                    {
                    setKeys.removeAll(ensureSafeSet((Set) o));
                    }
                }

            // Note: the NULL set doesn't get in
            }
        else
            {
            Map mapContents = index.getIndexContents();

            if (index.isPartial())
                {
                Set setGT = new HashSet();
                for (Object o : mapContents.entrySet())
                    {
                    Map.Entry  entry = (Map.Entry) o;
                    Comparable oTest = (Comparable) entry.getKey();
                    if (oTest != null && oTest.compareTo(oValue) > 0)
                        {
                        setGT.addAll(ensureSafeSet((Set) entry.getValue()));
                        }
                    }
                setKeys.retainAll(setGT);
                }
            else
                {
                for (Object o : mapContents.entrySet())
                    {
                    Map.Entry  entry = (Map.Entry) o;
                    Comparable oTest = (Comparable) entry.getKey();
                    if (oTest == null || oTest.compareTo(oValue) <= 0)
                        {
                        setKeys.removeAll(ensureSafeSet((Set) entry.getValue()));
                        }
                    }
                }
            }
        return null;
        }
    }
