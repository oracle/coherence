/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache.processors;

import com.tangosol.coherence.jcache.common.JCacheIdentifier;

import com.tangosol.util.BinaryEntry;

import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;

/**
 * Share operations common to remove entry processors.
 *
 * @author jf  2013.9.24
 * @since Coherence 12.1.3
 *
 * @param <K>
 * @param <V>
 */
public abstract class AbstractRemoveProcessor<K, V>
        extends AbstractEntryProcessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * For POF
     */
    AbstractRemoveProcessor()
        {
        super();
        }

    /**
     * Construct a common Removal
     *
     * @param id  Coherence JCache identifier
     */
    AbstractRemoveProcessor(JCacheIdentifier id)
        {
        super(id);
        }

    // ----- AbstractRemoveProcessor methods --------------------------------

    /**
     * Delete <code>binEntry</code> key from external resource.
     *
     * @param binEntry the entry to delete
     */
    protected void deleteCacheEntry(BinaryEntry binEntry)
        {
        CacheWriter writer = BinaryEntryHelper.getContext(m_cacheId, binEntry).getCacheWriter();

        if (writer != null)
            {
            try
                {
                writer.delete(binEntry.getKey());
                }
            catch (UnsupportedOperationException e)
                {
                // Have to wrapper UnsupportedOperationException since Coherence impl detects and
                // disables write-through erase impacting the entry.remove().
                // For JCache implementation, desire that Coherence reverts the entry.remove() so nest the
                // UnsupportedOperationException in JCache CacheWriterException so Coherence works as it needs
                // to for JCache.  This effectively causes the Cache.remove(K) to not occur due to write-through
                // failure. Just as specified in CacheWriter.delete(K).
                throw new CacheWriterException("CacheWriter implementation " + writer.getClass().getCanonicalName()
                                               + ".delete threw an exception", e);
                }
            }
        }
    }
