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

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;
import java.util.Set;


/**
* Filter which returns true for {@link com.tangosol.util.InvocableMap.Entry}
* objects that currently exist in a Map.
* <p>
* This Filter is intended to be used solely in combination with a
* {@link com.tangosol.util.processor.ConditionalProcessor} and is unnecessary
* for standard {@link com.tangosol.util.QueryMap} operations.
*
* @param <T> the type of the input argument to the filter
*
* @author jh  2005.12.16
*
* @see com.tangosol.util.InvocableMap.Entry#isPresent()
*/
public class PresentFilter<T>
        extends    ExternalizableHelper
        implements Filter<T>, EntryFilter<Object, T>, IndexAwareFilter<Object, T>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a PresentFilter.
    */
    public PresentFilter()
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
        return "IS PRESENT";
        }

    // ----- EntryFilter interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry entry)
        {
        return !(entry instanceof InvocableMap.Entry) ||
                ((InvocableMap.Entry) entry).isPresent();
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        // there will never be an index, and all the keys passed in setKeys
        // are present, by definition
        return setKeys.size();
        }

    /**
    * {@inheritDoc}
    */
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        // there will never be an index, and all the keys passed in setKeys
        // are present, by definition
        return null;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the PresentFilter with another object to determine equality.
    *
    * @return true iff this PresentFilter and the passed object are
    *         equivalent PresentFilters
    */
    public boolean equals(Object o)
        {
        return o instanceof PresentFilter;
        }

    /**
    * Determine a hash value for the PresentFilter object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this PresentFilter object
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
        return "PresentFilter";
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
    * An instance of the PresentFilter.
    */
    public static final PresentFilter INSTANCE = new PresentFilter();

    /**
    * Return an instance of the PresentFilter.
    *
    * @param <T>  the type of the input argument to the filter
    *
    * @return a PresentFilter instance
    */
    public static <T> PresentFilter<T> INSTANCE()
        {
        return INSTANCE;
        }
    }
