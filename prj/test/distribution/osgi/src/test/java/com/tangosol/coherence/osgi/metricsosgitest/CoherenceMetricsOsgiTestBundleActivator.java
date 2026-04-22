/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi.metricsosgitest;

import com.oracle.coherence.micrometer.CoherenceMicrometerMetrics;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;

/**
 * CoherenceMetricsOsgiTestBundleActivator validates Coherence Micrometer
 * classes are resolvable from this test bundle.
 */
public class CoherenceMetricsOsgiTestBundleActivator
        implements BundleActivator
    {
    @Override
    public void start(BundleContext context)
        {
        m_ctxBundle = context;
        Bundle bundle = getBundle("Coherence Micrometer");

        if (bundle == null)
            {
            throw new IllegalStateException("A dependent bundle (Coherence Micrometer) was not resolved.");
            }

        ClassLoader cl = getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        // compile-time dependency check to ensure wiring in this bundle space
        Class<CoherenceMicrometerMetrics.Adapter> clzAdapter = CoherenceMicrometerMetrics.Adapter.class;
        }

    @Override
    public void stop(BundleContext context)
        {
        m_ctxBundle = null;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a {@link Bundle} known by the container as the name specified.
     *
     * @param sBundleName  the name of the bundle
     *
     * @return a bundle if a name is matched or null
     */
    protected Bundle getBundle(String sBundleName)
        {
        Bundle match = null;
        for (Bundle proposedBundle : m_ctxBundle.getBundles())
            {
            Dictionary<String, String> dictHeaders = proposedBundle.getHeaders();
            String                     sName       = dictHeaders.get("Bundle-Name");

            if (sName.equalsIgnoreCase(sBundleName))
                {
                match = proposedBundle;
                break;
                }
            }
        return match;
        }

    /**
     * The {@link BundleContext} associated with this bundle.
     */
    private BundleContext m_ctxBundle;
    }
