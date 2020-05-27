/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;

import com.google.protobuf.ByteString;

import com.oracle.coherence.grpc.proxy.client.BinaryHelper;
import com.oracle.coherence.grpc.proxy.client.CacheDestroyedResponse;
import com.oracle.coherence.grpc.proxy.client.CacheTruncatedResponse;
import com.oracle.coherence.grpc.proxy.client.MapEventResponse;
import com.oracle.coherence.grpc.proxy.client.MapListenerErrorResponse;
import com.oracle.coherence.grpc.proxy.client.MapListenerRequest;
import com.oracle.coherence.grpc.proxy.client.MapListenerResponse;
import com.oracle.coherence.grpc.proxy.client.MapListenerSubscribedResponse;
import com.oracle.coherence.grpc.proxy.client.MapListenerUnsubscribedResponse;

import com.tangosol.coherence.component.net.message.MapEventMessage;

import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.io.Serializer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.CacheEvent;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Binary;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.Converter;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.SegmentedConcurrentMap;
import com.tangosol.util.filter.InKeySetFilter;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A class to encapsulate bi-directional streaming of map events for a single cache.
 *
 * @author Jonathan Knight  2019.12.03
 * @since 14.1.2
 */
class MapListenerProxy
        implements StreamObserver<MapListenerRequest>, MapListener<Object, Object>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link MapListenerProxy} to handle a{@link com.tangosol.util.MapListener}
     * subscription to a cache.
     *
     * @param service   the {@link NamedCacheService} to proxy
     * @param observer  the {@link StreamObserver} to stream {@link com.tangosol.util.MapEvent}
     *                  instances to
     */
    @SuppressWarnings("unchecked")
    MapListenerProxy(NamedCacheService service, StreamObserver<MapListenerResponse> observer)
        {
        this.f_service              = service;
        this.f_observer             = observer;
        this.f_mapFilter            = new SegmentedConcurrentMap();
        this.f_mapKeys              = new SegmentedConcurrentMap();
        this.f_setKeys              = new HashSet<>();
        this.f_listenerDeactivation = new DeactivationListener(this);
        }

    // ----- StreamObserver methods -----------------------------------------

    @Override
    public synchronized void onNext(MapListenerRequest request)
        {
        try
            {
            if (m_holder == null)
                {
                m_holder = f_service.supplyHolderInternal(request, request.getCache(), request.getFormat());
                m_holder.getCache().addMapListener(f_listenerDeactivation);
                }
            else if (!m_holder.getCacheName().equals(request.getCache()))
                {
                throw new IllegalArgumentException("request for different cache name, original cache name is "
                                                   + m_holder.getCacheName()
                                                   + " requested cache name is "
                                                   + request.getCache());
                }

            boolean                    subscribe    = request.getSubscribe();
            ByteString                 triggerBytes = request.getTrigger();
            MapTrigger<Binary, Binary> trigger      = null;
            if (triggerBytes != null && !triggerBytes.isEmpty())
                {
                trigger = BinaryHelper.fromByteString(triggerBytes, m_holder.getSerializer());
                }

            switch (request.getType())
                {
                case KEY:
                    onKeyRequest(request, trigger);
                    break;
                case FILTER:
                    onFilterRequest(request, trigger);
                    break;
                case INIT:
                    subscribe = true;
                    break;
                case UNRECOGNIZED:
                default:
                    throw new IllegalArgumentException("unrecognised request type");
                }

            if (subscribe)
                {
                // notify the observer that the listener is subscribed.
                MapListenerSubscribedResponse subscribed = MapListenerSubscribedResponse.newBuilder()
                        .setUid(request.getUid())
                        .build();

                f_observer.onNext(MapListenerResponse.newBuilder().setSubscribed(subscribed).build());
                }
            else
                {
                // notify the observer that the listener is unsubscribed.
                MapListenerUnsubscribedResponse unsubscribed = MapListenerUnsubscribedResponse.newBuilder()
                        .setUid(request.getUid())
                        .build();

                f_observer.onNext(MapListenerResponse.newBuilder().setUnsubscribed(unsubscribed).build());
                }
            }
        catch (Throwable t)
            {
            CacheFactory.err(t);
            f_observer.onNext(MapListenerResponse.newBuilder().setError(error(request.getUid(), t)).build());
            }
        }

    @Override
    public synchronized void onError(Throwable throwable)
        {
        CacheFactory.err("Error received in MapListenerProxy onError");
        CacheFactory.err(throwable);
        try
            {
            removeAllListeners();
            }
        catch (Throwable t)
            {
            // ignored - we already have errors.
            }
        }

    @Override
    public void onCompleted()
        {
        if (!m_fCompleted)
            {
            synchronized (this)
                {
                if (!m_fCompleted)
                    {
                    m_fCompleted = true;
                    removeAllListeners();
                    f_observer.onCompleted();
                    m_holder = null;
                    }
                }
            }
        }

    // ----- MapListener methods --------------------------------------------

    @Override
    public void entryInserted(MapEvent<Object, Object> mapEvent)
        {
        onMapEvent(mapEvent);
        }

    @Override
    public void entryUpdated(MapEvent<Object, Object> mapEvent)
        {
        onMapEvent(mapEvent);
        }

    @Override
    public void entryDeleted(MapEvent<Object, Object> mapEvent)
        {
        onMapEvent(mapEvent);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the {@link DeactivationListener} for this proxy.
     *
     * @return the {@link DeactivationListener} for this proxy
     */
    protected MapListener<Object, Object> getDeactivationListener()
        {
        return f_listenerDeactivation;
        }

    /**
     * Invoked when {@link MapListenerRequest.RequestType} is {@link MapListenerRequest.RequestType#KEY KEY}.
     *
     * @param request  the {@link MapListenerRequest}
     * @param trigger  the {@link MapTrigger}
     */
    @SuppressWarnings("unchecked")
    protected void onKeyRequest(MapListenerRequest request, MapTrigger<?, ?> trigger)
        {
        Object key = m_holder.deserializeRequest(request.getKey());
        if (trigger == null)
            {
            if (request.getSubscribe())
                {
                addListener(key, request.getLite(), request.getPriming());
                }
            else
                {
                removeListener(key, request.getPriming(), /*fUnregister*/ true);
                }
            }
        else
            {
            NamedCache<Object, Object> cache     = m_holder.getNonPassThruCache();
            MapListener<Object, Object> listener = new MapTriggerListener(trigger);
            if (request.getSubscribe())
                {
                cache.addMapListener(listener, key, request.getLite());
                }
            else
                {
                cache.removeMapListener(listener, key);
                }
            }
        }

    /**
     * Invoked when {@link MapListenerRequest.RequestType} is {@link MapListenerRequest.RequestType#FILTER KEY}.
     *
     * @param request  the {@link MapListenerRequest}
     * @param trigger  the {@link MapTrigger}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void onFilterRequest(MapListenerRequest request, MapTrigger<Binary, Binary> trigger)
        {
        if (trigger == null)
            {
            Filter<Binary> filter = f_service.ensureFilter(request.getFilter(), m_holder.getSerializer());
            if (request.getSubscribe())
                {
                addListener(filter, request.getFilterId(), request.getLite(), request.getPriming());
                }
            else
                {
                removeListener(filter, request.getPriming());
                }
            }
        else
            {
            NamedCache  cache    = m_holder.getNonPassThruCache();
            Filter      filter   = f_service.getFilter(request.getFilter(), m_holder.getSerializer());
            MapListener listener = new MapTriggerListener(trigger);
            if (request.getSubscribe())
                {
                cache.addMapListener(listener, filter, request.getLite());
                }
            else
                {
                cache.removeMapListener(listener, filter);
                }
            }
        }

    /**
     * Add this MapListenerProxy as a filter-based listener of the given NamedCache.
     *
     * @param filter    the Filter to listen to
     * @param filterId  the unique positive identifier of the Filter
     * @param lite      true to add a "lite" listener
     * @param priming   true if the listener is a priming listener
     */
    protected void addListener(Filter<?> filter, long filterId, boolean lite, boolean priming)
        {
        if (filterId <= 0)
            {
            throw new IllegalArgumentException("filter id must be a non-zero positive long value");
            }

        NamedCache<Object, Object> cache = m_holder.getNonPassThruCache();

        if (filter instanceof InKeySetFilter)
            {
            InKeySetFilter<?> filterKeys = (InKeySetFilter<?>) filter;
            if (filterKeys.isConverted())
                {
                for (Object binaryKey : filterKeys.getKeys())
                    {
                    Object key = m_holder.deserialize((Binary) binaryKey);
                    addListener(key, lite, priming, false /*fRegister*/);
                    }
                }
            else
                {
                for (Object key : filterKeys.getKeys())
                    {
                    addListener(key, lite, priming, false /*fRegister*/);
                    }
                }

            // make sure we don't "double dip" at the $ViewMap
            filterKeys.markConverted();

            MapListener<Object, Object> listener = priming ? ensurePrimingListener() : this;
            cache.addMapListener(listener, filterKeys, lite);
            }
        else if (priming)
            {
            throw new IllegalArgumentException("Priming listeners are only supported with InKeySetFilter");
            }
        else
            {
            f_mapFilter.lock(filter, -1L);
            try
                {
                f_mapFilter.put(filter, new FilterInfo(filterId, lite));
                MapListener<Object, Object> listener = this;
                cache.addMapListener(listener, filter, lite);
                }
            finally
                {
                f_mapFilter.unlock(filter);
                }
            }
        }

    /**
     * Add this MapListenerProxy as a key-based listener of the given NamedCache.
     *
     * @param key      the key to listen to deserialized in Object form
     * @param lite     {@code true} to add a "lite" listener
     * @param priming  {@code true} if the listener is a priming listener
     */
    protected void addListener(Object key, boolean lite, boolean priming)
        {
        addListener(key, lite, priming, /*fRegister*/ true);
        }

    /**
     * Add this MapListenerProxy as a key-based listener of the given NamedCache.
     *
     * @param key       the key to listen to deserialized in Object form
     * @param lite      {@code true} to add a "lite" listener
     * @param priming   {@code true} if the listener is a priming listener
     * @param register  {@code true} if the listener should be added to the underlying cache
     */
    protected void addListener(Object key, boolean lite, boolean priming, boolean register)
        {
        NamedCache<Object, Object> cache = m_holder.getNonPassThruCache();

        f_mapKeys.lock(key, -1L);
        try
            {
            int nFlags = lite ? LITE : 0;
            nFlags |= priming ? PRIMING : nFlags;

            if (f_mapKeys.containsKey(key))
                {
                // either a priming or non-priming listener was already registered

                nFlags = f_mapKeys.get(key);
                if ((nFlags & PRIMING) == PRIMING)
                    {
                    register = false;
                    }

                if (priming)
                    {
                    // was priming therefore nothing to do
                    register = false;

                    // as we have already registered a map listener and now
                    // we have a NearCache.get it is unnecessary to register
                    // the listener with PartitionedCache but we must dispatch
                    // the MapEvent

                    MapEventResponse eventResponse = MapEventResponse.newBuilder()
                            .setId(MapEventMessage.ENTRY_UPDATED
                                   | MapEventMessage.EVT_SYNTHETIC
                                   | MapEventMessage.EVT_PRIMING)
                            .setKey(BinaryHelper.toByteString(key, m_holder.getSerializer()))
                            .setSynthetic(true)
                            .setTransformationStateValue(MapEventResponse.TransformationState.TRANSFORMABLE.ordinal())
                            .setPriming(true)
                            .setNewValue(BinaryHelper.toByteString(cache.get(key), m_holder.getSerializer()))
                            .build();

                    f_observer.onNext(MapListenerResponse.newBuilder().setEvent(eventResponse).build());

                    nFlags |= PRIMING;
                    }
                // else re-registration of map listener on the same key

                if (!lite)
                    {
                    // switching from lite to heavy requires re-registration with storage
                    nFlags &= ~LITE;
                    register = true;
                    }
                }

            f_mapKeys.put(key, nFlags);
            f_setKeys.add(key);

            if (register)
                {
                MapListener<Object, Object> listener = priming ? ensurePrimingListener() : this;
                cache.addMapListener(listener, key, lite);
                }
            }
        finally
            {
            f_mapKeys.unlock(key);
            }
        }

    /**
     * Remove this MapListenerProxy as a filter-based listener of the given NamedCache.
     *
     * @param filter   the {@link Filter} to stop listening to
     * @param priming  {@code true} if the listener is a priming listener
     */
    void removeListener(Filter<Binary> filter, boolean priming)
        {
        NamedCache<Object, Object> cache = m_holder.getNonPassThruCache();

        if (filter instanceof InKeySetFilter)
            {
            InKeySetFilter<Binary> filterKeys = (InKeySetFilter<Binary>) filter;
            if (filterKeys.isConverted())
                {
                for (Object binaryKey : filterKeys.getKeys())
                    {
                    Object key = m_holder.deserialize((Binary) binaryKey);
                    removeListener(key, priming, false /*fRegister*/);
                    }
                }
            else
                {
                for (Object key : filterKeys.getKeys())
                    {
                    removeListener(key, priming, false /*fRegister*/);
                    }
                }

            filterKeys.ensureConverted(new KeyConverter(m_holder));
            // make sure we don't "double dip" at the $ViewMap
            filterKeys.markConverted();

            cache.removeMapListener(priming ? ensurePrimingListener() : this, filterKeys);
            }
        else if (priming)
            {
            throw new IllegalArgumentException("Priming listeners are only supported with InKeySetFilter");
            }
        else
            {
            f_mapFilter.lock(filter, -1L);
            try
                {
                if (f_mapFilter.remove(filter) != null)
                    {
                    cache.removeMapListener(this, filter);
                    }
                }
            finally
                {
                f_mapFilter.unlock(filter);
                }
            }
        }

    /**
     * Remove this MapListenerProxy as a key-based listener of the given NamedCache.
     *
     * @param key         the key to stop listening to deserialized in Object form
     * @param priming     {@code true} if the listener is a priming listener
     * @param unregister  {@code true} if the listener should be removed from the underlying cache
     */
    void removeListener(Object key, boolean priming, boolean unregister)
        {
        NamedCache<Object, Object> cache = m_holder.getNonPassThruCache();

        // normalize the key, if necessary
        f_mapKeys.lock(key, -1L);
        try
            {
            Integer nFlags = f_mapKeys.remove(key);
            if (nFlags != null)
                {
                // only remove the priming listener if it was actually registered
                // @see addListener
                priming &= (nFlags & PRIMING) == PRIMING;

                if (f_setKeys.remove(key))
                    {
                    if (unregister)
                        {
                        cache.removeMapListener(priming ? ensurePrimingListener() : this, key);
                        }
                    }
                else
                    {
                    throw new IllegalStateException("attempt to remove key listener for unregistered key");
                    }
                }
            }
        finally
            {
            f_mapKeys.unlock(key);
            }
        }

    /**
     * Remove this MapListenerProxy as a listener of the given NamedCache.
     */
    void removeAllListeners()
        {
        if (m_holder == null)
            {
            // no listeners were registered
            return;
            }

        if (!m_holder.getCache().isActive())
            {
            // cache is no-longer active so nothing to remove
            return;
            }

        NamedCache<Object, Object> cache = m_holder.getNonPassThruCache();

        // remove the deactivation listener
        cache.removeMapListener(f_listenerDeactivation);

        // unregister all filter-based listeners
        f_mapFilter.lock(ConcurrentMap.LOCK_ALL, -1L);
        try
            {
            for (Filter<?> filter : f_mapFilter.keySet())
                {
                cache.removeMapListener(this, filter);
                }
            f_mapFilter.clear();
            }
        finally
            {
            f_mapFilter.unlock(ConcurrentMap.LOCK_ALL);
            }

        // unregister all key-based listeners
        f_mapKeys.lock(ConcurrentMap.LOCK_ALL, -1L);
        try
            {
            for (Object key : f_setKeys)
                {
                Integer nFlags = f_mapKeys.remove(key);
                boolean fPriming = nFlags != null && (nFlags & PRIMING) != 0;
                cache.removeMapListener(fPriming ? ensurePrimingListener() : this, key);
                }
            f_setKeys.clear();
            }
        finally
            {
            f_mapKeys.unlock(ConcurrentMap.LOCK_ALL);
            }
        }

    /**
     * Return the priming listener, or it not already cached, create it.
     *
     * @return the priming listener
     */
    protected MapListenerSupport.PrimingListener<Object, Object> ensurePrimingListener()
        {
        if (m_primingListener == null)
            {
            synchronized (this)
                {
                if (m_primingListener == null)
                    {
                    m_primingListener = new WrapperPrimingListener(this);
                    }
                }
            }
        return m_primingListener;
        }

    /**
     * Convert a {@link MapEvent} into a {@link MapEventResponse} and send
     * it to the {@link StreamObserver}, converting the{@link Binary} key
     * and values if required.
     *
     * @param event  the event to send to the observer
     */
    protected void onMapEvent(MapEvent<?, ?> event)
        {
        try
            {
            MapEventResponse eventResponse = createMapEventResponse(event);
            f_observer.onNext(MapListenerResponse.newBuilder().setEvent(eventResponse).build());
            }
        catch (Throwable thrown)
            {
            CacheFactory.err("Error processing MapEvent");
            CacheFactory.err(thrown);
            }
        }

    /**
     * Factory method to create new {@link MapEventResponse} instances using the information
     * in the supplied {@link MapEvent}.
     *
     * @param mapEvent  the {@link MapEvent} used to configure the newly created {@link MapEventResponse}
     *
     * @return a {@link MapEventResponse} created from the {@link MapEvent}
     */
    protected MapEventResponse createMapEventResponse(MapEvent<?, ?> mapEvent)
        {
        int              nEventId     = mapEvent.getId();
        Object           oKey         = mapEvent.getKey();
        Integer          nFlags       = f_mapKeys.get(oKey);
        boolean          fKeyLite     = nFlags == null || (nFlags & LITE) != 0;
        boolean          fPriming     = nFlags != null && (nFlags & PRIMING) != 0;
        boolean          fFilterLite  = true;
        Collection<Long> colFilterIds = Collections.emptyList();
        MapEvent<?, ?>   unwrapped    = MapListenerSupport.unwrapEvent(mapEvent);
        CacheEvent<?, ?> evtCache     = unwrapped instanceof CacheEvent ? (CacheEvent<?, ?>) unwrapped : null;
        boolean          fSynthetic   = evtCache != null && evtCache.isSynthetic();

        // determine the identifier(s) of Filter(s) associated with the MapEvent
        if (unwrapped instanceof MapListenerSupport.FilterEvent)
            {
            Filter<?>[] filters = ((MapListenerSupport.FilterEvent) unwrapped).getFilter();
            colFilterIds = new ArrayList<>();

            for (Filter<?> filter : filters)
                {
                FilterInfo filterInfo = f_mapFilter.get(filter);
                if (filterInfo != null)
                    {
                    // see #addListener
                    boolean fLite = filterInfo.isLite();
                    colFilterIds.add(filterInfo.getId());
                    if (!fLite)
                        {
                        fFilterLite = false;
                        }
                    }
                }
            }
        else
            {
            FilterInfo filterInfo = f_mapFilter.get(null); // there was no filter
            if (filterInfo != null)
                {
                boolean fLite = filterInfo.isLite();
                colFilterIds = Collections.singleton(filterInfo.getId());
                fFilterLite = fLite;
                }
            }

        int tranformationState = evtCache == null
                                 ? CacheEvent.TransformationState.TRANSFORMABLE.ordinal()
                                 : evtCache.getTransformationState().ordinal();

        Serializer serializer = m_holder.getSerializer();
        MapEventResponse.Builder builder = MapEventResponse.newBuilder()
                .setId(nEventId)
                .addAllFilterIds(colFilterIds)
                .setKey(BinaryHelper.toByteString(oKey, serializer))
                .setSynthetic(fSynthetic)
                .setTransformationStateValue(tranformationState)
                .setPriming(evtCache != null && evtCache.isPriming());


        if (!fKeyLite || !fFilterLite || fPriming)
            {
            Object     newValue = mapEvent.getNewValue();
            ByteString newBytes = newValue == null
                                  ? ByteString.EMPTY
                                  : BinaryHelper.toByteString(newValue, serializer);
            builder.setNewValue(newBytes);
            if (!fPriming)
                {
                // priming events don't need the old value
                Object     oldValue = mapEvent.getOldValue();
                ByteString oldBytes = oldValue == null
                                      ? ByteString.EMPTY
                                      : BinaryHelper.toByteString(oldValue, serializer);
                builder.setOldValue(oldBytes);
                }
            }

        return builder.build();
        }

    /**
     * Create a {@link MapListenerErrorResponse}.
     *
     * @param uid  the UID of the request
     * @param t    the error that occurred
     *
     * @return a {@link MapListenerErrorResponse}.
     */
    protected MapListenerErrorResponse error(String uid, Throwable t)
        {
        MapListenerErrorResponse.Builder builder = MapListenerErrorResponse.newBuilder()
                .setUid(uid)
                .setMessage(String.valueOf(t.getMessage()));

        if (t instanceof StatusException)
            {
            builder.setCode(((StatusException) t).getStatus().getCode().value());
            }
        else if (t instanceof StatusRuntimeException)
            {
            builder.setCode(((StatusRuntimeException) t).getStatus().getCode().value());
            }
        else if (t instanceof IllegalArgumentException)
            {
            builder.setCode(Status.Code.INVALID_ARGUMENT.value());
            }
        else if (t instanceof IllegalStateException)
            {
            builder.setCode(Status.Code.FAILED_PRECONDITION.value());
            }
        else
            {
            builder.setCode(Status.Code.INTERNAL.value());
            }

        for (StackTraceElement element : t.getStackTrace())
            {
            builder.addStack(element.toString());
            }

        return builder.build();
        }

    // ----- inner class: DeactivationListener ------------------------------

    /**
     * {@link NamedCacheDeactivationListener} that will communicate cache truncation
     * and destruction events over the proxy.
     */
    protected static class DeactivationListener
            extends AbstractMapListener
            implements NamedCacheDeactivationListener
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct the DeactivationListener for the provided {@link MapListenerProxy}.
         *
         * @param proxy  the {@link MapListenerProxy}
         */
        protected DeactivationListener(MapListenerProxy proxy)
            {
            this.f_proxy = proxy;
            }

        // ----- AbstractMapListener methods --------------------------------

        @Override
        public void entryDeleted(MapEvent evt)
            {
            Object source = evt.getSource();
            if (source instanceof NamedCache)
                {
                String sCacheName = ((NamedCache) source).getCacheName();
                try
                    {
                    CacheDestroyedResponse response = CacheDestroyedResponse
                            .newBuilder()
                            .setCache(sCacheName)
                            .build();

                    f_proxy.f_observer.onNext(MapListenerResponse.newBuilder().setDestroyed(response).build());
                    f_proxy.onCompleted();
                    }
                catch (Throwable t)
                    {
                    CacheFactory.err("Failed to send cache destroy response for cache " + sCacheName);
                    CacheFactory.err(t);
                    }
                }
            }

        @Override
        public void entryUpdated(MapEvent evt)
            {
            Object source = evt.getSource();
            if (source instanceof NamedCache)
                {
                CacheTruncatedResponse response = CacheTruncatedResponse
                        .newBuilder()
                        .setCache(((NamedCache) source).getCacheName())
                        .build();
                f_proxy.f_observer.onNext(MapListenerResponse.newBuilder().setTruncated(response).build());
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link MapListenerProxy}.
         */
        protected final MapListenerProxy f_proxy;
        }

    // ----- inner class: KeyConverter --------------------------------------

    /**
     * Converter for cache key instances.
     */
    protected static class KeyConverter
            implements Converter<Object, Binary>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code KeyConverter}.
         *
         * @param holder  the {@link CacheRequestHolder} which may have converters that
         *                can be used to convert the key
         */
        @SuppressWarnings("unchecked")
        protected KeyConverter(CacheRequestHolder<MapListenerRequest, Void> holder)
            {
            this.f_holder    = holder;
            this.f_converter = holder.getNonPassThruCache().getCacheService().getBackingMapManager().getContext()
                    .getKeyToInternalConverter();
            }

        @Override
        public Binary convert(Object oKey)
            {
            Binary binKey;
            if (oKey instanceof Binary)
                {
                binKey = f_holder.convertKeyDown((Binary) oKey);
                }
            else if (oKey instanceof ByteString)
                {
                binKey = f_holder.convertKeyDown((ByteString) oKey);
                }
            else
                {
                binKey = f_converter.convert(oKey);
                }
            return binKey;
            }

        // ----- data members -----------------------------------------------

        /**
         * Cache request holder to handle conversion of {@link Binary} and {@link ByteString} objects.
         */
        protected final CacheRequestHolder<MapListenerRequest, Void> f_holder;

        /**
         * Converter to handle the conversion cases {@link #f_holder} doesn't cover.
         */
        protected final Converter<Object, Binary> f_converter;
        }

    // ----- inner class: FilterInfo ----------------------------------------

    /**
     * A holder for filter information.
     */
    protected static class FilterInfo
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new FilterInfo instance.
         *
         * @param lId    the filter identifier
         * @param fLite  flag indicating whether the filter was registered for lite events
         */
        protected FilterInfo(long lId, boolean fLite)
            {
            this.f_lId   = lId;
            this.f_fLite = fLite;
            }

        // ----- public methods ---------------------------------------------

        /**
         * Return the filter identifier.
         *
         * @return the filter identifier
         */
        public long getId()
            {
            return f_lId;
            }

        /**
         * Return {@code true} if the filter was registered for lite events.
         *
         * @return {@code true} if the filter was registered for lite events
         */
        public boolean isLite()
            {
            return f_fLite;
            }

        // ----- data members -----------------------------------------------

        /**
         * The filter identifier.
         */
        protected final long f_lId;

        /**
         * A flag indicating whether the filter was registered for lite events.
         */
        protected final boolean f_fLite;
        }

    // ----- inner class: WrapperPrimingListener ----------------------------

    /**
     * {@link MapListenerSupport.PrimingListener} that delegates calls to the wrapped {@link MapListener}.
     */
    protected static class WrapperPrimingListener
            implements MapListenerSupport.PrimingListener<Object, Object>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a new {@code WrapperPrimingListener} that will delegate map events
         * to the provided {@link MapListener}.
         *
         * @param wrapped  the {@link MapListener} delegate
         */
        protected WrapperPrimingListener(MapListener<Object, Object> wrapped)
            {
            this.f_listenerWrapped = wrapped;
            }

        // ----- MapListener interface --------------------------------------

        @Override
        public void entryInserted(MapEvent<Object, Object> mapEvent)
            {
            f_listenerWrapped.entryInserted(mapEvent);
            }

        @Override
        public void entryUpdated(MapEvent<Object, Object> mapEvent)
            {
            f_listenerWrapped.entryUpdated(mapEvent);
            }

        @Override
        public void entryDeleted(MapEvent<Object, Object> mapEvent)
            {
            f_listenerWrapped.entryDeleted(mapEvent);
            }

        // ----- data members -----------------------------------------------

        /**
         * The wrapped {@link MapListener}.
         */
        protected final MapListener<Object, Object> f_listenerWrapped;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Constant to indicate that the listener is registered for "lite" events.
     */
    public static final int LITE = 1;

    /**
     * Constant to indicate that the listener is registered for "priming" events.
     */
    public static final int PRIMING = 2;

    // ----- data members ---------------------------------------------------

    protected volatile boolean m_fCompleted;

    /**
     * The owning {@link NamedCacheService}.
     */
    protected final NamedCacheService f_service;

    /**
     * The {@link StreamObserver} to stream {@link com.tangosol.util.MapEvent} instances to.
     */
    protected final StreamObserver<MapListenerResponse> f_observer;

    /**
     * The map of {@link Filter Filters} that this {@link MapListenerProxy} was registered with.
     */
    protected final ConcurrentMap<Filter<?>, FilterInfo> f_mapFilter;

    /**
     * The map of keys that this {@link MapListenerProxy} was registered with.
     */
    protected final ConcurrentMap<Object, Integer> f_mapKeys;

    /**
     * The set of keys that this {@link MapListenerProxy} was registered with.
     */
    protected final Set<Object> f_setKeys;

    /**
     * The listener used to detect cache deactivation.
     */
    protected final MapListener<Object, Object> f_listenerDeactivation;

    /**
     * The {@link CacheRequestHolder} to hold the {@link MapListenerRequest}
     * that can convert between different serialization formats.
     */
    protected CacheRequestHolder<MapListenerRequest, Void> m_holder;

    /**
     * Wrapper map event listener. This listener registration should force a synthetic
     * event containing the current value to the requesting client.
     */
    protected volatile WrapperPrimingListener m_primingListener;
    }
