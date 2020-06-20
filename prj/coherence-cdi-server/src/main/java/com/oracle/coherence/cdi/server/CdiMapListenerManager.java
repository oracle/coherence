/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.CdiMapListener;
import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.events.Created;

import com.tangosol.net.NamedCache;
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
    private void registerListeners(@Observes @Created CacheLifecycleEvent event)
        {
        Set<CdiMapListener<?, ?>> setListeners = m_extension.getMapListeners(removeScope(event.getServiceName()), event.getCacheName());

        NamedCache cache       = event.getCache();
        String     sEventScope = cache.getCacheService().getBackingMapManager().getCacheFactory().getScopeName();

        for (CdiMapListener<?, ?> listener : setListeners)
            {
            String sScope = listener.getScopeName();
            if (sScope == null || sScope.equals(sEventScope))
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

                MapEventTransformer transformer = listener.getTransformer();
                if (transformer != null)
                    {
                    mapEventFilter = new MapEventTransformerFilter(mapEventFilter, transformer);
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
        int nIndex = sServiceName.indexOf(':');
        return nIndex > -1 ? sServiceName.substring(nIndex + 1) : sServiceName;
        }

    // ---- data members ---------------------------------------------------

    @Inject
    private CoherenceExtension m_extension;
    }
