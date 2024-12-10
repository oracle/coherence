/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Filter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;
import java.util.Set;


/**
* Filter which always evaluates to true.
*
* @param <T> the type of the input argument to the filter
*
* @author gg 2003.09.18
*/
public class AlwaysFilter<T>
        extends    AbstractQueryRecorderFilter<T>
        implements EntryFilter<Object, T>, IndexAwareFilter<Object, T>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an AlwaysFilter.
    */
    public AlwaysFilter()
        {
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(T o)
        {
        return true;
        }

    public String toExpression()
        {
        return "TRUE";
        }

    // ----- EntryFilter interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry entry)
        {
        return true;
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        return setKeys.size();
        }

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        return null;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the AlwaysFilter with another object to determine equality.
    *
    * @return true iff this AlwaysFilter and the passed object are
    *         equivalent AlwaysFilters
    */
    public boolean equals(Object o)
        {
        return o instanceof AlwaysFilter;
        }

    /**
    * Determine a hash value for the AlwaysFilter object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this AlwaysFilter object
    */
    public int hashCode()
        {
        return 0xAF;
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        return "AlwaysFilter";
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        }


    // ----- constants ------------------------------------------------------

    /**
    * An instance of the AlwaysFilter.
    */
    public static final AlwaysFilter INSTANCE = new AlwaysFilter();

    /**
    * Return an instance of the AlwaysFilter.
    * 
    * @param <T>  the type of the input argument to the filter
    *
    * @return a AlwaysFilter instance
    */
    public static <T> AlwaysFilter<T> INSTANCE()
        {
        return INSTANCE;
        }
    }
