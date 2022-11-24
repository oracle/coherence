/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.oracle.coherence.testing.SystemPropertyIsolation;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test license mode
 *
 * @author mg  2022.11.23
 */
public class LicenseModeTests
    {
    @BeforeClass
    public static void setup()
        {
        // default is "dev"
        System.setProperty("coherence.mode", "prod");
        }

    @Test
    public void shouldBeDynamicLambdas()
        {
        Cluster cluster  = CacheFactory.getCluster();

        assertEquals(2, cluster.getDependencies().getMode());
        }
    }
