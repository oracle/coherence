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
import com.tangosol.util.UUID;

/**
 * A {@link RemoteCallable} to obtain the local member's {@link UUID}.
 *
 * @author Jonathan Knight  2022.06.06
 * @since 22.06
 */
public class GetLocalMemberUUID
        implements RemoteCallable<UUID>
    {
    @Override
    public UUID call() throws Exception
        {
        // attempt to get the cluster
        Cluster cluster = CacheFactory.getCluster();

        // when there's no cluster there's no result
        return cluster == null ? null : cluster.getLocalMember().getUuid();
        }
    }
