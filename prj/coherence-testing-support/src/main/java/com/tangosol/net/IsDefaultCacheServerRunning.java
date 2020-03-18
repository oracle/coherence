/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

/**
 * An implementation of a {@link RemoteCallable} that
 * checks whether there is a singleton instance of
 * {@link DefaultCacheServer} and that the DefaultCacheServer's
 * {@link ServiceMonitor} is running.
 *
 * @author jk 2015.02.16
 */
public class IsDefaultCacheServerRunning
        implements RemoteCallable<Boolean>
    {
    // ----- constructors ---------------------------------------------------

    public IsDefaultCacheServerRunning()
        {
        }

    // ----- RemoteCallable methods -----------------------------------------

    @Override
    public Boolean call() throws Exception
        {

        DefaultCacheServer dcs = DefaultCacheServer.getInstance();

        if (dcs == null)
            {
            return false;
            }

        ServiceMonitor serviceMonitor = dcs.m_serviceMon;

        if (serviceMonitor == null || !serviceMonitor.isMonitoring())
            {
            return false;
            }

        Thread thread = serviceMonitor.getThread();

        return thread != null && thread.isAlive() && areAutoStartServicesRunning();
        }

    protected boolean areAutoStartServicesRunning()
        {
        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder().
                    getConfigurableCacheFactory(Base.getContextClassLoader());

        if (factory instanceof DefaultConfigurableCacheFactory)
            {
            DefaultConfigurableCacheFactory dccf = (DefaultConfigurableCacheFactory) factory;

            for (Object o : dccf.getConfig().getSafeElement("caching-schemes").getElementList())
                {
                XmlElement xmlScheme = (XmlElement) o;

                if (xmlScheme.getSafeElement("autostart").getBoolean())
                    {
                    Service service = dccf.ensureService(xmlScheme);
                    if (!service.isRunning())
                        {
                        return false;
                        }
                    }
                }
            }
        else if (factory instanceof ExtensibleConfigurableCacheFactory)
            {
            ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) factory;
            for (Service service : eccf.getServiceMap().keySet())
                {
                if (!service.isRunning())
                    {
                    return false;
                    }
                }
            }

        return true;
        }

    // ----- constants ------------------------------------------------------

    public static final IsDefaultCacheServerRunning INSTANCE = new IsDefaultCacheServerRunning();
    }
