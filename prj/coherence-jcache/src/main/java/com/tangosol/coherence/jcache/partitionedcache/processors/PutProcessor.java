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
 * JCache Put Entry Processor
 *
 * @param <K>   key type
 *
 * @author jf  2013.12.18
 * @since Coherence 12.1.3
 */
public class PutProcessor<K>
        extends AbstractEntryProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public PutProcessor()
        {
        super();
        m_binValue = new Binary();
        }

    /**
     * Constructs JCache Put entry processor
     *
     * @param binValue  binary value (internal format) to put.
     * @param id  unique JCache cache identifier
     */
    public PutProcessor(Binary binValue, JCacheIdentifier id)
        {
        super(id);
        m_binValue = binValue;
        }

    // ----- InvocableMap.EntryProcessor interface --------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        BinaryEntry              binEntry  = (BinaryEntry) entry;
        JCacheContext            jcacheCtx = BinaryEntryHelper.getContext(m_cacheId, binEntry);
        JCacheStatistics         stats     = jcacheCtx.getStatistics();
        long                     ldtStart  = stats == null ? 0L : Helper.getCurrentTimeMillis();
        Binary                   binValue  = binEntry.isPresent() ? binEntry.getBinaryValue() : null;
        BackingMapManagerContext ctx       = binEntry.getContext();
        JCacheEntryMetaInf       valueMetaInf;
        boolean                  fExpired = false;

        if (binEntry.isPresent())
            {
            valueMetaInf = BinaryEntryHelper.getValueMetaInf(binEntry);

            assert(valueMetaInf != null);

            if (BinaryEntryHelper.isExpired(binEntry, valueMetaInf, ldtStart))
                {
                // do not expire since reusing binaryEntry in createMode.
                // BinaryEntryHelper.expireEntry(binEntry);
                Byte jcache_synthetic_kind = (Byte) ctx.getInternalValueDecoration(binValue,
                                                 ExternalizableHelper.DECO_JCACHE_SYNTHETIC);

                if (jcache_synthetic_kind != null
                    && !BinaryEntryHelper.JCACHE_SYNTHETIC_EXPIRY.equals(jcache_synthetic_kind))
                    {
                    fExpired = true;
                    }
                }
            else
                {
                // modify mode
                valueMetaInf.modified(ldtStart, jcacheCtx.getExpiryPolicy());

                // is next line needed ?? since it is add rather than set or replace, thought next line was necessary.
                ctx.removeInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE);
                binValue = (Binary) ctx.addInternalValueDecoration(m_binValue, ExternalizableHelper.DECO_JCACHE,
                    valueMetaInf);
                binEntry.updateBinaryValue(binValue);
                stats.registerPuts(1, ldtStart);

                return null;
                }
            }

        // create mode
        valueMetaInf = new JCacheEntryMetaInf(ldtStart, jcacheCtx.getExpiryPolicy());

        // as documented in javax.cache.expiry.ExpiryPolicy.getExpiryForCreation,
        // do not add created entry to cache if it is already expired.
        // (occurs when ExpiryPolicy.getExpiryForCreation method returns Duration#ZERO.)
        // No Expiry event is raised for this case.
        if (!valueMetaInf.isExpiredAt(ldtStart))
            {
            binValue = (Binary) ctx.addInternalValueDecoration(m_binValue, ExternalizableHelper.DECO_JCACHE,
                valueMetaInf);

            if (fExpired)
                {
                binValue = BinaryEntryHelper.jcacheSyntheticExpiryEventForReusedBinaryEntry(binValue, ctx);
                }

            binEntry.updateBinaryValue(binValue);

            stats.registerPuts(1, ldtStart);
            }

        return null;
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

    @Override
    public void readExternal(PofReader pofReader)
            throws IOException
        {
        super.readExternal(pofReader);
        m_binValue = pofReader.readBinary(1);

        }

    @Override
    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        super.writeExternal(pofWriter);
        pofWriter.writeBinary(1, m_binValue);
        }

    // ----- data members ---------------------------------------------------

    private Binary m_binValue;
    }
