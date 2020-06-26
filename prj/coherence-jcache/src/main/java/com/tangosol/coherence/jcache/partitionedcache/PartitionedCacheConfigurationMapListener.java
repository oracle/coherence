/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.jcache.CoherenceBasedCache;
import com.tangosol.coherence.jcache.common.JCacheContext;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;
import com.tangosol.coherence.jcache.common.SerializingInternalConverter;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.ResourceRegistry;

import javax.cache.expiry.ExpiryPolicy;
import java.io.Closeable;
import java.io.IOException;

/**
 * MapListener for meta-information about JCache {@link PartitionedCache} instances.
 * This cache is named {@link CoherenceBasedCache#JCACHE_CONFIG_CACHE_NAME}.
 * This cache is a meta-cache where the key is a JCacheIdentifier for a {@link PartitionedCache}
 * and the value is the PartitionedCacheConfiguration.
 *
 * This listener manages the JCacheContext in storage-enabled nodes for {@link CoherenceBasedCache#JCACHE_PARTITIONED_SCHEME_NAME}.
 *
 * This maplistener is registered on each near-cache of {@link CoherenceBasedCache#JCACHE_CONFIG_SCHEME_NAME}.
 * The map listener is required to execute on each storage-enabled node that has accessed the JCacheContext for
 * the map's key, a JCacheIdentifier.
 *
 * @author jf  2014.04.15
 * @since Coherence 12.1.3
 */

public class PartitionedCacheConfigurationMapListener
        extends AbstractMapListener
        implements MapListenerSupport.SynchronousListener
    {
    // ----- MapListener interface ------------------------------------------

    /**
     * Remove JCacheContext for JCache identified by evt.getKey().
     *
     * @param evt  the key of this map event identifies which JCacheContext needs to be unregistered.
     */
    @Override
    public void entryDeleted(MapEvent evt)
        {
        JCacheIdentifier jcacheId = getJCacheIdentifierFromKey(evt);
        JCacheContext    ctx      = JCacheContext.getContext(m_registry, jcacheId);

        if (ctx != null)
            {
            // close the configured CacheLoader
            if (ctx.getCacheLoader() instanceof Closeable)
                {
                try
                    {
                    ((Closeable) ctx.getCacheLoader()).close();
                    }
                catch (IOException e)
                    {
                    Logger.fine("Unexpected exception in closable CacheLoader: ", e);
                    }
                }

            // close the configured CacheWriter
            if (ctx.getCacheWriter() instanceof Closeable)
                {
                try
                    {
                    ((Closeable) ctx.getCacheWriter()).close();
                    }
                catch (IOException e)
                    {
                    Logger.fine("Unexpected exception in closable CacheWriter: ", e);
                    }
                }

            if (ctx.getExpiryPolicy() instanceof Closeable)
                {
                try
                    {
                    ((Closeable) ctx.getExpiryPolicy()).close();
                    }
                catch (IOException e)
                    {
                    Logger.fine("Unexpected exception in closable ExpiryPolicy: ", e);
                    }
                }

            JCacheContext.unregister(m_registry, jcacheId);
            }

        if (Logger.isEnabled(Logger.FINEST))
            {
            String unregisterLogMessage = ctx == null
                                          ? " no JCacheContext found in registry " + m_registry
                                          : " unregister JCacheContext from resource registry " + m_registry;

            Logger.finest("PartitionedCacheConfigurationMap: entryDeleted event JCacheId=" + jcacheId
                             + unregisterLogMessage + " configuration=" + evt.getOldValue());
            }
        }

    /**
     * Inject {@link ResourceRegistry} associated with the {@link com.tangosol.net.ConfigurableCacheFactory} that
     * created the cache that this maplistener is added to.
     *
     * @param registry  the {@link ResourceRegistry}
     */
    @Injectable
    public void setResourceRegistry(ResourceRegistry registry)
        {
        m_registry = registry;
        }

    // ----- helper ---------------------------------------------------------

    /**
     * Return JCacheIdentifier from {code}mapEvent{/code} key.
     * Deserializes key when necessary.
     *
     * @param mapEvent PartitiionedCacheConfiguration mapEvent with key of jcache identfier and value of
     *                 its JCache configuration
     *
     * @return {@link JCacheIdentifier} for the {code}mapEvent{/code} key.
     */
    private JCacheIdentifier getJCacheIdentifierFromKey(MapEvent mapEvent)
        {
        String sJCacheId = null;
        Object key       = mapEvent.getKey();

        if (key instanceof String)
            {
            sJCacheId = (String) key;
            }
        else if (key instanceof Binary)
            {
            // deserialize
            if (m_converterKey == null)
                {
                SerializerFactory factorySerializer = m_registry.getResource(SerializerFactory.class, "serializer");
                Serializer        serializer;

                if (factorySerializer == null)
                    {
                    // when there's no default serializer in the ResourceRegistry we use the
                    // ExternalizableHelper to determine a suitable serializer
                    serializer = ExternalizableHelper.ensureSerializer(null);
                    }
                else
                    {
                    serializer = factorySerializer.createSerializer(null);
                    }

                m_converterKey = new SerializingInternalConverter<String>(serializer);
                }

            sJCacheId = m_converterKey.fromInternal(key);
            }

        return new JCacheIdentifier(sJCacheId);
        }

    // ----- data members ---------------------------------------------------

    private ResourceRegistry                     m_registry;
    private SerializingInternalConverter<String> m_converterKey;
    }
