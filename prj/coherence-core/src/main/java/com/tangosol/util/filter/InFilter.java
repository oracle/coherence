/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.Base;
import com.tangosol.util.ChainedCollection;
import com.tangosol.util.Filter;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
* Filter which checks whether the result of a method invocation belongs to a
* predefined set of values.
*
* @author cp/gg/hr 2002.11.08
*/
@SuppressWarnings({"unchecked", "rawtypes"})
public class InFilter<T, E>
        extends    ComparisonFilter<T, E, Set<? extends E>>
        implements IndexAwareFilter<Object, T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public InFilter()
        {
        }

    /**
    * Construct an InFilter for testing "In" condition.
    *
    * @param extractor  the ValueExtractor to use by this filter
    * @param setValues  the set of values to compare the result with
    */
    public InFilter(ValueExtractor<? super T, ? extends E> extractor, Set<? extends E> setValues)
        {
        super(extractor, new HashSet<>(setValues));
        }

    /**
    * Construct an InFilter for testing "In" condition.
    *
    * @param sMethod    the name of the method to invoke via reflection
    * @param setValues  the set of values to compare the result with
    */
    public InFilter(String sMethod, Set<? extends E> setValues)
        {
        super(sMethod, new HashSet<>(setValues));
        }

    // ----- Filter interface -----------------------------------------------

    protected String getOperator()
        {
        return "IN";
        }

    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        return getValue().contains(extracted);
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

            Set<? extends E> colValues   = getValue();
            Map<E, Set<?>>   mapContents = index.getIndexContents();
            int              cMatch      = 0;

            for (E value : colValues)
                {
                Set<?> setEQ = mapContents.get(value);

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
            // there is no relevant index; evaluate individual entries
            return this;
            }
        else if (index.getIndexContents().isEmpty())
            {
            // there are no entries in the index, which means no entries match this filter
            setKeys.clear();
            return null;
            }
        else
            {
            Map<E, Set<?>>   mapContents = index.getIndexContents();
            Set<? extends E> colValues   = getValue();

            List<Set<?>> listInverseKeys = new ArrayList<>(colValues.size());
            for (E value : colValues)
                {
                Set<?> setEQ = mapContents.get(value);

                if (setEQ != null && !setEQ.isEmpty())
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

    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        // Note: intentionally overridden to not delegate to the super in order
        //       to specialize the serialization of the value
        m_extractor = in.readObject(0);
        m_value = in.readCollection(1, new HashSet<>());
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        // Note: intentionally overridden to not delegate to the super in order
        //       to specialize the serialization of the value
        out.writeObject(0, m_extractor);
        out.writeCollection(1, (Collection) m_value);
        }
    }
