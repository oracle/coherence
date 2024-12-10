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
 * JCache Conditional Replace Processor - only perform replace if value matches specified orginal value.
 *
 * @author jf  2013.12.18
 * @since Coherence 12.1.3
 *
 *
 * @param <K>
 */
public class ConditionalReplaceProcessor<K>
        extends AbstractEntryProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Added due to ExternalizableLite support.
     *
     */
    public ConditionalReplaceProcessor()
        {
        super();
        m_binValueOrig = new Binary();
        m_binValue     = new Binary();
        }

    /**
     * Constructs Conditional Replace Entry Procesor
     *
     *
     * @param binValueOrig  perform replace if current value is equal to this value
     * @param binValueNew   replacement value for entry
     * @param id            JCache cache identifier (to look up JCacheContext to use)
     */
    public ConditionalReplaceProcessor(Binary binValueOrig, Binary binValueNew, JCacheIdentifier id)
        {
        super(id);
        m_binValueOrig = binValueOrig;
        m_binValue     = binValueNew;
        }

    // ----- InvocableMap.EntryProcessor interface --------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        long             ldtStart  = Helper.getCurrentTimeMillis();
        BinaryEntry      binEntry  = (BinaryEntry) entry;
        Boolean          fReplaced = Boolean.FALSE;
        JCacheContext    jcacheCtx = BinaryEntryHelper.getContext(m_cacheId, binEntry);
        JCacheStatistics stats     = jcacheCtx.getStatistics();

        if (binEntry.isPresent())
            {
            BackingMapManagerContext ctx          = binEntry.getContext();
            JCacheEntryMetaInf       valueMetaInf = BinaryEntryHelper.getValueMetaInf(binEntry);
            Binary binValueOrig = (Binary) ctx.removeInternalValueDecoration(binEntry.getBinaryValue(),
                                      ExternalizableHelper.DECO_JCACHE);

            binValueOrig = (Binary) ctx.removeInternalValueDecoration(binValueOrig,
                ExternalizableHelper.DECO_JCACHE_SYNTHETIC);

            if (BinaryEntryHelper.isExpired(binEntry, valueMetaInf, ldtStart))
                {
                BinaryEntryHelper.expireEntry(binEntry);
                stats.registerMisses(1, ldtStart);
                }
            else if (binValueOrig.equals(m_binValueOrig))
                {
                valueMetaInf.modified(ldtStart, jcacheCtx.getExpiryPolicy());

                // add the meta information back to the entry
                Binary binValue = (Binary) ctx.addInternalValueDecoration(m_binValue, ExternalizableHelper.DECO_JCACHE,
                                      valueMetaInf);

                binEntry.updateBinaryValue(binValue);
                fReplaced = Boolean.TRUE;
                stats.registerHits(1, ldtStart);
                stats.registerPuts(1, ldtStart);
                }
            else
                {
                // ExpiryPolicy table in JSR 107 spec states that if replace does not occur, that it is considered an access.
                // This update is only considered a synthetic update and the entry's value has not been replaced.
                fReplaced = Boolean.FALSE;

                valueMetaInf.accessed(ldtStart, jcacheCtx.getExpiryPolicy());

                binValueOrig = BinaryEntryHelper.decorateBinValueWithJCacheMetaInf(binValueOrig, valueMetaInf, ctx);
                binValueOrig = BinaryEntryHelper.jcacheSyntheticUpdateEntry(binValueOrig, ctx);

                // must update value due to decorate update of JCACHE metaInfo for value.  Should be synthetic update in
                // get. (not working the Coherence Update listener does get fired even though no change in user visible
                // value, only meta info change to reflect this access via get.
                // used to be updateBinaryValue(binValue, true (isSynthetic)) but 3.7.1 does not have this method.
                // adding isSynthetic by implementing in the adapter now since isSynthetic in 12.2.1 and up was not
                // sufficient for our use case.
                binEntry.updateBinaryValue(binValueOrig);
                stats.registerHits(1, ldtStart);
                }
            }
        else
            {
            stats.registerMisses(1, ldtStart);
            }

        return fReplaced;

        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput dataInput)
            throws IOException
        {
        super.readExternal(dataInput);
        m_binValueOrig.readExternal(dataInput);
        m_binValue.readExternal(dataInput);

        }

    @Override
    public void writeExternal(DataOutput dataOutput)
            throws IOException
        {
        super.writeExternal(dataOutput);
        m_binValueOrig.writeExternal(dataOutput);
        m_binValue.writeExternal(dataOutput);

        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader pofReader)
            throws IOException
        {
        super.readExternal(pofReader);
        m_binValueOrig = pofReader.readBinary(1);
        m_binValue     = pofReader.readBinary(2);

        }

    @Override
    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        super.writeExternal(pofWriter);
        pofWriter.writeBinary(1, m_binValueOrig);
        pofWriter.writeBinary(2, m_binValue);

        }

    // ----- data members ---------------------------------------------------
    private Binary m_binValueOrig;
    private Binary m_binValue;
    }
