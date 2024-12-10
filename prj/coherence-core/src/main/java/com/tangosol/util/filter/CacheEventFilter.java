/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.cache.CacheEvent;

import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An extension of the {@link MapEventFilter} which allows selection of client
 * driven (natural) events, cache internal (synthetic) events, or both.
 *
 * @param <K>  the type of the cache entry keys
 * @param <V>  the type of the cache entry values
 *
 * @author sw 2013.04.04
 * @since Coherence 3.7.1.9
 */
public class CacheEventFilter<K, V>
        extends MapEventFilter<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public CacheEventFilter()
        {
        }

    /**
     * Construct a CacheEventFilter that evaluates MapEvent objects based on the
     * specified combination of event types.
     * <p>
     * Using this constructor is equivalent to:
     * <tt>
     * new CacheEventFilter(nMask, null, nMaskSynthetic);
     * </tt>
     *
     * @param nMask           any combination of E_INSERTED, E_UPDATED and E_DELETED,
     *                        E_UPDATED_ENTERED, E_UPDATED_WITHIN, E_UPDATED_LEFT
     * @param nMaskSynthetic  any combination of E_SYNTHETIC and E_NATURAL
     */
    public CacheEventFilter(int nMask, int nMaskSynthetic)
        {
        this(nMask, null, nMaskSynthetic);
        }

    /**
     * Construct a CacheEventFilter that evaluates MapEvent objects that would
     * affect the results of a keySet filter issued by a previous call to
     * {@link com.tangosol.util.QueryMap#keySet(com.tangosol.util.Filter)}. It
     * is possible to easily implement <i>continuous query</i> functionality.
     * <p>
     * Using this constructor is equivalent to:
     * <tt>
     * new CacheEventFilter(E_KEYSET, filter, nMaskSynthetic);
     * </tt>
     *
     * @param filter          the filter passed previously to a keySet() query method
     * @param nMaskSynthetic  any combination of E_SYNTHETIC and E_NATURAL
     */
    public CacheEventFilter(Filter<V> filter, int nMaskSynthetic)
        {
        this(E_KEYSET, filter, nMaskSynthetic);
        }

    /**
     * Construct a CacheEventFilter that evaluates MapEvent objects
     * based on the specified combination of event types.
     *
     * @param nMask           combination of any of the E_* values
     * @param filter          (optional) the filter used for evaluating event values
     * @param nMaskSynthetic  any combination of E_SYNTHETIC and E_NATURAL
     */
    public CacheEventFilter(int nMask, Filter<V> filter, int nMaskSynthetic)
        {
        super(nMask, filter);

        m_nMaskSynthetic = nMaskSynthetic;
        }


    // ----- Filter interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean evaluate(MapEvent<K, V> o)
        {
        if (o instanceof CacheEvent)
            {
            int        nMaskSynthetic = m_nMaskSynthetic;
            CacheEvent evt            = (CacheEvent) o;
            boolean    fSynthetic     = evt.isSynthetic();

            return (((nMaskSynthetic & E_SYNTHETIC) != 0 && fSynthetic) ||
                    ((nMaskSynthetic & E_NATURAL) != 0 && !fSynthetic)) &&
                   super.evaluate(o);
            }
        else
            {
            return super.evaluate(o);
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
     * Compare the CacheEventFilter with another object to determine equality.
     *
     * @return true iff this CacheEventFilter and the passed object are
     *         equivalent filters
     */
    public boolean equals(Object o)
        {
        if (o instanceof CacheEventFilter)
            {
            CacheEventFilter that = (CacheEventFilter) o;
            return this.m_nMaskSynthetic == that.m_nMaskSynthetic && super.equals(o);
            }

        return false;
        }

    /**
     * Determine a hash value for the CacheEventFilter object according to the
     * general {@link Object#hashCode()} contract.
     *
     * @return an integer hash value for this MapEventFilter object
     */
    public int hashCode()
        {
        return super.hashCode() ^ m_nMaskSynthetic;
        }

    /**
     * {@inheritDoc}
     */
    protected String getDescription()
        {
        StringBuilder sb         = new StringBuilder();
        int           nMaskSynth = m_nMaskSynthetic;

        sb.append(super.getDescription());
        sb.append(", synthetic-mask=");

        StringBuilder sbMask = new StringBuilder();
        if ((nMaskSynth & E_NATURAL) != 0)
            {
            sbMask.append("E_NATURAL|");
            }
        if ((nMaskSynth & E_SYNTHETIC) !=0)
            {
            sbMask.append("E_SYNTHETIC|");
            }

        if (sbMask.length() == 0)
            {
            sb.append("<none>");
            }
        else
            {
            sbMask.setLength(sbMask.length() - 1);
            sb.append(sbMask);
            }

        return sb.toString();
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);
        m_nMaskSynthetic = readInt(in);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);
        writeInt(out, m_nMaskSynthetic);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);
        m_nMaskSynthetic = in.readInt(10);
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);
        out.writeInt(10, m_nMaskSynthetic);
        }


    // ----- constants and data members -------------------------------------

    /**
     * Bitmask to select synthetic events.
     */
    public static final int E_SYNTHETIC = 0x1;

    /**
     * Bitmask to select natural events.
     */
    public static final int E_NATURAL   = 0x2;

    /**
     * Bitmask that selects whether to include synthetic, natural, or all events.
     */
    @JsonbProperty("syntheticMask")
    protected int m_nMaskSynthetic;
    }
