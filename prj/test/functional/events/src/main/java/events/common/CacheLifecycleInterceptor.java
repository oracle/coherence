/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events.common;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.CacheLifecycleEvents;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.util.CompositeKey;

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
        NamedCache<CompositeKey<Integer, String>, String> cacheResults = ensureResultsCache();

        int nMemberId = CacheFactory.getCluster().getLocalMember().getId();

        cacheResults.put(
                new CompositeKey<>(nMemberId, event.getType().name()),
                event.getCacheName());
        }

    protected NamedCache<CompositeKey<Integer, String>, String> ensureResultsCache()
        {
        if (m_cacheResults == null)
            {
            m_cacheResults = CacheFactory.getCache("result");
            }
        return m_cacheResults;
        }

    private NamedCache<CompositeKey<Integer, String>, String> m_cacheResults = null;
    }
