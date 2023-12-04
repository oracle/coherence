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

import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
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
        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        if (index == null)
            {
            // there is no relevant index
            return -1;
            }
        
        Map<E, Set<?>> mapContents = index.getIndexContents();
        if (mapContents.isEmpty())
            {
            return 0;
            }
        
        int cMatch = 0;
        if (mapContents instanceof SortedMap)
            {
            SortedMap<E, Set<?>> mapSorted     = (SortedMap<E, Set<?>>) mapContents;
            Integer              cAllOrNothing = allOrNothing(index, mapSorted, setKeys);
            if (cAllOrNothing != null)
                {
                return cAllOrNothing;
                }

            SortedMap<E, Set<?>> subMap = mapSorted.tailMap(getValue());
            for (Set<?> set : subMap.values())
                {
                cMatch += ensureSafeSet(set).size();
                }

            cMatch = subtractEqualKeys(mapSorted, cMatch);
            }
        else
            {
            for (Map.Entry<E, Set<?>> entry : mapContents.entrySet())
                {
                if (evaluateExtracted(entry.getKey()))
                    {
                    cMatch += ensureSafeSet(entry.getValue()).size();
                    }
                }
            }

        return cMatch;
        }

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        E value = getValue();
        if (value == null)
            {
            // nothing could be compared to null
            setKeys.clear();
            return null;
            }

        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        if (index == null)
            {
            // there is no relevant index; evaluate individual entries
            return this;
            }
        else if (index.getIndexContents().isEmpty())
            {
            // there are no entries in the index, which means no entries match this filter
            setKeys.clear();
            return null;
            }

        if (index.isOrdered())
            {
            SortedMap<E, Set<?>> mapContents   = (SortedMap<E, Set<?>>) index.getIndexContents();
            Integer              cAllOrNothing = allOrNothing(index, mapContents, setKeys);
            if (cAllOrNothing != null)
                {
                if (cAllOrNothing == 0)
                    {
                    setKeys.clear();
                    }
                return null;
                }

            SortedMap mapLT       = mapContents.headMap(value);
            SortedMap mapGE       = mapContents.tailMap(value);
            boolean   fHeadHeavy  = mapLT.size() > mapContents.size() / 2;

            removeEqualKeys(mapContents, setKeys);

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
            Map<E, Set<?>> mapContents = index.getIndexContents();

            if (index.isPartial())
                {
                Set setGT = new HashSet();
                for (Map.Entry<E, Set<?>> entry : mapContents.entrySet())
                    {
                    if (evaluateExtracted(entry.getKey()))
                        {
                        setGT.addAll(ensureSafeSet(entry.getValue()));
                        }
                    }
                setKeys.retainAll(setGT);
                }
            else
                {
                for (Map.Entry<E, Set<?>> entry : mapContents.entrySet())
                    {
                    if (!evaluateExtracted(entry.getKey()))
                        {
                        setKeys.removeAll(ensureSafeSet(entry.getValue()));
                        }
                    }
                }
            }

        return null;
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Subtract the number of equal keys from the specified match count.
     *
     * @param mapContents  the index contents
     * @param cMatch       current match count
     *
     * @return the updated match count, after equal keys were subtracted
     */
    protected int subtractEqualKeys(SortedMap<E, Set<?>> mapContents, int cMatch)
        {
        Set<?> setEQ = mapContents.get(getValue());
        if (setEQ != null)
            {
            cMatch -= setEQ.size();
            }
        return cMatch;
        }

    /**
     * Remove equal keys from the specified key set.
     *
     * @param mapContents  the index contents
     * @param setKeys      the mutable set of keys that remain to be filtered
     */
    protected void removeEqualKeys(SortedMap<E, Set<?>> mapContents, Set setKeys)
        {
        Set<?> setEQ = mapContents.get(getValue());
        if (setEQ != null)
            {
            setKeys.removeAll(setEQ);
            }
        }

    /**
     * Determine if the filter will match all or none of the entries in the index.
     *
     * @param index        the index
     * @param mapContents  the index contents
     * @param setKeys      the set of keys to filter
     *
     * @return {@code 0} if no entries match; {@code setKeys.size()} if all entries match;
     *         and {@code null} if only some entries match or no conclusive determination
     *         can be made
     */
    protected Integer allOrNothing(MapIndex index, SortedMap<E, Set<?>> mapContents, Set setKeys)
        {
        if (!index.isPartial())
            {
            try
                {
                E loValue = mapContents.firstKey();
                if (evaluateExtracted(loValue))
                    {
                    // all entries match, nothing to remove
                    return setKeys.size();
                    }

                E hiValue = mapContents.lastKey();
                if (!evaluateExtracted(hiValue))
                    {
                    // no entries match, remove all keys
                    return 0;
                    }
                }
            catch (NoSuchElementException e)
                {
                // could only happen if the index contents became empty, in which case no entries match
                return 0;
                }
            }

        return null;
        }
    }
