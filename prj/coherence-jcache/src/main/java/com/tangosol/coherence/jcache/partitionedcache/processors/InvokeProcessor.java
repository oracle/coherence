/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache.processors;

import com.tangosol.coherence.jcache.common.CoherenceEntryProcessorResult;
import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.JCacheContext;
import com.tangosol.coherence.jcache.common.JCacheEntryMetaInf;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.common.JCacheStatistics;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardian;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.cache.CacheException;

import javax.cache.expiry.ExpiryPolicy;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorResult;
import javax.cache.processor.MutableEntry;

/**
 * InvokeProcessor EntryProcessor for executing JCache Entry Processors.
 *
 * @param <K> key type
 * @param <V> value type
 * @param <T> invoke return type
 *
 * @author jf  2013.12.18
 * @since Coherence 12.1.3
 */
public class InvokeProcessor<K, V, T>
        extends AbstractEntryProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * For POF and ExternalizeableLite
     *
     */
    public InvokeProcessor()
        {
        super();
        }

    /**
     * Construct an InvokeProcessor entry processor to invoke a JCache EntryProcessor.
     *
     * @param id       JCache cache identifier
     * @param proc     a JCache API level EntryProcessor
     * @param args     optional arguments to be passed to each call of processor.process().
     */
    public InvokeProcessor(JCacheIdentifier id, EntryProcessor<K, V, T> proc, Object... args)
        {
        super(id);
        m_processor = proc;
        m_varargs   = args.length == 0 ? null : args;
        }

    // ----- InvocableMap.EntryProcessor methods ----------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
            throws CacheException
        {
        // assume there's no result
        EntryProcessorResult<T> result = null;

        try
            {
            long                      ldtStart  = Helper.getCurrentTimeMillis();
            BinaryEntry               binEntry  = entry instanceof BinaryEntry ? (BinaryEntry) entry : null;
            JCacheContext             jcacheCtx = BinaryEntryHelper.getContext(m_cacheId, binEntry);
            JCacheStatistics          stats     = jcacheCtx.getStatistics();
            BackingMapManagerContext  ctx       = binEntry != null ? binEntry.getContext() : null;
            Binary                    binValue;

            EntryProcessorEntry<K, V> epe = new EntryProcessorEntry<K, V>(entry, jcacheCtx.getExpiryPolicy(), ldtStart);
            JCacheEntryMetaInf        valueMetaInf = null;

            // execute the JCache EntryProcessor
            T oResult = m_varargs == null ? m_processor.process(epe) : m_processor.process(epe, m_varargs);

            result = oResult == null ? null : new CoherenceEntryProcessorResult<T>(oResult);

            // ensure if cache loading occurred that binary value has been updated.
            binValue     = epe.getBinaryValueNoCacheLoading();
            valueMetaInf = BinaryEntryHelper.getValueMetaInf(binValue, ctx);

            if (entry.isPresent())
                {
                stats.registerHits(1, ldtStart);
                }
            else
                {
                stats.registerMisses(1, ldtStart);
                }

            // commit results of user provided entry processor.
            switch (epe.m_operation)
                {
                case NONE:
                    break;

                case ACCESS:
                    boolean fExpired = binValue == null || (valueMetaInf != null && valueMetaInf.isExpiredAt(ldtStart));

                    if (fExpired)
                        {
                        BinaryEntryHelper.expireEntry(binEntry);

                        return null;
                        }

                    valueMetaInf.accessed(ldtStart, jcacheCtx.getExpiryPolicy());

                    binValue = BinaryEntryHelper.decorateBinValueWithJCacheMetaInf(binValue, valueMetaInf, ctx);
                    binValue = BinaryEntryHelper.jcacheSyntheticUpdateEntry(binValue, ctx);

                    // must update value due to decorate update of JCACHE metaInfo for value.  Should be synthetic update in
                    // get. (not working the Coherence Update listener does get fired even though no change in user visible
                    // value, only meta info change to reflect this access via get.
                    // used to be updateBinaryValue(binValue, true (isSynthetic)) but 3.7.1 does not have this method.
                    // adding isSynthetic by implementing in the adapter m_ldtNow since isSynthetic in 12.2.1 and up was not
                    // sufficient for our use case.
                    binEntry.updateBinaryValue(binValue);
                    break;

                case CREATE:
                    if (valueMetaInf == null)
                        {
                        // add missing JCache decoration to binValue.
                        // the EntryProcessorEntry.setValue and cache loading should set the metainf already,
                        // this step is just in case.
                        valueMetaInf = new JCacheEntryMetaInf(ldtStart, jcacheCtx.getExpiryPolicy());
                        }

                    // as documented in javax.cache.expiry.ExpiryPolicy.getExpiryForCreation,
                    // do not add created entry to cache if it is already expired.
                    // (occurs when ExpiryPolicy.getExpiryForCreation method returns Duration#ZERO.)
                    // No Expiry event is raised for this case.
                    if (!valueMetaInf.isExpiredAt(ldtStart))
                        {
                        binValue = BinaryEntryHelper.decorateBinValueWithJCacheMetaInf(binValue, valueMetaInf, ctx);
                        binEntry.updateBinaryValue(binValue);
                        stats.registerPuts(1, ldtStart);
                        }

                    break;

                case UPDATE:

                    // all the valueMetainf work was performed in EntryProcessor.setValue
                    // on its binValue.  So metainf is already on binValue.
                    binEntry.updateBinaryValue(binValue);
                    stats.registerPuts(1, ldtStart);
                    break;

                case REMOVE:
                    binEntry.remove(false);
                    stats.registerRemoves(1, ldtStart);
                    break;

                default:

                    // log error if do not handle a newly added mode and do not handle above.
                    break;
                }
            }
        catch (Exception e)
            {
            result = new CoherenceEntryProcessorResult<T>(e);
            }

        return result;
        }

    @Override
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
    public void readExternal(DataInput dataInput)
            throws IOException
        {
        super.readExternal(dataInput);
        m_processor = (EntryProcessor) ExternalizableHelper.readObject(dataInput);

        ArrayList<Object> varargs = new ArrayList<Object>(10);

        ExternalizableHelper.readCollection(dataInput, varargs, getContextClassLoader());
        m_varargs = varargs.toArray();

        if (m_varargs.length == 0)
            {
            m_varargs = null;
            }
        }

    @Override
    public void writeExternal(DataOutput dataOutput)
            throws IOException
        {
        super.writeExternal(dataOutput);
        ExternalizableHelper.writeObject(dataOutput, m_processor);

        List l = m_varargs == null ? new ArrayList() : Arrays.asList(m_varargs);

        ExternalizableHelper.writeCollection(dataOutput, l);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader pofReader)
            throws IOException
        {
        super.readExternal(pofReader);
        m_processor = (EntryProcessor) pofReader.readObject(1);
        m_varargs   = pofReader.readObjectArray(2, new Object[20]);
        }

    @Override
    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        super.writeExternal(pofWriter);
        pofWriter.writeObject(1, m_processor);
        pofWriter.writeObjectArray(2, m_varargs);
        }

    // ----- inner class ----------------------------------------------------

    /**
     * A {@link javax.cache.processor.MutableEntry} that is used by {@link javax.cache.processor.EntryProcessor}s.
     */
    private static class EntryProcessorEntry<K, V>
            implements MutableEntry<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a {@link javax.cache.processor.MutableEntry}
         *
         * @param entry         the initial entry for the {@link javax.cache.processor.MutableEntry}
         * @param expiryPolicy  expiryPolicy
         * @param ldtNow         the current time
         */
        public EntryProcessorEntry(InvocableMap.Entry entry, ExpiryPolicy expiryPolicy, long ldtNow)
            {
            m_binEntry     = entry instanceof BinaryEntry ? (BinaryEntry) entry : null;
            f_expiryPolicy = expiryPolicy;
            m_ldtNow       = ldtNow;
            m_ctx          = m_binEntry == null ? null : m_binEntry.getContext();
            m_valueMetaInf = m_binEntry == null ? null : BinaryEntryHelper.getValueMetaInf(m_binEntry);
            m_binValue     = null;
            m_value        = null;
            m_operation    = MutableEntryOperation.NONE;
            }

        // ----- MutableEntry interface -------------------------------------
        @Override
        public K getKey()
            {
            return (K) m_binEntry.getKey();
            }

        @Override
        public V getValue()
            {
            Binary binValueResult = getBinaryValueNoCacheLoading();

            if (binValueResult == null)
                {
                if (m_binEntry != null && m_binEntry.isPresent())
                    {
                    // TODO: very convoluted but needed a way to have cache loader registered with
                    // coherence to get called here. In future, consider replacing this with lookup of
                    // ParitionedCacheBinaryEntryStore instance from resource registry.
                    //
                    // m_binEntry value is JCache expired or has been removed,
                    // set to null and call getValue to see if cacheLoader will create a new value.
                    m_binEntry.updateBinaryValue(null);
                    }

                // cache loading could cause this to return non-null.
                binValueResult = m_binEntry.getBinaryValue();

                if (binValueResult != null)
                    {
                    // cache loading occurred.
                    m_binValue  = binValueResult;
                    m_operation = MutableEntryOperation.CREATE;
                    m_valueMetaInf = (JCacheEntryMetaInf) m_ctx.getInternalValueDecoration(binValueResult,
                        ExternalizableHelper.DECO_JCACHE);

                    // cache loading is currently adding metainfo so next if is not getting executed.
                    // leaving this just in case cache loading does not add jcache metainf in future.cd .
                    if (m_valueMetaInf == null)
                        {
                        m_valueMetaInf = new JCacheEntryMetaInf(m_ldtNow, f_expiryPolicy);

                        m_binValue = BinaryEntryHelper.decorateBinValueWithJCacheMetaInf(m_binValue, m_valueMetaInf,
                            m_ctx);
                        }
                    }
                else
                    {
                    // no cache loading. just return null.
                    return null;
                    }
                }

            if (m_operation == MutableEntryOperation.NONE)
                {
                m_operation = MutableEntryOperation.ACCESS;
                }

            return binValueResult == null ? null : (V) m_ctx.getValueFromInternalConverter().convert(binValueResult);
            }

        @Override
        public boolean exists()
            {
            switch (m_operation)
                {
                case NONE:
                case ACCESS:

                    if (m_binEntry == null || !m_binEntry.isPresent())
                        {
                        return false;
                        }
                case CREATE:
                case UPDATE:
                    return !isExpiredAt();

                case REMOVE:
                    return false;

                default:
                    throw new IllegalStateException("missing case in exists");
                }
            }

        @Override
        public void remove()
            {
            m_operation = m_operation == MutableEntryOperation.CREATE
                          ? MutableEntryOperation.NONE : MutableEntryOperation.REMOVE;
            m_binValue     = null;
            m_value        = null;
            m_valueMetaInf = null;
            }

        @Override
        public void setValue(V oValue)
            {
            if (oValue == null)
                {
                throw new NullPointerException();
                }

            if (m_operation != MutableEntryOperation.CREATE && exists())
                {
                // Update path
                m_operation = MutableEntryOperation.UPDATE;
                m_valueMetaInf = m_binValue != null
                                 ? (JCacheEntryMetaInf) m_ctx.getInternalValueDecoration(m_binValue,
                                     ExternalizableHelper.DECO_JCACHE) : BinaryEntryHelper.getValueMetaInf(m_binEntry);
                m_value    = oValue;
                m_binValue = (Binary) m_ctx.getValueToInternalConverter().convert(oValue);
                m_valueMetaInf.modified(m_ldtNow, f_expiryPolicy);
                m_binValue = BinaryEntryHelper.decorateBinValueWithJCacheMetaInf(m_binValue, m_valueMetaInf, m_ctx);
                }
            else
                {
                // Create path
                Binary             binValue        = (Binary) m_ctx.getValueToInternalConverter().convert(oValue);
                JCacheEntryMetaInf binValueMetaInf = new JCacheEntryMetaInf(m_ldtNow, f_expiryPolicy);

                if (!binValueMetaInf.isExpiredAt(m_ldtNow))
                    {
                    m_binValue = BinaryEntryHelper.decorateBinValueWithJCacheMetaInf(binValue, binValueMetaInf, m_ctx);
                    m_valueMetaInf = binValueMetaInf;
                    m_value        = oValue;
                    m_operation    = MutableEntryOperation.CREATE;
                    }

                // Currently treating set of an expired oValue as a noop for create mode case.
                // is there anything to do if it is expired here.
                }
            }

        @Override
        public <T> T unwrap(Class<T> clz)
            {
            throw new IllegalArgumentException("Can't unwrap an EntryProcessor Entry");
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Get binary entry value, taking measures to avoid native Coherence cache loading if it is enabled.
         *
         * @return binary value
         */
        private Binary getBinaryValueNoCacheLoading()
            {
            if (m_operation == MutableEntryOperation.REMOVE)
                {
                return null;
                }

            if (m_operation == MutableEntryOperation.CREATE || m_operation == MutableEntryOperation.UPDATE)
                {
                return BinaryEntryHelper.isExpired(m_ctx, m_binValue, m_valueMetaInf, m_ldtNow) ? null : m_binValue;
                }
            else
                {
                // NOTE: only getBinaryValue if m_binEntry is present;
                // do not want native coherence cache loading occurring in this call to getBinaryValue.
                Binary binValue = (m_binEntry != null && m_binEntry.isPresent()) ? m_binEntry.getBinaryValue() : null;

                return binValue != null && !BinaryEntryHelper.isExpired(m_ctx, binValue, m_valueMetaInf, m_ldtNow)
                       ? binValue : null;
                }
            }

        /**
         * return true if this Cache.Entry representation is expired.
         *
         * @return true iff this Cache.Entry representation is expired.
         */
        private boolean isExpiredAt()
            {
            return (m_binValue != null)
                   ? BinaryEntryHelper.isExpired(m_ctx, m_binValue, m_valueMetaInf, m_ldtNow)
                   : BinaryEntryHelper.isExpired(m_ctx, getBinaryValueNoCacheLoading(), m_valueMetaInf, m_ldtNow);
            }

        // ----- data members -----------------------------------------------

        /**
         * The current external form value for this entry.
         */
        private V m_value;

        /**
         * The current internal binary value for this entry.
         */
        private Binary m_binValue;

        /**
         * The {@link MutableEntryOperation} to be performed on the {@link javax.cache.processor.MutableEntry}.
         */
        private MutableEntryOperation m_operation;

        /**
         * The time (since the Epoc) when a process m_operation was performed on MutableEntry.
         * Use this value for calculating expiry.
         */
        private long m_ldtNow;

        /**
         * Initial entry before possible mutation on it by current Entry Processor process invocation on this entry.
         */
        private final BinaryEntry              m_binEntry;
        private final BackingMapManagerContext m_ctx;
        private JCacheEntryMetaInf             m_valueMetaInf;
        private final ExpiryPolicy             f_expiryPolicy;
        }

    /**
     * The m_operation to perform on a {@link BinaryEntry} as a result of
     * actions performed on a {@link javax.cache.processor.MutableEntry}.
     */
    private enum MutableEntryOperation
        {
        /**
         * Don't perform any operations on the {@link BinaryEntry}.
         */
        NONE,

        /**
         * Decorate the BinaryEntry#getBinaryValue() with ACCESSED expiration.
         */
        ACCESS,

        /**
         * Create a new {@link BinaryEntry}.
         */
        CREATE,

        /**
         * Remove the {@link BinaryEntry}.
         */
        REMOVE,

        /**
         * Update the {@link BinaryEntry}.
         */
        UPDATE
        }

    // ----- data members ---------------------------------------------------

    private EntryProcessor<K, V, T> m_processor;
    private Object[]                m_varargs;
    }
