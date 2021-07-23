/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.localcache.processors;

import com.tangosol.coherence.jcache.common.CoherenceCacheEntry;
import com.tangosol.coherence.jcache.common.CoherenceCacheEventEventDispatcher;
import com.tangosol.coherence.jcache.common.CoherenceEntryProcessorResult;
import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.JCacheStatistics;
import com.tangosol.coherence.jcache.localcache.LocalCache;
import com.tangosol.coherence.jcache.localcache.LocalCacheValue;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardian;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.cache.expiry.Duration;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;

/**
 * An {@link com.tangosol.util.InvocableMap.EntryProcessor} to
 * invoke a JCache {@link javax.cache.processor.EntryProcessor}.
 *
 * @author bo  2013.10.31
 * @since Coherence 12.1.3
 */
public class InvokeProcessor<K, V, T>
        extends AbstractEntryProcessor
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link com.tangosol.coherence.jcache.localcache.processors.InvokeProcessor}.
     *
     * @param cache   cache
     * @param processor  the {@link javax.cache.processor.EntryProcessor}
     * @param arguments  optional arguments for the {@link javax.cache.processor.EntryProcessor}
     */
    public InvokeProcessor(LocalCache cache, EntryProcessor<K, V, T> processor, Object... arguments)
        {
        super(cache);

        if (processor == null)
            {
            throw new NullPointerException("processor can't be null");
            }

        m_processor    = processor;
        m_arrArguments = new Object[arguments.length];

        //TODO: there's no requirement to copy the arguments here as the execution is "local"
        // (and blocking)
        System.arraycopy(arguments, 0, m_arrArguments, 0, arguments.length);
        }

    // ----- EntryProcessor interface ---------------------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        long                                     ldtNow             = Helper.getCurrentTimeMillis();
        boolean                                  fStatisticsEnabled = isStatisticsEnabled();
        JCacheStatistics                         stats              = fStatisticsEnabled ? getJCacheStatistics() : null;
        CoherenceCacheEventEventDispatcher<K, V> dispatcher         = new CoherenceCacheEventEventDispatcher<K, V>();

        if (fStatisticsEnabled)
            {
            if (entry.isPresent())
                {
                stats.registerHits(1, ldtNow);
                }
            else
                {
                stats.registerMisses(1, ldtNow);
                }
            }

        EntryProcessorEntry<K, V>        internalEntry = new EntryProcessorEntry<K, V>(getCache(), entry, ldtNow);
        T                                result        = null;
        CoherenceEntryProcessorResult<T> theResult     = null;

        try
            {
            result    = (T) m_processor.process(internalEntry, m_arrArguments);
            theResult = result == null ? null : new CoherenceEntryProcessorResult<T>(result);
            }
        catch (Exception e)
            {
            theResult = new CoherenceEntryProcessorResult<T>(e);

            return theResult;
            }

        K               oKey        = internalEntry.getKey();
        LocalCacheValue cachedValue = (LocalCacheValue) internalEntry.getCacheValue();

        Duration        duration;
        long            expiryTime;

        switch (internalEntry.getOperation())
            {
            case NONE :
                break;

            case ACCESS :
                cachedValue.accessInternalValue(ldtNow, getContext().getExpiryPolicy());
                entry.setValue(cachedValue);
                break;

            case CREATE :
            case LOAD :

                if (internalEntry.getOperation() == MutableEntryOperation.CREATE)
                    {
                    writeCacheEntry(new CoherenceCacheEntry<K, V>(oKey, internalEntry.getValue()));
                    }

                if (!cachedValue.isExpiredAt(ldtNow))
                    {
                    entry.setValue(cachedValue);

                    // do not count LOAD as a put for cache statistics.
                    if (isStatisticsEnabled() && internalEntry.getOperation() == MutableEntryOperation.CREATE)
                        {
                        stats.registerPuts(1, ldtNow);
                        }
                    }

                break;

            case UPDATE :
                V oldValue = (V) fromInternalValue(cachedValue.get());

                writeCacheEntry(new CoherenceCacheEntry<K, V>(oKey, internalEntry.getValue(), oldValue));

                Object internalValue = toInternalValue(internalEntry.getValue());

                LocalCacheValue newCachedValue = new LocalCacheValue(cachedValue);
                newCachedValue.updateInternalValue(internalValue, ldtNow, getContext().getExpiryPolicy());
                entry.setValue(newCachedValue);

                if (isStatisticsEnabled())
                    {
                    stats.registerPuts(1, ldtNow);
                    }

                break;

            case REMOVE :
                deleteCacheEntry(oKey);
                entry.remove(false);

                if (isStatisticsEnabled())
                    {
                    stats.registerRemoves(1, ldtNow);
                    }

                break;

            default :
                break;
            }

        getCache().dispatch(dispatcher);

        return theResult;
        }

    public Map processAll(Set setEntries)
        {
        Map                   mapResults = new LiteMap();
        Guardian.GuardContext ctxGuard   = GuardSupport.getThreadContext();
        long                  cMillis    = ctxGuard == null ? 0L : ctxGuard.getTimeoutMillis();

        for (Iterator iter = setEntries.iterator(); iter.hasNext(); )
            {
            InvocableMap.Entry entry  = (InvocableMap.Entry) iter.next();

            T                  result = (T) process(entry);

            if (result != null)
                {
                mapResults.put(entry.getKey(), result);
                }

            if (ctxGuard != null)
                {
                ctxGuard.heartbeat(cMillis);
                }
            }

        return mapResults;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        // super.readExternal(in);
        m_processor = (EntryProcessor<K, V, T>) ExternalizableHelper.readObject(in);

        int cArguments = ExternalizableHelper.readInt(in);
        azzert(cArguments < 256, "Unexpected number of arguments.");

        m_arrArguments = new Object[cArguments];

        for (int i = 0; i < cArguments; i++)
            {
            m_arrArguments[i] = ExternalizableHelper.readObject(in);
            }
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        // super.writeExternal(out);
        ExternalizableHelper.writeObject(out, m_processor);
        ExternalizableHelper.writeObject(out, m_arrArguments.length);

        for (Object oArgument : m_arrArguments)
            {
            ExternalizableHelper.writeObject(out, oArgument);
            }
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        // super.readExternal(reader);
        m_processor = (EntryProcessor<K, V, T>) reader.readObject(1);

        int cArguments = reader.readInt(2);

        m_arrArguments = new Object[cArguments];

        reader.readObjectArray(3, m_arrArguments);
        }

    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        // super.writeExternal(writer);
        writer.writeObject(1, m_processor);
        writer.writeInt(2, m_arrArguments.length);
        writer.writeObjectArray(3, m_arrArguments);
        }

    // ------ InternalEntry class -------------------------------------------

    /**
     * An internal implementation of a {@link javax.cache.processor.MutableEntry}.
     *
     * @param <K>  the type of the entry key
     * @param <V>  the type of the entry value
     */
    static class InternalEntry<K, V>
            implements MutableEntry<K, V>
        {
        /**
         * Constructs an {@link InternalEntry}.
         *
         * @param entry  the {@link com.tangosol.util.InvocableMap} entry
         */
        public InternalEntry(InvocableMap.Entry entry)
            {
            m_entry = entry;
            }

        @Override
        public boolean exists()
            {
            return m_entry.isPresent();
            }

        @Override
        public void remove()
            {
            m_entry.remove(false);
            }

        @Override
        public void setValue(V value)
            {
            m_entry.setValue(value);
            }

        @Override
        public K getKey()
            {
            return (K) m_entry.getKey();
            }

        @Override
        public V getValue()
            {
            return (V) m_entry.getValue();
            }

        @Override
        public <T> T unwrap(Class<T> clz)
            {
            if (clz != null && clz.isInstance(m_entry))
                {
                return (T) m_entry;
                }
            else
                {
                throw new IllegalArgumentException("Unsupported unwrap(" + clz + ")");
                }
            }

        // ------ data members ----------------------------------------------

        /**
         * The underlying {@link com.tangosol.util.InvocableMap} entry.
         */
        private InvocableMap.Entry m_entry;
        }

    // ------ data members --------------------------------------------------

    /**
     * The {@link javax.cache.processor.EntryProcessor}.
     */
    private EntryProcessor<K, V, T> m_processor;

    /**
     * The optional arguments.
     */
    private Object[] m_arrArguments;
    }
