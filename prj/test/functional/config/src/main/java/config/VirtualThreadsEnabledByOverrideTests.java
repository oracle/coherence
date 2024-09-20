/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package config;

import com.tangosol.net.CacheFactory;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertFalse;


/**
 * Validate enabling/disabling the use of virtual threads.
 *
 * @author lh  2024.01.12
 */
@SuppressWarnings("unchecked")
public class VirtualThreadsEnabledByOverrideTests
    {
    @BeforeClass
    public static void setup()
        {
        System.clearProperty("coherence.virtualthreads.enabled");
        System.setProperty("coherence.override", "tangosol-coherence-override-virtual-threads.xml");
        }

    @Test
    public void shouldDisableVirtualThreads()
        {
        assertFalse(CacheFactory.ensureCluster().getDependencies().isVirtualThreadsEnabled());
        }
    }
