/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.passthroughcache;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.cache.configuration.Factory;

import javax.cache.event.CacheEntryEventFilter;

/**
 * An Coherence {@link Filter} that delegates {@link com.tangosol.util.MapEvent}s
 * onto a {@link CacheEntryEventFilter}.
 *
 * @param <K>  the type of the {@link javax.cache.Cache} keys
 * @param <V>  the type of the {@link javax.cache.Cache} values
 *
 * @author bo  2013.11.04
 * @since Coherence 12.1.3
 */
public class PassThroughFilterAdapter<K, V>
        implements Filter, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link PassThroughFilterAdapter}.
     * (required for serialization)
     */
    public PassThroughFilterAdapter()
        {
        m_factoryCacheEntryEventFilter = null;
        }

    /**
     * Constructs a {@link PassThroughFilterAdapter}.
     *
     * @param factory  a {@link Factory} to produce the {@link CacheEntryEventFilter}
     */
    public PassThroughFilterAdapter(Factory<CacheEntryEventFilter<? super K, ? super V>> factory)
        {
        m_factoryCacheEntryEventFilter = factory;
        }

    // ----- Filter methods -------------------------------------------------

    @Override
    public boolean evaluate(Object object)
        {
        // determine the filter to which we shall delegate events
        CacheEntryEventFilter<? super K, ? super V> filter = getCacheEntryEventFilter();

        if (filter != null && object instanceof MapEvent)
            {
            PassThroughCacheEntryEvent<K, V> entryEvent = PassThroughCacheEntryEvent.<K, V>from((MapEvent) object);

            return filter.evaluate(entryEvent);
            }
        else
            {
            return false;
            }
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_factoryCacheEntryEventFilter = (Factory<CacheEntryEventFilter<? super K,
            ? super V>>) ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_factoryCacheEntryEventFilter);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_factoryCacheEntryEventFilter = reader.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeObject(0, m_factoryCacheEntryEventFilter);
        }

    // ----- PassThroughFilterAdapter methods -------------------------------

    /**
     * Obtains the {@link CacheEntryEventFilter} to which to delegate
     * {@link javax.cache.event.CacheEntryEvent}s.
     *
     *  @return  the {@link CacheEntryEventFilter} (or <code>null</code> if unavailable
     */
    protected CacheEntryEventFilter<? super K, ? super V> getCacheEntryEventFilter()
        {
        if (m_cacheEntryEventFilter == null && m_factoryCacheEntryEventFilter != null)
            {
            try
                {
                m_cacheEntryEventFilter = m_factoryCacheEntryEventFilter.create();
                }
            catch (Exception e)
                {
                m_cacheEntryEventFilter = null;

                if (!m_logged)
                    {
                    m_logged = true;
                    Logger.warn("no CacheEntryEventFilter."
                                + " handled unexpected exception while attempting to create CacheEntryEventFilter:", e);

                    }
                }

            return m_cacheEntryEventFilter;
            }
        else
            {
            return null;
            }
        }

    // ------ data members --------------------------------------------------

    /**
     * The {@link Factory} for the {@link CacheEntryEventFilter}.
     */
    private Factory<CacheEntryEventFilter<? super K, ? super V>> m_factoryCacheEntryEventFilter;

    /**
     * The realized {@link CacheEntryEventFilter}.
     */
    private transient CacheEntryEventFilter<? super K, ? super V> m_cacheEntryEventFilter;

    /**
     * Only log failure to create EventFilter once.
     */
    private transient boolean m_logged = false;
    }
