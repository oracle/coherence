/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi.testcacheserver;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.ServiceMonitor;
import com.tangosol.net.SimpleServiceMonitor;

import com.tangosol.run.xml.XmlHelper;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;

/**
 * TestCacheServerBundleActivator provides a {@link BundleActivator}
 * implementation which starts a Coherence Cache Server within an OSGi
 * container. This activator permits the following to be configured in the
 * manifest of the bundle this activator is started with:
 * <ol>
 *     <li><b>{@literal Coherence-Override}</b> - Location of the Coherence
 *     operational configuration file used when initializing the cluster.</li>
 *     <li><b>{@literal Coherence-CacheConfig}</b> - Location of the Coherence
 *     cache configuration file used to determine the services to start</li>
 * </ol>
 *
 * @author hr  2012.04.11
 * @since Coherence 12.1.2
 */
public class TestCacheServerBundleActivator
        implements BundleActivator
    {

    // ----- BundleActivator methods ----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(BundleContext ctx) throws Exception
        {
        Dictionary<String,String> dictHdrs = ctx.getBundle().getHeaders();
        String sOverride    = dictHdrs.get("Coherence-Override");
        String sCacheConfig = dictHdrs.get("Coherence-CacheConfig");

        if (sOverride != null && sOverride.length() > 0)
            {
            System.setProperty("tangosol.coherence.override", sOverride);
            }

        ClassLoader loader = getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(loader);

        if (sCacheConfig != null && sCacheConfig.length() > 0)
            {
            CacheFactory.getCacheFactoryBuilder().setCacheConfiguration(null,
                XmlHelper.loadFileOrResource(sCacheConfig, "cache configuration", loader));
            }

        ConfigurableCacheFactory ccf = m_ccf = CacheFactory.getConfigurableCacheFactory();

        if (ccf instanceof ExtensibleConfigurableCacheFactory)
            {
            ExtensibleConfigurableCacheFactory eccf = (ExtensibleConfigurableCacheFactory) ccf;
            ServiceMonitor monitor = new SimpleServiceMonitor();
            monitor.setConfigurableCacheFactory(eccf);
            eccf.startServices();
            monitor.registerServices(eccf.getServiceMap());
            eccf.activate();
            }
        else
            {
            DefaultCacheServer.startDaemon();
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(BundleContext ctx) throws Exception
        {
        ConfigurableCacheFactory ccf = m_ccf;

        if (ccf instanceof ExtensibleConfigurableCacheFactory)
            {
            ((ExtensibleConfigurableCacheFactory) ccf).dispose();
            }
        else
            {
            DefaultCacheServer.shutdown();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * {@link ConfigurableCacheFactory} used by this bundle.
     */
    protected ConfigurableCacheFactory m_ccf;
    }
