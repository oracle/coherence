/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.events;

import com.oracle.coherence.common.base.Exceptions;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.util.Filter;
import com.tangosol.util.MapEventTransformer;
import com.tangosol.util.MapListener;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.MapEventTransformerFilter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registers discovered CDI observer-based {@link MapListener}s when the cache is
 * created, and unregisters them when it's destroyed.
 *
 * @author Aleks Seovic  2020.06.09
 * @since 20.06
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class AnnotatedMapListenerManager
    {
    public AnnotatedMapListenerManager(
            AnnotatedMapListener.FilterProducer filterProducer,
            AnnotatedMapListener.MapEventTransformerProducer transformerProducer)
        {
        f_filterProducer      = filterProducer;
        f_transformerProducer = transformerProducer;
        }

    protected void registerListeners(String sCacheName, String sEventScope, String sEventSession, String sEventService)
        {
        Set<AnnotatedMapListener<?, ?>> setListeners = getMapListeners(removeScope(sEventService), sCacheName);

        Session session = Coherence.findSession(sEventSession)
              .orElseThrow(() -> new IllegalStateException("Cannot find a Session with name " + sEventSession));
        NamedCache cache = session.getCache(sCacheName);

        for (AnnotatedMapListener<?, ?> listener : setListeners)
            {
            if (listener.hasFilterAnnotation())
                {
                // ensure that the listener's filter has been resolved as this
                // was not possible as discovery time.
                listener.resolveFilter(f_filterProducer);
                }

            if (listener.hasTransformerAnnotation())
                {
                // ensure that the listener's transformer has been resolved as this
                // was not possible as discovery time.
                listener.resolveTransformer(f_transformerProducer);
                }

            String  sScope     = listener.getScopeName();
            boolean fScopeOK   = sScope == null || sScope.equals(sEventScope);
            String  sSession   = listener.getSessionName();
            boolean fSessionOK = sSession == null || sSession.equals(sEventSession);

            if (fScopeOK && fSessionOK)
                {
                Filter filter = listener.getFilter();
                if (filter != null && !(filter instanceof MapEventFilter))
                    {
                    filter = new MapEventFilter(MapEventFilter.E_ALL, filter);
                    }

                MapEventTransformer transformer = listener.getTransformer();
                if (transformer != null)
                    {
                    filter = new MapEventTransformerFilter(filter, transformer);
                    }

                try
                    {
                    boolean fLite = listener.isLite();
                    if (listener.isSynchronous())
                        {
                        cache.addMapListener(listener.synchronous(), filter, fLite);
                        }
                    else
                        {
                        cache.addMapListener(listener, filter, fLite);
                        }
                    }
                catch (Exception e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                }
            }
        }

    // ---- helpers --------------------------------------------------------

    /**
     * Remove the scope prefix from a specified service name.
     *
     * @param sServiceName  the service name to remove scope prefix from
     *
     * @return service name with scope prefix removed
     */
    private String removeScope(String sServiceName)
        {
        if (sServiceName == null)
            {
            return "";
            }
        int nIndex = sServiceName.indexOf(':');
        return nIndex > -1 ? sServiceName.substring(nIndex + 1) : sServiceName;
        }

    /**
     * Add specified listener to the collection of discovered observer-based listeners.
     *
     * @param listener  the listener to add
     */
    public void addMapListener(AnnotatedMapListener<?, ?> listener)
        {
        String svc   = listener.getServiceName();
        String cache = listener.getCacheName();

        Map<String, Set<AnnotatedMapListener<?, ?>>> mapByCache = m_mapListeners.computeIfAbsent(svc, s -> new HashMap<>());
        Set<AnnotatedMapListener<?, ?>> setListeners = mapByCache.computeIfAbsent(cache, c -> new HashSet<>());
        setListeners.add(listener);
        }

    /**
     * Return all map listeners that should be registered for a particular
     * service and cache combination.
     *
     * @param serviceName  the name of the service
     * @param cacheName    the name of the cache
     *
     * @return a set of all listeners that should be registered
     */
    public Set<AnnotatedMapListener<?, ?>> getMapListeners(String serviceName, String cacheName)
        {
        HashSet<AnnotatedMapListener<?, ?>> setResults = new HashSet<>();
        collectMapListeners(setResults, "*", "*");
        collectMapListeners(setResults, "*", cacheName);
        collectMapListeners(setResults, serviceName, "*");
        collectMapListeners(setResults, serviceName, cacheName);

        return setResults;
        }

    /**
     * Return all map listeners that should be registered against a specific
     * remote cache or map in a specific session.
     *
     * @return  all map listeners that should be registered against a
     *          specific cache or map in a specific session
     */
    public Collection<AnnotatedMapListener<?, ?>> getNonWildcardMapListeners()
        {
        return m_mapListeners.values()
                             .stream()
                             .flatMap(map -> map.values().stream())
                             .flatMap(Set::stream)
                             .filter(listener -> listener.getSessionName() != null)
                             .filter(listener -> !listener.isWildCardCacheName())
                             .sorted()
                             .collect(Collectors.toList());
        }

    /**
     * Add all map listeners for the specified service and cache combination to
     * the specified result set.
     *
     * @param setResults   the set of results to accumulate listeners into
     * @param serviceName  the name of the service
     * @param cacheName    the name of the cache
     */
    private void collectMapListeners(HashSet<AnnotatedMapListener<?, ?>> setResults, String serviceName, String cacheName)
        {
        Map<String, Set<AnnotatedMapListener<?, ?>>> mapByCache = m_mapListeners.get(serviceName);
        if (mapByCache != null)
            {
            setResults.addAll(mapByCache.getOrDefault(cacheName, Collections.emptySet()));
            }
        }

    // ---- data members ---------------------------------------------------

    private final AnnotatedMapListener.FilterProducer f_filterProducer;

    private final AnnotatedMapListener.MapEventTransformerProducer f_transformerProducer;

    /**
     * A list of event interceptors for all discovered observer methods.
     */
    private final Map<String, Map<String, Set<AnnotatedMapListener<?, ?>>>> m_mapListeners = new HashMap<>();
    }
