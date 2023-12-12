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
* Filter which always evaluates to false.
*
* @author gg 2003.09.18
*/
public class NeverFilter<T>
        extends    AbstractQueryRecorderFilter<T>
        implements EntryFilter<Object, T>, IndexAwareFilter<Object, T>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a NeverFilter.
    */
    public NeverFilter()
        {
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(T o)
        {
        return false;
        }

    public String toExpression()
        {
        return "FALSE";
        }

    // ----- EntryFilter interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry entry)
        {
        return false;
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        return 0;
        }

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        setKeys.clear();
        return null;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the NeverFilter with another object to determine equality.
    *
    * @return true iff this NeverFilter and the passed object are
    *         equivalent NeverFilters
    */
    public boolean equals(Object o)
        {
        return o instanceof NeverFilter;
        }

    /**
    * Determine a hash value for the NeverFilter object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this NeverFilter object
    */
    public int hashCode()
        {
        return 0xEF;
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        return "NeverFilter";
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


    // ---- constants --------------------------------------------------------

    /**
    * An instance of the NeverFilter.
    */
    public static final NeverFilter INSTANCE = new NeverFilter();

    /**
    * Return an instance of the NeverFilter.
    *
    * @param <T>  the type of the input argument to the filter
    *
    * @return a NeverFilter instance
    */
    public static <T> NeverFilter<T> INSTANCE()
        {
        return INSTANCE;
        }
    }
