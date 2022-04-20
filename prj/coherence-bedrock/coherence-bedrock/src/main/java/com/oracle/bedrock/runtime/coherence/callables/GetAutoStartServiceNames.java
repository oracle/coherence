/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.scheme.AbstractCompositeScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.run.xml.XmlElement;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GetAutoStartServiceNames
        implements RemoteCallable<Set<String>>
    {
    @Override
    public Set<String> call() throws Exception
        {
        ConfigurableCacheFactory configurableCacheFactory = CacheFactory.getConfigurableCacheFactory();

        if (configurableCacheFactory instanceof DefaultConfigurableCacheFactory)
            {
            DefaultConfigurableCacheFactory cacheFactory = (DefaultConfigurableCacheFactory) configurableCacheFactory;

            // obtain the XmlElements representing the service scheme configurations
            XmlElement xmlCacheConfig = cacheFactory.getConfig();

            Map<String, XmlElement> serviceSchemes = cacheFactory.collectServiceSchemes(xmlCacheConfig);

            HashSet<String> serviceNames = new HashSet<>();

            for (String serviceName : serviceSchemes.keySet())
                {
                XmlElement xmlServiceScheme = serviceSchemes.get(serviceName);

                boolean isAutoStart = xmlServiceScheme.getSafeElement("autostart").getBoolean(false);

                if (isAutoStart)
                    {
                    serviceNames.add(serviceName);
                    }
                }

            return serviceNames;
            }
        else if (configurableCacheFactory instanceof ExtensibleConfigurableCacheFactory)
            {
            ExtensibleConfigurableCacheFactory cacheFactory =
                    (ExtensibleConfigurableCacheFactory) configurableCacheFactory;

            CacheConfig cacheConfig = cacheFactory.getCacheConfig();

            if (cacheConfig == null)
                {
                throw new RuntimeException("Failed to determine the CacheConfig for the ExtensibleConfigurableCacheFactory");
                }
            else
                {
                LinkedHashSet<String> serviceNames = new LinkedHashSet<>();

                for (ServiceScheme serviceScheme : cacheConfig.getServiceSchemeRegistry())
                    {
                    if (serviceScheme.isAutoStart())
                        {
                        if (serviceScheme instanceof AbstractCompositeScheme)
                            {
                            serviceScheme = ((AbstractCompositeScheme) serviceScheme).getBackScheme();

                            if (isAutoStartable(serviceScheme))
                                {
                                serviceNames.add(serviceScheme.getScopedServiceName());
                                }
                            }
                        else
                            {
                            serviceNames.add(serviceScheme.getScopedServiceName());
                            }
                        }
                    }

                return serviceNames;
                }
            }
        else
            {
            throw new RuntimeException("The ConfigurableCacheFactory is neither a DefaultConfigurableCacheFactory or a ExtensibleConfigurableCacheFactory");
            }
        }


    /**
     * Determine if the specified {@link ServiceScheme} is auto-startable
     * (typically if it requires a cluster to operate).
     *
     * @param scheme the {@link ServiceScheme}
     * @return if the service scheme is auto-startable
     */
    public boolean isAutoStartable(ServiceScheme scheme)
        {
        return scheme.getServiceBuilder().isRunningClusterNeeded();
        }
    }
