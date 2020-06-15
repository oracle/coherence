/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
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


    // ----- ExtractorFilter methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected boolean evaluateExtracted(E extracted)
        {
        return ((Collection) getValue()).contains(extracted);
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
            int        cValues   = colValues.size();
            int        cKeys     = setKeys.size();

            // an empirically chosen factor that suits 90% of the data sets
            // tested; the aim is to accommodate for common use cases in which
            // colValues will contain between 10 - 1000 elements, thus iterating
            // 128000 keys (upper bound) is considered the inflection point and
            // as setKeys increases past this point, the value of collecting the
            // inverse keys becomes more beneficial
            final int FACTOR = 128;

            // optimized branch for relatively small number of keys
            use_fwd_index:
            if (cKeys < Math.min(1000, cValues) * FACTOR)
                {
                for (Iterator iter = setKeys.iterator(); iter.hasNext(); )
                    {
                    Object oValue = index.get(iter.next());
                    if (oValue == MapIndex.NO_VALUE)
                        {
                        // forward index is not supported
                        break use_fwd_index;
                        }

                    if (!colValues.contains(oValue))
                        {
                        iter.remove();
                        }
                    }
                return null;
                }

            List listInverseKeys = new ArrayList(colValues.size());
            for (Iterator iter = colValues.iterator(); iter.hasNext(); )
                {
                Object oValue = iter.next();
                Set    setEQ  = (Set) index.getIndexContents().get(oValue);

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
                setKeys.retainAll(new ChainedCollection(listInverseKeys));
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