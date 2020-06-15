/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package events.common;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.CacheLifecycleEvents;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.annotation.Interceptor;

import java.io.Serializable;


/**
 * Test EventInterceptor for {@link CacheLifecycleEvent}.
 *
 * @author bbc 2015.09.15
 * @since 12.2.1.1
 */
@Interceptor(identifier = "CacheLifecycle")
@CacheLifecycleEvents
public class CacheLifecycleInterceptor
        implements EventInterceptor<CacheLifecycleEvent>, Serializable
    {
    // --------- constructor ------------------------------------------------

    public CacheLifecycleInterceptor()
        {
        super();
        }

    // --------- EventInterceptor method ------------------------------------

    @Override
    public void onEvent(CacheLifecycleEvent event)
        {
        String sCache = event.getCacheName();
        if (m_resultCache == null)
            {
            m_resultCache = CacheFactory.getCache("result");
            }

        switch (event.getType())
            {
            case CREATED:
                m_resultCache.put("created", sCache);
            case TRUNCATED:
                m_resultCache.put("truncated", sCache);
            case DESTROYED:
                m_resultCache.put("destroyed", sCache);
            }
        }

    private NamedCache m_resultCache = null;
    }
