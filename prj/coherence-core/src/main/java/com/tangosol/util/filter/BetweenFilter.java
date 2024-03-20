/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.QueryContext;
import com.tangosol.util.QueryMap;
import com.tangosol.util.QueryRecord;
import com.tangosol.util.ValueExtractor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.NavigableMap;

import static com.tangosol.util.filter.ExtractorFilter.ensureSafeSet;

/**
* Filter which compares the result of a method invocation with a value for
* "Between" condition.  We use the standard ISO/IEC 9075:1992 semantic,
* according to which "X between Y and Z" is equivalent to "X &gt;= Y &amp;&amp; X &lt;= Z".
* In a case when either result of a method invocation or a value to compare
* are equal to null, the <tt>evaluate</tt> test yields <tt>false</tt>.
* This approach is equivalent to the way the NULL values are handled by SQL.
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the extracted attribute to use for comparison
*
* @author cp/gg 2002.10.29
* @author jk    2014.05.20
*/
// This class extends AndFilter to maintain backward compatibility with previous
// versions of Coherence. The methods of AndFilter are overridden in this class
// so that it effectively behave more like an ExtractorFilter.
@SuppressWarnings({"unchecked", "rawtypes"})
public class BetweenFilter<T, E extends Comparable<? super E>>
        extends AndFilter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for serialization).
    */
    public BetweenFilter()
        {
        }

    /**
     * Construct a BetweenFilter for testing "Between" condition.
     *
     * @param extractor  the ValueExtractor to use by this filter
     * @param from       the object to compare the "Greater or Equals"
     *                   boundary with
     * @param to         the object to compare the "Less or Equals" boundary
     *                   with
     */
    public BetweenFilter(ValueExtractor<? super T, ? extends E> extractor, E from, E to)
        {
        this(extractor, from, to, true, true);
        }

    /**
    * Construct a BetweenFilter for testing "Between" condition.
    *
    * @param sMethod  the name of the method to invoke via reflection
    * @param from     the object to compare the "Greater or Equals" boundary
    *                 with
    * @param to       the object to compare the "Less or Equals" boundary
    *                 with
    */
    public BetweenFilter(String sMethod, E from, E to)
        {
        this(sMethod, from, to, true, true);
        }

    /**
     * Construct a BetweenFilter for testing "Between" condition.
     *
     * @param sMethod             the name of the method to invoke via reflection
     * @param lowerBound          the lower bound of the range
     * @param upperBound          the upper bound of the range
     * @param fIncludeLowerBound  a flag indicating whether values matching the lower bound evaluate to true
     * @param fIncludeUpperBound  a flag indicating whether values matching the upper bound evaluate to true
     */
    public BetweenFilter(String sMethod, E lowerBound, E upperBound,
                         boolean fIncludeLowerBound, boolean fIncludeUpperBound)
        {
        super(fIncludeLowerBound
              ? new GreaterEqualsFilter<>(sMethod, lowerBound)
              : new GreaterFilter<>(sMethod, lowerBound),
              fIncludeUpperBound
              ? new LessEqualsFilter<>(sMethod, upperBound)
              : new LessFilter<>(sMethod, upperBound));
        }

    /**
     * Construct a BetweenFilter for testing "Between" condition.
     *
     * @param extractor           the {@link ValueExtractor} to be used by this filter
     * @param lowerBound          the lower bound of the range
     * @param upperBound          the upper bound of the range
     * @param fIncludeLowerBound  a flag indicating whether values matching the lower bound evaluate to true
     * @param fIncludeUpperBound  a flag indicating whether values matching the upper bound evaluate to true
     */
    public BetweenFilter(
            ValueExtractor<? super T, ? extends E> extractor, E lowerBound, E upperBound,
            boolean fIncludeLowerBound, boolean fIncludeUpperBound)
        {
        super(fIncludeLowerBound
                ? new GreaterEqualsFilter<>(extractor, lowerBound)
                : new GreaterFilter<>(extractor, lowerBound),
              fIncludeUpperBound
                ? new LessEqualsFilter<>(extractor, upperBound)
                : new LessFilter<>(extractor, upperBound));
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Obtain the ValueExtractor used by this filter.
     *
     * @return the ValueExtractor used by this filter
     */
    public ValueExtractor getValueExtractor()
        {
        return getLowerFilter().getValueExtractor();
        }

    /**
     * Obtain the lower bound of the range being used to evaluate
     * values by this BetweenFilter.
     *
     * @return the lower bound of the range being used to evaluate
     *         values by this BetweenFilter
     */
    public E getLowerBound()
        {
        return getLowerFilter().getValue();
        }

    protected ComparisonFilter<T, E, E> getLowerFilter()
        {
        return (ComparisonFilter<T, E, E>) getFilters()[0];
        }

    /**
     * Obtain the upper bound of the range being used to evaluate
     * values by this BetweenFilter.
     *
     * @return the upper bound of the range being used to evaluate
     *         values by this BetweenFilter
     */
    public E getUpperBound()
        {
        return getUpperFilter().getValue();
        }

    protected ComparisonFilter<T, E, E> getUpperFilter()
        {
        return (ComparisonFilter<T, E, E>) getFilters()[1];
        }

    /**
     * Obtain the flag indicating whether values matching the lower bound
     * of the range evaluate to true.
     *
     * @return the flag indicating whether values matching the lower bound
     *         of the range evaluate to true
     */
    public boolean isLowerBoundInclusive()
        {
        return getLowerFilter() instanceof GreaterEqualsFilter;
        }

    /**
     * Obtain the flag indicating whether values matching the upper bound
     * of the range evaluate to true.
     *
     * @return the flag indicating whether values matching the upper bound
     *         of the range evaluate to true
     */
    public boolean isUpperBoundInclusive()
        {
        return getUpperFilter() instanceof LessEqualsFilter;
        }

    // ----- Filter methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean evaluate(Object oTarget)
        {
        return evaluateExtracted(getValueExtractor().extract(oTarget));
        }

    public String toExpression()
        {
        return getValueExtractor().getCanonicalName() + " BETWEEN " +
               (isLowerBoundInclusive() ? '[' : '(') +
               getLowerBound() + ", " + getUpperBound() +
               (isUpperBoundInclusive() ? ']' : ')');
        }

    // ----- EntryFilter interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry entry)
        {
        ValueExtractor extractor = getValueExtractor();

        return evaluateExtracted(entry instanceof QueryMap.Entry
                ? ((QueryMap.Entry) entry).extract(extractor)
                : InvocableMapHelper.extractFromEntry(extractor, entry));
        }

    // ----- ArrayFilter methods --------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean evaluateEntry(Map.Entry entry, QueryContext ctx, QueryRecord.PartialResult.TraceStep step)
        {
        return evaluateFilter(this, entry, ctx, step == null ? null : step.ensureStep(this));
        }

    @Override
    protected void optimizeFilterOrder(Map mapIndexes, Set setKeys)
        {
        // no-op; order is important here
        }

    // ----- IndexAwareFilter methods ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        if (getLowerBound() == null || getUpperBound() == null)
            {
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

        Map<Object, Set> mapInverse = index.getIndexContents();

        if (mapInverse instanceof NavigableMap)
            {
            applySortedIndex(index, setKeys, (NavigableMap) mapInverse);
            return null;
            }

        Set setToRetain = new HashSet();

        for (Map.Entry<Object, Set> entry : mapInverse.entrySet())
            {
            if (evaluateExtracted(entry.getKey()))
                {
                setToRetain.addAll(ensureSafeSet(entry.getValue()));
                }
            }

        setKeys.retainAll(setToRetain);

        return null;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        if (index == null)
            {
            // there is no relevant index
            return -1;
            }

        Map<E, Set<?>> mapContents = index.getIndexContents();
        int cMatch = 0;
        if (mapContents instanceof NavigableMap)
            {
            NavigableMap<E, Set<?>> mapSorted     = (NavigableMap<E, Set<?>>) mapContents;
            Integer                 cAllOrNothing = allOrNothing(index, mapSorted, setKeys);
            if (cAllOrNothing != null)
                {
                return cAllOrNothing;
                }

            NavigableMap<E, Set<?>> subMap = mapSorted.subMap(getLowerBound(), isLowerBoundInclusive(), getUpperBound(), isUpperBoundInclusive());
            for (Set<?> set : subMap.values())
                {
                cMatch += ensureSafeSet(set).size();
                }
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

    // ----- QueryRecorderFilter methods ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void explain(QueryContext ctx, QueryRecord.PartialResult.ExplainStep step, Set setKeys)
        {
        AbstractQueryRecorderFilter.explain(this, ctx.getBackingMapContext().getIndexMap(),
                                            setKeys, step, getValueExtractor());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Filter trace(QueryContext ctx, QueryRecord.PartialResult.TraceStep step, Set setKeys)
        {
        return AbstractQueryRecorderFilter.trace(this, ctx.getBackingMapContext().getIndexMap(),
                                                 setKeys, step, getValueExtractor());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean trace(QueryContext ctx, QueryRecord.PartialResult.TraceStep step, Map.Entry entry)
        {
        return AbstractQueryRecorderFilter.trace(this, entry, step);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        ValueExtractor extractor = getValueExtractor();

        return getClass().getSimpleName() + "(" + extractor
                + (isLowerBoundInclusive() ? " >= " : " > ") + getLowerBound() +
                " and " + extractor
                + (isUpperBoundInclusive() ? " <= " : " < ") + getUpperBound() + ")";
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Evaluate the specified extracted value.
     *
     * @param oExtracted  an extracted value to evaluate
     *
     * @return true if the test passes
     */
    protected boolean evaluateExtracted(Object oExtracted)
        {
        Comparable oLowerBound        = getLowerBound();
        Comparable oUpperBound        = getUpperBound();
        boolean    fIncludeLowerBound = isLowerBoundInclusive();
        boolean    fIncludeUpperBound = isUpperBoundInclusive();

        if (oExtracted == null || oLowerBound == null || oUpperBound == null)
            {
            return false;
            }

        int cl = oLowerBound.compareTo(oExtracted);

        if ((fIncludeLowerBound && cl > 0) || (!fIncludeLowerBound && cl >= 0))
            {
            return false;
            }

        int cu = ((Comparable) oExtracted).compareTo(oUpperBound);

        return (!fIncludeUpperBound || cu <= 0) && (fIncludeUpperBound || cu < 0);
        }

    /**
     * Called by the {@link #applyIndex(java.util.Map, java.util.Set)} method
     * if the index corresponding to this filter's value extractor is a
     * sorted index.
     *
     * @param index        the index to apply
     * @param setKeys      the set of keys of the entries being filtered
     * @param mapContents  the index contents to evaluate
     */
    protected void applySortedIndex(MapIndex index, Set setKeys, NavigableMap<E, Set<?>> mapContents)
        {
        Integer cAllOrNothing = allOrNothing(index, mapContents, setKeys);
        if (cAllOrNothing != null)
            {
            if (cAllOrNothing == 0)
                {
                setKeys.clear();
                }
            return;
            }

        NavigableMap<E, Set<?>> mapRange        = mapContents.subMap(getLowerBound(), isLowerBoundInclusive(), getUpperBound(), isUpperBoundInclusive());
        Collection              colKeysToRetain = new HashSet<>();

        for (Map.Entry<E, Set<?>> entry : mapRange.entrySet())
            {
            colKeysToRetain.addAll(ensureSafeSet(entry.getValue()));
            }

        if (colKeysToRetain.isEmpty())
            {
            setKeys.clear();
            }
        else
            {
            setKeys.retainAll(colKeysToRetain);
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
    protected Integer allOrNothing(MapIndex index, NavigableMap<E, Set<?>> mapContents, Set setKeys)
        {
        if (!index.isPartial())
            {
            // optimize for corner cases when either all or none of the values match
            Map.Entry<E, Set<?>> loEntry = mapContents.firstEntry();
            Map.Entry<E, Set<?>> hiEntry = mapContents.lastEntry();
            if (loEntry == null || hiEntry == null)
                {
                // the map is empty, remove all keys
                return 0;
                }

            E loValue = loEntry.getKey();
            E hiValue = hiEntry.getKey();
            if (evaluateExtracted(loValue) && evaluateExtracted(hiValue))
                {
                // all entries match, nothing to remove
                return setKeys.size();
                }
            else if (!getLowerFilter().evaluateExtracted(hiValue) || (loValue != null && !getUpperFilter().evaluateExtracted(loValue)))
                {
                // no entries match, remove all keys
                return 0;
                }
            }

        return null;
        }
    }
