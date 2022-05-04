/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.coherence.config.scheme.ProxyScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Service;

import java.util.Iterator;
import java.util.concurrent.Callable;

/**
 * A {@link Callable} to be used with the Oracle Tools
 * test framework that can be submitted to a cluster
 * member to determine whether all of the proxy services
 * defined in that members cache configuration are running.
 *
 * @author jk 2014.09.17
 */
public class AreAllProxyServicesRunning
        implements RemoteCallable<Boolean>
    {
    @Override
    public Boolean call() throws Exception
        {
        ProxyScheme                        proxyScheme = new ProxyScheme();
        String                             sProxyType  = proxyScheme.getServiceType();
        Cluster                            cluster     = CacheFactory.getCluster();
        ExtensibleConfigurableCacheFactory eccf
                = (ExtensibleConfigurableCacheFactory) CacheFactory.getConfigurableCacheFactory();

        Iterator<ServiceScheme> iterator = eccf.getCacheConfig().getServiceSchemeRegistry().iterator();
        while (iterator.hasNext())
        {
        ServiceScheme serviceScheme = iterator.next();
        String        sType         = serviceScheme.getServiceType();

        if (sProxyType.equals(sType))
            {
            Service service = cluster.getService(serviceScheme.getServiceName());
            if (service == null || !service.isRunning())
                {
                return false;
                }
            }
        }

        return true;
        }
    }
