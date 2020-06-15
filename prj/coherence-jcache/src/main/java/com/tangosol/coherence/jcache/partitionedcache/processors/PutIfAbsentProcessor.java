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
import com.tangosol.coherence.jcache.common.JCacheStatistics;

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

/**
 * JCache PutIfAbsent Entry Processor
 *
 * @param <K> key type
 *
 * @author jf  2013.12.18
 * @since Coherence 12.1.3
 */
public class PutIfAbsentProcessor<K>
        extends AbstractEntryProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * To support ExternalizableLite
     */
    public PutIfAbsentProcessor()
        {
        super();
        m_binValue = new Binary();
        }

    /**
     *
     * Constructs JCache PutIfAbsent Entry Processor
     *
     * @param binValue binary value
     * @param id    JCache cache unique identifier
     */
    public PutIfAbsentProcessor(Binary binValue, JCacheIdentifier id)
        {
        super(id);
        m_binValue = binValue;
        }

    // ----- InvocableMap.EntryProcessor interface --------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        long                     ldtStart  = Helper.getCurrentTimeMillis();

        BinaryEntry              binEntry  = (BinaryEntry) entry;
        JCacheContext            jcacheCtx = BinaryEntryHelper.getContext(m_cacheId, binEntry);
        JCacheStatistics         stats     = jcacheCtx.getStatistics();

        BackingMapManagerContext ctx       = binEntry.getContext();
        boolean                  fExpired  = false;

        if (binEntry.isPresent() && binEntry.getBinaryValue() != null)
            {
            // if entry is expired, fall through to create mode. If not expired, just return false;
            if (BinaryEntryHelper.isExpired(binEntry, ldtStart))
                {
                // TODO; when coherence implements expire immediately this can be put back in.
                // currently coherence only supports expire in 1 millisecond.
                // Thus, expiring this entry results in expiring the soon to be created entry.
                // At this time, this is a replace entry in coherence implementation, but it is
                // a putIfAbsent as far as JCache semantics.
                // BinaryEntryHelper.expireEntry(binEntry);
                // do not expire since reusing binaryEntry in createMode.
                // BinaryEntryHelper.expireEntry(binEntry);
                Byte jcache_synthetic_kind = (Byte) ctx.getInternalValueDecoration(binEntry.getBinaryValue(),
                                                 ExternalizableHelper.DECO_JCACHE_SYNTHETIC);

                if (jcache_synthetic_kind != null
                    && !BinaryEntryHelper.JCACHE_SYNTHETIC_EXPIRY.equals(jcache_synthetic_kind))
                    {
                    fExpired = true;
                    }
                }
            else
                {
                stats.registerHits(1, ldtStart);
                return Boolean.FALSE;
                }
            }

        // create mode
        JCacheEntryMetaInf valueMetaInf = new JCacheEntryMetaInf(ldtStart, jcacheCtx.getExpiryPolicy());

        // as documented in javax.cache.expiry.ExpiryPolicy.getExpiryForCreation,
        // do not add created entry to cache if it is already expired.
        // (occurs when ExpiryPolicy.getExpiryForCreation method returns Duration#ZERO.)
        // No Expiry event is raised for this case.
        if (!valueMetaInf.isExpiredAt(ldtStart))
            {
            Binary binValue = (Binary) ctx.addInternalValueDecoration(m_binValue, ExternalizableHelper.DECO_JCACHE,
                                  valueMetaInf);

            if (fExpired)
                {
                binValue = BinaryEntryHelper.jcacheSyntheticExpiryEventForReusedBinaryEntry(binValue, ctx);
                }

            binEntry.updateBinaryValue(binValue);

            stats.registerPuts(1, ldtStart);
            stats.registerMisses(1, ldtStart);

            return Boolean.TRUE;
            }
        else
            {
            stats.registerHits(1, ldtStart);
            return Boolean.FALSE;
            }
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput dataInput)
            throws IOException
        {
        super.readExternal(dataInput);
        m_binValue.readExternal(dataInput);
        }

    @Override
    public void writeExternal(DataOutput dataOutput)
            throws IOException
        {
        super.writeExternal(dataOutput);
        m_binValue.writeExternal(dataOutput);
        }

    // ----- PortableObject interface ---------------------------------------

    public void readExternal(PofReader pofReader)
            throws IOException
        {
        super.readExternal(pofReader);
        m_binValue = pofReader.readBinary(1);

        }

    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        super.writeExternal(pofWriter);
        pofWriter.writeBinary(1, m_binValue);
        }

    // ----- data members ---------------------------------------------------

    private Binary m_binValue;
    }
