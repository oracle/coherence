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
 * A {@link RemoteCallable} to obtain the names of the services
 * known to the Coherence cluster.
 */
public class GetServiceNames
        implements RemoteCallable<Set<String>>
    {
    @Override
    public Set<String> call() throws Exception
        {
        // attempt to get the cluster
        com.tangosol.net.Cluster cluster = CacheFactory.getCluster();
        Set<String>              set     = new HashSet<>();

        // when there's no cluster there's no result
        if (cluster == null || !cluster.isRunning())
            {
            return set;
            }

        Enumeration<String> en  = cluster.getServiceNames();
        while (en.hasMoreElements())
            {
            set.add(en.nextElement());
            }
        return set;
        }
    }
