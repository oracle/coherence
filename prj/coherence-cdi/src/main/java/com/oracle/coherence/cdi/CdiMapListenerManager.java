/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.Created;

import com.tangosol.net.NamedMap;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.MapEventTransformer;
import com.tangosol.util.MapListener;
import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.filter.MapEventTransformerFilter;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import javax.inject.Inject;

/**
 * Registers discovered CDI observer-based {@link MapListener}s when the cache is
 * created, and unregisters them when it's destroyed.
 *
 * @author Aleks Seovic  2020.06.09
 * @since 20.06
 */
@SuppressWarnings({"unchecked", "rawtypes"})
@ApplicationScoped
public class CdiMapListenerManager
    {
    private void registerCacheListeners(@Observes @Created CacheLifecycleEvent event)
        {
        registerListeners(event.getCache(), event.getScope(), null, event.getServiceName());
        }

    private void registerRemoteListeners(@Observes @Created RemoteMapLifecycleEvent event)
        {
        registerListeners(event.getMap(), event.getScope(), event.getSessionName(), event.getServiceName());
        }

    private void registerListeners(NamedMap cache, String sEventScope, String sEventSession, String sEventService)
        {
        Set<CdiMapListener<?, ?>> setListeners = m_extension
                .getMapListeners(removeScope(sEventService), cache.getName());

        for (CdiMapListener<?, ?> listener : setListeners)
            {
            if (listener.hasFilterAnnotation())
                {
                // ensure that the listener's filter has been resolved as this
                // was not possible as discovery time.
                listener.resolveFilter(m_filterProducer);
                }

            if (listener.hasTransformerAnnotation())
                {
                // ensure that the listener's transformer has been resolved as this
                // was not possible as discovery time.
                listener.resolveTransformer(m_transformerProducer);
                }

            String  sScope     = listener.getScopeName();
            boolean fScopeOK   = sScope == null || sScope.equals(sEventScope);
            String  sSession   = listener.getRemoteSessionName();
            boolean fSessionOK = sSession == null || sSession.equals(sEventSession);

            if (fScopeOK && fSessionOK)
                {
                Filter mapEventFilter;
                Filter filter = listener.getFilter();
                if (filter instanceof MapEventFilter)
                    {
                    mapEventFilter = filter;
                    }
                else if (filter != null)
                    {
                    mapEventFilter = new MapEventFilter(MapEventFilter.E_ALL, filter);
                    }
                else
                    {
                    mapEventFilter = Filters.always();
                    }

                boolean fLite = listener.isLite();

                if (listener.isSynchronous())
                    {
                    cache.addMapListener(listener.synchronous(), mapEventFilter, fLite);
                    }
                else
                    {
                    cache.addMapListener(listener, mapEventFilter, fLite);
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

    // ---- data members ---------------------------------------------------

    @Inject
    private CoherenceExtension m_extension;

    @Inject
    private FilterProducer m_filterProducer;

    @Inject
    private MapEventTransformerProducer m_transformerProducer;
    }
