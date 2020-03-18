/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;


/**
* Filter which compares the result of a method invocation with a value.
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the extracted attribute to use for comparison
* @param <C> the type of value to compare extracted attribute with
*
* @author cp/gg 2002.10.27
*/
public abstract class ComparisonFilter<T, E, C>
        extends ExtractorFilter<T, E>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ComparisonFilter()
        {
        }

    /**
    * Construct a ComparisonFilter.
    *
    * @param extractor  the ComparisonFilter to use by this filter
    * @param value      the object to compare the result with
    */
    public ComparisonFilter(ValueExtractor<? super T, ? extends E> extractor, C value)
        {
        super(extractor);
        m_value = value;
        }

    /**
    * Construct a ComparisonFilter.
    *
    * @param sMethod  the name of the method to invoke via reflection
    * @param value    the object to compare the result with
    */
    public ComparisonFilter(String sMethod, C value)
        {
        super(sMethod);
        m_value = value;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the object to compare the reflection result with.
    *
    * @return the object to compare the reflection result with
    */
    public C getValue()
        {
        return m_value;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Helper method to calculate effectiveness for ComparisonFilters that need
    * no more than a single index match in order to retrieve all necessary
    * keys to perform the applyIndex() operation.
    * Such filters are: Contains, Equals, NotEquals.
    *
    * @param mapIndexes  the available MapIndex objects keyed by the related
    *                    ValueExtractor; read-only
    * @param setKeys     the set of keys that will be filtered; read-only
    *
    * @return an effectiveness estimate of how well this filter can use the
    *         specified indexes to filter the specified keys
    */
    protected int calculateMatchEffectiveness(Map mapIndexes, Set setKeys)
        {
        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        return index == null ? calculateIteratorEffectiveness(setKeys.size()) : 1;
        }

    /**
    * Helper method to calculate effectiveness for ComparisonFilters that need
    * a range of values from an index in order to retrieve all necessary
    * keys to perform the applyIndex() operation.
    * Such filters are: Less, LessEquals, Greater, GreaterEquals.
    *
    * @param mapIndexes  the available MapIndex objects keyed by the related
    *                    ValueExtractor; read-only
    * @param setKeys     the set of keys that will be filtered; read-only
    *
    * @return an effectiveness estimate of how well this filter can use the
    *         specified indexes to filter the specified keys
    */
    protected int calculateRangeEffectiveness(Map mapIndexes, Set setKeys)
        {
        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        if (index == null)
            {
            return calculateIteratorEffectiveness(setKeys.size());
            }
        else if (index.isOrdered())
            {
            // TODO we could be more precise if the position of the value
            // in the SortedMap could be quickly calculated
            return Math.max(index.getIndexContents().size() / 4, 1);
            }
        else
            {
            return index.getIndexContents().size();
            }
        }

    /**
    * Return the string representation of the value.
    *
    * @return the string representation of the value
    */
    protected String toStringValue()
        {
        return String.valueOf(getValue());
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ComparisonFilter with another object to determine equality.
    * Two ComparisonFilter objects are considered equal iff they belong to
    * exactly the same class and their extractor and value are equal.
    *
    * @return true iff this ComparisonFilter and the passed object are
    *         equivalent ComparisonFilters
    */
    public boolean equals(Object o)
        {
        if (o instanceof ComparisonFilter)
            {
            ComparisonFilter that = (ComparisonFilter) o;
            return this.getClass() ==       that.getClass()
                && equals(this.m_extractor, that.m_extractor)
                && equals(this.m_value,    that.m_value)
                ;
            }

        return false;
        }

    /**
    * Determine a hash value for the ComparisonFilter object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ComparisonFilter object
    */
    public int hashCode()
        {
        return hashCode(m_extractor) + hashCode(m_value);
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        String sClass = getClass().getName();

        return sClass.substring(sClass.lastIndexOf('.') + 1) +
            '(' + getValueExtractor() + ", " + toStringValue() + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);

        m_value = readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        writeObject(out, m_value);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);

        m_value = in.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeObject(1, m_value);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The value to compare to.
    */
    @JsonbProperty("value")
    protected C m_value;
    }
