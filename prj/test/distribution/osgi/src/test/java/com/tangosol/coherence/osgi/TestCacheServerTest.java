/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.osgi.testcacheserver.TestCacheServerBundleActivator;

import com.tangosol.net.CacheFactory;

import org.junit.Test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.oracle.coherence.testing.matcher.CoherenceMatchers.hasThreadGroupSize;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * A test that illustrates the ability to start Coherence as a cache server
 * within an OSGi container.
 *
 * @author hr  2012.04.12
 * @since Coherence 12.1.2
 */
public class TestCacheServerTest
        extends AbstractOsgiTest
    {
    @Test
    public void testCacheServer() throws BundleException, InterruptedException
        {
        Container container = m_container;
        String    sPackage  = TestCacheServerBundleActivator.class.getPackage().getName().replace('.','/');

        listBundles();

        // deploy dependencies
        libDeploy("bdb.bnd");
        deployDependency(CacheFactory.class);

        // deploy this test as a bundle
        container.packageAndDeploy("/" + sPackage + "/test-cache-server.properties");

        listBundles();

        // ensure all known bundles are deployed and active and the threads
        // associated with the cache configuration are started
        assertThat(container.getBundle("Coherence"),                hasState(Bundle.ACTIVE));
        assertThat(container.getBundle("CoherenceTestCacheServer"), hasState(Bundle.ACTIVE));

        Eventually.assertThat(invoking(hasThreadGroupSize(is(5))).matches("TestCacheServerPartitionedService"), is(true));

        // stop the bundle and ensure there are no threads associated
        // with the service remaining
        container.getBundle("CoherenceTestCacheServer").stop();
        listBundles();

        Eventually.assertThat(invoking(hasThreadGroupSize(anyOf(nullValue(), is(0)))).matches("TestCacheServerPartitionedService"), is(true));
        }
    }
