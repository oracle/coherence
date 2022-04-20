/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.CacheFactory;

public class GetClusterSize
        implements RemoteCallable<Integer>
    {
    @Override
    public Integer call() throws Exception
        {
        // attempt to get the cluster
        com.tangosol.net.Cluster cluster = CacheFactory.getCluster();

        // when there's no cluster or we're not yet a member there's no result
        return cluster == null || cluster.getLocalMember().getId() <= 0 ? -1 : cluster.getMemberSet().size();
        }
    }
