/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.cache;

import com.oracle.coherence.common.base.Continuation;

import com.tangosol.internal.net.NamedCacheDeactivationListener;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.AsyncNamedMap;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.FlowControl;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;

import com.tangosol.util.AbstractKeySetBasedMap;
import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.FilterEnumerator;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.ListMap;
import com.tangosol.util.LiteMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;
import com.tangosol.util.MapListenerSupport.FilterEvent;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.ObservableHashMap;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.SafeHashMap;
import com.tangosol.util.TaskDaemon;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.processor.AsynchronousProcessor;
import com.tangosol.util.processor.ExtractorProcessor;

import com.tangosol.util.transformer.ExtractorEventTransformer;
import com.tangosol.util.transformer.SemiLiteEventTransformer;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.KeyAssociatedFilter;
import com.tangosol.util.filter.LimitFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.MapEventTransformerFilter;
import com.tangosol.util.filter.NotFilter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Create a materialized view of a {@link NamedCache} using the {@code Coherence}
 * <i>Continuous Query</i> capability.
 * <p>
 * In addition to providing an up-to-date view of the backing {@link NamedCache}, this
 * class supports server-side transformation of cache values. For example, you
 * could create a client-side view of a cache containing {@code Portfolio}
 * instances that only contains the value of each portfolio:
 * <pre>{@code
 *   NamedCache<String, Portfolio> cache =
 *         CacheFactory.getTypedCache("portfolio", TypeAssertion.withoutTypeChecking());
 *   NamedCache<String, Double>    cqc   = new ContinuousQueryCache<>(cache,
 *                                                                    AlwaysFilter.INSTANCE,
 *                                                                    Portfolio::getValue);
 * }</pre>
 *
 * @param <K>        the type of the cache entry keys
 * @param <V_BACK>   the type of the entry values in the back cache that is used
 *                   as the source for this {@code ContinuousQueryCache}
 * @param <V_FRONT>  the type of the entry values in this {@code ContinuousQueryCache}, which
 *                   will be the same as {@code V_BACK}, unless a {@code transformer} is specified
 *                   when creating this {@code ContinuousQueryCache}
 *
 * @author cp 2006.01.19
 * @since Coherence 3.1
 */
@SuppressWarnings("unchecked")
public class ContinuousQueryCache<K, V_BACK, V_FRONT>
        extends AbstractKeySetBasedMap<K, V_FRONT>
        implements NamedCache<K, V_FRONT>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a locally materialized view of a {@link NamedCache} using a {@link Filter}. A
     * materialized view is an implementation of <i>Continuous Query</i> exposed
     * through the standard {@link NamedCache} API.
     * <p>
     * This constructor will result in a {@code ContinuousQueryCache} that caches both
     * its keys and values locally.
     *
     * @param cache  the {@link NamedCache} with which the view will be created
     *
     * @since 12.2.1.4
     */
    public ContinuousQueryCache(NamedCache<K, V_BACK> cache)
        {
        this(cache, AlwaysFilter.INSTANCE(), true, null, null);
        }

    /**
     * Create a locally materialized view of a {@link NamedCache} using a {@link Filter}. A
     * materialized view is an implementation of <i>Continuous Query</i>
     * exposed through the standard {@link NamedCache} API.
     * <p>
     * This constructor will result in a {@code ContinuousQueryCache} that caches both
     * its keys and values locally.
     *
     * @param supplierCache  a {@link Supplier} that returns a {@link NamedCache}
     *                       with which the {@code ContinuousQueryCache} will be created
     *                       The Supplier <em>must</em> return a new instance each time
     *                       {@link Supplier#get()} is called
     *
     * @since 12.2.1.4
     */
    public ContinuousQueryCache(Supplier<NamedCache<K, V_BACK>> supplierCache)
        {
        this(supplierCache, AlwaysFilter.INSTANCE(), true, null, null, null);
        }

    /**
     * Create a locally materialized view of a {@link NamedCache} using a Filter. A
     * materialized view is an implementation of <i>Continuous Query</i>
     * exposed through the standard {@link NamedCache} API.
     * <p>
     * This constructor will result in a {@code ContinuousQueryCache} that caches both
     * its keys and values locally.
     *
     * @param cache   the {@link NamedCache} to create a view of
     * @param filter  the filter that defines the view
     */
    public ContinuousQueryCache(NamedCache<K, V_BACK> cache, Filter filter)
        {
        this(cache, filter, true, null, null);
        }

    /**
     * Create a locally materialized view of a {@link NamedCache} using a {@link Filter}. A
     * materialized view is an implementation of <i>Continuous Query</i>
     * exposed through the standard {@link NamedCache} API.
     * <p>
     * This constructor will result in a {@code ContinuousQueryCache} that caches both
     * its keys and values locally.
     *
     * @param supplierCache  a {@link Supplier} that returns a {@link NamedCache}
     *                       with which the {@code ContinuousQueryCache} will be created
     *                       The Supplier <em>must</em> return a new instance each time
     *                       {@link Supplier#get()} is called
     * @param filter         the {@link Filter} that defines the view
     *
     * @since 12.2.1.4
     */
    public ContinuousQueryCache(Supplier<NamedCache<K, V_BACK>> supplierCache, Filter filter)
        {
        this(supplierCache, filter, true, null, null, null);
        }

    /**
     * Create a locally materialized view of a {@link NamedCache} using a {@link Filter} and
     * a transformer. A materialized view is an implementation of
     * <i>Continuous Query</i> exposed through the standard {@link NamedCache} API.
     * <p>
     * This constructor will result in a <b>read-only</b> ContinuousQueryCache
     * that caches both its keys and transformed values locally.
     *
     * @param cache        the {@link NamedCache} to create a view of
     * @param filter       the {@link Filter} that defines the view
     * @param transformer  the {@link ValueExtractor} that should be used to transform
     *                     values retrieved from the underlying {@link NamedCache}
     *                     before storing them locally
     */
    public ContinuousQueryCache(NamedCache<K, V_BACK> cache, Filter filter,
                                ValueExtractor<? super V_BACK, ? extends V_FRONT> transformer)
        {
        this(cache, filter, true, null, transformer);
        }

    /**
     * Create a materialized view of a {@link NamedCache} using a Filter. A
     * materialized view is an implementation of <i>Continuous Query</i>
     * exposed through the standard {@link NamedCache} API.
     *
     * @param cache         the {@link NamedCache} to create a view of
     * @param filter        the {@link Filter} that defines the view
     * @param fCacheValues  pass {@code true} to cache both the keys and values of the
     *                      materialized view locally, or {@code false} to only cache
     *                      the keys
     */
    public ContinuousQueryCache(NamedCache<K, V_BACK> cache, Filter filter, boolean fCacheValues)
        {
        this(cache, filter, fCacheValues, null, null);
        }

    /**
     * Create a materialized view of a {@link NamedCache} using a Filter. A
     * materialized view is an implementation of <i>Continuous Query</i>
     * exposed through the standard {@link NamedCache} API. This constructor allows
     * a client to receive all events, including those that result from the
     * initial population of the {@code ContinuousQueryCache}. In other words, all
     * contents of the {@code ContinuousQueryCache} will be delivered to the listener
     * as a sequence of events, including those items that already exist in
     * the underlying (unfiltered) cache.
     *
     * @param cache     the {@link NamedCache} to create a view of
     * @param filter    the {@link Filter} that defines the view
     * @param listener  a {@link MapListener} that will receive all the events from
     *                  the {@code ContinuousQueryCache}, including those corresponding
     *                  to its initial population
     */
    public ContinuousQueryCache(NamedCache<K, V_BACK> cache, Filter filter,
                                MapListener<? super K, ? super V_FRONT> listener)
        {
        this(cache, filter, false, listener, null);
        }

    /**
     * Create a materialized view of a {@link NamedCache} using a {@link Filter}. A
     * materialized view is an implementation of <i>Continuous Query</i>
     * exposed through the standard {@link NamedCache} API.
     * <p>
     * This constructor will result in a <b>read-only</b> {@code ContinuousQueryCache}
     * that caches both its keys and transformed values locally. It will also allow
     * a client to receive all events, including those that result from the
     * initial population of the {@code ContinuousQueryCache}. In other words, all
     * contents of the {@code ContinuousQueryCache} will be delivered to the listener
     * as a sequence of events, including those items that already exist in
     * the underlying (unfiltered) cache.
     *
     * @param cache        the {@link NamedCache} to create a view of
     * @param filter       the {@link Filter} that defines the view
     * @param listener     a {@link MapListener} that will receive all the events from the
     *                     {@code ContinuousQueryCache}, including those corresponding
     *                     to its initial population
     * @param transformer  the {@link ValueExtractor} that should be used to transform
     *                     values retrieved from the underlying {@link NamedCache}
     *                     before storing them locally
     */
    public ContinuousQueryCache(NamedCache<K, V_BACK> cache, Filter filter,
                                MapListener<? super K, ? super V_FRONT> listener,
                                ValueExtractor<? super V_BACK, ? extends V_FRONT> transformer)
        {
        this(cache, filter, true, listener, transformer);
        }

    /**
     * Construct the ContinuousQueryCache.
     *
     * @param cache         the {@link NamedCache} to create a view of
     * @param filter        the {@link Filter} that defines the view
     * @param fCacheValues  pass true to cache both the keys and values of the
     *                      materialized view locally, or false to only cache
     *                      the keys
     * @param listener      an optional {@link MapListener} that will receive all
     *                      events starting from the initialization of the {@code ContinuousQueryCache}
     * @param transformer   an optional {@link ValueExtractor} that would be used to
     *                      transform values retrieved from the underlying cache
     *                      before storing them locally; if specified, this
     *                      {@code ContinuousQueryCache} will become "read-only"
     */
    public ContinuousQueryCache(NamedCache<K, V_BACK> cache, Filter filter,
                                   boolean fCacheValues, MapListener<? super K, ? super V_FRONT> listener,
                                   ValueExtractor<? super V_BACK, ? extends V_FRONT> transformer)
        {
        this(() -> cache, filter, fCacheValues, listener, transformer, null);
        }

    /**
     * Create a materialized view of a {@link NamedCache} using a {@link Filter}. A
     * materialized view is an implementation of <i>Continuous Query</i>
     * exposed through the standard {@link NamedCache} API.
     * <p>
     * This constructor will result in a <b>read-only</b> {@code ContinuousQueryCache}
     * that caches both its keys and transformed values locally. It will also allow
     * a client to receive all events, including those that result from the
     * initial population of the {@code ContinuousQueryCache}. In other words, all
     * contents of the {@code ContinuousQueryCache} will be delivered to the listener
     * as a sequence of events, including those items that already exist in
     * the underlying (unfiltered) cache.
     *
     * @param supplierCache  a {@link Supplier} that returns a {@link NamedCache}
     *                       with which the {@code ContinuousQueryCache} will be created.
     *                       The Supplier <em>must</em> return a new instance each
     *                       time {@link Supplier#get()} is called
     * @param filter         the {@link Filter} that defines the view
     * @param fCacheValues   pass {@code true} to cache both the keys and values of the
     *                       materialized view locally, or {@code false} to only cache
     *                       the keys
     * @param listener       an optional {@link MapListener} that will receive all
     *                       events starting from the initialization of the
     *                       {@code ContinuousQueryCache}
     * @param transformer    an optional {@link ValueExtractor} that would be used to
     *                       transform values retrieved from the underlying cache
     *                       before storing them locally; if specified, this
     *                       {@code ContinuousQueryCache} will become <em>read-only</em>
     * @param loader         an optional {@link ClassLoader}
     *
     * @since 12.2.1.4
     */
    public ContinuousQueryCache(Supplier<NamedCache<K, V_BACK>> supplierCache, Filter filter,
                                boolean fCacheValues, MapListener<? super K, ? super V_FRONT> listener,
                                ValueExtractor<? super V_BACK, ? extends V_FRONT> transformer,
                                ClassLoader loader)
        {
        NamedCache<K, V_BACK> cache = supplierCache.get();
        if (cache == null)
            {
            throw new IllegalArgumentException("NamedCache must be specified");
            }

        if (filter == null)
            {
            throw new IllegalArgumentException("Filter must be specified");
            }

        if (filter instanceof LimitFilter)
            {
            // FUTURE TODO: it would be nice to eventually be able to have a
            // cache of the "top ten" items, etc.
            throw new UnsupportedOperationException("LimitFilter may not be used");
            }

        m_loader        = loader;
        f_supplierCache = supplierCache;
        m_cache         = ensureConverters(cache);
        m_filter        = filter;
        m_fCacheValues  = fCacheValues;
        m_transformer   = Lambdas.ensureRemotable(transformer);
        m_fReadOnly     = transformer != null;
        m_nState        = STATE_DISCONNECTED;

        if (listener instanceof MapTriggerListener)
            {
            throw new IllegalArgumentException("ContinuousQueryCache does not support MapTriggerListeners");
            }

        if (listener instanceof NamedCacheDeactivationListener)
            {
            addMapListener(listener);
            }
        else
            {
            m_mapListener = listener;
            }


        // by including information about the underlying cache, filter and
        // transformer, the resulting cache name is convoluted but extremely
        // helpful for tasks such as debugging
        m_sName = getDefaultName(cache.getCacheName(), filter, transformer);

        ensureInternalCache();
        ensureSynchronized(false);
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Obtain the {@link NamedCache} that this {@code ContinuousQueryCache} is based on.
     *
     * @return the underlying {@link NamedCache}
     */
    public NamedCache<K, V_BACK> getCache()
        {
        NamedCache<K, V_BACK> cache = m_cache;
        if (cache == null)
            {
            synchronized (this)
                {
                if (m_cache == null)
                    {
                    cache = f_supplierCache.get();
                    if (cache == null)
                        {
                        throw new IllegalStateException("NamedCache is not active");
                        }

                    cache = m_cache = ensureConverters(cache);
                    }
                }
            }
        return cache;
        }

    /**
     * Obtain the {@link Filter} that this {@code ContinuousQueryCache} is using to query the
     * underlying {@link NamedCache}.
     *
     * @return the {@link Filter} that this cache uses to select its contents
     *         from the underlying {@link NamedCache}
     */
    public Filter getFilter()
        {
        return m_filter;
        }

    /**
     * Obtain the transformer that this {@code ContinuousQueryCache} is using to transform the results from
     * the underlying cache prior to storing them locally.
     *
     * @return the {@link ValueExtractor} that this cache uses to transform entries from the underlying cache
     *
     * @since 12.2.1.4
     */
    public ValueExtractor<? super V_BACK, ? extends V_FRONT> getTransformer()
        {
        return m_transformer;
        }

    /**
     * Obtain the configured {@link MapListener} for this {@code ContinuousQueryCache}.
     *
     * @return the {@link MapListener} for this {@code ContinuousQueryCache}
     *
     * @since 12.2.1.4
     */
    public MapListener<? super K, ? super V_FRONT> getMapListener()
        {
        return m_mapListener;
        }

    /**
     * Determine if this {@code ContinuousQueryCache} caches values locally.
     *
     * @return {@code true} if this object caches values locally, and {@code false} if it
     *         relies on the underlying {@link NamedCache}
     */
    public boolean isCacheValues()
        {
        return m_fCacheValues || isObserved();
        }

    /**
     * Modify the local-caching option for the {@link ContinuousQueryCache}. By
     * changing this value from <tt>false</tt> to <tt>true</tt>, the
     * {@code ContinuousQueryCache} will fully realize its contents locally and
     * maintain them coherently in a manner analogous to the Coherence Near
     * Cache. By changing this value from <tt>true</tt> to <tt>false</tt>,
     * the {@code ContinuousQueryCache} will discard its locally cached data and
     * rely on the underlying NamedCache.
     * <p>
     *
     * @param fCacheValues  pass {@code true} to enable local caching, or {@code false}
     *                      to disable it
     */
    public synchronized void setCacheValues(boolean fCacheValues)
        {
        if (fCacheValues != m_fCacheValues)
            {
            boolean fDidCacheValues = isCacheValues();

            // If we are no longer caching the values then we don't need the
            // local indexes.
            if (fDidCacheValues)
                {
                releaseIndexMap();
                }

            m_fCacheValues = fCacheValues;

            if (isCacheValues() != fDidCacheValues)
                {
                configureSynchronization(false);
                }
            }
        }

    /**
     * Determine if this {@code ContinuousQueryCache} transforms values.
     *
     * @return {@code true} if this {@code ContinuousQueryCache} has been configured to transform
     *         values
     */
    public boolean isTransformed()
        {
        return m_transformer != null;
        }

    /**
     * Determine if this {@code ContinuousQueryCache} disallows data modification
     * operations.
     *
     * @return {@code true} if this {@code ContinuousQueryCache} has been configured as
     *         read-only
     */
    public boolean isReadOnly()
        {
        return m_fReadOnly;
        }

    /**
     * Modify the read-only option for the {@code ContinuousQueryCache}. Note that the
     * cache can be made read-only, but the opposite (making it mutable) is
     * explicitly disallowed.
     *
     * @param fReadOnly  pass {@code true} to prohibit clients from making
     *                   modifications to this cache
     */
    public synchronized void setReadOnly(boolean fReadOnly)
        {
        if (fReadOnly != isReadOnly())
            {
            // once the cache is read-only, changing its read-only setting is
            // a mutating operation and thus is dis-allowed
            checkReadOnly();

            m_fReadOnly = fReadOnly;
            }
        }

    /**
     * Create the internal cache used by the {@code ContinuousQueryCache}.
     *
     * @return a new {@link ObservableMap} that will represent the materialized view
     *         of the {@code ContinuousQueryCache}
     */
    protected ObservableMap<K, V_FRONT> instantiateInternalCache()
        {
        return new ObservableHashMap<>();
        }

    /**
     * Create and initialize this {@code ContinuousQueryCache}'s (if not already present) internal cache.
     * This method is called by {@link #configureSynchronization(boolean)}, as such, it shouldn't be called
     * directly.  Use {@link #getInternalCache()}.
     *
     * @return the {@link ObservableMap} functioning as this {@code ContinuousQueryCache}'s internal cache
     *
     * @since 12.2.1.4
     */
    protected ObservableMap<K, V_FRONT> ensureInternalCache()
        {
        if (m_mapLocal == null)
            {
            m_mapLocal = instantiateInternalCache();

            MapListener mapListener = m_mapListener;
            if (mapListener != null)
                {
                // the initial listener has to hear the initial events
                ensureEventQueue();
                ensureListenerSupport().addListener(
                        instantiateEventRouter(mapListener, false), (Filter) null, false);
                m_fListeners = true;
                }
            }
        return m_mapLocal;
        }

    /**
     * Obtain a reference to the internal cache. The internal cache maintains
     * all of the keys in the {@code ContinuousQueryCache}, and if
     * {@link #isCacheValues()} is true, it also maintains the up-to-date
     * values corresponding to those keys.
     *
     * @return the internal cache that represents the materialized view of the
     *         {@code ContinuousQueryCache}
     */
    protected ObservableMap<K, V_FRONT> getInternalCache()
        {
        ensureSynchronized(true);
        return m_mapLocal;
        }

    /**
     * Determine if the {@code ContinuousQueryCache} has any listeners that cannot be
     * served by this Map listening to lite events.
     *
     * @return {@code true} iff there is at least one {@link MapListener listener}
     */
    protected boolean isObserved()
        {
        return m_fListeners;
        }

    /**
     * Specify whether the {@code ContinuousQueryCache} has any listeners that cannot
     * be served by this Map listening to lite events.
     *
     * @param fObserved  {@code true} iff there is at least one {@link MapListener listener}
     */
    protected synchronized void setObserved(boolean fObserved)
        {
        if (fObserved != isObserved())
            {
            boolean fDidCacheValues = isCacheValues();

            m_fListeners = fObserved;

            if (isCacheValues() != fDidCacheValues)
                {
                configureSynchronization(false);
                }
            }
        }

    /**
     * Obtain the state of the {@code ContinuousQueryCache}.
     *
     * @return one of the {@code STATE_} enums
     */
    public int getState()
        {
        return m_nState;
        }

    /**
     * Change the state of the {@code ContinuousQueryCache}.
     *
     * @param nState  one of the {@code STATE_} enums
     */
    protected void changeState(int nState)
        {
        switch (nState)
            {
            case STATE_DISCONNECTED:
                resetCacheRefs();
                m_nState = STATE_DISCONNECTED;
                break;

            case STATE_CONFIGURING:
                synchronized (m_nState)
                    {
                    int nStatePrev = m_nState;
                    azzert(nStatePrev == STATE_DISCONNECTED ||
                           nStatePrev == STATE_SYNCHRONIZED);

                    m_mapSyncReq = new SafeHashMap();
                    m_nState     = STATE_CONFIGURING;
                    }
                break;

            case STATE_CONFIGURED:
                synchronized (m_nState)
                    {
                    if (m_nState == STATE_CONFIGURING)
                        {
                        m_nState = STATE_CONFIGURED;
                        }
                    else
                        {
                        throw new IllegalStateException(getCacheName() +
                                " has been invalidated");
                        }
                    }
                break;

            case STATE_SYNCHRONIZED:
                synchronized (m_nState)
                    {
                    if (m_nState == STATE_CONFIGURED)
                        {
                        m_mapSyncReq = null;
                        m_nState     = STATE_SYNCHRONIZED;
                        }
                    else
                        {
                        throw new IllegalStateException(getCacheName() +
                                " has been invalidated");
                        }
                    }
                break;

            default:
                throw new IllegalArgumentException("unknown state: " + nState);
            }
        }

    /**
     * Reset cache references to null.
     */
    protected void resetCacheRefs()
        {
        m_converterFromBinary = null;
        m_converterToBinary   = null;
        m_cache               = null;
        }

    /**
     * Return the reconnection interval (in milliseconds). This value indicates the period
     * in which re-synchronization with the underlying cache will be delayed in the case the
     * connection is severed.  During this time period, local content can be accessed without
     * triggering re-synchronization of the local content.
     *
     * @return a reconnection interval (in milliseconds)
     *
     * @see #setReconnectInterval
     * @since Coherence 3.4
     */
    public long getReconnectInterval()
        {
        return m_cReconnectMillis;
        }

    /**
     * Specify the reconnection interval (in milliseconds). This value indicates the period
     * in which re-synchronization with the underlying cache will be delayed in the case the
     * connection is severed.  During this time period, local content can be accessed without
     * triggering re-synchronization of the local content.
     *
     * @param cReconnectMillis  reconnection interval (in milliseconds). A value of zero
     *                          or less means that the {@code ContinuousQueryCache} cannot
     *                          be used when not connected.
     *
     * @since Coherence 3.4
     */
    public void setReconnectInterval(long cReconnectMillis)
        {
        m_cReconnectMillis = cReconnectMillis;
        }

    /**
     * Set the cache name for this {@code ContinuousQueryCache} as returned
     * by {@link #getCacheName()}.
     * <p>
     * Note: setting the cache name to be consistent with the
     * {@link NamedCache#getCacheName() cache name} of
     * the {@link NamedCache} this CQC is backed by will
     * ensure data structures that cache {@code NamedCache}
     * instances based upon the reported cache name would
     * result in an appropriate cache hit.
     *
     * @param sCacheName  the name this CQC should report as its
     *                    {@link NamedCache#getCacheName() cache name}
     *
     * @since 12.2.1.4
     */
    public void setCacheName(String sCacheName)
        {
        m_sName = sCacheName == null
                  ? getDefaultName(getCache().getCacheName(), m_filter, m_transformer)
                  : sCacheName;
        }

    // ----- Map interface --------------------------------------------------

    @Override
    public void clear()
        {
        checkReadOnly();
        getCache().keySet().removeAll(getInternalKeySet());
        }

    @Override
    public V_FRONT get(Object oKey)
        {
        Object oResult = isCacheValues()
                ? getInternalCache().get(oKey)
                : containsKey(oKey)
                        ? getInternal(oKey)
                        : null;

        return ensureInflated(oKey, oResult);
        }

    @Override
    public V_FRONT put(K oKey, V_FRONT oValue)
        {
        V_FRONT oOrig;

        checkReadOnly();
        checkEntry(oKey, oValue);

        // see if the putAll() optimization will work; this requires the
        // return value to be locally cached, or knowledge that the orig
        // value is null (because it is not present in the
        // ContinuousQueryCache)
        NamedCache<K, V_FRONT> cache       = (NamedCache<K, V_FRONT>) getCache();   // safe (no transformer)
        boolean                fLocalCache = isCacheValues();
        boolean                fPresent    = containsKey(oKey);
        if (fLocalCache || !fPresent)
            {
            oOrig = fPresent ? getInternalCache().get(oKey) : null;
            cache.putAll(Collections.singletonMap(oKey, oValue));
            }
        else
            {
            oOrig = cache.put(oKey, oValue);
            if (!InvocableMapHelper.evaluateEntry(getFilter(), oKey, oOrig))
                {
                oOrig = null;
                }
            }

        return fromInternal(oOrig);
        }

    @Override
    public void putAll(Map<? extends K, ? extends V_FRONT> map)
        {
        checkReadOnly();
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
            {
            checkEntry((Map.Entry) iter.next());
            }
        ((NamedCache<K, V_FRONT>) getCache()).putAll(map);   // safe (no transformer)
        }

    @Override
    public V_FRONT remove(Object oKey)
        {
        checkReadOnly();

        V_FRONT oOrig = null;
        if (containsKey(oKey))
            {
            NamedCache<K, V_FRONT> cache = (NamedCache<K, V_FRONT>) getCache();  // safe (no transformer)
            if (isCacheValues())
                {
                oOrig = ensureInflated(oKey, /*oValue*/ null);
                removeBlind(oKey);
                }
            else
                {
                oOrig = cache.remove(oKey);
                }
            }
        return oOrig;
        }


    // ----- CacheMap interface ---------------------------------------------

    @Override
    public Map<K, V_FRONT> getAll(Collection<? extends K> colKeys)
        {
        Map<K, V_FRONT> mapResult;

        Map<K, V_FRONT> mapLocal = getInternalCache();
        if (isCacheValues())
            {
            mapResult = new ListMap();
            for (Iterator<? extends K> iter = colKeys.iterator(); iter.hasNext(); )
                {
                K       oKey = iter.next();
                V_FRONT oVal = ensureInflated(oKey, /*oValue*/ null);
                if (oVal != null || containsKey(oKey))
                    {
                    mapResult.put(oKey, oVal);
                    }
                }
            }
        else if (colKeys.size() <= 1)
            {
            // optimization: the requested set is either empty or the caller
            // is doing a combined "containsKey() and get()"
            mapResult = new ListMap();
            for (Iterator<? extends K> iter = colKeys.iterator(); iter.hasNext(); )
                {
                K oKey = iter.next();
                if (mapLocal.containsKey(oKey))
                    {
                    V_FRONT oValue = getInternal(oKey);
                    if ((oValue != null || mapLocal.containsKey(oKey))
                        && InvocableMapHelper.evaluateEntry(getFilter(), oKey, oValue))
                        {
                        mapResult.put(oKey, oValue);
                        }
                    }
                }
            }
        else
            {
            // since the values are not cached, delegate the processing to
            // the underlying NamedCache
            Collection<K> collView = new HashSet<>(colKeys);
            collView.retainAll(mapLocal.keySet());
            mapResult = getAllInternal(collView);

            // verify that the returned contents should all be in this
            // cache
            Filter filter = getFilter();
            if (!mapResult.isEmpty() && new FilterEnumerator(
                    mapResult.values().iterator(), new NotFilter<>(filter)).hasNext())
                {
                Iterator<Map.Entry<K, V_FRONT>> iter = mapResult.entrySet().iterator();
                mapResult = new HashMap<>();
                while (iter.hasNext())
                    {
                    Map.Entry<K, V_FRONT> entry = iter.next();
                    if (InvocableMapHelper.evaluateEntry(filter, entry))
                        {
                        mapResult.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
            }

        return mapResult;
        }

    @Override
    public V_FRONT put(K oKey, V_FRONT oValue, long cMillis)
        {
        if (cMillis == EXPIRY_DEFAULT)
            {
            return put(oKey, oValue);
            }
        else
            {
            checkReadOnly();
            checkEntry(oKey, oValue);

            NamedCache<K, V_FRONT> cache = (NamedCache<K, V_FRONT>) getCache();    // safe (no transformer)
            V_FRONT                oOrig = fromInternal(cache.put(oKey, oValue, cMillis));
            return InvocableMapHelper.evaluateEntry(getFilter(), oKey, oOrig)
                    ? oOrig : null;
            }
        }


    // ----- AbstractKeyBasedMap methods ------------------------------------

    /**
    * Removes the mapping for this key from this map if present. This method
    * exists to allow sub-classes to optimize remove functionality for
    * situations in which the original value is not required.
    *
    * @param oKey  key whose mapping is to be removed from the map
    *
    * @return {@code true} iff the {@code Map} changed as the result of this operation
    */
    @Override
    protected boolean removeBlind(Object oKey)
        {
        checkReadOnly();
        //noinspection SuspiciousMethodCalls
        return containsKey(oKey) && getCache().keySet().remove(oKey);
        }

    @Override
    protected Set<K> getInternalKeySet()
        {
        return getInternalCache().keySet();
        }


    // ----- ObservableMap interface ----------------------------------------

    @Override
    public void addMapListener(MapListener<? super K, ? super V_FRONT> listener)
        {
        addMapListener(listener, (Filter) null, false);
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V_FRONT> listener)
        {
        removeMapListener(listener, (Filter) null);
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V_FRONT> listener, K oKey, boolean fLite)
        {
        azzert(listener != null);

        if (listener instanceof MapTriggerListener)
            {
            throw new IllegalArgumentException("ContinuousQueryCache does not support MapTriggerListeners");
            }

        m_listenerLock.lock();
        try
            {
            if (listener instanceof NamedCacheDeactivationListener)
                {
                m_listDeactivationListener.add((NamedCacheDeactivationListener) listener);
                }
            else
                {
                // need to cache values locally to provide standard (not lite) events
                if (!fLite)
                    {
                    setObserved(true);
                    }

                ensureEventQueue();

                ensureListenerSupport().addListener(instantiateEventRouter(listener, fLite), oKey, fLite);
                }
            }
        finally
            {
            m_listenerLock.unlock();
            }
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V_FRONT> listener, K oKey)
        {
        azzert(listener != null);

        m_listenerLock.lock();
        try
            {
            if (listener instanceof NamedCacheDeactivationListener)
                {
                m_listDeactivationListener.remove((NamedCacheDeactivationListener) listener);
                }
            else
                {
                MapListenerSupport listenerSupport = m_listenerSupport;
                if (listenerSupport != null)
                    {
                    listenerSupport.removeListener(instantiateEventRouter(listener, false), oKey);
                    }
                }
            }
        finally
            {
            m_listenerLock.unlock();
            }
        }

    @Override
    public void addMapListener(MapListener<? super K, ? super V_FRONT> listener,
                                            Filter filter, boolean fLite)
        {
        azzert(listener != null);

        if (listener instanceof MapTriggerListener)
            {
            throw new IllegalArgumentException("ContinuousQueryCache does not support MapTriggerListeners");
            }

        m_listenerLock.lock();
        try
            {
            if (listener instanceof NamedCacheDeactivationListener)
                {
                m_listDeactivationListener.add((NamedCacheDeactivationListener) listener);
                }
            else
                {
                // need to cache values locally to provide event filtering and to
                // provide standard (not lite) events
                if (filter != null || !fLite)
                    {
                    setObserved(true);
                    }

                ensureEventQueue();

                ensureListenerSupport().addListener(instantiateEventRouter(listener, fLite), filter, fLite);
                }
            }
        finally
            {
            m_listenerLock.unlock();
            }
        }

    @Override
    public void removeMapListener(MapListener<? super K, ? super V_FRONT> listener, Filter filter)
        {
        azzert(listener != null);

        m_listenerLock.lock();
        try
            {
            if (listener instanceof NamedCacheDeactivationListener)
                {
                m_listDeactivationListener.remove((NamedCacheDeactivationListener) listener);
                }
            else
                {
                MapListenerSupport listenerSupport = m_listenerSupport;
                if (listenerSupport != null)
                    {
                    listenerSupport.removeListener(instantiateEventRouter(listener, false), filter);
                    }
                }
            }
        finally
            {
            m_listenerLock.unlock();
            }
        }


    // ----- QueryMap interface ---------------------------------------------

    @Override
    public Set<K> keySet(Filter filter)
        {
        return isCacheValues()
                ? InvocableMapHelper.query(this, getIndexMap(), filter, false, false, null)
                : getCache().keySet(mergeFilter(filter));
        }

    @Override
    public Set<Map.Entry<K, V_FRONT>> entrySet(Filter filter)
        {
        return isCacheValues()
                ? InvocableMapHelper.query(this, getIndexMap(), filter, true, false, null)
                : entrySetInternal(mergeFilter(filter));
        }

    @Override
    public Set<Map.Entry<K, V_FRONT>> entrySet(Filter filter, Comparator comparator)
        {
        return isCacheValues()
                ? InvocableMapHelper.query(this, getIndexMap(), filter, true, true, comparator)
                : entrySetInternal(mergeFilter(filter), comparator);
        }

    /**
    * If {@link #isCacheValues()} is {@code true}, the index will be created locally as well as
    * on the {@link NamedCache} this {@code ContinuousQueryCache} wraps, otherwise, the index will be
    * created on the wrapped {@link NamedCache} only.
    *
    * @throws IllegalArgumentException if {@code extractor} is an instance of
    *                                  {@link com.tangosol.util.MapTriggerListener}
    *
    * @see com.tangosol.util.QueryMap#addIndex(ValueExtractor, boolean, Comparator)
    */
    @Override
    public <T, E> void addIndex(ValueExtractor<? super T, ? extends E> extractor, boolean fOrdered,
            Comparator<? super E> comparator)
        {
        // add the index locally if we are caching values
        synchronized (this)
            {
            if (isCacheValues())
                {
                // Note: we pass 'this' rather than a direct reference to the internal map to intercept
                //       MapListener registrations and deserialize the value if necessary
                InvocableMapHelper.addIndex(extractor, fOrdered, comparator,
                        this, ensureIndexMap());
                }
            }
        // addIndex is a no-op if many clients are trying to add the same one
        getCache().addIndex(extractor, fOrdered, comparator);
        }

    /**
    * If {@link #isCacheValues()} is {@code true}, the index will be removed locally, however, this call
    * will not cause the index on the {@link NamedCache} this {@code ContinuousQueryCache} wraps.
    * Developers must remove the index on the wrapped cache manually.
    *
    * @see com.tangosol.util.QueryMap#removeIndex(ValueExtractor)
    * @see #getCache()
    */
    @Override
    public <T, E> void removeIndex(ValueExtractor<? super T, ? extends E> extractor)
        {
        // remove the index locally if we are caching values but do not
        // attempt to remove it from the underlying cache ...
        // removeIndex would kill all the other clients' performance if every
        // client balanced their add and remove index calls, so this cache
        // ignores the suggestion (since it cannot know if it was the cache
        // that originally added the index)
        synchronized (this)
            {
            if (isCacheValues())
                {
                // Note: we pass 'this' rather than a direct reference to the internal map to intercept
                //       MapListener registrations and deserialize the value if necessary
                InvocableMapHelper.removeIndex(extractor, this, ensureIndexMap());
                }
            }
        }

    // ----- NamedCache interface -------------------------------------------

    @Override
    public void truncate()
        {
        checkReadOnly();
        getCache().truncate();
        }

    // ----- InvocableMap interface -----------------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * In order to invoke an entry processor on a back cache in a type-safe
    * manner you must use {@link #getCache()}.{@link #invoke(Object, EntryProcessor) invoke()}
    * instead.
    */
    @Override
    public <R> R invoke(K key, EntryProcessor<K, V_FRONT, R> processor)
        {
        NamedCache<K, V_FRONT> cache = (NamedCache<K, V_FRONT>) getCache();

        return (R) fromInternal(cache.invoke(key, ensureConverted(processor)));
        }

    /**
    * {@inheritDoc}
    * <p>
    * In order to invoke an entry processor on a back cache in a type-safe
    * manner you must use {@link #getCache()}.{@link #invokeAll(Collection, EntryProcessor) invokeAll()}
    * instead.
    */
    @Override
    public <R> Map<K, R> invokeAll(Collection<? extends K> collKeys, EntryProcessor<K, V_FRONT, R> processor)
        {
        if (collKeys.isEmpty())
            {
            return Collections.EMPTY_MAP;
            }

        NamedCache<K, V_FRONT> cache = (NamedCache<K, V_FRONT>) getCache();

        return instantiateConverterMap(cache.invokeAll(collKeys, ensureConverted(processor)));
        }

    /**
    * {@inheritDoc}
    * <p>
    * In order to invoke an entry processor on a back cache in a type-safe
    * manner you must use {@link #getCache()}.{@link #invokeAll(Filter, EntryProcessor) invokeAll()}
    * instead.
    */
    @Override
    public <R> Map<K, R> invokeAll(Filter filter, EntryProcessor<K, V_FRONT, R> processor)
        {
        NamedCache<K, V_FRONT> cache = (NamedCache<K, V_FRONT>) getCache();
        return instantiateConverterMap(cache.invokeAll(mergeFilter(filter), ensureConverted(processor)));
        }

    @Override
    public <R> R aggregate(Collection<? extends K> collKeys, EntryAggregator<? super K, ? super V_FRONT, R> aggregator)
        {
        if (collKeys.isEmpty())
            {
            return aggregator.aggregate(Collections.emptySet());
            }

        if (isCacheValues())
            {
            return aggregator.aggregate(InvocableMapHelper.makeEntrySet(this, collKeys, true));
            }
        else if (isTransformed())
            {
            throw new UnsupportedOperationException(
                    "Aggregation cannot be performed on a transforming CQC that does not cache values locally");
            }
        else
            {
            NamedCache<K, V_FRONT> cache = (NamedCache<K, V_FRONT>) getCache();

            return cache.aggregate(collKeys, aggregator);
            }
        }

    @Override
    public <R> R aggregate(Filter filter, EntryAggregator<? super K, ? super V_FRONT, R> aggregator)
        {
        if (isCacheValues())
            {
            return aggregate(keySet(filter), aggregator);
            }
        else if (isTransformed())
            {
            throw new UnsupportedOperationException(
                    "Aggregation cannot be performed on a transforming CQC that does not cache values locally");
            }
        else
            {
            NamedCache<K, V_FRONT> cache = (NamedCache<K, V_FRONT>) getCache();   // safe (no transformer)
            return cache.aggregate(mergeFilter(filter), aggregator);
            }
        }


    // ----- ConcurrentMap interface ----------------------------------------

    @Override
    public boolean lock(Object oKey, long cWait)
        {
        // locking is counted as a mutating operation
        checkReadOnly();

        return getCache().lock(oKey, cWait);
        }

    @Override
    public boolean lock(Object oKey)
        {
        return lock(oKey, 0);
        }

    @Override
    public boolean unlock(Object oKey)
        {
        // we intentionally don't do the ReadOnly check as you must
        // hold the lock in order to release it
        return getCache().unlock(oKey);
        }


    // ----- NamedCache interface -------------------------------------------

    @Override
    public String getCacheName()
        {
        return m_sName;
        }

    @Override
    public CacheService getCacheService()
        {
        return getCache().getCacheService();
        }

    /**
     * {@inheritDoc}
     * <p>
     *  This method returns an AsyncNamedCache instantiated from the back NamedMap. Transformers and filters
     *  defined on this CQC are not applied to the returned AsyncNamedCache.
     */
    @Override
    public AsyncNamedCache<K, V_FRONT> async(AsyncNamedMap.Option... options)
        {
        CacheService service = getCacheService();

        return service.ensureCache(getCacheName(), service.getContextClassLoader()).async(options);
        }

    @Override
    public boolean isActive()
        {
        NamedCache cache = m_cache;
        return cache != null && cache.isActive();
        }

    @Override
    public boolean isReady()
        {
        NamedCache cache = m_cache;
        return cache != null && cache.isReady();
        }

    @Override
    public void release()
        {
        // shut down the event queue
        shutdownEventQueue();

        synchronized (this)
            {
            releaseListeners();
            resetCacheRefs();

            m_mapLocal = null;
            m_nState   = STATE_DISCONNECTED;
            }
        }

    @Override
    public void destroy()
        {
        // destroys the view but not the underlying cache
        release();
        }

    @Override
    public boolean isDestroyed()
        {
        NamedCache cache = m_cache;
        return cache != null && cache.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        NamedCache cache = m_cache;
        return cache == null || cache.isReleased();
        }

    // ----- internal -------------------------------------------------------

    /**
     * Return the value from the back cache, transforming it in the process if
     * necessary.
     *
     * @param oKey  the key to get the associated value for
     *
     * @return the value for the given key
     */
    protected V_FRONT getInternal(Object oKey)
        {
        //noinspection SuspiciousMethodCalls
        V_BACK value = getCache().get(oKey);
        return m_transformer == null ? (V_FRONT) value : m_transformer.extract(value);
        }

    /**
     * Return multiple values from the back cache, transforming them in the
     * process if necessary.
     *
     * @param colKeys  the keys to get the associated values for
     *
     * @return the value for the given key
     */
    protected Map<K, V_FRONT> getAllInternal(Collection<? extends K> colKeys)
        {
        Map<K, V_BACK> mapResults = getCache().getAll(colKeys);
        return m_transformer == null
               ? (Map<K, V_FRONT>) mapResults
               : transform(mapResults.entrySet(), m_transformer);
        }

    /**
     * Return multiple values from the back cache based on a filter,
     * transforming them in the process if necessary.
     *
     * @param filter  the {@link Filter} to find the entries for
     *
     * @return the value for the given key
     */
    protected Set<Map.Entry<K, V_FRONT>> entrySetInternal(Filter filter)
        {
        Set<Map.Entry<K, V_BACK>> setResults = getCache().entrySet(filter);
        //noinspection RedundantCast
        return m_transformer == null
               ? (Set) setResults
               : transform(setResults, m_transformer).entrySet();
        }

    /**
     * Return multiple values from the back cache based on a filter,
     * transforming them in the process if necessary.
     *
     * @param filter      the {@link Filter} to find the entries for
     * @param comparator  the {@link Comparator}
     *
     * @return the value for the given key
     */
    protected Set<Map.Entry<K, V_FRONT>> entrySetInternal(Filter filter, Comparator comparator)
        {
        Set<Map.Entry<K, V_BACK>> setResults = getCache().entrySet(filter, comparator);
        //noinspection RedundantCast
        return m_transformer == null
               ? (Set) setResults
               : transform(setResults, m_transformer).entrySet();
        }

    /**
     * Transform a set of entries.
     *
     * @param setIn        the set of entries to transform
     * @param transformer  the {@link ValueExtractor transformer} to use
     *
     * @return a Map containing transformed entries
     */
    protected Map<K, V_FRONT> transform(Set<Map.Entry<K, V_BACK>> setIn,
                                        ValueExtractor<? super V_BACK, ? extends V_FRONT> transformer)
        {
        Map<K, V_FRONT> mapOut = new LiteMap<>();
        setIn.forEach(entry -> mapOut.put(entry.getKey(), transformer.extract(entry.getValue())));
        return mapOut;
        }

    /**
     * Return a {@link Filter filter} which merges the {@code ContinuousQueueCache}'s {@link Filter filter} with the
     * supplied {@link Filter filter}.
     *
     * @param filter  the {@link Filter filter} to merge with this cache's {@link Filter filter}
     *
     * @return the merged {@link Filter filter}
     */
    protected Filter mergeFilter(Filter filter)
        {
        if (filter == null)
            {
            return m_filter;
            }

        Filter filterMerged;

        // strip off key association
        Filter  filterCQC = getFilter();
        boolean fKeyAssoc = false;
        Object  oKeyAssoc = null;
        if (filterCQC instanceof KeyAssociatedFilter)
            {
            KeyAssociatedFilter filterAssoc = (KeyAssociatedFilter) filterCQC;

            oKeyAssoc = filterAssoc.getHostKey();
            filterCQC = filterAssoc.getFilter();
            fKeyAssoc = true;

            // if the passed filter is also key-associated, strip it off too
            if (filter instanceof KeyAssociatedFilter)
                {
                filter = ((KeyAssociatedFilter) filter).getFilter();
                }
            }
        else if (filter instanceof KeyAssociatedFilter)
            {
            KeyAssociatedFilter filterAssoc = (KeyAssociatedFilter) filter;

            oKeyAssoc = filterAssoc.getHostKey();
            filter    = filterAssoc.getFilter();
            fKeyAssoc = true;
            }

        if (filter instanceof LimitFilter)
            {
            // To merge a LimitFilter with the CQC Filter we cannot
            // simply And the two, we must And the CQC Filter with the
            // LimitFilter's internal Filter, and then apply the limit
            // on top of that
            LimitFilter filterNew;
            LimitFilter filterOrig = (LimitFilter) filter;
            int         iPageSize  = filterOrig.getPageSize();
            Object      oCookie    = filterOrig.getCookie();
            if (oCookie instanceof LimitFilter)
                {
                // apply the page size as it could have changed since the
                // wrapper was created
                filterNew = (LimitFilter) oCookie;
                filterNew.setPageSize(iPageSize);
                }
            else
                {
                // cookie either didn't exist, or was not our cookie
                // construct the wrapper and stick it in the cookie for
                // future re-use
                filterNew = new LimitFilter(new AndFilter(filterCQC, filterOrig.getFilter()), iPageSize);
                filterOrig.setCookie(filterNew);
                }

            // apply current page number;
            // all other properties are for use by the query processor
            // and only need to be maintained within the wrapper
            filterNew.setPage(filterOrig.getPage());

            filterMerged = filterNew;
            }
        else
            {
            filterMerged = new AndFilter(filterCQC, filter);
            }

        // apply key association
        if (fKeyAssoc)
            {
            filterMerged = new KeyAssociatedFilter(filterMerged, oKeyAssoc);
            }

        return filterMerged;
        }

    /**
     * Check the read-only setting to verify that the cache is NOT read-only.
     *
     * @throws IllegalStateException if the {@code ContinuousQueryCache} is read-only
     */
    protected void checkReadOnly()
        {
        if (isReadOnly())
            {
            throw new IllegalStateException(getCacheName() + " is read-only");
            }
        }

    /**
     * Check the passed value to verify that it does belong in this
     * ContinuousQueryCache.
     *
     * @param entry  a key value pair to check.
     *
     * @throws IllegalArgumentException if the entry does not belong in this
     *                                  {@code ContinuousQueryCache} (based on the cache's filter)
     */
    protected void checkEntry(Map.Entry entry)
        {
        if (!InvocableMapHelper.evaluateEntry(getFilter(), entry))
            {
            throw new IllegalArgumentException(getCacheName()
                    + ": Attempted modification violates filter; key=\""
                    + entry.getKey() + "\", value=\""
                    + entry.getValue() + "\"");
            }
        }

    /**
     * Check the passed value to verify that it does belong in this
     * ContinuousQueryCache.
     *
     * @param oKey    the key for the entry
     * @param oValue  the value for the entry
     *
     * @throws IllegalArgumentException if the entry does not belong in this
     *                                  {@code ContinuousQueryCache} (based on the cache's filter)
     */
    protected void checkEntry(Object oKey, Object oValue)
        {
        if (!InvocableMapHelper.evaluateEntry(getFilter(), oKey, oValue))
            {
            throw new IllegalArgumentException(getCacheName()
                    + ": Attempted modification violates filter; key=\""
                    + oKey + "\", value=\"" + oValue + "\"");
            }
        }

    /**
     * Return a String description of the provided {@code STATE_*} variables.
     *
     * @param nState  the state for which a description will be returned
     *
     * @return the state description
     *
     * @throws IllegalStateException if an unknown state is provided
     *
     * @since 12.2.1.4.5
     */
    protected String getStateString(int nState)
        {
        switch (nState)
            {
            case STATE_CONFIGURED:
                return "STATE_CONFIGURED";
            case STATE_CONFIGURING:
                return "STATE_CONFIGURING";
            case STATE_DISCONNECTED:
                return "STATE_DISCONNECTED";
            case STATE_SYNCHRONIZED:
                return "STATE_SYNCHRONIZED";
            default:
                throw new IllegalStateException("unknown state: " + nState);
            }
        }

    /**
     * Set up the listeners that keep the {@code ContinuousQueryCache} up-to-date.
     *
     * @param fReload  pass {@code true} to force a data reload
     */
    protected synchronized void configureSynchronization(boolean fReload)
        {
        ObservableMap mapLocal = null;

        try
            {
            changeState(STATE_CONFIGURING);
            m_ldtConnectionTimestamp = getSafeTimeMillis();

            NamedCache cache     = getCache();
            Filter     filter        = getFilter();
            boolean    fCacheValues = isCacheValues();

            // get the old filters and listeners
            MapEventFilter filterAddPrev   = m_filterAdd;
            MapListener    listenerAddPrev = m_listenerAdd;

            // determine if this is initial configuration
            boolean fFirstTime = filterAddPrev == null;
            if (fFirstTime)
                {
                // register for service restart notification
                registerServiceListener();
                registerDeactivationListener();

                // create the "remove listener"
                int            nMask          = MapEventFilter.E_UPDATED_LEFT | MapEventFilter.E_DELETED;
                MapEventFilter filterRemove   = new MapEventFilter(nMask, filter);
                MapListener    listenerRemove = instantiateRemoveListener();
                cache.addMapListener(listenerRemove, filterRemove, true);

                m_filterRemove   = filterRemove;
                m_listenerRemove = listenerRemove;
                }
            else
                {
                cache.addMapListener(m_listenerRemove, m_filterRemove, true);
                }

            // configure the "add listener"
            int nMask = MapEventFilter.E_INSERTED | MapEventFilter.E_UPDATED_ENTERED;
            if (fCacheValues)
                {
                nMask |= MapEventFilter.E_UPDATED_WITHIN;
                }
            if (fFirstTime || nMask != filterAddPrev.getEventMask())
                {
                MapEventFilter filterAdd = new MapEventFilter(nMask, filter);
                MapListener listenerAdd  = instantiateAddListener();
                cache.addMapListener(listenerAdd, createTransformerFilter(filterAdd), !fCacheValues);

                m_filterAdd   = filterAdd;
                m_listenerAdd = listenerAdd;

                if (listenerAddPrev != null)
                    {
                    azzert(filterAddPrev != null);
                    cache.removeMapListener(listenerAddPrev, createTransformerFilter(filterAddPrev));
                    }
                }
            else
                {
                cache.addMapListener(listenerAddPrev, createTransformerFilter(filterAddPrev), !fCacheValues);
                }

            // update the local query image
            mapLocal = ensureInternalCache();
            if (fFirstTime || fReload)
                {
                // populate the internal cache
                if (isCacheValues())
                    {
                    Set set = m_transformer == null
                              ? cache.entrySet(filter)
                              : cache.invokeAll(filter, new ExtractorProcessor(m_transformer)).entrySet();

                    // first remove anything that is not in the query
                    if (!mapLocal.isEmpty())
                        {
                        HashSet setQueryKeys = new HashSet();
                        for (Iterator iter = set.iterator(); iter.hasNext(); )
                            {
                            setQueryKeys.add(((Map.Entry) iter.next()).getKey());
                            }
                        mapLocal.keySet().retainAll(setQueryKeys);
                        }

                    // next, populate the local cache
                    for (Iterator iter = set.iterator(); iter.hasNext(); )
                        {
                        Map.Entry entry = (Map.Entry) iter.next();
                        mapLocal.put(entry.getKey(), entry.getValue());
                        }
                    }
                else
                    {
                    // first remove the keys that are not in the query
                    Set setQueryKeys = cache.keySet(filter);
                    if (!mapLocal.isEmpty())
                        {
                        mapLocal.keySet().retainAll(setQueryKeys);
                        }

                    // next, populate the local cache with keys from the query
                    for (Iterator iter = setQueryKeys.iterator(); iter.hasNext(); )
                        {
                        mapLocal.put(iter.next(), null);
                        }
                    }
                }
            else
                {
                // not the first time; internal cache is already populated
                if (fCacheValues)
                    {
                    // used to cache only keys, now caching values too
                    Object[] aoKey;
                    synchronized (mapLocal) // COH-1418
                        {
                        aoKey = mapLocal.keySet().toArray();
                        }
                    Map mapValues = cache.getAll(new ImmutableArrayList(aoKey));
                    mapLocal.putAll(mapValues);
                    }
                else
                    {
                    // used to cache values, now caching only keys
                    for (Iterator iter = mapLocal.entrySet().iterator(); iter.hasNext(); )
                        {
                        ((Map.Entry) iter.next()).setValue(null);
                        }
                    }
                }

            int nCurrentState = getState();
            if (nCurrentState != STATE_CONFIGURING)
                {
                // This is possible if the service thread has set the state
                // to STATE_DISCONNECTED. In this case, throw and let the caller
                // handle retry logic
                throw createUnexpectedStateException(STATE_CONFIGURED, nCurrentState);
                }
            changeState(STATE_CONFIGURED);

            // resolve all changes that occurred during configuration
            Map mapSyncReq = m_mapSyncReq;
            if (!mapSyncReq.isEmpty())
                {
                Object[] aoKey;
                synchronized (mapSyncReq) // COH-1418
                    {
                    aoKey = mapSyncReq.keySet().toArray();
                    }
                Map mapSyncVals = cache.getAll(new ImmutableArrayList(aoKey));
                synchronized (mapSyncReq)
                    {
                    for (Iterator iter = mapSyncReq.keySet().iterator(); iter.hasNext(); )
                        {
                        Object oKey     = iter.next();
                        Object oValue   = mapSyncVals.get(oKey);
                        boolean fExists = oValue != null ||
                                          mapSyncVals.containsKey(oKey);

                        // COH-3847 - an update event was received and deferred
                        // while configuring the CQC, but we need to double-check
                        // that the new value satisfies the filter
                        if (fExists && InvocableMapHelper.evaluateEntry(filter, oKey, oValue))
                            {
                            mapLocal.put(oKey, oValue);
                            }
                        else
                            {
                            mapLocal.remove(oKey);
                            }
                        }

                    // notify other threads that there is nothing to resolve
                    mapSyncReq.clear();
                    }
                }

            nCurrentState = getState();
            if (nCurrentState != STATE_CONFIGURED)
                {
                // This is possible if the service thread has set the state
                // to STATE_DISCONNECTED. In this case, throw and let the caller
                // handle retry logic
                throw createUnexpectedStateException(STATE_CONFIGURED, nCurrentState);
                }
            changeState(STATE_SYNCHRONIZED);
            }
        catch (Throwable e)
            {
            if (mapLocal != null)
                {
                // exception during initial load (COH-2625) or reconciliation;
                // in either case we need to unregister listeners and
                // start from scratch
                releaseListeners();
                }

            // mark as disconnected
            changeState(STATE_DISCONNECTED);
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Simple helper to create an exception for communicating invalid state transitions.
     *
     * @param nExpectedState  expected state
     * @param nActualState    actual state
     *
     * @return a new {@link RuntimeException} with a description of the invalid state transition
     *
     * @since 12.2.1.4.5
     */
    protected RuntimeException createUnexpectedStateException(int nExpectedState, int nActualState)
        {
        String sMsg = "Unexpected synchronization state.  Expected: %s, actual: %s";
        return new IllegalStateException(String.format(sMsg,
                                                       getStateString(nExpectedState),
                                                       getStateString(nActualState)));
        }

    /**
     * Wrap specified {@link MapEventFilter} with a {@link MapEventTransformerFilter} that
     * will either transform cache value using transformer defined for this
     * {@code ContinuousQueryCache}, or remove the old value from the event using
     * {@link SemiLiteEventTransformer}, if no transformer is defined for this
     * {@code ContinuousQueryCache}.
     *
     * @param filterAdd  add {@link MapEventFilter} to wrap
     *
     * @return {@link MapEventTransformerFilter} that wraps specified add {@link MapEventFilter}
     */
    @SuppressWarnings("unchecked")
    protected Filter createTransformerFilter(MapEventFilter filterAdd)
        {
        return new MapEventTransformerFilter(filterAdd, m_transformer == null
                ? SemiLiteEventTransformer.INSTANCE
                : new ExtractorEventTransformer(null, m_transformer));
        }

    /**
     * Ensure that the {@code ContinuousQueryCache} listeners have been registered
     * and its content synchronized with the underlying {@link NamedCache}.
     *
     * @param fReload  the value to pass to the #configureSynchronization
     *                 method if the {@code ContinuousQueryCache} needs to be
     *                 configured and synchronized
     */
    protected void ensureSynchronized(boolean fReload)
        {
        // configure and synchronize the ContinuousQueryCache, if necessary
        if (getState() != STATE_SYNCHRONIZED)
            {
            long    cReconnectMillis = getReconnectInterval();
            boolean fAllowDisconnect = cReconnectMillis > 0;

            if (fAllowDisconnect && getSafeTimeMillis() < m_ldtConnectionTimestamp + cReconnectMillis)
                {
                // don't try to re-connect just yet
                return;
                }

            Throwable eConfig   = null;
            int       cAttempts = fAllowDisconnect ? 1 : 3;
            for (int i = 0; i < cAttempts; ++i)
                {
                synchronized (this)
                    {
                    int nState = getState();
                    if (nState == STATE_DISCONNECTED)
                        {
                        try
                            {
                            configureSynchronization(fReload);
                            return;
                            }
                        catch (Throwable e)
                            {
                            eConfig = e;
                            }
                        }
                    else
                        {
                        azzert(nState == STATE_SYNCHRONIZED);
                        return;
                        }
                    }
                }

            if (!fAllowDisconnect)
                {
                String sMsg = "This ContinuousQueryCache is disconnected. Retry the operation again.";
                if (CacheFactory.isLogEnabled(CacheFactory.LOG_MAX))
                    {
                    throw new IllegalStateException(sMsg, eConfig);
                    }
                else
                    {
                    throw new IllegalStateException(sMsg);
                    }
                }
            }
        }

    /**
     * Called when an event has occurred. Allows the key to be logged as
     * requiring deferred synchronization if the event occurs during the
     * configuration or population of the {@code ContinuousQueryCache}.
     *
     * @param oKey  the key that the event is related to
     *
     * @return {@code true} if the event processing has been deferred
     */
    protected boolean isEventDeferred(Object oKey)
        {
        boolean fDeferred = false;

        Map mapSyncReq = m_mapSyncReq;
        if (mapSyncReq != null)
            {
            synchronized (m_nState)
                {
                if (getState() <= STATE_CONFIGURING)
                    {
                    // handle a truncation event being received during configuration
                    // clear any currently pending events.
                    if (DeactivationListener.class.getName().equals(oKey))
                        {
                        mapSyncReq.clear();
                        }
                    else
                        {
                        // since the listeners are being configured and the local
                        // cache is being populated, assume that the event is
                        // being processed out-of-order and requires a subsequent
                        // synchronization of the corresponding value
                        mapSyncReq.put(oKey, null);
                        }
                    fDeferred = true;
                    }
                else
                    {
                    // since an event has arrived after the configuration
                    // completed, the event automatically resolves the sync
                    // requirement
                    mapSyncReq.keySet().remove(oKey);
                    }
                }
            }
        return fDeferred;
        }

    /**
     * Ensure that the map of indexes maintained by this cache exists.
     *
     * @return the map of indexes.
     */
    protected Map ensureIndexMap()
        {
        synchronized (this)
            {
            if (m_mapIndex == null)
                {
                m_mapIndex = new SafeHashMap();
                }
            return m_mapIndex;
            }
        }

    /**
     * Get the map of indexes maintained by this cache.
     *
     * @return the map of indexes.
     */
    protected Map getIndexMap()
        {
        return m_mapIndex;
        }

    /**
     * Release the entire index map.
     */
    protected void releaseIndexMap()
        {
        Map mapIndex = getIndexMap();
        if (mapIndex != null)
            {
            HashSet setExtractors = new HashSet(mapIndex.keySet());
            for (Iterator iter = setExtractors.iterator(); iter.hasNext(); )
                {
                removeIndex((ValueExtractor) iter.next());
                }
            }
        }

    /**
     * Release the {@link MapListener listeners}.
     */
    protected void releaseListeners()
        {
        NamedCache cache = m_cache;
        if (cache != null)
            {
            unregisterServiceListener();
            unregisterDeactivationListener();

            MapListener listenerAdd = m_listenerAdd;
            if (listenerAdd != null)
                {
                try
                    {
                    cache.removeMapListener(listenerAdd, createTransformerFilter(m_filterAdd));
                    }
                catch (Exception ignored)
                    {
                    }
                m_listenerAdd = null;
                }
            m_filterAdd = null;

            MapListener listenerRemove = m_listenerRemove;
            if (listenerRemove != null)
                {
                try
                    {
                    cache.removeMapListener(listenerRemove, m_filterRemove);
                    }
                catch (Exception ignored)
                    {
                    }
                m_listenerRemove = null;
                }
            m_filterRemove = null;
            }
        m_listenerSupport = null;
        }


    // ----- inner class: AddListener ---------------------------------------

    /**
     * Factory Method: Instantiate a {@link MapListener} for adding items to the
     * {@code ContinuousQueryCache}, and (if there are listeners on the
     * {@code ContinuousQueryCache}) for dispatching inserts and updates.
     *
     * @return a new {@link MapListener} that will add items to and update items in
     *         the {@code ContinuousQueryCache}
     */
    protected MapListener<K, V_FRONT> instantiateAddListener()
        {
        return new AddListener();
        }

    /**
     * A {@link MapListener} for adding items to the {@code ContinuousQueryCache}.
     */
    public class AddListener
            extends MultiplexingMapListener<K, V_FRONT>
            implements MapListenerSupport.SynchronousListener<K, V_FRONT>
        {
        @Override
        protected void onMapEvent(MapEvent<K, V_FRONT> evt)
            {
            ContinuousQueryCache cqc  = ContinuousQueryCache.this;
            K                    oKey = evt.getKey();
            if (!cqc.isEventDeferred(oKey))
                {
                // guard against possible NPE; one could theoretically occur
                // during construction or after release; one occurred during
                // testing of a deadlock issue (COH-1418)
                Map<K, V_FRONT> map = cqc.m_mapLocal;
                if (map != null)
                    {
                    map.put(oKey, cqc.isCacheValues() ? evt.getNewValue() : null);
                    }
                }
            }

        /**
         * Produce a human-readable description of this object.
         *
         * @return a {@link String} describing this object
         */
        @Override
        public String toString()
            {
            return "AddListener[" + ContinuousQueryCache.this.toString() + "]";
            }
        }

    // ----- inner class: RemoveListener ------------------------------------

    /**
     * Factory Method: Instantiate a {@link MapListener} for evicting items from the
     * {@code ContinuousQueryCache}.
     *
     * @return a new {@link MapListener} that will listen to all events that will
     *         remove items from the {@code ContinuousQueryCache}
     */
    protected MapListener<K, V_FRONT> instantiateRemoveListener()
        {
        return new RemoveListener();
        }

    /**
     * A {@link MapListener} for evicting items from the {@code ContinuousQueryCache}.
     */
    public class RemoveListener
            extends MultiplexingMapListener<K, V_FRONT>
            implements MapListenerSupport.SynchronousListener<K, V_FRONT>
        {
        @Override
        protected void onMapEvent(MapEvent<K, V_FRONT> evt)
            {
            ContinuousQueryCache cqc  = ContinuousQueryCache.this;
            K                    oKey = evt.getKey();
            if (!cqc.isEventDeferred(oKey))
                {
                // guard against possible NPE; one could theoretically occur
                // during construction or after release; one occurred during
                // testing of a deadlock issue (COH-1418)
                Map<K, V_FRONT> map = cqc.m_mapLocal;
                if (map != null)
                    {
                    map.remove(oKey);
                    }
                }
            }

        /**
         * Produce a human-readable description of this object.
         *
         * @return a {@link String} describing this object
         */
        @Override
        public String toString()
            {
            return "RemoveListener[" + ContinuousQueryCache.this.toString() + "]";
            }
        }


    // ----- inner class: Service Listener ----------------------------------

    /**
     * Instantiate and register a {@link MemberListener} with the underlying cache
     * service.
     * <p>
     * The primary goal of that {@link MemberListener listener} is invalidation of the front map
     * in case of the service [automatic] restart.
     */
    protected void registerServiceListener()
        {
        // automatic front map clean up (upon service restart)
        // requires a MemberListener implementation
        CacheService service = getCacheService();
        if (service != null)
            {
            try
                {
                MemberListener listener = new ServiceListener();
                service.addMemberListener(listener);
                m_listenerService = listener;
                }
            catch (UnsupportedOperationException ignored)
                {
                }
            }
        }

    /**
     * Unregister underlying cache service {@link MemberListener member listener}.
     */
    protected void unregisterServiceListener()
        {
        try
            {
            getCacheService().removeMemberListener(m_listenerService);
            }
        catch (RuntimeException ignored)
            {
            }
        }

    /**
     * {@link MemberListener} for the underlying cache's service.
     * <p>
     * The primary goal of that listener is invalidation of the
     * {@code ContinuousQueryCache} in case of the corresponding CacheService
     * [automatic] restart.
     */
    protected class ServiceListener
            implements MemberListener
        {
        @Override
        public void memberJoined(MemberEvent evt)
            {
            }

        @Override
        public void memberLeaving(MemberEvent evt)
            {
            }

        @Override
        public void memberLeft(MemberEvent evt)
            {
            if (evt.isLocal())
                {
                changeState(STATE_DISCONNECTED);
                }
            }

        @Override
        public void memberRecovered(MemberEvent evt)
            {
            changeState(STATE_DISCONNECTED);
            }

        /**
         * Produce a human-readable description of this object.
         *
         * @return a String describing this object
         */
        @Override
        public String toString()
            {
            return "ServiceListener[" + ContinuousQueryCache.this.toString() + "]";
            }
        }

    /**
     * Instantiate and register a {@link NamedCacheDeactivationListener} with the underlying cache
     * service.
     * <p>
     * The primary goal of that {@link NamedCacheDeactivationListener listener} is invalidation of the named cache
     * in case the named caches is destroyed / truncated.
     *
     * @since 12.2.1.4
     */
    protected void registerDeactivationListener()
        {
        // automatic named cache clean up (upon cache destruction)
        // requires a NamedCacheDeactivationListener implementation
        CacheService service = getCacheService();
        if (service != null)
            {
            try
                {
                NamedCacheDeactivationListener deactivationListener;
                deactivationListener = m_listenerDeactivation = new DeactivationListener();
                m_cache.addMapListener(deactivationListener);
                }
            catch (UnsupportedOperationException ignored)
                {
                }
            }
        }

    /**
     * Unregister underlying cache service member listener.
     */
    protected void unregisterDeactivationListener()
        {
        MapListener deactivationListener = m_listenerDeactivation;
        if (deactivationListener != null)
            {
            try
                {
                NamedCache cache = m_cache;
                if (cache != null)
                    {
                    cache.removeMapListener(deactivationListener);
                    }
                }
            catch (RuntimeException ignored)
                {
                }
            }
        }

    // ----- inner class: InternalMapListener -------------------------------

    /**
     * This listener allows interception of all events triggered by the internal
     * {@link ObservableMap} of the {@code ContinuousQueryCache}.
     *
     * @since 12.2.1.4
     */
    protected class InternalMapListener
            extends MultiplexingMapListener<K, V_FRONT>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct the {@link MapListener} to be registered with the internal {@link ObservableMap}.
         *
         * @param listenerSupport  the {@link MapListenerSupport} to dispatch events with
         * @param convKey          the {@link Converter} for keys
         * @param convValue        the {@link Converter} for values
         */
        public InternalMapListener(MapListenerSupport listenerSupport, Converter convKey, Converter convValue)
            {
            f_listenerSupport = listenerSupport;
            f_convKey         = convKey;
            f_convValue       = convValue;
            }

        // ----- MultiplexingMapListener methods ----------------------------

        /**
        * Dispatch events received from the internal map to the {@link MapListenerSupport}'s registered
        * {@link MapListener}s.
        *
        * @param evt  the {@link MapEvent} carrying the insert, update or delete
        */
        @Override
        protected void onMapEvent(MapEvent<K, V_FRONT> evt)
            {
            if (evt.getId() == MapEvent.ENTRY_UPDATED)
                {
                if (!(evt.getNewValue() instanceof Binary) && evt.getOldValue() instanceof Binary)
                    {
                    // suppress events caused due to lazy deserialization
                    return;
                    }
                }
            f_listenerSupport.fireEvent(ConverterCollections.getMapEvent(
                    ContinuousQueryCache.this, evt, f_convKey, f_convValue), false);
            }

        // ----- data members ---------------------------------------------------

        /**
         * The {@link Converter} to be applied to keys.
         */
        protected final Converter f_convKey;

        /**
         * The {@link Converter} to be applied to values.
         */
        protected final Converter f_convValue;

        /**
         * The {@link MapListenerSupport} to dispatch events to.
         */
        protected final MapListenerSupport f_listenerSupport;
        }

    // ----- inner class: EventRouter ---------------------------------------

    /**
     * Factory Method: Instantiate a listener on the internal map that will
     * direct events to the passed listener, either synchronously or
     * asynchronously as appropriate.
     *
     * @param listener  the {@link MapListener listener} to route to
     * @param fLite     {@code true} to indicate that the {@link MapEvent} objects do
     *                  not have to include the OldValue and NewValue
     *                  property values in order to allow optimizations
     *
     * @return a new {@link EventRouter} specific to the passed {@link MapListener listener}
     */
    protected EventRouter<K, V_FRONT> instantiateEventRouter(MapListener<? super K, ? super V_FRONT> listener,
                                                             boolean fLite)
        {
        return new EventRouter<>(listener, fLite);
        }

    /**
     * An EventRouter routes events from the internal cache of the
     * {@code ContinuousQueryCache} to the client listeners, and it can do so
     * asynchronously when appropriate.
     *
     * @param <K>  the type parameter
     * @param <V>  the type parameter
     */
    @SuppressWarnings("TypeParameterHidesVisibleType")
    protected class EventRouter<K, V>
            extends MultiplexingMapListener<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an EventRouter to route events from the internal cache
         * of the {@code ContinuousQueryCache} to the client listeners.
         *
         * @param listener  a client listener
         * @param fLite     true to indicate that the {@link MapEvent} objects do
         *                  not have to include the OldValue and NewValue
         *                  property values in order to allow optimizations
         */
        public EventRouter(MapListener<? super K, ? super V> listener, boolean fLite)
            {
            m_listener = listener;
            f_fLite    = fLite;
            }

        // ----- MultiplexingMapListener methods ----------------------------

        @Override
        protected void onMapEvent(MapEvent<K, V> evt)
            {
            final MapListener<? super K, ? super V> listener = m_listener;

            MapEvent<K, V> event = evt;

            if (f_fLite)
                {
                event = new MapEvent(ContinuousQueryCache.this, evt.getId(), evt.getKey(), null,
                        null);
                event = evt instanceof FilterEvent
                        ? new FilterEvent(event, ((((FilterEvent) evt).getFilter())))
                        : event;
                }

            final MapEvent<K, V> eventRoute = event;
            if (listener instanceof MapListenerSupport.SynchronousListener)
                {
                try
                    {
                    eventRoute.dispatch(listener);
                    }
                catch (RuntimeException e)
                    {
                    err(e);
                    }
                }
            else
                {
                TaskDaemon eventQueue = getEventQueue();

                // COH-2413 - guard against IllegalStateException after release()
                if (eventQueue != null)
                    {
                    Runnable task = () -> eventRoute.dispatch(listener);

                    eventQueue.executeTask(task);
                    }
                }
            }

        // ----- Object methods ---------------------------------------------

        /**
        * Determine a hash value for the EventRouter object according to the
        * general {@link Object#hashCode()} contract.
        *
        * @return an integer hash value for this EventRouter
        */
        @Override
        public int hashCode()
            {
            return m_listener.hashCode();
            }

        /**
         * Compare the EventRouter with another object to determine equality.
         *
         * @return {@code true} iff this {@link EventRouter} and the passed object are
         *         equivalent listeners
         */
        @Override
        public boolean equals(Object o)
            {
            return o instanceof EventRouter && this.m_listener.equals(((EventRouter) o).m_listener);
            }

        /**
         * Produce a human-readable description of this EventRouter.
         *
         * @return a String describing this EventRouter
         */
        @Override
        public String toString()
            {
            return "EventRouter[" + m_listener + "]";
            }

        // ----- data members -----------------------------------------------

        /**
         * The MapListener to route to.
         */
        protected MapListener<? super K, ? super V> m_listener;

        /**
         * Flag indicating {@link MapEvent} objects do not have to include the OldValue and NewValue
         * property values in order to allow optimizations.
         */
        protected final boolean f_fLite;
        }

    // ----- inner class: EventQueue ----------------------------------------

    /**
     * Create a self-processing event queue.
     *
     * @return a {@link TaskDaemon} onto which events can be placed in order to be
     *         dispatched asynchronously
     */
    protected TaskDaemon instantiateEventQueue()
        {
        return new TaskDaemon("EventQueue:" + getCacheName());
        }

    /**
     * Obtain this {@code ContinuousQueryCache}'s event queue.
     *
     * @return the event queue that this {@code ContinuousQueryCache} uses to dispatch
     *         its events to its non-synchronous listeners
     */
    protected TaskDaemon getEventQueue()
        {
        return m_eventQueue;
        }

    /**
     * Obtain the existing event queue or create one if none exists.
     *
     * @return the event queue that this {@code ContinuousQueryCache} uses to dispatch its events to its
     *         non-synchronous listeners
     */
    @SuppressWarnings("UnusedReturnValue")
    protected synchronized TaskDaemon ensureEventQueue()
        {
        TaskDaemon queue = getEventQueue();
        if (queue == null)
            {
            m_eventQueue = queue = instantiateEventQueue();
            }
        return queue;
        }

    /**
     * Shut down running event queue.
     */
    protected void shutdownEventQueue()
        {
        TaskDaemon eventQueue = getEventQueue();
        if (eventQueue != null)
            {
            m_eventQueue = null;
            eventQueue.stop(false);
            }
        }

    // ----- inner class: DeactivationListener ------------------------------

    /**
     * DeactivationListener for the underlying NamedCache.
     * <p>
     * The primary goal of that listener is invalidation of the named cache when
     * the named cache is destroyed or to truncate the local cache if the back cache has been truncated.
     *
     * @since 12.2.1.4
     */
    protected class DeactivationListener
            extends AbstractMapListener
            implements NamedCacheDeactivationListener
        {
        // ----- MapListener methods ----------------------------------------
        @Override
        public void entryDeleted(MapEvent evt)
            {
            // destroy/disconnect event
            changeState(STATE_DISCONNECTED);
            dispatchDeactivationEvent(evt.getId());
            }

        @Override
        public void entryUpdated(MapEvent evt)
            {
            // don't process if event should be deferred.  Record
            // the event happening to re-trigger synchronization
            if (!isEventDeferred(DeactivationListener.class.getName()))
                {
                // process truncate
                Map<?, ?> local = m_mapLocal;
                if (local != null)
                    {
                    if (local instanceof ObservableHashMap)
                        {
                        ((ObservableHashMap<?, ?>) local).truncate();
                        }
                    else
                        {
                        local.clear();
                        }
                    }
                dispatchDeactivationEvent(evt.getId());
                }
            }
        }

    // ----- inner class: ConverterAsynchronousProcessorWrapper -------------

    /**
     * Wraps an {@link AsynchronousProcessor} to ensure the result of the EntryProcessor
     * execution is deserialized prior to passing to the provided AsynchronousProcessor.
     *
     * @since 12.2.1.4
     */
    protected class ConverterAsynchronousProcessor
            extends AsynchronousProcessor
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct the processor to wrap the provided {@link AsynchronousProcessor} in order to
         * ensure results are properly converted prior to return.
         *
         * @param processor  the processor to wrap
         */
        public ConverterAsynchronousProcessor(AsynchronousProcessor processor)
            {
            super(processor);

            f_processor = processor;
            // save reference in case CQC is released before result is accessed
            f_convUp    = ContinuousQueryCache.this.m_converterFromBinary;
            }

        // ----- AsynchronousProcessor methods ------------------------------

        @Override
        public void onResult(final Map.Entry entry)
            {
            Converter convUp   = f_convUp;
            Converter convDown = NullImplementation.getConverter();

            f_processor.onResult(ConverterCollections.getEntry(entry, convUp, convUp, convDown));
            }

        @Override
        public void onException(Throwable eReason)
            {
            f_processor.onException(eReason);
            }

        @Override
        public void onComplete()
            {
            f_processor.onComplete();
            }

        @Override
        public int getUnitOfOrderId()
            {
            return f_processor.getUnitOfOrderId();
            }

        @Override
        public EntryProcessor getProcessor()
            {
            return f_processor.getProcessor();
            }

        @Override
        public Object process(InvocableMap.Entry entry)
            {
            return f_processor.process(entry);
            }

        @Override
        public Map processAll(Set setEntries)
            {
            return f_processor.processAll(setEntries);
            }

        // ----- AsynchronousAgent methods ----------------------------------

        @Override
        public void bind(FlowControl control)
            {
            f_processor.bind(control);
            }

        @Override
        public void flush()
            {
            f_processor.flush();
            }

        @Override
        public boolean checkBacklog(Continuation continueNormal)
            {
            return f_processor.checkBacklog(continueNormal);
            }

        @Override
        public long drainBacklog(long cMillis)
            {
            return f_processor.drainBacklog(cMillis);
            }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
            {
            return f_processor.cancel(mayInterruptIfRunning);
            }

        @Override
        public boolean isCancelled()
            {
            return f_processor.isCancelled();
            }

        @Override
        public boolean isDone()
            {
            return f_processor.isDone();
            }

        @Override
        public Object get() throws InterruptedException, ExecutionException
            {
            return f_processor.get();
            }

        @Override
        public Object get(long cTimeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException
            {
            return f_processor.get(cTimeout, unit);
            }

        @Override
        public Object getResult()
            {
            return f_processor.getResult();
            }

        @Override
        public Throwable getException()
            {
            return f_processor.getException();
            }

        @Override
        public boolean isCompletedExceptionally()
            {
            return f_processor.isCompletedExceptionally();
            }

        @Override
        public CompletableFuture getCompletableFuture()
            {
            return f_processor.getCompletableFuture();
            }

        // ----- data members -----------------------------------------------

        /**
         * The delegate {@link AsynchronousProcessor}.
         */
        protected final AsynchronousProcessor f_processor;

        /**
         * Converter to deserialize {@link Binary} values.
         */
        protected final Converter f_convUp;
        }

    // ----- helper methods -------------------------------------------------

    /**
    * Configure the local {@link MapListenerSupport} and register the intercepting
    * {@link MapListener} with the internal {@link ObservableMap}.
    *
    * @since 12.2.1.4
    */
    protected synchronized MapListenerSupport ensureListenerSupport()
        {
        MapListenerSupport listenerSupport = m_listenerSupport;
        if (listenerSupport == null)
            {
            listenerSupport = m_listenerSupport = new MapListenerSupport();

            Converter convKey   = NullImplementation.getConverter();
            Converter convValue = m_converterFromBinary;
            m_mapLocal.addMapListener(new InternalMapListener(listenerSupport, convKey, convValue));
            }
        return listenerSupport;
        }

    private void dispatchDeactivationEvent(int eventType)
        {
        m_listenerLock.lock();
        try
            {
            MapEvent<K, V_FRONT> evt = new MapEvent<>(ContinuousQueryCache.this, eventType, null, null, null);
            for (MapListener<K, V_FRONT> listener : m_listDeactivationListener)
                {
                switch (evt.getId())
                    {
                    case MapEvent.ENTRY_UPDATED:
                        listener.entryUpdated(evt);
                        break;
                    case MapEvent.ENTRY_DELETED:
                        listener.entryDeleted(evt);
                        break;
                    }
                }
            }
        finally
            {
            m_listenerLock.unlock();
            }
        }

    /**
     * If the internal cache value associated with the provided key is {@link Binary},
     * deserialize the value and store it back to the internal cache in its deserialized form.
     *
     * @param oKey    the key
     * @param oValue  optional original value associated with the key.  If not provided, there
     *                will be a cost of an additional call to obtain the value currently
     *                associated with the key
     *
     * @return the deserialized value
     *
     * @since 12.2.1.4
     */
    protected V_FRONT ensureInflated(Object oKey, Object oValue)
        {
        ObservableMap mapInternal = getInternalCache();

        Object oInflated = oValue == null ? mapInternal.get(oKey) : oValue;
        if (oInflated instanceof Binary)
            {
            oInflated = fromInternal(oInflated);
            mapInternal.replace(oKey, oValue, oInflated);
            }
        return (V_FRONT) oInflated;
        }

    /**
     * Deserialize the provided {@link Binary} value.
     *
     * @param <T>       the type parameter
     * @param binValue  the {@link Binary} value to deserialize
     *
     * @return the deserialized result
     *
     * @since 12.2.1.4
     */
    protected <T> T fromInternal(Object binValue)
        {
        return (T) m_converterFromBinary.convert(binValue);
        }

    /**
     * Serialize the provided value into a {@link Binary}.
     *
     * @param oValue  the object to serialize.
     *
     * @return the serialized result
     *
     * @since 12.2.1.4
     */
    protected Binary toInternal(Object oValue)
        {
        return (Binary) m_converterToBinary.convert(oValue);
        }

    /**
     * Provides out-bound conversion (i.e. conversion to values clients expect) of internal values.
     *
     * @param map  the {@link Map} to wrap
     *
     * @return the {@link Map} that will be returned to the client
     *
     * @since 12.2.1.4
     */
    protected Map<K, V_FRONT> instantiateConverterMap(Map<K, V_FRONT> map)
        {
        if (isBinaryNamedCache())
            {
            Converter convUp   = m_converterFromBinary;
            Converter convDown = NullImplementation.getConverter();
            return ConverterCollections.getMap(map, convUp, convDown, convUp, convDown);
            }
        return map;
        }

    /**
     * Wrap any {@link AsynchronousProcessor} instances with a custom wrapper to perform
     * conversion of result returned by the processor.
     *
     * @param processor  the {@link EntryProcessor}
     *
     * @return the {@link EntryProcessor} to leverage when dispatching aggregation requests.
     *
     * @since 12.2.1.4
     */
    protected EntryProcessor ensureConverted(EntryProcessor processor)
        {
        return processor instanceof AsynchronousProcessor && isBinaryNamedCache()
               ? new ConverterAsynchronousProcessor((AsynchronousProcessor) processor)
               : processor;
        }

    /**
     * Returns {@code true} if provided cache is configured to use the
     * {@link NullImplementation#getClassLoader() NullImplementation classloader} which means
     * the values to/from from the cache will be {@link Binary}.
     *
     * @param cache  the cache
     *
     * @return {@code true} if the cache is configured to use the
     *         {@link NullImplementation#getClassLoader() NullImplementation classloader}
     *
     * @since 12.2.1.4
     */
    protected boolean isBinaryNamedCache(NamedCache cache)
        {
        ClassLoader loader = null;
        if (cache instanceof ClassLoaderAware)
            {
            loader = ((ClassLoaderAware) cache).getContextClassLoader();
            }

        return loader == NullImplementation.getClassLoader();
        }

    /**
     * Return {@code true} if the current back cache is configured to use {@link Binary} values.
     *
     * @return {@code true} if the back cache is configured to use {@link Binary} values
     *
     * @since 12.2.1.4
     */
    protected boolean isBinaryNamedCache()
        {
        azzert(m_converterFromBinary != null);

        return m_converterFromBinary != NullImplementation.getConverter();
        }

    /**
     * Create a {@link Serializer} appropriate for the mode this cache is operating under
     * (i.e., binary vs non-binary).
     *
     * @return a new {@link Serializer}
     *
     * @since 12.2.1.4
     */
    protected Serializer instantiateSerializer()
        {
        CacheService      service = getCacheService();
        SerializerFactory factory = service.getDependencies().getSerializerFactory();

        return factory == null
               ? ExternalizableHelper.ensureSerializer(m_loader)
               : factory.createSerializer(m_loader);
        }

    /**
     * Instantiate the {@link Converter converters} necessary to support the processing mode that this
     * {@code ContinuousQueryCache} will be operating under.
     *
     * @param cache  the underlying cache
     *
     * @return the named cache
     *
     * @since 12.2.1.4
     */
    protected NamedCache ensureConverters(NamedCache cache)
        {
        Converter  convDown   = m_converterFromBinary;
        Converter  convUp     = m_converterToBinary;
        Converter  convNull   = NullImplementation.getConverter();
        NamedCache cacheLocal = cache;

        if (convDown == null && convUp == null)
            {
            if (isBinaryNamedCache(cacheLocal))
                {
                ClassLoader  loader     = m_loader;
                CacheService service    = cacheLocal.getCacheService();
                Serializer   serializer = loader == null || loader == service.getContextClassLoader()
                        ? service.getSerializer()
                        : instantiateSerializer();

                convDown = value -> ExternalizableHelper.toBinary(value, serializer);
                convUp   = value -> value instanceof Binary
                        ? ExternalizableHelper.fromBinary((Binary) value, serializer)
                        : value;
                }
            else
                {
                convDown = convUp = NullImplementation.getConverter();
                }

            // Note: value up converter is intentionally a no-op converter to avoid
            //       deserialization until it needs to be surfaced to the client
            cacheLocal = ConverterCollections.getNamedCache(cacheLocal, convUp, convDown, convNull, convDown);

            m_converterFromBinary = convUp;
            m_converterToBinary   = convDown;
            }
        return cacheLocal;
        }

    /**
     * Return the default name used by the CQC.
     *
     * @param sCacheName   the cache name this CQC is backed by
     * @param filter       the filter that reduces the set of entries of this CQC
     * @param transformer  the {@link ValueExtractor transformer} to apply to the
     *                     raw entries
     *
     * @return the default name used by the CQC
     */
    protected static String getDefaultName(String sCacheName, Filter filter, ValueExtractor transformer)
        {
        return String.format("ContinuousQueryCache{Cache=%s, Filter=%s, Transformer=%s}",
                sCacheName, filter, transformer);
        }

    // ----- constants ------------------------------------------------------

    /**
     * State: Disconnected state. The content of the {@code ContinuousQueryCache} is not
     * fully synchronized with the underlying [clustered] cache. If the value of
     * the ReconnectInterval property is zero, it must be configured
     * (synchronized) before it can be used.
     *
     * @since Coherence 3.4
     */
    public static final int STATE_DISCONNECTED  = 0;

    /**
     * State: The {@code ContinuousQueryCache} is configuring or re-configuring its
     * listeners and content.
     */
    public static final int STATE_CONFIGURING  = 1;

    /**
     * State: The {@code ContinuousQueryCache} has been configured.
     */
    public static final int STATE_CONFIGURED   = 2;

    /**
     * State: The {@code ContinuousQueryCache} has been configured and fully
     * synchronized.
     */
    public static final int STATE_SYNCHRONIZED = 3;


    // ----- data members ---------------------------------------------------

    /**
     * The Supplier of the {@link NamedCache} to create a view of.
     * The {@link Supplier} must return a new instance every time the
     * {@link Supplier supplier's} {@link Supplier#get get()} method is called.
     *
     * @since 12.2.1.4
     */
    private Supplier<NamedCache<K, V_BACK>> f_supplierCache;

    /**
     * The underlying {@link NamedCache} object.
     */
    private volatile NamedCache<K, V_BACK> m_cache;

    /**
     * The name of the underlying {@link NamedCache}. A copy is kept here because the
     * reference to the underlying {@link NamedCache} is discarded when this cache is
     * released.
     */
    protected String m_sName;

    /**
     * The filter that represents the subset of information from the
     * underlying {@link NamedCache} that this {@code ContinuousQueryCache} represents.
     */
    protected Filter m_filter;

    /**
     * The option of whether or not to locally cache values.
     */
    protected boolean m_fCacheValues;

    /**
     * The transformer that should be used to convert values from the
     * underlying cache.
     */
    protected ValueExtractor<? super V_BACK, ? extends V_FRONT> m_transformer;

    /**
     * The option to disallow modifications through this {@code ContinuousQueryCache}
     * interface.
     */
    protected boolean m_fReadOnly;

    /**
     * The interval (in milliseconds) that indicates how often the
     * {@code ContinuousQueryCache} should attempt to synchronize its content with the
     * underlying cache in case the connection is severed.
     */
    protected long m_cReconnectMillis;

    /**
     * The timestamp when the synchronization was last attempted.
     */
    protected volatile long m_ldtConnectionTimestamp;

    /**
     * The keys that are in this {@code ContinuousQueryCache}, and (if
     * {@link #m_fCacheValues} is true) the corresponding values as well.
     */
    protected ObservableMap<K, V_FRONT> m_mapLocal;

    /**
     * State of the {@code ContinuousQueryCache}. One of the {@code STATE_*} enums.
     */
    protected volatile Integer m_nState;

    /**
     * While the {@code ContinuousQueryCache} is configuring or re-configuring its
     * listeners and content, any events that are received must be logged to
     * ensure that the corresponding content is in sync.
     */
    protected volatile Map m_mapSyncReq;

    /**
     * The event queue for this {@code ContinuousQueryCache}.
     */
    protected volatile TaskDaemon m_eventQueue;

    /**
     * Keeps track of whether the {@code ContinuousQueryCache} has listeners that
     * require this cache to cache values.
     */
    protected boolean m_fListeners;

    /**
     * The {@link MapEventFilter} that uses the {@code ContinuousQueryCache's} filter to
     * select events that would add elements to this cache's contents.
     */
    protected MapEventFilter m_filterAdd;

    /**
     * The {@link MapEventFilter} that uses the {@code ContinuousQueryCache's} filter to
     * select events that would remove elements from this cache's contents.
     */
    protected MapEventFilter m_filterRemove;

    /**
     * The {@link MapListener listener} that gets information about what should be in this cache.
     */
    protected MapListener<K, V_FRONT> m_listenerAdd;

    /**
     * The {@link MapListener listener} that gets information about what should be thrown out of
     * this cache.
     */
    protected MapListener<K, V_FRONT> m_listenerRemove;

    /**
     * The cache service {@link MemberListener} for the underlying {@link NamedCache}.
     */
    protected MemberListener m_listenerService;

    /**
     * The map of indexes maintained by this cache. The keys of the Map are
     * {@link ValueExtractor} objects, and for each key, the corresponding value
     * stored in the Map is a MapIndex object.
     */
    protected Map m_mapIndex;

    /**
     * The {@link NamedCacheDeactivationListener}.
     *
     * @since 12.2.1.4
     */
    protected NamedCacheDeactivationListener m_listenerDeactivation;

    /**
     * The optional {@link MapListener} that may be provided during {@code ContinuousQueryCache}
     * construction.
     *
     * @since 12.2.1.4
     */
    protected MapListener<? super K, ? super V_FRONT> m_mapListener;

    /**
     * {@link Converter} that will be used to convert values from {@link Binary binary}.
     *
     * @since 12.2.1.4
     */
    protected Converter m_converterFromBinary;

    /**
     * {@link Converter} that will be used to convert values to {@link Binary binary}.
     *
     * @since 12.2.1.4
     */
    protected Converter m_converterToBinary;

    /**
     * The {@link ClassLoader} to use when de-serializing/serializing keys and values.
     *
     * @since 12.2.1.4
     */
    protected ClassLoader m_loader;

    /**
     * Local {@link MapListenerSupport listener support} to allow the {@code ContinuousQueryCache} to intercept
     * all events dispatched by the internal {@link ObservableMap}.
     *
     * @since 12.2.1.4
     */
    protected MapListenerSupport m_listenerSupport;

    /**
     * Local list of {@link NamedCacheDeactivationListener} to allow the {@code ContinuousQueryCache} to intercept
     * deactivation events dispatched by the back cache.
     */
    protected final List<NamedCacheDeactivationListener> m_listDeactivationListener = new ArrayList<>();

    /**
     * The lock to control adding and removing map listeners and firing of deactivation events.
     */
    protected final Lock m_listenerLock = new ReentrantLock();
    }
