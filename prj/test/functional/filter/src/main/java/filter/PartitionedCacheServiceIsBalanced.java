/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package filter;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;

import java.io.Serializable;

/**
 * @author jk 2014.09.08
 */
public class PartitionedCacheServiceIsBalanced
        implements RemoteCallable<Boolean>, Serializable
    {

    public PartitionedCacheServiceIsBalanced(String sServiceName)
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

        int nUnbalanced = ((PartitionedCache) service)
                .calculateUnbalanced();

        return nUnbalanced == 0;
        }

    protected String m_sServiceName;
    }
