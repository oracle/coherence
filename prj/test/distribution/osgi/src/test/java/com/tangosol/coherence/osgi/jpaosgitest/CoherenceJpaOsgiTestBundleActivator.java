/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi.jpaosgitest;

import com.oracle.coherence.jpa.JpaCacheStore;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;

/**
 * CoherenceJpaOsgiTestBundleActivator is a part of a bundle that has a
 * dependency on Coherence and Coherence JPA and will exercise some basic
 * functionality. The failure of this bundle will result in this bundle being
 * in an inactive state. Subsequently this will result in the failure of the
 * {@link com.tangosol.coherence.osgi.JpaOsgiTest} as it asserts the
 * state of this bundle.
 *
 * @author hr  2012.01.27
 * @since Coherence 12.1.2
 */
public class CoherenceJpaOsgiTestBundleActivator
        implements BundleActivator
    {

    // ----- BundleActivator methods ----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(BundleContext context) throws Exception
        {
        m_ctxBundle = context;
        Bundle bundle = getBundle("Coherence JPA Integration");

        if (bundle == null)
            {
            throw new IllegalStateException("A dependent bundle (Coherence JPA) was not resolved.");
            }

        ClassLoader cl = getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        // introduce a compile-time dependency to JpaCacheStore to ensure
        // the dependency is resolved and valid within this bundle's class
        // space
        Class<JpaCacheStore> clzJpaStore = JpaCacheStore.class;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(BundleContext context) throws Exception
        {
        m_ctxBundle = null;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Convenience method to return a {@link Bundle} known by the container
     * as the name specified.
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
            Dictionary<String,String> dictHeaders = proposedBundle.getHeaders();
            String                    sName       = dictHeaders.get("Bundle-Name");

            if (sName.equalsIgnoreCase(sBundleName))
                {
                match = proposedBundle;
                break;
                }
            }
        return match;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link BundleContext} associated with this Bundle.
     */
    private BundleContext m_ctxBundle;
    }
