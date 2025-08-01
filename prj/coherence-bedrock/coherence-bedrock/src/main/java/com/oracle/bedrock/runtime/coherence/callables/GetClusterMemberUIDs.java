/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;
import com.tangosol.util.UID;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unchecked")
public class GetClusterMemberUIDs
        implements RemoteCallable<Set<UID>>
    {
    @Override
    public Set<UID> call() throws Exception
        {
        // attempt to get the cluster
        com.tangosol.net.Cluster cluster = CacheFactory.getCluster();

        if (cluster == null)
            {
            return null;
            }
        else
            {
            Set<UID> memberUIDs = new HashSet<>();

            Set<Member> memberSet = cluster.getMemberSet();

            for (Member member : memberSet)
                {
                memberUIDs.add(member.getUid());
                }

            return memberUIDs;
            }
        }
    }
