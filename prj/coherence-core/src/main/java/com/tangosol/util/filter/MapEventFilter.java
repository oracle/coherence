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

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.ObservableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* Filter which evaluates the content of a MapEvent object according to the
* specified criteria.  This filter is intended to be used by various
* {@link ObservableMap} listeners that are interested in particular subsets
* of MapEvent notifications emitted by the map.
* <p>
* Usage examples:
* <ul>
* <li>a filter that evaluates to true if an Employee object is inserted into
*     a cache with a value of Married property set to true.
* <pre>
*   new MapEventFilter(MapEventFilter.E_INSERT,
*       new EqualsFilter("isMarried", Boolean.TRUE));
* </pre>
* <li>a filter that evaluates to true if any object is removed from a cache.
* <pre>
*   new MapEventFilter(MapEventFilter.E_DELETED);
* </pre>
* <li>a filter that evaluates to true if there is an update to an Employee
*     object where either an old or new value of LastName property equals to
*     "Smith"
* <pre>
*   new MapEventFilter(MapEventFilter.E_UPDATED,
*       new EqualsFilter("LastName", "Smith"));
* </pre>
* <li>a filter that is used to keep a cached keySet result based on some map
*     filter up-to-date.
* <pre>
*   final Set    setKeys   = new HashSet();
*   final Filter filterEvt = new MapEventFilter(filterMap);
*   MapListener  listener  = new AbstractMapListener()
*       {
*       public void entryInserted(MapEvent evt)
*           {
*           setKeys.add(evt.getKey());
*           }
*
*       public void entryDeleted(MapEvent evt)
*           {
*           setKeys.remove(evt.getKey());
*           }
*       };
*
*   map.addMapListener(listener, filterEvt, true);
*   setKeys.addAll(map.keySet(filterMap));
* </pre>
* </ul>
*
* @see ValueChangeEventFilter
*
* @author gg 2003.09.22
* @since Coherence 2.3
*/
public class MapEventFilter<K, V>
        extends    ExternalizableHelper
        implements Filter<MapEvent<K, V>>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public MapEventFilter()
        {
        }

    /**
    * Construct a MapEventFilter that evaluates MapEvent objects based on the
    * specified combination of event types.
    * <p>
    * Using this constructor is equivalent to:
    * <tt>
    * new MapEventFilter(nMask, null);
    * </tt>
    *
    * @param nMask  any combination of E_INSERTED, E_UPDATED and E_DELETED,
    *               E_UPDATED_ENTERED, E_UPDATED_WITHIN, E_UPDATED_LEFT
    *
    * @since Coherence 3.1
    */
    public MapEventFilter(int nMask)
        {
        this(nMask, null);
        }

    /**
    * Construct a MapEventFilter that evaluates MapEvent objects that would
    * affect the results of a keySet filter issued by a previous call to
    * {@link com.tangosol.util.QueryMap#keySet(com.tangosol.util.Filter)}. It
    * is possible to easily implement <i>continuous query</i> functionality.
    * <p>
    * Using this constructor is equivalent to:
    * <tt>
    * new MapEventFilter(E_KEYSET, filter);
    * </tt>
    *
    * @param filter  the filter passed previously to a keySet() query method
    *
    * @since Coherence 3.1
    */
    public MapEventFilter(Filter<V> filter)
        {
        this(E_KEYSET, filter);
        }

    /**
    * Construct a MapEventFilter that evaluates MapEvent objects
    * based on the specified combination of event types.
    *
    * @param nMask   combination of any of the E_* values
    * @param filter  (optional) the filter used for evaluating event values
    */
    public MapEventFilter(int nMask, Filter<V> filter)
        {
        if ((nMask & (E_ALL | E_KEYSET | E_UPDATED_WITHIN)) == 0)
            {
            throw new IllegalArgumentException("At least one E_* type must be specified");
            }

        m_nMask  = nMask;
        m_filter = filter;
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(MapEvent<K, V> event)
        {
        // check if the event is of a type ("id") that the client is
        // interested in evaluating
        int nId   = event.getId();
        int nMask = getEventMask();
        try
            {
            if ((MASK[nId] & nMask) == 0)
                {
                return false;
                }
            }
        catch (IndexOutOfBoundsException e)
            {
            return false;
            }

        // check for a client-specified event filter
        Filter<V> filter = getFilter();
        if (filter == null)
            {
            return true;
            }

        // evaluate the filter
        switch (nId)
            {
            case MapEvent.ENTRY_INSERTED:
                return InvocableMapHelper.evaluateEntry(filter, event.getNewEntry());

            case MapEvent.ENTRY_UPDATED:
                // note that the old value evaluation is deferred, because
                // the event itself may be deferring loading the old value,
                // e.g. if the event is coming from a disk-backed cache
                boolean fNew = InvocableMapHelper.evaluateEntry(filter, event.getNewEntry());

                switch (nMask & (E_UPDATED_ENTERED | E_UPDATED_LEFT |
                                 E_UPDATED | E_UPDATED_WITHIN))
                    {
                    case E_UPDATED_ENTERED:
                        return fNew && !InvocableMapHelper.evaluateEntry(filter, event.getOldEntry());

                    case E_UPDATED_LEFT:
                        return !fNew && InvocableMapHelper.evaluateEntry(filter, event.getOldEntry());

                    case E_UPDATED_ENTERED | E_UPDATED_LEFT:
                        return fNew != InvocableMapHelper.evaluateEntry(filter, event.getOldEntry());

                    case E_UPDATED_WITHIN:
                        return fNew && InvocableMapHelper.evaluateEntry(filter, event.getOldEntry());

                    case E_UPDATED_WITHIN | E_UPDATED_ENTERED:
                        return fNew;

                    case E_UPDATED_WITHIN | E_UPDATED_LEFT:
                        return InvocableMapHelper.evaluateEntry(filter, event.getOldEntry());

                    default:
                        // all other combinations evaluate to the same as
                        // E_UPDATED
                        return fNew || InvocableMapHelper.evaluateEntry(filter, event.getOldEntry());
                    }

            case MapEvent.ENTRY_DELETED:
                return InvocableMapHelper.evaluateEntry(filter, event.getOldEntry());

            default:
                return false;
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the event mask. The mask value is concatenation of any of the
    * E_* values.
    *
    * @return the event mask
    */
    public int getEventMask()
        {
        return m_nMask;
        }

    /**
    * Obtain the Filter object used to evaluate the event value(s).
    *
    * @return the filter used to evaluate the event value(s)
    */
    public Filter<V> getFilter()
        {
        return m_filter;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the MapEventFilter with another object to determine equality.
    *
    * @return true iff this MapEventFilter and the passed object are
    *         equivalent filters
    */
    public boolean equals(Object o)
        {
        if (o != null && o.getClass() == this.getClass())
            {
            MapEventFilter that = (MapEventFilter) o;
            return         this.m_nMask == that.m_nMask
                 && equals(this.m_filter,  that.m_filter);
            }

        return false;
        }

    /**
    * Determine a hash value for the MapEventFilter object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this MapEventFilter object
    */
    public int hashCode()
        {
        int    nHash  = m_nMask;
        Filter filter = m_filter;
        if (filter != null)
            {
            nHash += filter.hashCode();
            }
        return nHash;
        }

    /**
     * Get the filter's description.
     *
     * @return this filter's description
     */
    protected String getDescription()
        {
        StringBuilder sb = new StringBuilder("mask=");
        int           nMask = getEventMask();

        if (nMask == E_ALL)
            {
            sb.append("ALL");
            }
        else if (nMask == E_KEYSET)
            {
            sb.append("KEYSET");
            }
        else
            {
            if ((nMask & E_INSERTED) != 0)
                {
                sb.append("INSERTED|");
                }
            if ((nMask & E_UPDATED) != 0)
                {
                sb.append("UPDATED|");
                }
            if ((nMask & E_DELETED) != 0)
                {
                sb.append("DELETED|");
                }
            if ((nMask & E_UPDATED_ENTERED) != 0)
                {
                sb.append("UPDATED_ENTERED|");
                }
            if ((nMask & E_UPDATED_LEFT) != 0)
                {
                sb.append("UPDATED_LEFT|");
                }
            if ((nMask & E_UPDATED_WITHIN) != 0)
                {
                sb.append("UPDATED_WITHIN|");
                }
            sb.setLength(sb.length() - 1);
            }

        Filter<V> filter = getFilter();
        if (filter != null)
            {
            sb.append(", filter=")
              .append(filter);
            }

        return sb.toString();
        }

    /**
    * Return a human-readable description for this Filter.
    *
    * @return a String description of the Filter
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + '(' + getDescription() + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_nMask  = readInt(in);
        m_filter = (Filter<V>) readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeInt(out, m_nMask);
        writeObject(out, m_filter);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_nMask  = in.readInt(0);
        m_filter = (Filter<V>) in.readObject(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_nMask);
        out.writeObject(1, m_filter);
        }


    // ----- constants ------------------------------------------------------

    /**
    * This value indicates that {@link MapEvent#ENTRY_INSERTED
    * ENTRY_INSERTED} events should be evaluated. The event will be fired if
    * there is no filter specified or the filter evaluates to true for a new
    * value.
    */
    public static final int E_INSERTED       = 0x0001;

    /**
    * This value indicates that {@link MapEvent#ENTRY_UPDATED ENTRY_UPDATED}
    * events should be evaluated. The event will be fired if there is no
    * filter specified or the filter evaluates to true when applied to either
    * old or new value.
    */
    public static final int E_UPDATED        = 0x0002;

    /**
    * This value indicates that {@link MapEvent#ENTRY_DELETED ENTRY_DELETED}
    * events should be evaluated. The event will be fired if there is no
    * filter specified or the filter evaluates to true for an old value.
    */
    public static final int E_DELETED        = 0x0004;

    /**
    * This value indicates that {@link MapEvent#ENTRY_UPDATED ENTRY_UPDATED}
    * events should be evaluated, but only if filter evaluation is false for
    * the old value and true for the new value. This corresponds to an item
    * that was not in a keySet filter result changing such that it would now
    * be in that keySet filter result.
    *
    * @since Coherence 3.1
    */
    public static final int E_UPDATED_ENTERED = 0x0008;

    /**
    * This value indicates that {@link MapEvent#ENTRY_UPDATED ENTRY_UPDATED}
    * events should be evaluated, but only if filter evaluation is true for
    * the old value and false for the new value. This corresponds to an item
    * that was in a keySet filter result changing such that it would no
    * longer be in that keySet filter result.
    *
    * @since Coherence 3.1
    */
    public static final int E_UPDATED_LEFT   = 0x0010;

    /**
    * This value indicates that {@link MapEvent#ENTRY_UPDATED ENTRY_UPDATED}
    * events should be evaluated, but only if filter evaluation is true for
    * both the old and the new value. This corresponds to an item
    * that was in a keySet filter result changing but not leaving the keySet
    * filter result.
    *
    * @since Coherence 3.1
    */
    public static final int E_UPDATED_WITHIN = 0x0020;

    /**
    * This value indicates that all events should be evaluated.
    */
    public static final int E_ALL            = E_INSERTED | E_UPDATED |
                                               E_DELETED;

    /**
    * This value indicates that all events that would affect the result of
    * a {@link com.tangosol.util.QueryMap#keySet(com.tangosol.util.Filter)}
    * query should be evaluated.
    *
    * @since Coherence 3.1
    */
    public static final int E_KEYSET         = E_INSERTED | E_DELETED |
                                               E_UPDATED_ENTERED  | E_UPDATED_LEFT;

    /**
    * Event id to event mask translation array.
    */
    private static final int[] MASK;
    static
        {
        MASK = new int[4];
        MASK[MapEvent.ENTRY_INSERTED /*1*/] = E_INSERTED;
        MASK[MapEvent.ENTRY_UPDATED  /*2*/] = E_UPDATED | E_UPDATED_WITHIN |
                                              E_UPDATED_ENTERED | E_UPDATED_LEFT;
        MASK[MapEvent.ENTRY_DELETED  /*3*/] = E_DELETED;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The event mask.
    */
    @JsonbProperty("mask")
    protected int m_nMask;

    /**
    * The event value(s) filter.
    */
    @JsonbProperty("filter")
    protected Filter<V> m_filter;
    }
