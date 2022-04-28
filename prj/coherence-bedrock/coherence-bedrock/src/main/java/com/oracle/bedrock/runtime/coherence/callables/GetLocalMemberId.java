/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

public class GetLocalMemberId
        implements RemoteCallable<Integer>
    {
    @Override
    public Integer call() throws Exception
        {
        // attempt to get the cluster
        Cluster cluster = CacheFactory.getCluster();

        // when there's no cluster there's no result
        return cluster == null ? -1 : cluster.getLocalMember().getId();
        }
    }
