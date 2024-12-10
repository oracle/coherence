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

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.cache.configuration.Factory;

import javax.cache.integration.CacheLoader;

/**
 * TODO: currently unused class.
 * Switched to using native Coherence CacheStore for read-through and write-through support.
 *
 * This class should be deleted when fully committed to native Coherence CacheStore
 *
 * @param <K> key type
 * @param <V> value type
 *
 * @author jf  2013.07.07
 * @since Coherence 12.1.3
 */
public class CacheLoaderProcessor<K, V>
        extends AbstractEntryProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * necessary for ExternalizableLite
     */
    public CacheLoaderProcessor()
        {
        super();
        }

    /**
     * Construct a JCache CacheLoaderProcessor
     * @param next processor to run in a chain of entry processor execution
     * @param id   JCacheIdentifier to look up JCacheContext for entry processing.
     */
    public CacheLoaderProcessor(InvocableMap.EntryProcessor next, JCacheIdentifier id)
        {
        super(id);
        m_next = next;
        }

    // ----- AbstractEntryProcessor methods ---------------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        long        ldtNow   = Helper.getCurrentTimeMillis();
        BinaryEntry binEntry = (BinaryEntry) entry;

        if (!binEntry.isPresent() || (binEntry.isPresent() && BinaryEntryHelper.isExpired(binEntry, ldtNow)))
            {
            JCacheContext jcacheCtx = BinaryEntryHelper.getContext(m_cacheId, binEntry);

            if (jcacheCtx.isReadThrough())
                {
                V oValue = (V) jcacheCtx.getCacheLoader().load((K) entry.getKey());

                if (oValue == null)
                    {
                    throw new NullPointerException();
                    }
                else
                    {
                    // create mode
                    BackingMapManagerContext ctx          = binEntry.getContext();
                    Binary                   binValue     = (Binary) ctx.getValueToInternalConverter().convert(oValue);
                    JCacheEntryMetaInf       valueMetaInf = new JCacheEntryMetaInf(ldtNow, jcacheCtx.getExpiryPolicy());

                    // as documented in javax.cache.expiry.ExpiryPolicy.getExpiryForCreation,
                    // do not add created entry to cache if it is already expired.
                    // (occurs when ExpiryPolicy.getExpiryForCreation method returns Duration#ZERO.)
                    // No Expiry event is raised for this case.
                    if (!valueMetaInf.isExpiredAt(ldtNow))
                        {
                        binValue = (Binary) ctx.addInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE,
                            valueMetaInf);
                        binEntry.updateBinaryValue(binValue);
                        }
                    }
                }
            }

        return m_next.process(entry);
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput dataInput)
            throws IOException
        {
        super.readExternal(dataInput);
        m_cacheLoaderFactory = (Factory<CacheLoader<K, V>>) ExternalizableHelper.readObject(dataInput);
        m_next               = (InvocableMap.EntryProcessor) ExternalizableHelper.readObject(dataInput);
        }

    @Override
    public void writeExternal(DataOutput dataOutput)
            throws IOException
        {
        super.writeExternal(dataOutput);
        ExternalizableHelper.writeObject(dataOutput, m_cacheLoaderFactory);
        ExternalizableHelper.writeObject(dataOutput, m_next);

        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader pofReader)
            throws IOException
        {
        super.readExternal(pofReader);
        m_cacheLoaderFactory = (Factory<CacheLoader<K, V>>) pofReader.readObject(1);
        m_next               = (InvocableMap.EntryProcessor) pofReader.readObject(2);

        }

    @Override
    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        super.writeExternal(pofWriter);
        pofWriter.writeObject(1, m_cacheLoaderFactory);
        pofWriter.writeObject(2, m_next);
        }

    // ----- data members ---------------------------------------------------

    private Factory<CacheLoader<K, V>>  m_cacheLoaderFactory;
    private InvocableMap.EntryProcessor m_next;
    }
