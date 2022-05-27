/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.CacheFactory;

/**
 * A {@link RemoteCallable} to obtain the machine name.
 *
 * @author Jonathan Knight  2022.05.25
 * @since 22.06
 */
public class GetLocalMemberMachineName
        implements RemoteCallable<String>
    {
    @Override
    public String call() throws Exception
        {
        // attempt to get the cluster
        com.tangosol.net.Cluster cluster = CacheFactory.getCluster();

        // when there's no cluster there's no result
        return cluster == null ? null : cluster.getLocalMember().getMachineName();
        }
    }
