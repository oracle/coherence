/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi.managementosgitest;

import com.tangosol.coherence.management.RestManagement;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Dictionary;

/**
 * CoherenceManagementOsgiTestBundleActivator is a part of a bundle that has a
 * dependency on Coherence Management over REST and will exercise some basic
 * functionality. The failure of this bundle will result in this bundle being
 * in an inactive state. Subsequently this will result in the failure of the
 * {@link com.tangosol.coherence.osgi.ManagementOsgiTest} as it asserts the
 * state of this bundle.
 */
public class CoherenceManagementOsgiTestBundleActivator
        implements BundleActivator
    {

    // ----- BundleActivator methods ----------------------------------------

    @Override
    public void start(BundleContext context)
        {
        m_ctxBundle = context;
        Bundle bundle = getBundle("Coherence Management over REST");

        if (bundle == null)
            {
            throw new IllegalStateException("A dependent bundle (Coherence Management over REST) was not resolved.");
            }

        ClassLoader cl = getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        // introduce a compile-time dependency to RestManagement to ensure
        // the dependency is resolved and valid within this bundle's class
        // space
        Class<RestManagement> clzRestManagement = RestManagement.class;
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

    // ----- data members ---------------------------------------------------

    /**
     * The {@link BundleContext} associated with this Bundle.
     */
    private BundleContext m_ctxBundle;
    }
