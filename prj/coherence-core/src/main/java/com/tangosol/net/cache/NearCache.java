/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.AsyncNamedMap;
import com.tangosol.net.CacheService;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.Service;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.MapListener;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.function.Remote;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import java.util.function.Function;

/**
* A "near cache" is a CachingMap whose front map is a size-limited and/or
* auto-expiring local cache, and whose back map is a distributed cache.
*
* (A CachingMap is a map that has a "front" map and a "back" map; the front
* map is assumed to be low latency but incomplete, and the back map is
* assumed to be complete but high latency.)
*
* @see CachingMap Invalidation strategies
*
* @author ag/cp  2002.10.20
* @author gg     2003.10.16
*/
public class NearCache<K, V>
        extends CachingMap<K, V>
        implements NamedCache<K, V>, ClassLoaderAware
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a NearCache, using a <i>mapBack</i> NamedCache as the
    * complete (back) storage and <i>mapFront</i> Map as a near (front)
    * storage using the {@link CachingMap#LISTEN_AUTO} invalidation strategy.
    *
    * @param mapFront  Map to put in front of the back cache
    * @param mapBack   NamedCache to put behind the front cache
    */
    public NearCache(Map<K, V> mapFront, NamedCache<K, V> mapBack)
        {
        this(mapFront, mapBack, LISTEN_AUTO);
        }

    /**
    * Construct a NearCache, using a <i>mapBack</i> NamedCache as the
    * complete (back) storage and <i>mapFront</i> Map as a near (front)
    * storage.
    *
    * @param mapFront   Map to put in front of the back cache
    * @param mapBack    NamedCache to put behind the front cache
    * @param nStrategy  specifies the strategy used for the front map
    *                   invalidation; valid values are:<br>
    *                   {@link CachingMap#LISTEN_NONE LISTEN_NONE},
    *                   {@link CachingMap#LISTEN_PRESENT LISTEN_PRESENT},
    *                   {@link CachingMap#LISTEN_ALL LISTEN_ALL},
    *                   {@link CachingMap#LISTEN_AUTO LISTEN_AUTO}
    * @since Coherence 2.3
    */
    public NearCache(Map<K, V> mapFront, NamedCache<K, V> mapBack, int nStrategy)
        {
        super(mapFront, mapBack, nStrategy);

        f_sName               = mapBack.getCacheName();
        f_service             = mapBack.getCacheService();
        f_sServiceName        = f_service.getInfo().getServiceName();
        f_listenerBackService = registerBackServiceListener();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the NamedCache object that sits behind this NearCache.
    *
    * @return the NamedCache object, which is the back map of this NearCache
    *
    * @throws IllegalStateException  if this NamedCache has been released
    */
    public NamedCache<K, V> getBackCache()
        {
        return (NamedCache<K, V>) getBackMap();
        }

    /**
     * Obtain the context (tier) used to register a CacheMBean for this cache.
     *
     * @return the corresponding context string
     */
     public String getRegistrationContext()
          {
          return m_sTier;
          }

     /**
     * Set the context (tier) used to register a CacheMBean for this cache.
     *
     * @param sCtx the corresponding context string
     */
     public void setRegistrationContext(String sCtx)
          {
          m_sTier = sCtx;
          }


    // ----- NamedCache interface -------------------------------------------

    @Override
    public AsyncNamedCache<K, V> async(AsyncNamedMap.Option... options)
        {
        return getBackCache().async(options);
        }

    /**
    * Return the cache name.
    *
    * @return the cache name
    */
    public String getCacheName()
        {
        return f_sName;
        }

    /**
    * Return the CacheService that this NamedCache is a part of.
    *
    * @return the CacheService
    */
    public CacheService getCacheService()
        {
        return f_service;
        }

    /**
    * Specifies whether or not the NamedCache is active.
    *
    * @return <tt>true</tt> if the NamedCache is active; <tt>false</tt> otherwise
    */
    public boolean isActive()
        {
        try
            {
            return getFrontMap() != null && getBackCache().isActive();
            }
        catch (IllegalStateException e)
            {
            return false;
            }
        }

    @Override
    public boolean isReady()
        {
        try
            {
            return getFrontMap() != null && getBackCache().isReady();
            }
        catch (IllegalStateException e)
            {
            return false;
            }
        }

    /**
    * {@inheritDoc}
    */
    public void release()
        {
        release(false);
        }

    /**
    * {@inheritDoc}
    */
    public void destroy()
        {
        release(true);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void truncate()
        {
        getBackCache().truncate();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDestroyed()
        {
        try
            {
            return m_fDestroyed || getBackCache().isDestroyed();
            }
        catch (IllegalStateException e)
            {
            return false;
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReleased()
        {
        try
            {
            return getBackCache().isReleased();
            }
        catch (RuntimeException e)
            {
            // one of the following should be ignored:
            //   IllegalStateException("Cache is not active");
            //   RuntimeException("Storage is not configured");
            //   RuntimeException("Service has been terminated");
            return true;
            }
        }

    // ----- ObservableMap interface ----------------------------------------

    /**
    * Add a standard map listener.
    *
    * Expensive: Listening always occurs on the back cache.
    *
    * @param listener  the MapEvent listener to add
    */
    public void addMapListener(MapListener<? super K, ? super V> listener)
        {
        getBackCache().addMapListener(listener);
        }

    /**
    * Remove a standard map listener.
    *
    * @param listener  the MapEvent listener to remove
    */
    public void removeMapListener(MapListener<? super K, ? super V> listener)
        {
        getBackCache().removeMapListener(listener);
        }

    /**
    * Add a map listener for a specific key.
    *
    * Expensive: Listening always occurs on the back cache.
    *
    * @param listener  the listener to add
    * @param oKey      the key that identifies the entry for which to raise
    *                  events
    * @param fLite     true to indicate that the MapEvent objects do
    *                  not have to include the OldValue and NewValue
    *                  property values in order to allow optimizations
    *
    * @since Coherence 2.3
    */
    public void addMapListener(MapListener<? super K, ? super V> listener, K oKey, boolean fLite)
        {
        getBackCache().addMapListener(listener, oKey, fLite);
        }

    /**
    * Remove a map listener that previously signed up for events about a
    * specific key.
    *
    * @param listener  the listener to remove
    * @param oKey      the key that identifies the entry for which to raise
    *                  events
    *
    * @since Coherence 2.3
    */
    public void removeMapListener(MapListener<? super K, ? super V> listener, K oKey)
        {
        getBackCache().removeMapListener(listener, oKey);
        }

    /**
    * Add a map listener that receives events based on a filter evaluation.
    *
    * Expensive: Listening always occurs on the back cache.
    *
    * @param listener  the listener to add
    * @param filter    a filter that will be passed MapEvent objects to
    *                  select from; a MapEvent will be delivered to the
    *                  listener only if the filter evaluates to true for
    *                  that MapEvent; null is equivalent to a filter
    *                  that always returns true
    * @param fLite     true to indicate that the MapEvent objects do
    *                  not have to include the OldValue and NewValue
    *                  property values in order to allow optimizations
    *
    * @since Coherence 2.3
    */
    public void addMapListener(MapListener<? super K, ? super V> listener, Filter filter, boolean fLite)
        {
        getBackCache().addMapListener(listener, filter, fLite);
        }

    /**
    * Remove a map listener that previously signed up for events based on a
    * filter evaluation.
    *
    * @param listener  the listener to remove
    * @param filter    a filter used to evaluate events
    *
    * @since Coherence 2.3
    */
    public void removeMapListener(MapListener<? super K, ? super V> listener, Filter filter)
        {
        getBackCache().removeMapListener(listener, filter);
        }


    // ----- CacheMap interface ----------------------------------------

    /**
    * Associates the specified value with the specified key in this cache and
    * allows to specify an expiry for the cache entry. If the cache previously
    * contained a mapping for this key, the old value is replaced.
    *
    * @param oKey     key with which the specified value is to be associated
    * @param oValue   value to be associated with the specified key
    * @param cMillis  the number of milliseconds until the cache entry will
    *                 expire
    *
    * @return previous value associated with specified key, or <tt>null</tt>
    *          if there was no mapping for key.  A <tt>null</tt> return can
    *          also indicate that the map previously associated <tt>null</tt>
    *          with the specified key, if the implementation supports
    *          <tt>null</tt> values
    *
    * @throws UnsupportedOperationException if the requested expiry is a
    *         positive value and either the front map or the back map
    *         implementations do not support the expiration functionality
    *
    * @see CacheMap#put(Object oKey, Object oValue, long cMillis)
    */
    public V put(K oKey, V oValue, long cMillis)
        {
        return super.put(oKey, oValue, true, cMillis);
        }


    // ----- ConcurrentMap interface ----------------------------------------

    /**
    * Attempt to lock the specified item and return immediately.
    *
    * Expensive: Locking always occurs on the back cache.
    *
    * @param oKey key being locked
    *
    * @return <tt>true</tt> if the item was successfully locked;
    *         <tt>false</tt> otherwise
    */
    public boolean lock(Object oKey)
        {
        return lock(oKey, 0L);
        }

    /**
    * Attempt to lock the specified item within the specified period of time.
    *
    * Expensive: Locking always occurs on the back cache.
    *
    * @param oKey     key being locked
    * @param lMillis  the number of milliseconds to continue trying to obtain
    *                 a lock; pass zero to return immediately; pass -1 to block
    *                 the calling thread until the lock could be obtained
    *
    * @return true if the item was successfully locked within the
    *              specified time; false otherwise
    */
    public boolean lock(Object oKey, long lMillis)
        {
        if (getBackCache().lock(oKey, lMillis))
            {
            // back map listeners are always synchronous, so if there is one
            // the front map invalidation is not necessary
            if (getInvalidationStrategy() == LISTEN_NONE)
                {
                getFrontMap().remove(oKey);
                }
            return true;
            }
        else
            {
            return false;
            }
        }

    /**
    * Unlock the specified item.
    *
    * @param oKey key being unlocked
    *
    * @return true if the item was successfully unlocked; false otherwise
    */
    public boolean unlock(Object oKey)
        {
        return getBackCache().unlock(oKey);
        }


    // ----- QueryMap interface ---------------------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * The operation always executes against the back cache.
    */
    public Set<K> keySet(Filter filter)
        {
        return getBackCache().keySet(filter);
        }

    /**
    * {@inheritDoc}
    * <p>
    * The operation always executes against the back cache.
    */
    public Set<Map.Entry<K, V>> entrySet(Filter filter)
        {
        return getBackCache().entrySet(filter);
        }

    /**
    * {@inheritDoc}
    * <p>
    * The operation always executes against the back cache.
    */
    public Set<Map.Entry<K, V>> entrySet(Filter filter, Comparator comparator)
        {
        return getBackCache().entrySet(filter, comparator);
        }

    /**
    * {@inheritDoc}
    * <p>
    * The operation always executes against the back cache.
    */
    public <T, E> void addIndex(ValueExtractor<? super T, ? extends E> extractor, boolean fOrdered, Comparator<? super E> comparator)
        {
        getBackCache().addIndex(extractor, fOrdered, comparator);
        }

    /**
    * {@inheritDoc}
    * <p>
    * The operation always executes against the back cache.
    */
    public <T, E> void removeIndex(ValueExtractor<? super T, ? extends E> extractor)
        {
        getBackCache().removeIndex(extractor);
        }


    // ----- InvocableMap interface -----------------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * The operation executes against the back cache if the value is not in the front.
    */
    public V computeIfAbsent(K key, Remote.Function<? super K, ? extends V> mappingFunction)
        {
        V value = getFrontMap().get(key);
        return value == null ? getBackMap().computeIfAbsent(key, mappingFunction) : value;
        }

    /**
    * {@inheritDoc}
    * <p>
    * The operation executes against the back cache if the value is not in the front.
    */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
        {
        V value = getFrontMap().get(key);
        return value == null ? getBackMap().computeIfAbsent(key, mappingFunction) : value;
        }

    /**
    * {@inheritDoc}
    * <p>
    * The operation always executes against the back cache.
    */
    public <R> R invoke(K key, EntryProcessor<K, V, R> processor)
        {
        return getBackCache().invoke(key, processor);
        }

    /**
    * {@inheritDoc}
    * <p>
    * The operation always executes against the back cache.
    */
    public <R> Map<K, R> invokeAll(Collection<? extends K> collKeys, EntryProcessor<K, V, R> processor)
        {
        return getBackCache().invokeAll(collKeys, processor);
        }

    /**
    * {@inheritDoc}
    * <p>
    * The operation always executes against the back cache.
    */
    public <R> Map<K, R> invokeAll(Filter filter, EntryProcessor<K, V, R> processor)
        {
        return getBackCache().invokeAll(filter, processor);
        }

    /**
    * {@inheritDoc}
    * <p>
    * The operation always executes against the back cache.
    */
    public <R> R aggregate(Collection<? extends K> collKeys, EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return getBackCache().aggregate(collKeys, aggregator);
        }

    /**
    * {@inheritDoc}
    * <p>
    * The operation always executes against the back cache.
    */
    public <R> R aggregate(Filter filter, EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return getBackCache().aggregate(filter, aggregator);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    @SuppressWarnings("unchecked")
    public V getOrDefault(Object oKey, V defaultValue)
        {
        V value = (V) get(oKey);

        return value == null ? defaultValue : value;
        }

    // ----- internal helpers -----------------------------------------------

    /**
    * Release this cache, optionally destroying it.
    *
    * @param fDestroy  true to destroy the cache as well
    */
    protected void release(boolean fDestroy)
        {
        unregisterMBean();

        try
            {
            NamedCache cache = getBackCache();
            unregisterBackServiceListener();
            super.release();
            if (fDestroy)
                {
                m_fDestroyed = true;
                cache.destroy();
                }
            else
                {
                cache.release();
                }
            }
        catch (RuntimeException e)
            {
            // one of the following should be ignored:
            //   IllegalStateException("Cache is not active");
            //   RuntimeException("Storage is not configured");
            //   RuntimeException("Service has been terminated");
            }
        }

    /**
    * Instantiate and register a MemberListener with the back cache
    * service.
    * <p>
    * The primary goal of that listener is invalidation of the front map
    * in case of the service [automatic] restart.
    *
    * @return the instantiated and registered MemberListener object
    */
    protected MemberListener registerBackServiceListener()
        {
        // automatic front map clean up (upon service restart)
        // requires a MemberListener implementation
        CacheService service = getCacheService();
        if (service != null)
            {
            try
                {
                MemberListener listener = new BackServiceListener();
                service.addMemberListener(listener);
                return listener;
                }
            catch (UnsupportedOperationException e) {}
            }

        return null;
        }

    /**
    * Unregister back cache service member listener.
    */
    protected void unregisterBackServiceListener()
        {
        try
            {
            getCacheService().removeMemberListener(f_listenerBackService);
            }
        catch (RuntimeException e) {}
        }

    /**
     * Register an MBean representing this NearCache.
     */
    public void registerMBean()
        {
        MBeanHelper.registerCacheMBean(this, getRegistrationContext());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void unregisterMBean()
        {
        MBeanHelper.unregisterCacheMBean(f_service.getCluster(), f_sServiceName, f_sName, getRegistrationContext());
        }


    // ----- inner classes --------------------------------------------------

    /**
    * MemberListener for the back cache's service.
    * <p>
    * The primary goal of that listener is invalidation of the front map
    * in case of the corresponding CacheService [automatic] restart.
    */
    protected class BackServiceListener
            implements MemberListener
        {
        /**
        * Invoked when a Member has joined the service.
        */
        public void memberJoined(MemberEvent evt)
            {
            if (evt.isLocal())
                {
                resetFrontMap();
                registerMBean();
                }
            }

        /**
        * Invoked when a Member is leaving the service.
        */
        public void memberLeaving(MemberEvent evt)
            {
            }

        /**
        * Invoked when a Member has left the service.
        */
        public void memberLeft(MemberEvent evt)
            {
            if (getInvalidationStrategy() != LISTEN_NONE)
                {
                if (evt.isLocal())
                    {
                    resetFrontMap();
                    unregisterMBean();
                    }
                else
                    {
                    Service service = evt.getService();

                    // avoid iterating the memberset (getOwnershipSenior()) if partition 0 has an assignment 
                    if (service instanceof PartitionedService &&
                        ((PartitionedService) service).getPartitionOwner(0) == null &&
                        ((PartitionedService) service).getOwnershipSenior() == null)
                        {
                        resetFrontMap();
                        }
                    }
                }
            }

        /**
         * Invoked when a Member has recovered.
         */
        public void memberRecovered(MemberEvent evt)
            {
            resetFrontMap();
            }
        }


    // ----- ClassLoaderAware interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public ClassLoader getContextClassLoader()
        {
        NamedCache cacheBack = getBackCache();
        return cacheBack instanceof ClassLoaderAware
                ? ((ClassLoaderAware) cacheBack).getContextClassLoader()
                : Base.getContextClassLoader();
        }

    /**
    * {@inheritDoc}
    */
    public void setContextClassLoader(ClassLoader loader)
        {
        throw new UnsupportedOperationException();
        }


    // ----- data fields ----------------------------------------------------

    /**
     * True if {#link destroy()} has been called on this cache.
     */
    protected boolean m_fDestroyed = false;

    /**
    * The cache name.
    */
    protected final String f_sName;

    /**
    * The back cache service.
    */
    protected final CacheService f_service;

    /**
    * The back cache service MemberListener.
    */
    protected final MemberListener f_listenerBackService;

    /**
    * The context (tier) used to register a CacheMBean for this cache.
    */
    private String m_sTier;

     /**
     * The back cache service name.
     */
    protected final String f_sServiceName;
    }