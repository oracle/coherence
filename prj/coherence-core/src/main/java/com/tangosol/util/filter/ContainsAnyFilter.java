/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;


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


    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        Collection colValues = (Collection) getValue();

        if (extracted instanceof Collection)
            {
            Collection colExtracted = (Collection) extracted;
            for (Iterator iter = colValues.iterator(); iter.hasNext();)
                {
                if (colExtracted.contains(iter.next()))
                    {
                    return true;
                    }
                }
            }
        else if (extracted instanceof Object[])
            {
            Object[] aoExtracted = (Object[]) extracted;
            int      cExtracted  = aoExtracted.length;

            for (Iterator iter = colValues.iterator(); iter.hasNext();)
                {
                Object oValue = iter.next();
                for (int i = 0; i < cExtracted; ++i)
                    {
                    if (equals(aoExtracted[i], oValue))
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
        return Base.truncateString((Collection) getValue(), 255);
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        return index == null ? calculateIteratorEffectiveness(setKeys.size())
                             : ((Collection) getValue()).size();
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
            Collection colValues = (Collection) getValue();
            Set        setIn     = new HashSet();
            for (Iterator iter = colValues.iterator(); iter.hasNext();)
                {
                Object oValue = iter.next();
                Set    setEQ  = (Set) index.getIndexContents().get(oValue);

                if (setEQ != null)
                    {
                    setIn.addAll(setEQ);
                    }
                }

            if (setIn.isEmpty())
                {
                setKeys.clear();
                }
            else
                {
                setKeys.retainAll(setIn);
                }
            return null;
            }
        }
    }
