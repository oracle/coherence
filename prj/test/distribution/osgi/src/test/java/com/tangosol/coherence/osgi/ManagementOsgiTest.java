/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi;

import com.tangosol.coherence.management.RestManagement;
import com.tangosol.coherence.osgi.managementosgitest.CoherenceManagementOsgiTestBundleActivator;

import com.tangosol.net.CacheFactory;

import org.junit.Test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * ManagementOsgiTest tests the deployment and execution of the
 * <tt>coherence-management.jar</tt> within an OSGi container.
 *
 * @since 15.1.2.0
 */
public class ManagementOsgiTest
        extends AbstractOsgiTest
    {
    @Test
    public void testDeployBundle() throws BundleException
        {
        Container container = m_container;
        String    sPackage  = CoherenceManagementOsgiTestBundleActivator.class.getPackage().getName().replace('.', '/');

        listBundles();

        // deploy dependencies
        libDeploy("jackson-all.bnd");
        libDeploy("jersey-core.bnd");
        libDeploy("jersey-json.bnd");
        libDeploy("jersey-server.bnd");
        libDeploy("coherence-internal-http.bnd");
        deployDependency(CacheFactory.class);
        deployDependency(RestManagement.class);

        // deploy this test as a bundle
        container.packageAndDeploy("/" + sPackage + "/coherence-management-osgi-test.properties");

        listBundles();

        // ensure all known bundles are deployed and active
        assertThat(container.getBundle("Coherence"),                    hasState(Bundle.ACTIVE));
        assertThat(container.getBundle("Coherence Management over REST"), hasState(Bundle.ACTIVE));
        assertThat(container.getBundle("CoherenceManagementOsgiTest"),  hasState(Bundle.ACTIVE));
        }
    }
