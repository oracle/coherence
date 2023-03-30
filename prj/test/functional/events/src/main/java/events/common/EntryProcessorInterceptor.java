/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events.common;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.EntryProcessorEvents;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryProcessorEvent;

import java.io.Serializable;

/**
 * Test EventInterceptor for {@link EntryProcessorEvent}.
 *
 * @author mg 2022.05.27
 * @since 22.06
 */
@Interceptor(identifier = "EntryProcessorInterceptor")
@EntryProcessorEvents({EntryProcessorEvent.Type.EXECUTED})
public class EntryProcessorInterceptor
        implements EventInterceptor<EntryProcessorEvent>, Serializable
    {
    // --------- EventInterceptor method ------------------------------------

    @Override
    public void onEvent(EntryProcessorEvent event)
        {
        System.out.println("event " + event.getType() + " called: " + String.format("Entries=%d, processor=%s", event.getEntrySet().size(), event.getProcessor().toString()));
        int cEvents = event.getEntrySet().size();
        getResultsCache().compute("entryset-size", (k, v) -> v == null ? cEvents : v + cEvents);
        }

    protected NamedCache<String, Integer> getResultsCache()
        {
        if (events == null)
            {
            NamedCache<String, Integer> nc;
            synchronized (this)
                {
                nc = CacheFactory.getCache("results");
                if (events == null)
                    {
                    events = nc;
                    }
                }
            }

        return events;
        }

    // ----- data members ---------------------------------------------------

    protected NamedCache<String, Integer> events;
    }
