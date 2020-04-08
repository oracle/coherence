/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache.processors;

import com.tangosol.coherence.jcache.CoherenceBasedCache;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.cache.configuration.CompleteConfiguration;

/**
 * Common operations shared by Coherence JCache PartitionedCache Entry Processor implementations.
 *
 * @author jf  2013.12.17
 * @since Coherence 12.1.3
 *
 * @param <K>
 */
public abstract class AbstractEntryProcessor<K>
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject

    {
    // ----- constructors ---------------------------------------------------

    /**
     * required for Portable Object
     */
    AbstractEntryProcessor()
        {
        m_cacheId = null;
        }

    /**
     * Construct the EntryProcessor context with a Coherence JCache identifier.
     * @param cacheId  Coherence JCache Adapter cache identifier
     */
    AbstractEntryProcessor(JCacheIdentifier cacheId)
        {
        m_cacheId = cacheId;
        }

    // ----- AbstractEntryProcessor methods ---------------------------------

    /**
     * Obtains the {@link NamedCache} to be used for storing
     * {@link com.tangosol.coherence.jcache.CoherenceBasedConfiguration}s when they
     * need to be shared across a cluster or made available to clients.
     *
     * @param binEntry  the {@link BinaryEntry} to use to locate the appropriate Configuration cache
     *
     * @return  the {@link com.tangosol.net.NamedCache} for {@link com.tangosol.coherence.jcache.CoherenceBasedConfiguration}s
     */
    protected NamedCache getConfigurationCache(BinaryEntry binEntry)
        {
        // we don't specify a serializer to allow Coherence to use the service-level classloader
        ClassLoader loader = null;

        // determine the CCF
        ConfigurableCacheFactory ccf = binEntry.getContext().getManager().getCacheFactory();

        // acquire the NamedCache that holds the JCache Configurations (this will be on a separate service)
        return ccf.ensureCache(CoherenceBasedCache.JCACHE_CONFIG_CACHE_NAME, loader);
        }

    /**
     * Provide access to meta cache that maps JCache cache names to JCache configuration.
     * The mapping is a string to a JCache Configuration object.
     *
     * @param binEntry  the {@link BinaryEntry} to use to locate the appropriate Configuration cache
     * @param cacheId   JCache unique identifier
     *
     * @return a named cache that maps JCache cache names to JCache configurations.
     */
    protected CompleteConfiguration getCacheToConfigurationMapping(BinaryEntry binEntry, JCacheIdentifier cacheId)
        {
        return (CompleteConfiguration) getConfigurationCache(binEntry).get(cacheId.getCanonicalCacheName());
        }

    /**
     * Mark binEntry with JCache synthetic update clear.
     *
     * @param binEntry binary entry
     */
    public void clearEntry(BinaryEntry binEntry)
        {
        binEntry.remove(true);
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput dataInput)
            throws IOException
        {
        m_cacheId = (JCacheIdentifier) ExternalizableHelper.readObject(dataInput);
        }

    @Override
    public void writeExternal(DataOutput dataOutput)
            throws IOException
        {
        ExternalizableHelper.writeObject(dataOutput, m_cacheId);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader pofReader)
            throws IOException
        {
        m_cacheId = (JCacheIdentifier) pofReader.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter pofWriter)
            throws IOException
        {
        pofWriter.writeObject(0, m_cacheId);
        }

    // ----- data members ---------------------------------------------------

    protected JCacheIdentifier m_cacheId;
    }
