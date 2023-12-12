/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
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
* a method invocation for containment of all values in a Set.
* <p>
* More formally, if the specified extractor returns a Collection,
* {@link #evaluate evaluate(o)} is functionally equivalent to the following
* code:
* <pre>
* return ((Collection) extract(o)).containsAll((Collection) getValue());
* </pre>
* If the specified method returns an Object array, {@link #evaluate
* evaluate(o)} is functionally equivalent to the following code:
* <pre>
* return Collections.asList((Object[]) extract(o)).containsAll((Collection) getValue());
* </pre>
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the extracted attribute to use for comparison
*
* @author jh 2005.06.08
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class ContainsAllFilter<T, E>
        extends    ComparisonFilter<T, E, Set<?>>
        implements IndexAwareFilter<Object, T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ContainsAllFilter()
        {
        }

    /**
    * Construct an ContainsAllFilter for testing containment of the given Set
    * of values.
    *
    * @param extractor  the ValueExtractor used by this filter
    * @param setValues  the Set of values that a Collection or Object array
    *                   is tested to contain
    */
    @JsonbCreator
    public ContainsAllFilter(@JsonbProperty("extractor")
                                     ValueExtractor<? super T, ? extends E> extractor,
                             @JsonbProperty("value") Set<?> setValues)
        {
        super(extractor, new HashSet<>(setValues));
        }

    /**
    * Construct an ContainsAllFilter for testing containment of the given Set
    * of values.
    *
    * @param sMethod    the name of the method to invoke via reflection
    * @param setValues  the Set of values that a Collection or Object array
    *                   is tested to contain
    */
    public ContainsAllFilter(String sMethod, Set<?> setValues)
        {
        super(sMethod, new HashSet<>(setValues));
        }

    // ----- Filter interface -----------------------------------------------

    protected String getOperator()
        {
        return "CONTAINS ALL";
        }

    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        Collection collValues = (Collection) getValue();

        if (extracted instanceof Collection)
            {
            return ((Collection) extracted).containsAll(collValues);
            }
        else if (extracted instanceof Object[])
            {
            Object[] aoExtracted = (Object[]) extracted;
            int      cExtracted  = aoExtracted.length;

            Values: for (Iterator iter = collValues.iterator(); iter.hasNext();)
                {
                Object oValue = iter.next();
                for (int i = 0; i < cExtracted; ++i)
                    {
                    if (equals(aoExtracted[i], oValue))
                        {
                        continue Values;
                        }
                    }

                return false;
                }

            return true;
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
        if (index == null)
            {
            // there is no relevant index
            return -1;
            }
        else
            {
            Collection colValues     = getValue();
            Map        mapContents   = index.getIndexContents();
            int        cKeysContents = mapContents.keySet().size();

            if (cKeysContents == 0 || !mapContents.keySet().containsAll(colValues))
                {
                return 0;
                }

            // calculating the exact number of keys retained is too expensive;
            // assume normal distribution of keys across index and return
            // an estimate based on the number of values in a collection
            return setKeys.size() * (colValues.size() / cKeysContents);
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
            Collection colValues   = getValue();
            Map        mapContents = index.getIndexContents();

            for (Object oValue : colValues)
                {
                Set setEQ = (Set) mapContents.get(oValue);

                if (setEQ == null)
                    {
                    setKeys.clear();
                    break;
                    }
                else
                    {
                    setKeys.retainAll(setEQ);
                    if (setKeys.isEmpty())
                        {
                        break;
                        }
                    }
                }
            return null;
            }
        }
    }
