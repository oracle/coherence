/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Base;
import com.tangosol.util.ChainedCollection;
import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;


/**
* Filter which tests a {@link Collection} or Object array value returned from
* a method invocation for containment of any value in a Set.
* <p>
* More formally, if the specified extractor returns a Collection,
* {@link #evaluate evaluate(o)} is functionally equivalent to the following
* code:
* <pre>
* return ((Collection) extract(o)).removeAll((Collection) getValue());
* </pre>
* If the specified method returns an Object array, {@link #evaluate
* evaluate(o)} is functionally equivalent to the following code:
* <pre>
* return Collections.asList((Object[]) extract(o)).removeAll((Collection) getValue());
* </pre>
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the extracted attribute to use for comparison
*
* @author jh 2005.06.08
*/
@SuppressWarnings({"rawtypes", "unchecked"})
public class ContainsAnyFilter<T, E>
        extends    ComparisonFilter<T, E, Set<?>>
        implements IndexAwareFilter<Object, T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ContainsAnyFilter()
        {
        }

    /**
    * Construct an ContainsAnyFilter for testing containment of any value in
    * the given Set.
    *
    * @param extractor  the ValueExtractor used by this filter
    * @param setValues  the Set of values that a Collection or Object array
    *                   is tested to contain
    */
    @JsonbCreator
    public ContainsAnyFilter(@JsonbProperty("extractor")
                                         ValueExtractor<? super T, ? extends E> extractor,
                             @JsonbProperty("value")
                                         Set<?> setValues)
        {
        super(extractor, new HashSet<>(setValues));
        }

    /**
    * Construct an ContainsAnyFilter for testing containment of any value in
    * the given Set.
    *
    * @param sMethod    the name of the method to invoke via reflection
    * @param setValues  the Set of values that a Collection or Object array
    *                   is tested to contain
    */
    public ContainsAnyFilter(String sMethod, Set<?> setValues)
        {
        super(sMethod, new HashSet<>(setValues));
        }

    // ----- Filter interface -----------------------------------------------

    protected String getOperator()
        {
        return "CONTAINS ANY";
        }

    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        Collection colValues = getValue();

        if (extracted instanceof Collection)
            {
            Collection colExtracted = (Collection) extracted;
            for (Object oValue : colValues)
                {
                if (colExtracted.contains(oValue))
                    {
                    return true;
                    }
                }
            }
        else if (extracted instanceof Object[])
            {
            Object[] aoExtracted = (Object[]) extracted;

            for (Object oValue : colValues)
                {
                for (Object oExtracted : aoExtracted)
                    {
                    if (equals(oExtracted, oValue))
                        {
                        return true;
                        }
                    }
                }
            }

        return false;
        }


    // ----- ComparisonFilter methods ---------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public String toStringValue()
        {
        return Base.truncateString(getValue(), 255);
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
            // calculating the exact number of keys retained is too expensive;
            // ignore the fact that there may be duplicates and simply return
            // the worst possible number of keys retained, as if they were unique

            Collection colValues   = getValue();
            Map        mapContents = index.getIndexContents();
            int        cMatch      = 0;

            for (Object oValue : colValues)
                {
                Set setEQ = (Set) mapContents.get(oValue);

                if (setEQ != null)
                    {
                    cMatch += setEQ.size();
                    }
                }
            
            return Math.min(cMatch, setKeys.size());
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
            Collection   colValues       = getValue();
            Map          mapContents     = index.getIndexContents();
            List<Set<?>> listInverseKeys = new ArrayList<>(colValues.size());
            for (Object oValue : colValues)
                {
                Set setEQ = (Set) mapContents.get(oValue);

                if (setEQ != null)
                    {
                    listInverseKeys.add(setEQ);
                    }
                }

            if (listInverseKeys.isEmpty())
                {
                setKeys.clear();
                }
            else
                {
                setKeys.retainAll(new ChainedCollection<>(listInverseKeys.toArray(Set[]::new)));
                }
            return null;
            }
        }
    }
