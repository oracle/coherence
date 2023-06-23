/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi;

import com.tangosol.coherence.osgi.restosgitest.CoherenceRestOsgiTestBundleActivator;

import com.tangosol.coherence.rest.util.JsonMap;
import com.tangosol.net.CacheFactory;

import org.junit.Test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * RestOsgiTest tests the deployment and execution of the
 * <tt>coherence-rest.jar</tt> within an OSGi container.
 *
 * @author hr  2012.01.29
 * @since Coherence 12.1.2
 */
public class RestOsgiTest
        extends AbstractOsgiTest
    {
    @Test
    public void testDeployBundle() throws BundleException
        {
        Container container = m_container;
        String sPackage     = CoherenceRestOsgiTestBundleActivator.class.getPackage().getName().replace('.','/');

        listBundles();

        // deploy dependencies
        libDeploy("bdb.bnd");
        libDeploy("jackson-all.bnd");
        libDeploy("jersey-core.bnd");
        libDeploy("jersey-json.bnd");
        libDeploy("jersey-server.bnd");
        deployDependency(CacheFactory.class);
        deployDependency(JsonMap.class);

        // deploy this test as a bundle
        container.packageAndDeploy("/" + sPackage + "/coherence-rest-osgi-test.properties");

        listBundles();

        // ensure all known bundles are deployed and active
        assertThat(container.getBundle("Coherence"),             hasState(Bundle.ACTIVE));
        assertThat(container.getBundle("Coherence REST"),        hasState(Bundle.ACTIVE));
        assertThat(container.getBundle("CoherenceRestOsgiTest"), hasState(Bundle.ACTIVE));
        }
    }
