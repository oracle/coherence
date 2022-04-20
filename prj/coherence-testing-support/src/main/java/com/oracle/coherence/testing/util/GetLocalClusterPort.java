/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

/**
 * Return the local cluster group via a {@link RemoteCallable}.
 *
 * @author  tam 2015.06.09
 */
public class GetLocalClusterPort
        implements RemoteCallable<Integer>
    {
    public GetLocalClusterPort()
        {
        }

    // ----- RemoteCallable methods -----------------------------------------

    @Override
    public Integer call() throws Exception
        {
        Cluster cluster = CacheFactory.getCluster();

        return Integer.valueOf(cluster == null ? -1 : cluster.getDependencies().getGroupPort());
        }
    }
