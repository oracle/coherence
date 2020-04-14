/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.filter;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapTrigger;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* A generic Filter-based MapTrigger implementation. If an evaluation of the
* {@link com.tangosol.util.MapTrigger.Entry Entry} object representing a pending
* change fails (returns false), then one of the following actions is taken:
* <ul>
*   <li> ACTION_ROLLBACK - an IllegalArgumentException is thrown by the trigger
*        to reject the operation that would result in this change (default);
*   <li> ACTION_IGNORE - the change is ignored and the Entry's value is synthetically
*        reset to the original value returned by the {@link
*        com.tangosol.util.MapTrigger.Entry#getOriginalValue()
*        Entry.getOriginalValue()} method;
*   <li> ACTION_IGNORE_LOGICAL - same as ACTION_IGNORE except a non-synthetic
*        change is made;
*   <li> ACTION_REMOVE - the entry is synthetically removed from the underlying
*        backing map using the {@link com.tangosol.util.InvocableMap.Entry#remove(boolean)
*        Entry.remove(true)} call;
*   <li> ACTION_REMOVE_LOGICAL - same as ACTION_REMOVE except a non-synthetic
*        remove is invoked using the {@link com.tangosol.util.InvocableMap.Entry#remove(boolean)
*        Entry.remove(false)} call.
* </ul>
*
* Note: This trigger never prevents entries from being removed.
*
* @author gg 2008.03.11
* @since Coherence 3.4
*/
public class FilterTrigger
        extends ExternalizableHelper
        implements MapTrigger, ExternalizableLite, PortableObject
    {
    // ----- constructors -----------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public FilterTrigger()
        {
        }

    /**
    * Construct a FilterTrigger based on the specified Filter object and
    * ACTION_ROLLBACK.
    *
    * @param <T>     the type of the input argument to the filter
    * @param filter  the underlying Filter
    */
    public <T> FilterTrigger(Filter<T> filter)
        {
        this(filter, ACTION_ROLLBACK);
        }

    /**
    * Construct a FilterTrigger based on the specified Filter object and the
    * action constant.
    *
    * @param <T>      the type of the input argument to the filter
    * @param filter   the underlying Filter
    * @param nAction  one of the ACTION_* constants
    */
    public <T> FilterTrigger(Filter<T> filter, int nAction)
        {
        azzert(filter != null, "Null filter");
        m_filter  = filter;
        m_nAction = nAction;
        }


    // ----- MapTrigger interface -------------------------------------------

   /**
   * {@inheritDoc}
   */
   public void process(MapTrigger.Entry entry)
        {
        if (entry.isPresent()
            && !InvocableMapHelper.evaluateEntry(m_filter, entry))
            {
            int     nAction    = m_nAction;
            boolean fSynthetic = nAction == ACTION_IGNORE ||
                                 nAction == ACTION_REMOVE;
            switch (m_nAction)
                {
                case ACTION_ROLLBACK:
                default:
                    throw new IllegalArgumentException("Rejecting " + entry +
                        " by trigger " + this);

                case ACTION_IGNORE:
                case ACTION_IGNORE_LOGICAL:
                    Object oValue = entry.getOriginalValue();
                    if (oValue != null || entry.isOriginalPresent())
                        {
                        entry.setValue(oValue, fSynthetic);
                        }
                    else
                        {
                        entry.remove(fSynthetic);
                        }
                    break;

                case ACTION_REMOVE:
                case ACTION_REMOVE_LOGICAL:
                    entry.remove(fSynthetic);
                    break;
                }
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying Filter.
    *
    * @return the underlying Filter object
    */
    public Filter getFilter()
        {
        return m_filter;
        }

    /**
    * Obtain the action code for this FilterTrigger.
    *
    * @return one of the ACTION_* constants
    */
    public int getAction()
        {
        return m_nAction;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_filter  = (Filter) readObject(in);
        m_nAction = readInt(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_filter);
        writeInt(out, m_nAction);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_filter  = (Filter) in.readObject(0);
        m_nAction = in.readInt(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_filter);
        out.writeInt(1, m_nAction);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the FilterTrigger with another object to determine
    * equality. Two FilterTrigger objects are considered equal iff the
    * wrapped filters and action codes are equal.
    *
    * @return true iff this FilterTrigger and the passed object are
    *         equivalent FilterTrigger objects
    */
    public boolean equals(Object o)
        {
        if (o instanceof FilterTrigger)
            {
            FilterTrigger that = (FilterTrigger) o;
            return equals(this.m_filter, that.m_filter)
                && this.m_nAction == that.m_nAction;
            }

        return false;
        }

    /**
    * Determine a hash value for the FilterTrigger object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this FilterTrigger object
    */
    public int hashCode()
        {
        return hashCode(m_filter);
        }

    /**
    * Return a human-readable description for this FilterTrigger.
    *
    * @return a String description of the FilterTrigger
    */
    public String toString()
        {
        String sClass  = getClass().getName();
        int    nAction = m_nAction;
        String sAction;
        switch (nAction)
            {
            default:
            case ACTION_ROLLBACK:
                sAction = "ACTION_ROLLBACK";
                break;

            case ACTION_IGNORE:
                 ACTION_IGNORE_LOGICAL:
                sAction = "ACTION_IGNORE";
                break;

            case ACTION_REMOVE:
                 ACTION_REMOVE_LOGICAL:
                sAction = "ACTION_REMOVE";
                break;
            }

        if (nAction == ACTION_IGNORE_LOGICAL ||
            nAction == ACTION_REMOVE_LOGICAL)
            {
            sAction += "(LOGICAL)";
            }

        return sClass.substring(sClass.lastIndexOf('.') + 1) +
            '(' + m_filter + ", " + sAction + ')';
        }


    // ----- constants and  data members ------------------------------------

    /**
    * Evaluation failure results in an IllegalArgumentException thrown by the
    * trigger.
    */
    public static final int ACTION_ROLLBACK = 0;

    /**
    * Evaluation failure results in restoring the original Entry's value.
    */
    public static final int ACTION_IGNORE = 1;

    /**
    * Evaluation failure results in a removal of the entry.
    */
    public static final int ACTION_REMOVE = 2;

    /**
    * Evaluation failure results in restoring the original Entry's value using
    * the non-synthetic API.
    */
    public static final int ACTION_IGNORE_LOGICAL = 3;

    /**
    * Evaluation failure results in a non-synthetic removal of the entry.
    */
    public static final int ACTION_REMOVE_LOGICAL = 4;

    /**
    * The underlying filter.
    */
    @JsonbProperty("filter")
    protected Filter m_filter;

    /**
    * The action code.
    */
    @JsonbProperty("action")
    protected int m_nAction;
    }
