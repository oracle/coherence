/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.WrapperObservableMap;

import com.tangosol.util.extractor.IndexAwareExtractor;

import java.util.Map;


/**
* The BackingMapContext provides an execution context to server side agents such
* as {@link com.tangosol.util.InvocableMap.EntryProcessor EntryProcessors} and
* {@link com.tangosol.util.InvocableMap.EntryAggregator EntryAggregators}.
* As of Coherence 3.7, this context is also used to initialize {@link
* IndexAwareExtractor pluggable indexes} to provide contextual knowledge about
* the backing map and cache for which the index is created.
*
* @since Coherence 3.7
* @author coh 2010.12.04
*/
public interface BackingMapContext
    {
    /**
     * Return the "parent" {@link BackingMapManagerContext} for this context.
     * Inversely, this context could be retrieved using the {@link
     * BackingMapManagerContext#getBackingMapContext(String) getBackingMapContext}
     * API.
     *
     * @return  the enclosing {@link BackingMapManagerContext}
     */
    public BackingMapManagerContext getManagerContext();

    /**
     * Return the name of the {@link NamedCache cache} that this BackingMapContext
     * is associated with.
     *
     * @return  the corresponding cache name
     */
    public String getCacheName();

    /**
     * Return the backing map that this BackingMapContext is associated with.
     * Most commonly it is the same map that is created by the {@link
     * BackingMapManager#instantiateBackingMap(String) instantiateBackingMap} call.
     * In the case the returned map is not {@link ObservableMap observable},
     * it will be wrapped by the {@link WrapperObservableMap}.
     *
     * @return  the corresponding backing map
     *
     * @deprecated As of Coherence 12.1.3, replaced with {@link #getBackingMapEntry}
     */
    public ObservableMap getBackingMap();

    /**
     * Return a map of indexes defined for the {@link NamedCache cache} that
     * this BackingMapContext is associated with. The returned map must be
     * treated in the read-only manner.
     *
     * @return  the map of indexes defined on the cache
     */
    public Map<ValueExtractor, MapIndex> getIndexMap();

    /**
     * Return an InvocableMap.Entry for the specified key (in its internal
     * format) from the associated cache, obtaining exclusive access to that
     * cache entry.
     * <p>
     * This method may only be called within the context of an EntryProcessor
     * invocation. Any changes made to the entry will be persisted with the same
     * lifecycle as those made by the enclosing invocation. The returned entry
     * is only valid for the duration of the enclosing invocation and multiple
     * calls to this method within the same invocation context will return the
     * same entry object.
     * <p>
     * Because this method implicitly locks the specified cache entry, callers
     * may use it to access, insert, update, modify, or remove cache entries
     * from within the context of an EntryProcessor invocation. Operating on the
     * entries returned by this method differs from operating directly against
     * the backing map, as the returned entries provide an isolated,
     * read-consistent view. The implicit lock acquisition attempted by this
     * method could create a deadlock if entries are locked in conflicting
     * orders on different threads. It is the caller's responsibility to ensure
     * that cache entries are accessed (locked) in a deadlock-free manner.
     * <p>
     * The usage of this method is highly encouraged instead of direct
     * operations against the backing map retrieved by (now deprecated)
     * {@link BackingMapManagerContext#getBackingMap(String)} method.
     *
     * @param oKey  the key (in internal format) to obtain an entry for;
     *              must not be null
     *
     * @return an InvocableMap.Entry for the specified key, or null if the
     *         specified key is not owned by this service member
     *
     * @throws IllegalStateException if called from outside of an EntryProcessor
     *         invocation context
     * @throws IllegalMonitorStateException if a deadlock is detected while
     *         attempting to obtain exclusive access to the entry
     * @throws IllegalArgumentException  if attempting to access an entry that
     *         does not belong to partition(s) associated with the caller's context
     */
    public InvocableMap.Entry getBackingMapEntry(Object oKey);

    /**
     * Return an read-only InvocableMap.Entry for the specified key (in its internal
     * format) from the associated cache.
     * <p>
     * This method may be called within the context of an EntryProcessor or Aggregator
     * invocation, and may result in a {@link com.tangosol.net.cache.CacheLoader#load(java.lang.Object)
     * load}.
     * <p>
     * Note: to infer whether the InvocableMap.Entry exists in the cache use
     *       {@link InvocableMap.Entry#isPresent()}
     *
     * @param oKey  the key (in internal format) to obtain an entry for;
     *              must not be null
     *
     * @return an InvocableMap.Entry for the specified key, or null if the
     *         specified key is not owned by this service member
     *
     * @throws IllegalStateException if called from an inactive invocation context
     */
    public InvocableMap.Entry getReadOnlyEntry(Object oKey);
    }