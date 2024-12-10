/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;

import java.io.Serializable;

/**
 * @author bbc 2020.08.26
 */
public class PartitionedCacheServiceIsDistributionStable
        implements RemoteCallable<Boolean>, Serializable
    {

    public PartitionedCacheServiceIsDistributionStable(String sServiceName)
        {
        m_sServiceName = sServiceName;
        }

    @Override
    public Boolean call() throws Exception
        {
        ConfigurableCacheFactory cacheFactory = CacheFactory.getConfigurableCacheFactory();
        CacheService service = (CacheService) cacheFactory.ensureService(m_sServiceName);
        if (service instanceof SafeService)
            {
            service = (CacheService) ((SafeService) service).getService();
            }


        return ((PartitionedCache) service).isDistributionStable();
        }

    protected String m_sServiceName;
    }
