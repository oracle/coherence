/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.CacheFactory;

/**
 * A {@link RemoteCallable} to obtain the cluster port.
 *
 * @author Jonathan Knight 2022.12.09
 * @since 22.06.4
 */
public class GetLocalMemberClusterPort
        implements RemoteCallable<Integer>
    {
    @Override
    public Integer call() throws Exception
        {
        // attempt to get the cluster
        com.tangosol.net.Cluster cluster = CacheFactory.getCluster();

        // when there's no cluster there's no result
        return cluster == null ? -1 : cluster.getDependencies().getGroupPort();
        }
    }
