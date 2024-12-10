/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;


/**
* Filter which discards null references.
*
* @version 1.00 08/17/98
* @author Cameron Purdy
*/
public class NullFilter
        extends Base
        implements Filter, Serializable, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (for ExternalizableLite and PortableObject).
    */
    public NullFilter()
        {
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * Filter interface:  evaluate().
    */
    public boolean evaluate(Object o)
        {
        return o != null;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the NullFilter with another object to determine equality.
    *
    * @return true iff this NullFilter and the passed object are
    *         equivalent NullFilters
    */
    public boolean equals(Object o)
        {
        return o instanceof NullFilter;
        }

    /**
    * Determine a hash value for the NullFilter object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this NullFilter object
    */
    public int hashCode()
        {
        return 0x0F;
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        return "NullFilter";
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


    // ----- Singleton implementation ---------------------------------------

    /**
    * Returns an instance of the null enumerator.
    *
    * @return an Enumeration instance with no values to enumerate.
    */
    public static final NullFilter getInstance()
        {
        return FILTER;
        }


    // ----- constants ------------------------------------------------------

    /**
    * Singleton.
    */
    private static final NullFilter FILTER = new NullFilter();
    }