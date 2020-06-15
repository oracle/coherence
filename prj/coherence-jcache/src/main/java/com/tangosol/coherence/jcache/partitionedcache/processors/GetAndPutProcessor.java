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

import java.util.Map;
import java.util.Set;

/**
 * JCache GetAndPutProcessor Entry Processor
 *
 * @param <K> key type
 *
 * @author jf  2013.12.18
 * @since Coherence 12.1.3
 */
public class GetAndPutProcessor<K>
        extends AbstractEntryProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Added due to ExternalizableLite
     */
    public GetAndPutProcessor()
        {
        super();
        m_binValue = new Binary();

        }

    /**
     * Constructs a JCache GetAndPutProcessor
     *
     *
     * @param binValue the value to put
     * @param id       the JCache cache unique identifier
     */
    public GetAndPutProcessor(Binary binValue, JCacheIdentifier id)
        {
        super(id);
        m_binValue = binValue;
        }

    // ----- InvocableMap.EntryProcessor interface --------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        long                     ldtStart     = Helper.getCurrentTimeMillis();
        BinaryEntry              binEntry     = (BinaryEntry) entry;
        JCacheContext            jcacheCtx    = BinaryEntryHelper.getContext(m_cacheId, binEntry);
        JCacheStatistics         stats        = jcacheCtx.getStatistics();
        Binary                   binValueOrig = binEntry.isPresent() ? binEntry.getBinaryValue() : null;
        JCacheEntryMetaInf       valueMetaInf = null;
        BackingMapManagerContext ctx          = binEntry.getContext();
        Binary                   binValue;
        boolean                  fExpired = false;

        if (binEntry.isPresent())
            {
            valueMetaInf = BinaryEntryHelper.getValueMetaInf(binEntry);

            if (BinaryEntryHelper.isExpired(binEntry, valueMetaInf, ldtStart))
                {
                binValueOrig = null;
                valueMetaInf = null;

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
            }

        if (binValueOrig == null)
            {
            // create mode
            valueMetaInf = new JCacheEntryMetaInf(ldtStart, jcacheCtx.getExpiryPolicy());

            // as documented in javax.cache.expiry.ExpiryPolicy.getExpiryForCreation,
            // do not add created entry to cache if it is already expired.
            // (occurs when ExpiryPolicy.getExpiryForCreation method returns Duration#ZERO.)
            // No Expiry event is raised for this case.
            if (valueMetaInf.isExpiredAt(ldtStart))
                {
                stats.registerMisses(1, ldtStart);

                return binValueOrig;
                }

            binValue = (Binary) ctx.addInternalValueDecoration(m_binValue, ExternalizableHelper.DECO_JCACHE,
                valueMetaInf);

            if (fExpired)
                {
                binValue = BinaryEntryHelper.jcacheSyntheticExpiryEventForReusedBinaryEntry(binValue, ctx);
                }
            }
        else
            {
            // update mode
            valueMetaInf.modified(ldtStart, jcacheCtx.getExpiryPolicy());

            // is next line needed ?? since it is add rather than set or replace, thought next line was necessary.
            binValueOrig = (Binary) ctx.removeInternalValueDecoration(binValueOrig, ExternalizableHelper.DECO_JCACHE);
            binValueOrig = (Binary) ctx.removeInternalValueDecoration(binValueOrig,
                ExternalizableHelper.DECO_JCACHE_SYNTHETIC);
            binValue = (Binary) ctx.addInternalValueDecoration(m_binValue, ExternalizableHelper.DECO_JCACHE,
                valueMetaInf);
            }

        binEntry.updateBinaryValue(binValue);

        if (binValueOrig == null)
            {
            stats.registerMisses(1, ldtStart);
            }
        else
            {
            stats.registerHits(1, ldtStart);
            }

        stats.registerPuts(1, ldtStart);

        return binValueOrig;
        }

    @Override
    public Map processAll(Set setEntries)
        {
        throw new UnsupportedOperationException();
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
