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
 * JCache ConditionalRemoveProcessor - only remove entry if its value matches the specified value.
 *
 * @param <K>
 * @param <V>
 *
 * @author jf  2013.12.18
 * @since Coherence 12.1.3
 *
 */
public class ConditionalRemoveProcessor<K, V>
        extends AbstractRemoveProcessor<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs {@link ConditionalRemoveProcessor}
     *
     */
    public ConditionalRemoveProcessor()
        {
        super();
        m_binValueOrig = new Binary();
        }

    /**
     *
     * Constructs a {@link ConditionalRemoveProcessor}
     *
     * @param binValueOrig  only remove the entry if its value matches this parameter's value
     * @param id            unique JCache Cache identifier
     */
    public ConditionalRemoveProcessor(Binary binValueOrig, JCacheIdentifier id)
        {
        super(id);
        m_binValueOrig = binValueOrig;
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
        Binary                   binValue  = binEntry.isPresent() ? binEntry.getBinaryValue() : null;
        Boolean                  fResult   = Boolean.FALSE;

        if (binEntry.isPresent() && binValue != null)
            {
            // must strip decorations before comparing if value to remove equals value passed to remove method.
            binValue = (Binary) ctx.removeInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE);
            binValue = (Binary) ctx.removeInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE_SYNTHETIC);

            if (BinaryEntryHelper.isExpired(binEntry, ldtStart))
                {
                BinaryEntryHelper.expireEntry(binEntry);

                fResult = Boolean.FALSE;

                stats.registerMisses(1, ldtStart);
                }
            else if (binValue.equals(m_binValueOrig))
                {
                binEntry.remove(false);

                fResult = Boolean.TRUE;

                stats.registerHits(1, ldtStart);
                }
            else
                {
                JCacheEntryMetaInf valueMetaInf = BinaryEntryHelper.getValueMetaInf(binEntry);

                valueMetaInf.accessed(ldtStart, jcacheCtx.getExpiryPolicy());

                binValue = BinaryEntryHelper.decorateBinValueWithJCacheMetaInf(binValue, valueMetaInf, ctx);
                binValue = BinaryEntryHelper.jcacheSyntheticUpdateEntry(binValue, ctx);

                // must update value due to decorate update of JCACHE metaInfo for value.  Should be synthetic update in
                // get. (not working the Coherence Update listener does get fired even though no change in user visible
                // value, only meta info change to reflect this access via get.
                // used to be updateBinaryValue(binValue, true (isSynthetic)) but 3.7.1 does not have this method.
                // adding isSynthetic by implementing in the adapter now since isSynthetic in 12.2.1 and up was not
                // sufficient for our use case.
                binEntry.updateBinaryValue(binValue);
                stats.registerHits(1, ldtStart);
                }

            }
        else
            {
            stats.registerMisses(1, ldtStart);
            }

        if (fResult)
            {
            stats.registerRemoves(1, ldtStart);
            }

        return fResult;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput dataInput)
            throws IOException
        {
        super.readExternal(dataInput);
        m_binValueOrig.readExternal(dataInput);
        }

    @Override
    public void writeExternal(DataOutput dataOutput)
            throws IOException
        {
        super.writeExternal(dataOutput);
        m_binValueOrig.writeExternal(dataOutput);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader pofReader)
            throws IOException
        {
        super.readExternal(pofReader);
        m_binValueOrig = pofReader.readBinary(2);
        }

    @Override
    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        super.writeExternal(pofWriter);
        pofWriter.writeBinary(2, m_binValueOrig);
        }

    // ----- data members ---------------------------------------------------

    private Binary m_binValueOrig;
    }
