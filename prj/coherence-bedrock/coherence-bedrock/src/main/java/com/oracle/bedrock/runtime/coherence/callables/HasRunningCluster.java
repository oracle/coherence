/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.CacheFactory;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link RemoteCallable} to determine whether the
 * member has a running cluster.
 */
public class HasRunningCluster
        implements RemoteCallable<Boolean>
    {
    @Override
    public Boolean call() throws Exception
        {
        // attempt to get the cluster
        com.tangosol.net.Cluster cluster = CacheFactory.getCluster();
        return cluster != null && cluster.isRunning();
        }

    // ----- constants ------------------------------------------------------

    /**
     * A singleton instance of {@link HasRunningCluster}.
     */
    public static final HasRunningCluster INSTANCE = new HasRunningCluster();
    }
