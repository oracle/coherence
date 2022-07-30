/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.caffeine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Policy;
import com.github.benmanes.caffeine.cache.Policy.Eviction;
import com.github.benmanes.caffeine.cache.Policy.VarExpiration;
import com.github.benmanes.caffeine.cache.RemovalCause;

import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheEvent.TransformationState;
import com.tangosol.net.cache.CacheStatistics;
import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.OldCache;
import com.tangosol.net.cache.SimpleCacheStatistics;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport;

import java.time.Duration;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * A {@link ConfigurableCacheMap} backed by Caffeine. This implementation
 * provides high read and write concurrency, a near optimal hit rate, and
 * amortized {@code O(1)} expiration.
 * <p>
 * This implementation does not support providing an {@link EvictionPolicy} or
 * {@link EvictionApprover}, and always uses the TinyLFU policy. The maximum
 * size is set by {@link #setHighUnits(int)} and the low watermark, {@link
 * #setLowUnits(int)}, has no effect. Cache entries do not support {@code
 * touch()}, {@code getTouchCount()}, {@code getLastTouchMillis()}, or {@code
 * setUnits(c)}. By default, the cache is unbounded and will not be limited by
 * size or expiration until set.
 * <p>
 * Like {@code ConcurrentHashMap} but unlike {@code HashMap} and {@code
 * OldCache}, this cache does not support {@code null} keys or values.
 *
 * @see <a href="https://github.com/ben-manes/caffeine">Caffeine Project</a>
 * @see <a href="https://highscalability.com/blog/2016/1/25/design-of-a-modern-cache.html">Design of a Modern Cache</a>
 * @see <a href="https://dl.acm.org/authorize?N41277">TinyLFU: A Highly Efficient Cache Admission Policy</a>
 *
 * @author Ben Manes  2022.04.01
 * @since 22.06
 */
@SuppressWarnings({"rawtypes", "unchecked", "NullableProblems"})
public class CaffeineCache
        implements ConfigurableCacheMap, ConcurrentMap
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Create {@code CaffeineCache} instance.
     */
    public CaffeineCache()
        {
        f_cache = Caffeine.newBuilder()
                .ticker(() -> TimeUnit.MILLISECONDS.toNanos(getCurrentTimeMillis()))
                .evictionListener(this::notifyEvicted)
                .expireAfter(new ExpireAfterWrite())
                .maximumWeight(Long.MAX_VALUE)
                .executor(Runnable::run)
                .weigher(this::weigh)
                .build();

        f_expiration             = f_cache.policy().expireVariably().orElseThrow();
        f_eviction               = f_cache.policy().eviction().orElseThrow();
        f_listeners              = new MapListenerSupport();
        f_stats                  = new SimpleCacheStatistics();
        m_unitCalculator         = OldCache.INSTANCE_FIXED;
        m_cExpireAfterWriteNanos = Long.MAX_VALUE;
        m_nUnitFactor            = 1;
        }

    /**
     * Returns the statistics for this cache.
     *
     * @apiNote This method is called via reflection by {@code CacheModel},
     *          in order to populate {@code CacheMBean} attributes.
     */
    public CacheStatistics getCacheStatistics()
        {
        return f_stats;
        }

    /**
     * Specify whether this cache is used in the environment, where the
     * {@link Base#getSafeTimeMillis()} is used very frequently and as a result,
     * the {@link Base#getLastSafeTimeMillis} could be used without sacrificing
     * the clock precision. By default, the optimization is off.
     *
     * @param fOptimize pass true to turn the "last safe time" optimization on
     */
    public void setOptimizeGetTime(boolean fOptimize)
        {
        m_fOptimizeGetTime = fOptimize;
        }

    /**
     * Return the current {@link Base#getSafeTimeMillis() safe time} or {@link
     * Base#getLastSafeTimeMillis last safe time} depending on the optimization
     * flag.
     *
     * @return the current time
     */
    private long getCurrentTimeMillis()
        {
        return m_fOptimizeGetTime
               ? Base.getLastSafeTimeMillis()
               : Base.getSafeTimeMillis();
        }

    // ---- CacheMap interface ----------------------------------------------

    @Override
    public Map getAll(Collection colKeys)
        {
        Map map = f_cache.getAllPresent(colKeys);
        f_stats.registerHits(map.size(), 0);
        if (map.size() != colKeys.size())
            {
            f_stats.registerMisses(Set.copyOf(colKeys).size() - map.size(), 0);
            }
        return map;
        }

    @Override
    public Object put(Object oKey, Object oValue, long cMillis)
        {
        requireNonNull(oValue);

        Duration duration;
        if (cMillis == EXPIRY_NEVER)
            {
            duration = Duration.ofNanos(Long.MAX_VALUE);
            }
        else if (cMillis == EXPIRY_DEFAULT)
            {
            duration = Duration.ofNanos(m_cExpireAfterWriteNanos);
            }
        else
            {
            duration = Duration.ofMillis(cMillis);
            }

        Object[] aoPrevious = {null};
        f_expiration.compute(oKey, (k, oValueOld) ->
        {
        if (oValueOld == null)
            {
            notifyCreate(oKey, oValue);
            }
        else
            {
            notifyUpdate(oKey, oValueOld, oValue);
            aoPrevious[0] = oValueOld;
            }
        return oValue;
        }, duration);

        f_stats.registerPut(0L);
        return aoPrevious[0];
        }

    @Override
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public int getUnits()
        {
        return toExternalUnits(f_eviction.weightedSize().getAsLong(), getUnitFactor());
        }

    // ---- ConfigurableCacheMap interface ----------------------------------

    @Override
    public int getHighUnits()
        {
        return toExternalUnits(f_eviction.getMaximum(), getUnitFactor());
        }

    @Override
    public synchronized void setHighUnits(int units)
        {
        f_eviction.setMaximum(toInternalUnits(units, getUnitFactor()));
        }

    @Override
    public int getLowUnits()
        {
        return getHighUnits();
        }

    @Override
    public void setLowUnits(int units)
        {
        // no-op; not needed or supported by Caffeine
        }

    @Override
    public int getUnitFactor()
        {
        return m_nUnitFactor;
        }

    @Override
    public synchronized void setUnitFactor(int nUnitFactor)
        {
        if (nUnitFactor <= 0)
            {
            throw new IllegalArgumentException();
            }
        else if (!isEmpty())
            {
            throw new IllegalStateException(
                    "The unit factor cannot be set after the cache has been populated");
            }

        // only adjust the max units if there was no unit factor set previously
        if (m_nUnitFactor == 1)
            {
            long cCurrentMaxUnits = f_eviction.getMaximum();
            if (cCurrentMaxUnits < (Long.MAX_VALUE - Integer.MAX_VALUE))
                {
                long cMaxUnits = (cCurrentMaxUnits * nUnitFactor);
                f_eviction.setMaximum((cMaxUnits < 0) ? Long.MAX_VALUE : cMaxUnits);
                }
            }

        m_nUnitFactor = nUnitFactor;
        }

    @Override
    public UnitCalculator getUnitCalculator()
        {
        return m_unitCalculator;
        }

    @Override
    public void setUnitCalculator(UnitCalculator calculator)
        {
        m_unitCalculator = calculator == null
                           ? OldCache.INSTANCE_FIXED
                           : calculator;

        for (Object oKey : f_cache.asMap().keySet())
            {
            f_expiration.getExpiresAfter(oKey).ifPresent(duration ->
                    f_expiration.compute(oKey, (k, oValue) -> oValue, duration));
            }
        }

    /**
    * Convert from an external 32-bit unit value to an internal 64-bit unit
    * value using the configured units factor.
    *
    * @param cUnits   an external 32-bit units value
    * @param nFactor  the unit factor
    *
    * @return an internal 64-bit units value
    */
    protected static long toInternalUnits(int cUnits, int nFactor)
        {
        return (cUnits <= 0) || (cUnits == Integer.MAX_VALUE)
               ? Long.MAX_VALUE
               : ((long) cUnits) * nFactor;
        }

    /**
    * Convert from an internal 64-bit unit value to an external 32-bit unit
    * value using the configured units factor.
    *
    * @param cUnits   an internal 64-bit units value
    * @param nFactor  the unit factor
    *
    * @return an external 32-bit units value
    */
    protected static int toExternalUnits(long cUnits, int nFactor)
        {
        if (nFactor > 1)
            {
            cUnits = (cUnits + nFactor - 1) / nFactor;
            }

        return (cUnits > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) cUnits;
        }

    @Override
    public void evict(Object oKey)
        {
        f_expiration.setExpiresAfter(oKey, Duration.ZERO);
        }

    @Override
    public void evictAll(Collection colKeys)
        {
        colKeys.forEach(this::evict);
        }

    @Override
    public void evict()
        {
        f_cache.cleanUp();
        }

    @Override
    public int getExpiryDelay()
        {
        long cNanos = m_cExpireAfterWriteNanos;
        if (cNanos == Long.MAX_VALUE)
            {
            return 0;
            }
        return (int) Math.min(TimeUnit.NANOSECONDS.toMillis(cNanos), Integer.MAX_VALUE);
        }

    @Override
    public void setExpiryDelay(int cMillis)
        {
        if (cMillis < 0)
            {
            throw new IllegalArgumentException();
            }
        m_cExpireAfterWriteNanos = (cMillis == 0)
                                   ? Long.MAX_VALUE
                                   : TimeUnit.MILLISECONDS.toNanos(cMillis);
        }

    @Override
    public long getNextExpiryTime()
        {
        if (isEmpty())
            {
            return 0;
            }
        return f_expiration.oldest(Stream::findFirst)
                .map(entry -> getCurrentTimeMillis() + entry.expiresAfter().toMillis())
                .orElse(0L);
        }

    @Override
    public ConfigurableCacheMap.Entry getCacheEntry(Object oKey)
        {
        Policy.CacheEntry entry = f_cache.policy().getEntryIfPresentQuietly(oKey);
        if (entry == null)
            {
            return null;
            }
        long lExpiresAt = getCurrentTimeMillis() + entry.expiresAfter().toMillis();
        return new CacheEntry(oKey, entry.getValue(), entry.weight(), lExpiresAt);
        }

    @Override
    public EvictionApprover getEvictionApprover()
        {
        return null;
        }

    @Override
    public void setEvictionApprover(EvictionApprover approver)
        {
        // no-op; not needed or supported by Caffeine
        }

    @Override
    public EvictionPolicy getEvictionPolicy()
        {
        return null;
        }

    @Override
    public void setEvictionPolicy(EvictionPolicy policy)
        {
        // no-op; not needed or supported by Caffeine
        }

    // ---- ObservableMap interface -----------------------------------------

    @Override
    public void addMapListener(MapListener listener)
        {
        f_listeners.addListener(listener, (Filter) null, false);
        }

    @Override
    public void removeMapListener(MapListener listener)
        {
        f_listeners.removeListener(listener, (Filter) null);
        }

    @Override
    public void addMapListener(MapListener listener, Filter filter, boolean fLite)
        {
        f_listeners.addListener(listener, filter, fLite);
        }

    @Override
    public void removeMapListener(MapListener listener, Filter filter)
        {
        f_listeners.removeListener(listener, filter);
        }

    @Override
    public void addMapListener(MapListener listener, Object oKey, boolean fLite)
        {
        f_listeners.addListener(listener, oKey, fLite);
        }

    @Override
    public void removeMapListener(MapListener listener, Object oKey)
        {
        f_listeners.removeListener(listener, oKey);
        }

    // ---- ConcurrentMap interface -----------------------------------------

    @Override
    public boolean isEmpty()
        {
        return f_cache.asMap().isEmpty();
        }

    @Override
    public int size()
        {
        return f_cache.asMap().size();
        }

    @Override
    public void clear()
        {
        f_cache.asMap().keySet().forEach(this::remove);
        }

    @Override
    public boolean containsKey(Object oKey)
        {
        return f_cache.asMap().containsKey(oKey);
        }

    @Override
    public boolean containsValue(Object oValue)
        {
        return f_cache.asMap().containsValue(oValue);
        }

    @Override
    public Object get(Object oKey)
        {
        Object oValue = f_cache.getIfPresent(oKey);
        if (oValue == null)
            {
            f_stats.registerMiss();
            }
        else
            {
            f_stats.registerHit();
            }
        return oValue;
        }

    @Override
    public Object put(Object oKey, Object oValue)
        {
        return put(oKey, oValue, EXPIRY_DEFAULT);
        }

    @Override
    public void putAll(Map map)
        {
        map.forEach(this::put);
        }

    @Override
    public Object putIfAbsent(Object oKey, Object oValue)
        {
        requireNonNull(oValue);

        boolean[] afAdded = { false };
        Object oResult = f_cache.get(oKey, k ->
            {
            notifyCreate(oKey, oValue);
            afAdded[0] = true;
            return oValue;
            });
        return afAdded[0] ? null : oResult;
        }

    @Override
    public Object replace(Object oKey, Object oValue)
        {
        requireNonNull(oValue);

        Object[] aoReplaced = { null };
        f_cache.asMap().computeIfPresent(oKey, (k, oldValue) ->
            {
            notifyUpdate(oKey, oldValue, oValue);
            aoReplaced[0] = oldValue;
            return oValue;
            });
        return aoReplaced[0];
        }

    @Override
    public boolean replace(Object oKey, Object oValueOld, Object oValueNew)
        {
        requireNonNull(oValueOld);
        requireNonNull(oValueNew);

        boolean[] afReplaced = { false };
        f_cache.asMap().computeIfPresent(oKey, (k, v) ->
            {
            if (oValueOld.equals(v))
                {
                notifyUpdate(oKey, oValueOld, oValueNew);
                afReplaced[0] = true;
                return oValueNew;
                }
            return v;
            });
        return afReplaced[0];
        }

    @Override
    public void replaceAll(BiFunction function)
        {
        requireNonNull(function);

        f_cache.asMap().replaceAll((oKey, oValueOld) ->
                                   {
                                   Object oValueNew = function.apply(oKey, oValueOld);
                                   notifyUpdate(oKey, oValueOld, oValueNew);
                                   return oValueNew;
                                   });
        }

    @Override
    public Object remove(Object oKey)
        {
        Object[] aoRemoved = { null };
        f_cache.asMap().computeIfPresent(oKey, (k, oldValue) ->
            {
            notifyDelete(k, oldValue);
            aoRemoved[0] = oldValue;
            return null;
            });
        return aoRemoved[0];
        }

    @Override
    public boolean remove(Object oKey, Object oValue)
        {
        requireNonNull(oValue);
        boolean[] afRemoved = { false };
        f_cache.asMap().computeIfPresent(oKey, (k, oldValue) ->
            {
            if (oValue.equals(oldValue))
                {
                notifyDelete(k, oldValue);
                afRemoved[0] = true;
                return null;
                }
            return oldValue;
            });
        return afRemoved[0];
        }

    @Override
    public Object computeIfAbsent(Object oKey, Function mappingFunction)
        {
        requireNonNull(mappingFunction);
        boolean[] afComputed = { false };
        Object oResult = f_cache.asMap().computeIfAbsent(oKey, k ->
            {
            Object oValue = mappingFunction.apply(oKey);
            if (oValue != null)
                {
                notifyCreate(oKey, oValue);
                }
            afComputed[0] = true;
            return oValue;
            });

        if (afComputed[0])
            {
            f_stats.registerMiss();
            }
        else
            {
            f_stats.registerHit();
            }
        return oResult;
        }

    @Override
    public Object computeIfPresent(Object oKey, BiFunction remappingFunction)
        {
        requireNonNull(remappingFunction);

        return f_cache.asMap().computeIfPresent(oKey, (k, oValueOld) ->
            {
            Object oValueNew = remappingFunction.apply(oKey, oValueOld);
            if (oValueNew == null)
                {
                notifyDelete(oKey, oValueOld);
                return null;
                }
            notifyUpdate(oKey, oValueOld, oValueNew);
            return oValueNew;
            });
        }

    @Override
    public Object compute(Object oKey, BiFunction remappingFunction)
        {
        requireNonNull(remappingFunction);

        return f_cache.asMap().compute(oKey, (k, oValueOld) ->
            {
            Object oValueNew = remappingFunction.apply(oKey, oValueOld);
            if (oValueOld == null)
                {
                if (oValueNew == null)
                    {
                    return null;
                    }
                notifyCreate(oKey, oValueNew);
                return oValueNew;
                }
            else if (oValueNew == null)
                {
                notifyDelete(oKey, oValueOld);
                return null;
                }

            notifyUpdate(oKey, oValueOld, oValueNew);
            return oValueNew;
            });
        }

    @Override
    public Object merge(Object oKey, Object oValue, BiFunction remappingFunction)
        {
        requireNonNull(oValue);
        requireNonNull(remappingFunction);

        return f_cache.asMap().compute(oKey, (k, oValueOld) ->
            {
            if (oValueOld == null)
                {
                notifyCreate(oKey, oValue);
                return oValue;
                }

            Object oValueNew = remappingFunction.apply(oValueOld, oValue);
            if (oValueNew == null)
                {
                notifyDelete(oKey, oValueOld);
                return null;
                }

            notifyUpdate(oKey, oValueOld, oValueNew);
            return oValueNew;
            });
        }

    @Override
    public Set keySet()
        {
        Set<Object> setKeys = m_setKeys;
        return setKeys == null
               ? m_setKeys = new KeySetView()
               : setKeys;
        }

    @Override
    public Collection values()
        {
        Collection<Object> colValues = m_colValues;
        return colValues == null
               ? m_colValues = new ValuesView()
               : colValues;
        }

    @Override
    public Set<Map.Entry> entrySet()
        {
        Set<Map.Entry> setEntries = m_setEntries;
        return setEntries == null
               ? m_setEntries = new EntrySetView()
               : setEntries;
        }

    // ---- Object methods --------------------------------------------------

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o)
        {
        return f_cache.asMap().equals(o);
        }

    @Override
    public int hashCode()
        {
        return f_cache.asMap().hashCode();
        }

    @Override
    public String toString()
        {
        return f_cache.asMap().toString();
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Returns the weight of a cache entry.
     *
     * @param oKey   the key to weigh
     * @param oValue the value to weigh
     *
     * @return the weight of the entry; must be non-negative
     */
    private int weigh(Object oKey, Object oValue)
        {
        return m_unitCalculator.calculateUnits(oKey, oValue);
        }

    /**
     * Fires a cache event to notify listeners that the entry was inserted.
     *
     * @param oKey      the key
     * @param oValueNew the new value
     */
    private void notifyCreate(Object oKey, Object oValueNew)
        {
        if (!f_listeners.isEmpty())
            {
            CacheEvent event = new CacheEvent(this, MapEvent.ENTRY_INSERTED,
                                              oKey, null, oValueNew, false);
            f_listeners.fireEvent(event, false);
            }
        }

    /**
     * Fires a cache event to notify listeners that the entry was updated.
     *
     * @param oKey      the key
     * @param oValueOld the old value
     * @param oValueNew the new value
     */
    private void notifyUpdate(Object oKey, Object oValueOld, Object oValueNew)
        {
        if (!f_listeners.isEmpty())
            {
            CacheEvent event = new CacheEvent(this, MapEvent.ENTRY_UPDATED,
                                              oKey, oValueOld, oValueNew, false);
            f_listeners.fireEvent(event, false);
            }
        }

    /**
     * Fires a cache event to notify listeners that the entry was explicitly
     * removed.
     *
     * @param oKey      the key
     * @param oValueOld the old value
     */
    private void notifyDelete(Object oKey, Object oValueOld)
        {
        if (!f_listeners.isEmpty())
            {
            CacheEvent event = new CacheEvent(this, MapEvent.ENTRY_DELETED,
                                              oKey, oValueOld, null, false);
            f_listeners.fireEvent(event, false);
            }
        }

    /**
     * Fires a cache event to notify listeners that the entry was automatically
     * removed.
     *
     * @param oKey         the key
     * @param oValueOld    the old value
     * @param removalCause the eviction type (size, expired)
     */
    private void notifyEvicted(Object oKey, Object oValueOld, RemovalCause removalCause)
        {
        if (!f_listeners.isEmpty())
            {
            boolean fExpired = removalCause == RemovalCause.EXPIRED;
            CacheEvent event = new CacheEvent(this, MapEvent.ENTRY_DELETED, oKey, oValueOld,
                                              null, true, TransformationState.TRANSFORMABLE,
                                              false, fExpired);
            f_listeners.fireEvent(event, false);
            }
        }

    // ---- inner class: KeySetView -----------------------------------------

    /**
     * An adapter to safely externalize the keys.
     */
    final class KeySetView
            extends AbstractSet<Object>
        {
        @Override
        public int size()
            {
            return CaffeineCache.this.size();
            }

        @Override
        public void clear()
            {
            CaffeineCache.this.clear();
            }

        @Override
        public boolean contains(Object o)
            {
            return f_cache.asMap().containsKey(o);
            }

        @Override
        public boolean remove(Object o)
            {
            return CaffeineCache.this.remove(o) != null;
            }

        @Override
        public Iterator<Object> iterator()
            {
            return new KeyIterator();
            }

        @Override
        public Spliterator<Object> spliterator()
            {
            return f_cache.asMap().keySet().spliterator();
            }

        @Override
        public Object[] toArray()
            {
            return f_cache.asMap().keySet().toArray();
            }

        @Override
        public <T> T[] toArray(T[] array)
            {
            //noinspection SuspiciousToArrayCall
            return f_cache.asMap().keySet().toArray(array);
            }
        }

    // ---- inner class: KeyIterator ----------------------------------------

    /**
     * An adapter to safely externalize the key iterator.
     */
    final class KeyIterator
            implements Iterator<Object>
        {
        /**
         * Construct {@code KeyIterator} instance.
         */
        KeyIterator()
            {
            this.iterator = new EntryIterator();
            }

        @Override
        public boolean hasNext()
            {
            return iterator.hasNext();
            }

        @Override
        public Object next()
            {
            return iterator.nextKey();
            }

        @Override
        public void remove()
            {
            iterator.remove();
            }

        // ---- data members ------------------------------------------------

        /**
         * An iterator instance for the cache entries.
         */
        private final EntryIterator iterator;
        }

    // ---- inner class: ValuesView -----------------------------------------

    /**
     * An adapter to safely externalize the values.
     */
    final class ValuesView
            extends AbstractCollection<Object>
        {
        @Override
        public int size()
            {
            return CaffeineCache.this.size();
            }

        @Override
        public void clear()
            {
            CaffeineCache.this.clear();
            }

        @Override
        public boolean contains(Object o)
            {
            return f_cache.asMap().containsValue(o);
            }

        @Override
        public boolean removeIf(Predicate<? super Object> filter)
            {
            requireNonNull(filter);
            boolean fRemoved = false;
            for (Map.Entry entry : f_cache.asMap().entrySet())
                {
                if (filter.test(entry.getValue()))
                    {
                    fRemoved |= CaffeineCache.this.remove(entry.getKey(), entry.getValue());
                    }
                }
            return fRemoved;
            }

        @Override
        public Iterator iterator()
            {
            return new ValueIterator();
            }

        @Override
        public Spliterator spliterator()
            {
            return f_cache.asMap().values().spliterator();
            }
        }

    // ---- inner class: ValueIterator --------------------------------------

    /**
     * An adapter to safely externalize the value iterator.
     */
    final class ValueIterator
            implements Iterator<Object>
        {
        /**
         * Construct {@code ValueIterator} instance.
         */
        ValueIterator()
            {
            this.iterator = new EntryIterator();
            }

        @Override
        public boolean hasNext()
            {
            return iterator.hasNext();
            }

        @Override
        public Object next()
            {
            return iterator.nextValue();
            }

        @Override
        public void remove()
            {
            iterator.remove();
            }

        // ---- data members ------------------------------------------------

        /**
         * An iterator instance for the cache entries.
         */
        private final EntryIterator iterator;
        }

    // ---- inner class: EntrySetView ---------------------------------------

    /**
     * An adapter to safely externalize the entries.
     */
    final class EntrySetView
            extends AbstractSet<Map.Entry>
        {
        @Override
        public int size()
            {
            return CaffeineCache.this.size();
            }

        @Override
        public void clear()
            {
            CaffeineCache.this.clear();
            }

        @Override
        public boolean contains(Object obj)
            {
            return f_cache.asMap().entrySet().contains(obj);
            }

        @Override
        public boolean remove(Object obj)
            {
            if (!(obj instanceof Map.Entry))
                {
                return false;
                }
            Map.Entry entry = (Map.Entry) obj;
            return CaffeineCache.this.remove(entry.getKey(), entry.getValue());
            }

        @Override
        public boolean removeIf(Predicate<? super Map.Entry> filter)
            {
            requireNonNull(filter);
            boolean fRemoved = false;
            for (Map.Entry entry : this)
                {
                if (filter.test(entry))
                    {
                    fRemoved |= CaffeineCache.this.remove(entry.getKey(), entry.getValue());
                    }
                }
            return fRemoved;
            }

        @Override
        public Iterator<Map.Entry> iterator()
            {
            return new EntryIterator();
            }

        @Override
        public Spliterator<Map.Entry> spliterator()
            {
            return (Spliterator) f_cache.asMap().entrySet().spliterator();
            }
        }

    // ---- inner class: EntryIterator --------------------------------------

    /**
     * An adapter to safely externalize the entry iterator.
     */
    final class EntryIterator
            implements Iterator<Map.Entry>
        {
        EntryIterator()
            {
            this.iterator = f_cache.asMap().entrySet().iterator();
            }

        @Override
        public boolean hasNext()
            {
            return iterator.hasNext();
            }

        Object nextKey()
            {
            if (!hasNext())
                {
                throw new NoSuchElementException();
                }
            oRemovalKey = iterator.next().getKey();
            return oRemovalKey;
            }

        Object nextValue()
            {
            if (!hasNext())
                {
                throw new NoSuchElementException();
                }
            Map.Entry entry = iterator.next();
            oRemovalKey = entry.getKey();
            return entry.getValue();
            }

        @Override
        public Map.Entry next()
            {
            if (!hasNext())
                {
                throw new NoSuchElementException();
                }
            Map.Entry entry = iterator.next();
            oRemovalKey = entry.getKey();
            return new WriteThroughEntry(entry.getKey(), entry.getValue());
            }

        @Override
        public void remove()
            {
            if (oRemovalKey == null)
                {
                throw new IllegalStateException();
                }
            CaffeineCache.this.remove(oRemovalKey);
            oRemovalKey = null;
            }

        // ---- data members ------------------------------------------------

        /**
         * An iterator instance for the underyling cache's entries.
         */
        private final Iterator<Map.Entry<Object, Object>> iterator;

        /**
         * The current entry's key if a removal is requested.
         */
        private Object oRemovalKey;
        }

    // ---- inner class: ExpireAfterWrite -----------------------------------

    /**
     * An expiration policy that sets the entry's lifetime after every write
     * (create, update) to the fixed duration specified by {@link
     * #setExpiryDelay}. This policy is used except when an explicit duration is
     * provided by the caller, e.g. {@link #put(Object, Object, long)}.
     */
    private final class ExpireAfterWrite
            implements Expiry<Object, Object>
        {
        // ---- Expiry interface --------------------------------------------

        @Override
        public long expireAfterCreate(Object oKey, Object oValue, long lCurrentTime)
            {
            return m_cExpireAfterWriteNanos;
            }

        @Override
        public long expireAfterUpdate(Object oKey, Object oValue,
                                      long lCurrentTime, long lCurrentDuration)
            {
            return m_cExpireAfterWriteNanos;
            }

        @Override
        public long expireAfterRead(Object oKey, Object oValue,
                                    long lCurrentTime, long lCurrentDuration)
            {
            return lCurrentDuration;
            }
        }

    // ---- inner class: CacheEntry -----------------------------------------

    /**
     * A {@link Map.Entry} where {@link Entry#setValue(Object)} writes into the
     * cache.
     */
    private class WriteThroughEntry
            extends AbstractMap.SimpleEntry
        {
        /**
         * Construct {@code WriteThroughEntry} instance.
         *
         * @param oKey   the key
         * @param oValue the value
         */
        WriteThroughEntry(Object oKey, Object oValue)
            {
            super(oKey, oValue);
            }

        @Override
        public Object setValue(Object oValue)
            {
            put(getKey(), oValue);
            return super.setValue(oValue);
            }
        }

    // ---- inner class: CacheEntry -----------------------------------------

    /**
     * A {@link ConfigurableCacheMap.Entry} that has only partial support for
     * metadata operations.
     */
    private final class CacheEntry
            extends WriteThroughEntry
            implements ConfigurableCacheMap.Entry
        {
        // ---- constructors ------------------------------------------------

        /**
         * Construct {@code WriteThroughEntry} instance.
         *
         * @param oKey       the key
         * @param oValue     the value
         * @param nWeight    the number of units used by this entry
         * @param lExpiresAt the date/time, in milliseconds, when the entry will
         *                   expire
         */
        CacheEntry(Object oKey, Object oValue, int nWeight, long lExpiresAt)
            {
            super(oKey, oValue);

            f_nWeight = nWeight;
            f_lExpiresAt = lExpiresAt;
            }

        // ---- ConfigurableCacheMap.Entry interface ------------------------

        @Override
        public void touch()
            {
            }

        @Override
        public int getTouchCount()
            {
            return 0;
            }

        @Override
        public long getLastTouchMillis()
            {
            return 0;
            }

        @Override
        public long getExpiryMillis()
            {
            return f_lExpiresAt;
            }

        @Override
        public void setExpiryMillis(long cMillis)
            {
            if (cMillis < 0)
                {
                throw new IllegalArgumentException();
                }
            put(getKey(), getValue(), cMillis);
            }

        @Override
        public int getUnits()
            {
            return f_nWeight;
            }

        @Override
        public void setUnits(int cUnits)
            {
            // no-op; not needed or supported by Caffeine
            }

        // ---- data members ------------------------------------------------

        /**
         * The date/time, in milliseconds, when the entry will expire.
         */
        private final long f_lExpiresAt;

        /**
         * The number of units used by this entry.
         */
        private final int f_nWeight;
        }

    // ---- data members ----------------------------------------------------

    /**
     * Specifies whether or not this cache is used in the environment, where the
     * {@link Base#getSafeTimeMillis()} is used very frequently and as a result,
     * the {@link Base#getLastSafeTimeMillis} could be used without sacrificing
     * the clock precision. By default, the optimization is off.
     */
    private boolean m_fOptimizeGetTime;

    /**
     * Additional low-level operations a cache that supports an expiration
     * policy.
     */
    private final VarExpiration<Object, Object> f_expiration;

    /**
     * Additional low-level operations a cache that supports a size-based
     * eviction policy.
     */
    private final Eviction<Object, Object> f_eviction;

    /**
     * An aggregate of the listeners for advanced functionality.
     */
    private final MapListenerSupport f_listeners;

    /**
     * The underlying cache instance.
     */
    private final Cache<Object, Object> f_cache;

    /**
     * The accumulated cache statistics.
     */
    private final SimpleCacheStatistics f_stats;

    /**
     * The number of nanoseconds that a value will live in the cache.
     */
    private volatile long m_cExpireAfterWriteNanos;

    /**
     * The unit calculator to determine the weight of a cache entry.
     */
    private volatile UnitCalculator m_unitCalculator;

    /**
     * The unit factor.
     */
    private volatile int m_nUnitFactor;

    /**
     * The {@link Map#values()} view.
     */
    private Collection m_colValues;

    /**
     * The {@link Map#entrySet()} view.
     */
    private Set m_setEntries;

    /**
     * The {@link Map#keySet()} view.
     */
    private Set m_setKeys;
    }
