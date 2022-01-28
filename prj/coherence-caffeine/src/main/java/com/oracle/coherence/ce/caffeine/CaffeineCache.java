/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ce.caffeine;

import static java.util.Objects.requireNonNull;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Policy.Eviction;
import com.github.benmanes.caffeine.cache.Policy.VarExpiration;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.stats.StatsCounter;
import com.tangosol.net.cache.CacheEvent;
import com.tangosol.net.cache.CacheEvent.TransformationState;
import com.tangosol.net.cache.CacheStatistics;
import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.OldCache.InternalUnitCalculator;
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

/**
 * A {@link ConfigurableCacheMap} backed by Caffeine. This implementation provides high read and
 * write concurrency, a near optimal hit rate, and amortized O(1) expiration.
 * <p>
 * This implementation does not support providing an {@link EvictionPolicy} or
 * {@link EvictionApprover}. The maximum size is set by {@link #setHighUnits(int)} and the low
 * watermark, {@link #setLowUnits(int)}, has no effect. Cache entries do not support touch(),
 * getTouchCount(), getLastTouchMillis(), or setUnits(c). By default, the cache is unbounded and
 * will not limit by size or expiration until set.
 * <p>
 * Like ConcurrentHashMap but unlike HashMap and OldCache, this cache does not support null keys
 * or values.
 */
@SuppressWarnings({"deprecation", "rawtypes", "unchecked", "NullableProblems", })
public final class CaffeineCache implements ConfigurableCacheMap, ConcurrentMap {
  private final VarExpiration<Object, Object> expiration;
  private final Eviction<Object, Object> eviction;
  private final MapListenerSupport listeners;
  private final Cache<Object, Object> cache;
  private final SimpleCacheStatistics stats;

  private volatile long expireAfterWriteNanos;
  private volatile UnitCalculator calculator;
  private volatile int unitFactor;

  private Collection values;
  private Set entrySet;
  private Set keySet;

  public CaffeineCache() {
    cache = Caffeine.newBuilder()
        .evictionListener(this::notiyEvicted)
        .recordStats(SimpleStatsCounter::new)
        .expireAfter(new ExpireAfterWrite())
        .maximumWeight(Long.MAX_VALUE)
        .executor(Runnable::run)
        .weigher(this::weigh)
        .build();
    expiration = cache.policy().expireVariably().orElseThrow();
    eviction = cache.policy().eviction().orElseThrow();
    calculator = InternalUnitCalculator.INSTANCE;
    expireAfterWriteNanos = Long.MAX_VALUE;
    listeners = new MapListenerSupport();
    stats = new SimpleCacheStatistics();
    unitFactor = 1;
  }

  /** Returns the statistics for this cache. */
  public CacheStatistics getCacheStatistics() {
    return stats;
  }

  private int weigh(Object key, Object value) {
    int units = calculator.calculateUnits(key, value);
    if (units < 0) {
      throw new IllegalStateException(String.format(
          "Negative unit (%s) for %s=%s", units, key, value));
    }
    return (units / unitFactor);
  }

  private void notifyCreate(Object key, Object newValue) {
    fireEvent(new CacheEvent(this, MapEvent.ENTRY_INSERTED,
        key, /* oldValue */ null, newValue, /* synthetic */ false));
  }

  private void notifyUpdate(Object key, Object oldValue, Object newValue) {
    fireEvent(new CacheEvent(this, MapEvent.ENTRY_UPDATED,
        key, oldValue, newValue, /* synthetic */ false));
  }

  private void notifyDelete(Object key, Object oldValue) {
    fireEvent(new CacheEvent(this, MapEvent.ENTRY_DELETED,
        key, oldValue, /* newValue */ null, /* synthetic */ false));
  }

  private void notiyEvicted(Object key, Object oldValue, RemovalCause cause) {
    boolean expired = (cause == RemovalCause.EXPIRED);
    fireEvent(new CacheEvent(this, MapEvent.ENTRY_DELETED,
        key, oldValue, /* newValue */ null, /* synthetic */ true,
        TransformationState.TRANSFORMABLE, /* priming */ false, expired));
  }

  private void fireEvent(CacheEvent event) {
    if (!listeners.isEmpty()) {
      listeners.fireEvent(event, false);
    }
  }

  /* --------------- Concurrent Map Support --------------- */

  @Override
  public boolean isEmpty() {
    return cache.asMap().isEmpty();
  }

  @Override
  public int size() {
    return cache.asMap().size();
  }

  @Override
  public void clear() {
    cache.asMap().keySet().forEach(this::remove);
  }

  @Override
  public boolean containsKey(Object key) {
    return cache.asMap().containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return cache.asMap().containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return cache.getIfPresent(key);
  }

  @Override
  public Object put(Object key, Object value) {
    return put(key, value, EXPIRY_DEFAULT);
  }

  @Override
  public void putAll(Map map) {
    map.forEach(this::put);
  }

  @Override
  public Object putIfAbsent(Object key, Object value) {
    requireNonNull(value);
    boolean[] added = { false };
    var result = cache.get(key, k -> {
      notifyCreate(key, value);
      added[0] = true;
      return value;
    });
    return added[0] ? null : result;
  }

  @Override
  public Object replace(Object key, Object value) {
    requireNonNull(value);
    Object[] replaced = { null };
    cache.asMap().computeIfPresent(key, (k, oldValue) -> {
      notifyUpdate(key, oldValue, value);
      replaced[0] = oldValue;
      return value;
    });
    return replaced[0];
  }

  @Override
  public boolean replace(Object key, Object oldValue, Object newValue) {
    requireNonNull(oldValue);
    requireNonNull(newValue);
    boolean[] replaced = { false };
    cache.asMap().computeIfPresent(key, (k, v) -> {
      if (oldValue.equals(v)) {
        notifyUpdate(key, oldValue, newValue);
        replaced[0] = true;
        return newValue;
      }
      return v;
    });
    return replaced[0];
  }

  @Override
  public void replaceAll(BiFunction function) {
    requireNonNull(function);
    cache.asMap().replaceAll((key, oldValue) -> {
      var newValue = function.apply(key, oldValue);
      notifyUpdate(key, oldValue, newValue);
      return newValue;
    });
  }

  @Override
  public Object remove(Object key) {
    Object[] removed = { null };
    cache.asMap().computeIfPresent(key, (k, oldValue) -> {
      notifyDelete(k, oldValue);
      removed[0] = oldValue;
      return null;
    });
    return removed[0];
  }

  @Override
  public boolean remove(Object key, Object value) {
    requireNonNull(value);
    boolean[] removed = { false };
    cache.asMap().computeIfPresent(key, (k, oldValue) -> {
      if (value.equals(oldValue)) {
        notifyDelete(k, oldValue);
        removed[0] = true;
        return null;
      }
      return oldValue;
    });
    return removed[0];
  }

  @Override
  public Object computeIfAbsent(Object key, Function mappingFunction) {
    requireNonNull(mappingFunction);
    return cache.asMap().computeIfAbsent(key, k -> {
      var value = mappingFunction.apply(key);
      if (value != null) {
        notifyCreate(key, value);
      }
      return value;
    });
  }

  @Override
  public Object computeIfPresent(Object key, BiFunction remappingFunction) {
    requireNonNull(remappingFunction);
    return cache.asMap().computeIfPresent(key, (k, oldValue) -> {
      var newValue = remappingFunction.apply(key, oldValue);
      if (newValue == null) {
        notifyDelete(key, oldValue);
        return null;
      }
      notifyUpdate(key, oldValue, newValue);
      return newValue;
    });
  }

  @Override
  public Object compute(Object key, BiFunction remappingFunction) {
    requireNonNull(remappingFunction);
    return cache.asMap().compute(key, (k, oldValue) -> {
      var newValue = remappingFunction.apply(key, oldValue);
      if (oldValue == null) {
        if (newValue == null) {
          return null;
        }
        notifyCreate(key, newValue);
        return newValue;
      } else if (newValue == null) {
        notifyDelete(key, oldValue);
        return null;
      }
      notifyUpdate(key, oldValue, newValue);
      return newValue;
    });
  }

  @Override
  public Object merge(Object key, Object value, BiFunction remappingFunction) {
    requireNonNull(value);
    requireNonNull(remappingFunction);
    return cache.asMap().compute(key, (k, oldValue) -> {
      if (oldValue == null) {
        notifyCreate(key, value);
        return value;
      }
      var newValue = remappingFunction.apply(oldValue, value);
      if (newValue == null) {
        notifyDelete(key, oldValue);
        return null;
      }
      notifyUpdate(key, oldValue, newValue);
      return newValue;
    });
  }

  @Override
  public Set keySet() {
    Set<Object> ks = keySet;
    return (ks == null) ? (keySet = new KeySetView()) : ks;
  }

  @Override
  public Collection values() {
    Collection<Object> vs = values;
    return (vs == null) ? (values = new ValuesView()) : vs;
  }

  @Override
  public Set<Map.Entry> entrySet() {
    Set<Map.Entry> es = entrySet;
    return (es == null) ? (entrySet = new EntrySetView()) : es;
  }

  /** An adapter to safely externalize the keys. */
  final class KeySetView extends AbstractSet<Object> {

    @Override
    public int size() {
      return CaffeineCache.this.size();
    }

    @Override
    public void clear() {
      CaffeineCache.this.clear();
    }

    @Override
    public boolean contains(Object obj) {
      return cache.asMap().containsKey(obj);
    }

    @Override
    public boolean remove(Object obj) {
      return (CaffeineCache.this.remove(obj) != null);
    }

    @Override
    public Iterator<Object> iterator() {
      return new KeyIterator();
    }

    @Override
    public Spliterator<Object> spliterator() {
      return cache.asMap().keySet().spliterator();
    }

    @Override
    public Object[] toArray() {
      return cache.asMap().keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
      //noinspection SuspiciousToArrayCall
      return cache.asMap().keySet().toArray(array);
    }
  }

  /** An adapter to safely externalize the key iterator. */
  final class KeyIterator implements Iterator<Object> {
    final EntryIterator iterator;

    KeyIterator() {
      this.iterator = new EntryIterator();
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public Object next() {
      return iterator.nextKey();
    }

    @Override
    public void remove() {
      iterator.remove();
    }
  }

  /** An adapter to safely externalize the values. */
  final class ValuesView extends AbstractCollection<Object> {

    @Override
    public int size() {
      return CaffeineCache.this.size();
    }

    @Override
    public void clear() {
      CaffeineCache.this.clear();
    }

    @Override
    public boolean contains(Object o) {
      return cache.asMap().containsValue(o);
    }

    @Override
    public boolean removeIf(Predicate<? super Object> filter) {
      requireNonNull(filter);
      boolean removed = false;
      for (var entry : cache.asMap().entrySet()) {
        if (filter.test(entry.getValue())) {
          removed |= CaffeineCache.this.remove(entry.getKey(), entry.getValue());
        }
      }
      return removed;
    }

    @Override
    public Iterator iterator() {
      return new ValueIterator();
    }

    @Override
    public Spliterator spliterator() {
      return cache.asMap().values().spliterator();
    }
  }

  /** An adapter to safely externalize the value iterator. */
  final class ValueIterator implements Iterator<Object> {
    final EntryIterator iterator;

    ValueIterator() {
      this.iterator = new EntryIterator();
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    @Override
    public Object next() {
      return iterator.nextValue();
    }

    @Override
    public void remove() {
      iterator.remove();
    }
  }

  /** An adapter to safely externalize the entries. */
  final class EntrySetView extends AbstractSet<Map.Entry> {

    @Override
    public int size() {
      return CaffeineCache.this.size();
    }

    @Override
    public void clear() {
      CaffeineCache.this.clear();
    }

    @Override
    public boolean contains(Object obj) {
      return cache.asMap().entrySet().contains(obj);
    }

    @Override
    public boolean remove(Object obj) {
      if (!(obj instanceof Map.Entry<?, ?>)) {
        return false;
      }
      var entry = (Map.Entry<?, ?>) obj;
      return CaffeineCache.this.remove(entry.getKey(), entry.getValue());
    }

    @Override
    public boolean removeIf(Predicate<? super Map.Entry> filter) {
      requireNonNull(filter);
      boolean removed = false;
      for (var entry : this) {
        if (filter.test(entry)) {
          removed |= CaffeineCache.this.remove(entry.getKey(), entry.getValue());
        }
      }
      return removed;
    }

    @Override
    public Iterator<Map.Entry> iterator() {
      return new EntryIterator();
    }

    @Override
    public Spliterator<Map.Entry> spliterator() {
      return (Spliterator) cache.asMap().entrySet().spliterator();
    }
  }

  @Override
  public boolean equals(Object o) {
    return cache.asMap().equals(o);
  }

  @Override
  public int hashCode() {
    return cache.asMap().hashCode();
  }

  @Override
  public String toString() {
    return cache.asMap().toString();
  }

  /** An adapter to safely externalize the entry iterator. */
  final class EntryIterator implements Iterator<Map.Entry> {
    final Iterator<Map.Entry<Object, Object>> iterator;

    Object removalKey;

    EntryIterator() {
      this.iterator = cache.asMap().entrySet().iterator();
    }

    @Override
    public boolean hasNext() {
      return iterator.hasNext();
    }

    Object nextKey() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      removalKey = iterator.next().getKey();
      return removalKey;
    }

    Object nextValue() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      var entry = iterator.next();
      removalKey = entry.getKey();
      return entry.getValue();
    }

    @Override
    public Map.Entry next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      var entry = iterator.next();
      removalKey = entry.getKey();
      return new WriteThroughEntry(entry.getKey(), entry.getValue());
    }

    @Override
    public void remove() {
      if (removalKey == null) {
        throw new IllegalStateException();
      }
      CaffeineCache.this.remove(removalKey);
      removalKey = null;
    }
  }

  /* --------------- CacheMap --------------- */

  @Override
  public Map getAll(Collection keys) {
    return cache.getAllPresent(keys);
  }

  @Override
  public Object put(Object key, Object value, long millis) {
    requireNonNull(value);

    Duration duration;
    if (millis == EXPIRY_NEVER) {
      duration = Duration.ofNanos(Long.MAX_VALUE);
    } else if (millis == EXPIRY_DEFAULT) {
      duration = Duration.ofNanos(expireAfterWriteNanos);
    } else {
      duration = Duration.ofMillis(millis);
    }

    Object[] previous = { null };
    expiration.compute(key, (k, oldValue) -> {
      if (oldValue == null) {
        notifyCreate(key, value);
      } else {
        notifyUpdate(key, oldValue, value);
        previous[0] = oldValue;
      }
      return value;
    }, duration);
    stats.registerPut(0L);
    return previous[0];
  }

  @Override
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public int getUnits() {
    return (int) Math.min(eviction.weightedSize().getAsLong(), Integer.MAX_VALUE);
  }

  /* --------------- ConfigurableCacheMap --------------- */

  @Override
  public int getHighUnits() {
    return (int) Math.min(eviction.getMaximum(), Integer.MAX_VALUE);
  }

  @Override
  public synchronized void setHighUnits(int units) {
    eviction.setMaximum(units);
  }

  @Override
  public int getLowUnits() {
    return getHighUnits();
  }

  @Override
  public void setLowUnits(int units) {}

  @Override
  public int getUnitFactor() {
    return unitFactor;
  }

  @Override
  public synchronized void setUnitFactor(int unitFactor) {
    if (unitFactor <= 0) {
      throw new IllegalArgumentException();
    } else if (!isEmpty()) {
      throw new IllegalStateException(
          "The unit factor cannot be set after the cache has been populated");
    }
    this.unitFactor = unitFactor;
  }

  @Override
  public UnitCalculator getUnitCalculator() {
    return calculator;
  }

  @Override
  public void setUnitCalculator(UnitCalculator calculator) {
    this.calculator = requireNonNull(calculator);

    for (var key : cache.asMap().keySet()) {
      expiration.getExpiresAfter(key).ifPresent(duration ->
          expiration.compute(key, (k, value) -> value, duration));
    }
  }

  @Override
  public void evict(Object key) {
    expiration.setExpiresAfter(key, Duration.ZERO);
  }

  @Override
  public void evictAll(Collection keys) {
    keys.forEach(this::evict);
  }

  @Override
  public void evict() {
    cache.cleanUp();
  }

  @Override
  public int getExpiryDelay() {
    long nanos = expireAfterWriteNanos;
    if (nanos == Long.MAX_VALUE) {
      return 0;
    }
    return (int) Math.min(TimeUnit.NANOSECONDS.toMillis(nanos), Integer.MAX_VALUE);
  }

  @Override
  public void setExpiryDelay(int millis) {
    if (millis < 0) {
      throw new IllegalArgumentException();
    }
    expireAfterWriteNanos = (millis == 0) ? Long.MAX_VALUE : TimeUnit.MILLISECONDS.toNanos(millis);
  }

  @Override
  public long getNextExpiryTime() {
    if (isEmpty()) {
      return 0;
    }
    return expiration.oldest(stream -> stream.findFirst())
        .map(entry -> Base.getSafeTimeMillis() + entry.expiresAfter().toMillis())
        .orElse(0L);
  }

  @Override
  public ConfigurableCacheMap.Entry getCacheEntry(Object key) {
    var entry = cache.policy().getEntryIfPresentQuietly(key);
    if (entry == null) {
      return null;
    }
    var expiresAt = Base.getSafeTimeMillis() + entry.expiresAfter().toMillis();
    return new CacheEntry(key, entry.getValue(), entry.weight(), expiresAt);
  }

  @Override
  public EvictionApprover getEvictionApprover() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setEvictionApprover(EvictionApprover approver) {
    throw new UnsupportedOperationException();
  }

  @Override
  public EvictionPolicy getEvictionPolicy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setEvictionPolicy(EvictionPolicy policy) {
    throw new UnsupportedOperationException();
  }

  /* --------------- ObservableMap --------------- */

  @Override
  public void addMapListener(MapListener listener) {
    listeners.addListener(listener, (Filter) null, false);
  }

  @Override
  public void removeMapListener(MapListener listener) {
    listeners.removeListener(listener, (Filter) null);
  }

  @Override
  public void addMapListener(MapListener listener, Filter filter, boolean fLite) {
    listeners.addListener(listener, filter, fLite);
  }

  @Override
  public void removeMapListener(MapListener listener, Filter filter) {
    listeners.removeListener(listener, filter);
  }

  @Override
  public void addMapListener(MapListener listener, Object key, boolean fLite) {
    listeners.addListener(listener, key, fLite);
  }

  @Override
  public void removeMapListener(MapListener listener, Object key) {
    listeners.removeListener(listener, key);
  }

  /* --------------- Support --------------- */

  private final class ExpireAfterWrite implements Expiry<Object, Object> {

    @Override
    public long expireAfterCreate(Object key, Object value, long currentTime) {
      return expireAfterWriteNanos;
    }

    @Override
    public long expireAfterUpdate(Object key, Object value,
        long currentTime, long currentDuration) {
      return expireAfterWriteNanos;
    }

    @Override
    public long expireAfterRead(Object key, Object value,
        long currentTime, long currentDuration) {
      return currentDuration;
    }
  }

  private final class SimpleStatsCounter implements StatsCounter {

    @Override
    public void recordHits(int count) {
      stats.registerHits(count, 0);
    }

    @Override
    public void recordMisses(int count) {
      stats.registerMisses(count, 0);
    }

    @Override
    public void recordLoadSuccess(long loadTime) {}

    @Override
    public void recordLoadFailure(long loadTime) {}

    @Override
    public void recordEviction(int weight, RemovalCause removalCause) {}

    @Override
    public CacheStats snapshot() {
      return CacheStats.empty();
    }
  }

  private class WriteThroughEntry extends AbstractMap.SimpleEntry {

    WriteThroughEntry(Object key, Object value) {
      super(key, value);
    }

    @Override
    public Object setValue(Object value) {
      put(getKey(), value);
      return super.setValue(value);
    }
  }

  private final class CacheEntry extends WriteThroughEntry implements ConfigurableCacheMap.Entry {
    private long expiresAt;
    private int weight;

    CacheEntry(Object key, Object value, int weight, long expiresAt) {
      super(key, value);
      this.weight = weight;
      this.expiresAt = expiresAt;
    }

    @Override
    public void touch() {}

    @Override
    public int getTouchCount() {
      return 0;
    }

    @Override
    public long getLastTouchMillis() {
      return 0;
    }

    @Override
    public long getExpiryMillis() {
      return expiresAt;
    }

    @Override
    public void setExpiryMillis(long millis) {
      if (millis < 0) {
        throw new IllegalArgumentException();
      }
      put(getKey(), getValue(), millis);
    }

    @Override
    public int getUnits() {
      return weight * unitFactor;
    }

    @Override
    public void setUnits(int units) {
      throw new UnsupportedOperationException();
    }
  }
}
