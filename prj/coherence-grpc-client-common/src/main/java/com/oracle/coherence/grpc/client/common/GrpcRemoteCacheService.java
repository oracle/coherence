/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common;

import com.google.protobuf.Message;
import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.grpc.NamedCacheProtocol;
import com.oracle.coherence.grpc.messages.cache.v1.NamedCacheResponse;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;

import com.tangosol.internal.net.NamedCacheDeactivationListener;
import com.tangosol.internal.net.grpc.RemoteGrpcCacheServiceDependencies;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.RequestTimeoutException;

import com.tangosol.net.events.EventDispatcherRegistry;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.IteratorEnumerator;
import com.tangosol.util.MapEvent;
import com.tangosol.util.ResourceRegistry;

import io.grpc.Channel;
import io.grpc.ClientInterceptors;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.function.IntPredicate;

/**
 * A remote cache service that accesses caches via a remote gRPC proxy.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
@SuppressWarnings({"rawtypes", "PatternVariableCanBeUsed"})
public class GrpcRemoteCacheService
        extends GrpcRemoteService<RemoteGrpcCacheServiceDependencies>
        implements CacheService
    {
    public GrpcRemoteCacheService()
        {
        super(CacheService.TYPE_REMOTE_GRPC);
        }

    @Override
    protected Class<? extends Message> getResponseType()
        {
        return NamedCacheResponse.class;
        }

    @Override
    public BackingMapManager getBackingMapManager()
        {
        return m_backingMapManager;
        }

    @Override
    public void setBackingMapManager(BackingMapManager manager)
        {
        m_backingMapManager = manager;
        }

    @Override
    public NamedCache ensureCache(String sName, ClassLoader ignored)
        {
        ClassLoader loader = getContextClassLoader();
        if (loader == null)
            {
            throw new IllegalStateException("ContextClassLoader is missing");
            }

        AsyncNamedCacheClient<?, ?> cache = m_scopedCacheStore.get(sName, loader);
        if (cache == null || !cache.isActiveInternal())
            {
            // this is one of the few places that acquiring a distinct lock per cache
            // is beneficial due to the potential cost of createRemoteNamedCache
            long cWait = getDependencies().getRequestTimeoutMillis();
            if (cWait <= 0)
                {
                cWait = -1;
                }
            if (!m_scopedCacheStore.lock(sName, cWait))
                {
                throw new RequestTimeoutException("Failed to get a reference to cache '" +
                    sName + "' after " + cWait + "ms");
                }
            try
                {
                cache = (AsyncNamedCacheClient) m_scopedCacheStore.get(sName, loader);
                if (cache == null || !cache.isActiveInternal())
                    {
                    cache = ensureAsyncCache(sName);
                    m_scopedCacheStore.put(cache, loader);
                    }
                }
            finally
                {
                m_scopedCacheStore.unlock(sName);
                }
            }

        return cache.getNamedCache();
        }

    @Override
    public Enumeration getCacheNames()
        {
        // instead of synchronizing on the Map and blocking all
        // the "put" and "remove" operations, we just catch any
        // ConcurrentModificationException and try again
        return new IteratorEnumerator(Arrays.asList(m_scopedCacheStore.getNames().toArray()).iterator());
        }

    @Override
    public void releaseCache(NamedCache map)
        {
        if (!(map instanceof NamedCacheClient<?,?>))
            {
            throw new IllegalArgumentException("illegal map: " + map);
            }
        map.release();
        NamedCacheClient<?,?> cache = (NamedCacheClient<?,?>) map;
        m_scopedCacheStore.release(cache.getAsyncClient());
        }

    @Override
    public void destroyCache(NamedCache map)
        {
        if (!(map instanceof NamedCacheClient<?,?>))
            {
            throw new IllegalArgumentException("illegal map: " + map);
            }
        map.destroy();
        NamedCacheClient<?,?> cache = (NamedCacheClient<?,?>) map;
        m_scopedCacheStore.release(cache.getAsyncClient());
        }

    @Override
    public boolean isVersionCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nMajor, nMinor, nMicro, nPatchSet, nPatch);
        return CacheFactory.VERSION_ENCODED >= nEncoded;
        }

    @Override
    public boolean isVersionCompatible(int nYear, int nMonth, int nPatch)
        {
        int nEncoded = ServiceMemberSet.encodeVersion(nYear, nMonth, nPatch);
        return CacheFactory.VERSION_ENCODED >= nEncoded;
        }

    @Override
    public boolean isVersionCompatible(int nVersion)
        {
        return CacheFactory.VERSION_ENCODED >= nVersion;
        }

    @Override
    public boolean isVersionCompatible(IntPredicate predicate)
        {
        return predicate.test(CacheFactory.VERSION_ENCODED);
        }

    @Override
    public int getMinimumServiceVersion()
        {
        return CacheFactory.VERSION_ENCODED;
        }

    /**
     * Return {@code true} if the key association check is deferred to the proxy server.
     *
     *  @return {@code true} if the key association check is deferred to the proxy server
     */
    public boolean isDeferKeyAssociationCheck()
        {
        return getDependencies().isDeferKeyAssociationCheck();
        }

    // ----- helper methods -------------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    protected void stopInternal()
        {
        for (AsyncNamedCacheClient<?, ?> cache : m_scopedCacheStore.getAll())
            {
            cache.removeDeactivationListener(f_deactivationListener);
            try
                {
                cache.release();
                }
            catch (Throwable e)
                {
                Logger.err(e);
                }
            }
        }

    @Override
    protected EventDispatcherRegistry getDefaultEventDispatcherRegistry()
        {
        ResourceRegistry registry = getBackingMapManager().getCacheFactory().getResourceRegistry();
        return registry.getResource(EventDispatcherRegistry.class);
        }

    /**
     * Creates a {@link AsyncNamedCacheClient} based on the provided arguments.
     *
     * @param sCacheName  the cache name
     * @param <K>         the key type
     * @param <V>         the value type
     *
     * @return a new {@link AsyncNamedCacheClient}
     */
    @SuppressWarnings("unchecked")
    protected <K, V> AsyncNamedCacheClient<K, V> ensureAsyncCache(String sCacheName)
        {
        GrpcCacheLifecycleEventDispatcher dispatcher = new GrpcCacheLifecycleEventDispatcher(sCacheName, this);

        Channel channel = m_tracingInterceptor == null
                ? m_channel
                : ClientInterceptors.intercept(m_channel, m_tracingInterceptor);

        AsyncNamedCacheClient.Dependencies dependencies = createCacheDependencies(sCacheName, channel, dispatcher);

        GrpcConnection connection = connect(NamedCacheProtocol.PROTOCOL_NAME,
                NamedCacheProtocol.VERSION, NamedCacheProtocol.SUPPORTED_VERSION);

        NamedCacheClientChannel     protocol = NamedCacheClientChannel.createProtocol(dependencies, connection);
        AsyncNamedCacheClient<K, V> client   = new AsyncNamedCacheClient<>(dependencies, protocol);
        EventDispatcherRegistry     dispatcherReg = getEventDispatcherRegistry();

        if (dispatcherReg != null)
            {
            dispatcherReg.registerEventDispatcher(dispatcher);
            }

        client.setCacheService(this);
        client.addDeactivationListener(f_truncateListener);
        client.addDeactivationListener(f_deactivationListener);

        // We must dispatch the created event async
        m_executor.execute(() -> dispatcher.dispatchCacheCreated(client.getNamedCache()));

        return client;
        }

    private AsyncNamedCacheClient.Dependencies createCacheDependencies(String sCacheName, Channel channel,
            GrpcCacheLifecycleEventDispatcher dispatcher)
        {
        RemoteGrpcCacheServiceDependencies dependencies = getDependencies();
        String                             sScopeName   = dependencies.getRemoteScopeName();

        if (sScopeName == null)
            {
            sScopeName = Coherence.DEFAULT_SCOPE;
            }

        AsyncNamedCacheClient.DefaultDependencies deps
                = new  AsyncNamedCacheClient.DefaultDependencies(sCacheName, channel, dispatcher);

        deps.setScope(sScopeName);
        deps.setSerializer(m_serializer, m_serializer.getName());
        deps.setExecutor(m_executor);
        deps.setDeferKeyAssociationCheck(dependencies.isDeferKeyAssociationCheck());
        deps.setDeadline(dependencies.getDeadline());
        deps.setHeartbeatMillis(dependencies.getHeartbeatInterval());
        deps.setRequireHeartbeatAck(dependencies.isRequireHeartbeatAck());
        return deps;
        }

    // ----- inner class: ClientDeactivationListener ------------------------

    /**
     * A {@link DeactivationListener} that cleans up the internal caches map
     * as clients are released or destroyed.
     */
    private class ClientDeactivationListener<K, V>
            implements DeactivationListener<AsyncNamedCacheClient<? super K, ? super V>>
        {
        // ----- DeactivationListener interface -----------------------------

        @Override
        public void released(AsyncNamedCacheClient<? super K, ? super V> client)
            {
            GrpcRemoteCacheService.this.m_scopedCacheStore.remove(client.getCacheName());
            }

        @Override
        public void destroyed(AsyncNamedCacheClient<? super K, ? super V> client)
            {
            GrpcRemoteCacheService service = GrpcRemoteCacheService.this;
            boolean                removed = service.m_scopedCacheStore.release(client);
            if (removed)
                {
                GrpcCacheLifecycleEventDispatcher dispatcher    = client.getEventDispatcher();
                EventDispatcherRegistry           dispatcherReg = service.getEventDispatcherRegistry();
                dispatcher.dispatchCacheDestroyed(client.getNamedCache());
                dispatcherReg.unregisterEventDispatcher(client.getEventDispatcher());
                }
            }
        }

    // ----- inner class: TruncateListener ----------------------------------

    /**
     * A {@link DeactivationListener} that cleans up the internal caches map
     * as clients are released or destroyed.
     */
    @SuppressWarnings("rawtypes")
    private class TruncateListener
            extends AbstractMapListener
            implements NamedCacheDeactivationListener
        {
        // ----- NamedCacheDeactivationListener interface -------------------

        @Override
        public void entryUpdated(MapEvent evt)
            {
            NamedCache                  cache  = (NamedCache) evt.getMap();
            AsyncNamedCacheClient<?, ?> client = m_scopedCacheStore.get(cache.getCacheName(), getContextClassLoader());
            if (client != null)
                {
                client.getEventDispatcher().dispatchCacheTruncated((NamedCache) evt.getMap());
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link DeactivationListener} that will clean up client instances that
     * are destroyed or released.
     */
    @SuppressWarnings("rawtypes")
    private final ClientDeactivationListener f_deactivationListener = new ClientDeactivationListener<>();

    /**
     * The {@link TruncateListener} that will raise lifecycle events when
     * a map is truncated.
     */
    private final TruncateListener f_truncateListener = new TruncateListener();

    private BackingMapManager m_backingMapManager;

    /**
     * The store of cache references, optionally scoped by Subject.
     */
    private final ScopedGrpcAsyncCacheReferenceStore m_scopedCacheStore = new ScopedGrpcAsyncCacheReferenceStore();
    }
