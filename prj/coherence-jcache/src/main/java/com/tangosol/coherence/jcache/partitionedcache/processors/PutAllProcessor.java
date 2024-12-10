/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.jcache.partitionedcache.processors;

import com.tangosol.coherence.jcache.common.Helper;
import com.tangosol.coherence.jcache.common.JCacheContext;
import com.tangosol.coherence.jcache.common.JCacheEntryMetaInf;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.GuardSupport;
import com.tangosol.net.Guardian;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.CompositeKey;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * JCache EntryProcessor that performs PutAll.
 * <p>
 * Usage:
 * <code>
 *  cache.invokeAll(PutAllProcessor.setOf(map), PutAllProcessor.INSTANCE);
 * </code>
 *
 * @author bo  2013.12.18
 * @since Coherence 12.1.3
 */
public class PutAllProcessor<K>
        extends AbstractEntryProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs ...
     *
     */
    public PutAllProcessor()
        {
        super();
        m_fReplaceExistingValues = false;
        m_fUseWriteThrough       = false;
        m_fLoadAll               = false;
        }

    /**
     * Constructs a JCache PutAll entry processor
     *
     *
     * @param id  unique JCache cache identifier
     * @param replaceExistingValues true if existing entries should be replaced
     * @param useWriteThrough true if configured write-through should be called.
     * @param fLoadAll called from loadAll, do not use writeThrough
     */
    public PutAllProcessor(JCacheIdentifier id, boolean replaceExistingValues, boolean useWriteThrough,
                           boolean fLoadAll)
        {
        super(id);
        m_fReplaceExistingValues = replaceExistingValues;
        m_fUseWriteThrough       = useWriteThrough;
        m_fLoadAll               = fLoadAll;
        }

    // ----- AbstractEntryProcessor methods ---------------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        long        ldtNow   = Helper.getCurrentTimeMillis();
        BinaryEntry binEntry = (BinaryEntry) entry;

        if (binEntry.isPresent())
            {
            throw new IllegalStateException("An entry for the CompositeKey should not exist when using the PutAllProcessor");
            }
        else
            {
            Object oCompositeKey = binEntry.getKey();

            if (oCompositeKey instanceof CompositeKey)
                {
                CompositeKey             keyComposite = (CompositeKey) oCompositeKey;
                Object                   oValue       = keyComposite.getSecondaryKey();
                BackingMapManagerContext ctx          = binEntry.getContext();
                Binary binKey = (Binary) ctx.getKeyToInternalConverter().convert(keyComposite.getPrimaryKey());
                JCacheContext jcacheCtx = BinaryEntryHelper.getContext(m_cacheId, binEntry);


                // Binary binKey = (Binary) keyComposite.getPrimaryKey();
                Binary binValue = (Binary) oValue;

                binEntry = (BinaryEntry) binEntry.getBackingMapContext().getBackingMapEntry(binKey);

                JCacheEntryMetaInf valueMetaInf = BinaryEntryHelper.getValueMetaInf(binEntry);
                boolean            fExpired = valueMetaInf != null && BinaryEntryHelper.isExpired(binEntry, valueMetaInf, ldtNow);

                if (valueMetaInf == null || fExpired)
                    {
                    // create mode
                    valueMetaInf = new JCacheEntryMetaInf(ldtNow, jcacheCtx.getExpiryPolicy());

                    // as documented in javax.cache.expiry.ExpiryPolicy.getExpiryForCreation,
                    // do not add created entry to cache if it is already expired.
                    // (occurs when ExpiryPolicy.getExpiryForCreation method returns Duration#ZERO.)
                    // No Expiry event is raised for this case.
                    if (!valueMetaInf.isExpiredAt(ldtNow))
                        {
                        binValue = (Binary) ctx.addInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE, valueMetaInf);

                        if (fExpired)
                            {
                            Byte jcacheSynthetic = (Byte) ctx.getInternalValueDecoration(binValue,
                                    ExternalizableHelper.DECO_JCACHE_SYNTHETIC);

                            // don't expire same entry twice.
                            if (jcacheSynthetic != null && !BinaryEntryHelper.JCACHE_SYNTHETIC_EXPIRY.equals(jcacheSynthetic))
                                {
                                binValue = BinaryEntryHelper.jcacheSyntheticExpiryEventForReusedBinaryEntry(binValue, ctx);
                                }
                            }

                        if (m_fLoadAll)
                            {
                            binValue = BinaryEntryHelper.decorateUpdateJCacheSynthetic(binValue, ctx,
                              BinaryEntryHelper.JCACHE_SYNTHETIC_LOADED);
                            }

                        binEntry.updateBinaryValue(binValue);

                        return Boolean.TRUE;
                        }
                    }
                else if (m_fReplaceExistingValues)
                    {
                    // modify mode
                    valueMetaInf.modified(ldtNow, jcacheCtx.getExpiryPolicy());

                    // is next line needed ?? since it is add rather than set or replace, thought next line was necessary.
                    binValue = (Binary) ctx.addInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE, valueMetaInf);

                    if (m_fLoadAll)
                        {
                        binValue = BinaryEntryHelper.decorateUpdateJCacheSynthetic(binValue, ctx,
                          BinaryEntryHelper.JCACHE_SYNTHETIC_LOADED);
                        }

                    binEntry.updateBinaryValue(binValue);

                    return Boolean.TRUE;
                    }
                }
            }

        return Boolean.FALSE;
        }

    @Override
    public Map processAll(Set setEntries)
        {
        long                  ldtStart = Helper.getCurrentTimeMillis();
        int                   cPuts    = 0;
        Guardian.GuardContext ctxGuard = GuardSupport.getThreadContext();
        long                  cMillis  = ctxGuard == null ? 0L : ctxGuard.getTimeoutMillis();
        InvocableMap.Entry    entry    = null;

        for (Iterator iter = setEntries.iterator(); iter.hasNext(); )
            {
            entry = (InvocableMap.Entry) iter.next();

            boolean fPut = (Boolean) process(entry);

            if (fPut)
                {
                cPuts++;
                }

            if (ctxGuard != null)
                {
                ctxGuard.heartbeat(cMillis);
                }
            }

        BinaryEntry binEntry = entry instanceof BinaryEntry ? (BinaryEntry) entry : null;

        // if m_fLoadAll is true, this was called from loadAll, do not record puts for explicit loading.
        if (binEntry != null && !m_fLoadAll)
            {
            BinaryEntryHelper.getContext(m_cacheId, binEntry).getStatistics().registerPuts(cPuts, ldtStart);
            }

        return null;
        }
    // ----- PutAllProcessor methods ----------------------------------------

    /**
     * Creates the Set of CompositeKeys based on a Map that is to be used with a
     * PutAllProcessor.
     *
     * @param map  the Map to convert into a Set of CompositeKeys
     *
     * @return a Set of CompositeKey
     */
    public static Set<CompositeKey> setOf(Map<Object, Binary> map)
        {
        Set<CompositeKey> set = new HashSet<CompositeKey>();

        if (map != null)
            {
            for (Map.Entry<Object, Binary> entry : map.entrySet())
                {
                set.add(new CompositeKey(entry.getKey(), entry.getValue()));
                }
            }

        return set;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput dataInput)
            throws IOException
        {
        super.readExternal(dataInput);
        m_fReplaceExistingValues = (Boolean) ExternalizableHelper.readObject(dataInput);
        m_fUseWriteThrough       = (Boolean) ExternalizableHelper.readObject(dataInput);
        m_fLoadAll               = (Boolean) ExternalizableHelper.readObject(dataInput);
        }

    @Override
    public void writeExternal(DataOutput dataOutput)
            throws IOException
        {
        super.writeExternal(dataOutput);
        ExternalizableHelper.writeObject(dataOutput, m_fReplaceExistingValues);
        ExternalizableHelper.writeObject(dataOutput, m_fUseWriteThrough);
        ExternalizableHelper.writeObject(dataOutput, m_fLoadAll);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader pofReader)
            throws IOException
        {
        super.readExternal(pofReader);
        m_fReplaceExistingValues = pofReader.readBoolean(1);
        m_fUseWriteThrough       = pofReader.readBoolean(2);
        m_fLoadAll               = pofReader.readBoolean(3);
        }

    @Override
    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        super.writeExternal(pofWriter);
        pofWriter.writeBoolean(1, m_fReplaceExistingValues);
        pofWriter.writeBoolean(2, m_fUseWriteThrough);
        pofWriter.writeBoolean(3, m_fLoadAll);
        }

    // ----- data members ---------------------------------------------------

    private boolean m_fReplaceExistingValues;
    private boolean m_fUseWriteThrough;
    private boolean m_fLoadAll;
    }
