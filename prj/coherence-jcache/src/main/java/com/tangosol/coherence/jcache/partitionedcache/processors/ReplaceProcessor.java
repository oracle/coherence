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
 * JCache Replace EntryProcessor.
 *
 * @param <K> key type
 *
 * @author jf  2013.12.18
 * @since Coherence 12.1.3
 */
public class ReplaceProcessor<K>
        extends AbstractEntryProcessor
    {
    /**
     * Constructs ...
     *
     */
    public ReplaceProcessor()
        {
        super();
        m_binValue = new Binary();
        }

    // ----- constructors -----------------------------------------------------

    /**
     * Constructs a JCache replace entry processor
     *
     * @param binValue new value
     * @param id  unique JCache cache identifier
     */
    public ReplaceProcessor(Binary binValue, JCacheIdentifier id)
        {
        super(id);
        m_binValue = binValue;
        }

    // ----- InvocableMap.EntryProcessor interface ----------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        long                     ldtStart  = Helper.getCurrentTimeMillis();
        BinaryEntry              binEntry  = (BinaryEntry) entry;
        BackingMapManagerContext ctx       = binEntry.getContext();
        Boolean                  fReplaced = Boolean.FALSE;
        JCacheContext            jcacheCtx = BinaryEntryHelper.getContext(m_cacheId, binEntry);
        JCacheStatistics         stats     = jcacheCtx.getStatistics();

        if (binEntry.isPresent())
            {
            JCacheEntryMetaInf valueMetaInf = BinaryEntryHelper.getValueMetaInf(binEntry);

            if (BinaryEntryHelper.isExpired(binEntry, valueMetaInf, ldtStart))
                {
                BinaryEntryHelper.expireEntry(binEntry);
                }
            else
                {
                // modify mode
                valueMetaInf.modified(ldtStart, jcacheCtx.getExpiryPolicy());

                // is next line needed ?? since it is add rather than set or replace, thought next line was necessary.
                // ctx.removeInternalValueDecoration(binValueOrig, DECO_JCACHE);

                Binary binValue = (Binary) ctx.addInternalValueDecoration(m_binValue, ExternalizableHelper.DECO_JCACHE,
                                      valueMetaInf);

                binEntry.updateBinaryValue(binValue);
                fReplaced = Boolean.TRUE;
                }
            }

        if (fReplaced)
            {
            stats.registerHits(1, ldtStart);
            stats.registerPuts(1, ldtStart);
            }
        else
            {
            stats.registerMisses(1, ldtStart);
            }

        return fReplaced;

        }

    // ----- ExternalizableLite interface -------------------------------------

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

    // ----- PortableObject interface -----------------------------------------

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

    // ----- data members -----------------------------------------------------

    private Binary m_binValue;
    }
