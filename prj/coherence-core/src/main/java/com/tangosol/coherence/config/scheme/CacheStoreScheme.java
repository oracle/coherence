/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.BuilderCustomization;
import com.tangosol.coherence.config.builder.MapBuilder.Dependencies;
import com.tangosol.coherence.config.builder.NamedCacheBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.NullImplementation;

/**
 * The {@link CacheStoreScheme} class is responsible for building a fully
 * configured instance of a CacheStore, CacheLoader or remote NamedCache.
 * The remote cache is only used within a ReadWriteBackingMap scheme.  Also,
 * even though bundling is specified in the CacheStore scheme, it is not used
 * here.  Rather, it is used by {@link ReadWriteBackingMapScheme},
 * which contains a {@link CacheStoreScheme}.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class CacheStoreScheme
        extends AbstractScheme
        implements BuilderCustomization<Object>, BundlingScheme
    {
    // ----- CacheStoreScheme methods  --------------------------------------

    /**
     * Return an instance of a CacheStore, CacheLoader or a BinaryEntryStore.
     *
     * @param resolver      the {@link ParameterResolver}
     * @param dependencies  the {@link Dependencies}
     *
     * @return an instance of CacheStore, CacheLoader or BinaryEntryStore
     */
    public Object realizeLocal(ParameterResolver resolver, Dependencies dependencies)
        {
        ParameterizedBuilder<Object> bldr = getCustomBuilder();

        return bldr == null ? bldr : bldr.realize(resolver, dependencies.getClassLoader(), null);
        }

    /**
     * Realize (ensure) a remote NamedCache, CacheStore, CacheLoader, or BinaryEntryStore
     * as specified by the builder. The returned cache is fully configured and ready to be used.
     *
     * @param resolver      the ParameterResolver
     * @param dependencies  the Dependencies
     *
     * @return the NamedCache, CacheStore, CacheLoader, or BinaryEntryStore
     */
    public Object realize(ParameterResolver resolver, Dependencies dependencies)
        {
        Object obj = realizeLocal(resolver, dependencies);

        if (obj == null)
            {
            RemoteCacheScheme schemeRemote    = getRemoteCacheScheme();

            NamedCacheBuilder bldrRemoteCache = schemeRemote;

            if (bldrRemoteCache != null)
                {
                Dependencies depRemoteCache = new Dependencies(dependencies.getConfigurableCacheFactory(), null,
                                                  NullImplementation.getClassLoader(), dependencies.getCacheName(),
                                                  null);

                NamedCache cacheRemote = bldrRemoteCache.realizeCache(resolver, depRemoteCache);

                if (cacheRemote != null)
                    {
                    CacheService serviceThis = dependencies.getBackingMapManagerContext().getCacheService();

                    if (!isSerializerCompatible(cacheRemote.getCacheService(), serviceThis))
                        {
                        ExternalizableHelper.reportIncompatibleSerializers(cacheRemote,
                            serviceThis.getInfo().getServiceName(), serviceThis.getSerializer());
                        cacheRemote.release();
                        cacheRemote = bldrRemoteCache.realizeCache(resolver, dependencies);

                        }

                    obj = cacheRemote;
                    }
                }
            }

        return obj;
        }

    /**
     * Set the {@link BundleManager}.
     *
     * @param mgrBundle  the BundleManager
     */
    @Injectable("operation-bundling")
    public void setBundleManager(BundleManager mgrBundle)
        {
        m_mgrBundle = mgrBundle;
        }

    /**
     * Return the {@link RemoteCacheScheme}.
     *
     * @return the {@link RemoteCacheScheme}
     */
    public RemoteCacheScheme getRemoteCacheScheme()
        {
        return m_schemeRemoteCache;
        }

    /**
     * Set the {@link RemoteCacheScheme}.
     *
     * @param bldr  the {@link RemoteCacheScheme}
     */
    @Injectable("remote-cache-scheme")
    public void setRemoteCacheScheme(RemoteCacheScheme bldr)
        {
        m_schemeRemoteCache = bldr;
        }

    // ----- BundlingScheme  methods ----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public BundleManager getBundleManager()
        {
        return m_mgrBundle;
        }

    // ----- BuilderCustomization methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    public ParameterizedBuilder<Object> getCustomBuilder()
        {
        return m_bldrCacheStore;
        }

    /**
     * {@inheritDoc}
     */
    public void setCustomBuilder(ParameterizedBuilder<Object> bldr)
        {
        m_bldrCacheStore = bldr;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Determines whether or not the serializers for the specified services are
     * compatible.  In other words, this method returns true if object
     * serialized with the first Serializer can be deserialized by the second
     * and visa versa.
     *
     * @param serviceThis  the first Service
     * @param serviceThat  the second Service
     *
     * @return true if the two Serializers are stream compatible
     */
    protected boolean isSerializerCompatible(Service serviceThis, Service serviceThat)
        {
        return ExternalizableHelper.isSerializerCompatible(serviceThis.getSerializer(), serviceThat.getSerializer());
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link ParameterizedBuilder} for the cache store
     */
    private ParameterizedBuilder<Object> m_bldrCacheStore;

    /**
     * The {@link BundleManager}.
     */
    private BundleManager m_mgrBundle;

    /**
     * The {@link RemoteCacheScheme}.
     */
    private RemoteCacheScheme m_schemeRemoteCache;
    }
