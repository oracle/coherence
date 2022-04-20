/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.testing;


import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.tangosol.net.Cluster;
import com.tangosol.net.Member;


/**
* A collection of helper methods that call a method on a given object.
* This is to be used by Bedrock's Eventually.assertThat(invoking(...)) pattern.
*
* @author lh  2018.08.02
*/
public class TestInfrastructureHelper
    {
    // ----- helper methods -------------------------------------------------

    public int getLocalMemberId(CoherenceClusterMember member)
        {
        return member.getLocalMemberId();
        }

    public boolean matchMemberId(Cluster cluster, int nId)
        {
        return cluster.getMemberSet().stream().mapToInt(m -> ((Member) m).getId())
                .anyMatch(n -> n == nId);
        }

    /**
     * Get the cluster size of a given cluster.
     */
    public int getClusterSize(Cluster cluster)
        {
        return cluster.getMemberSet().size();
        }

    /**
     * Get the cluster size of a given Coherence cluster.
     */
    public int getClusterSize(CoherenceCluster cluster)
        {
        return cluster.getClusterSize();
        }

    /**
     * Get the cluster size of a given Coherence server.
     */
    public int getClusterSize(CoherenceCacheServer clusterMember)
        {
        return clusterMember.getClusterSize();
        }

    /**
     * Submit a {@link RemoteCallable<Integer>} on a given cluster server
     * and returns its result.
     */
    public int submitInt(CoherenceCacheServer server, RemoteCallable<Integer> remoteCallable)
        {
        try
            {
            return server.submit(remoteCallable).get();
            }
        catch (Exception e)
            {}

        return 0;
        }

    /**
     * Submit a {@link RemoteCallable<Boolean>} on a given cluster server
     * and returns its result.
     */
    public boolean submitBool(CoherenceCacheServer server, RemoteCallable<Boolean> remoteCallable)
        {
        try
            {
            return server.submit(remoteCallable).get();
            }
        catch (Exception e)
            {}

        return false;
        }

    /**
     * Submit a {@link RemoteCallable<Boolean>} of a given cluster member
     * and returns its result.
     */
    public boolean submitBool(CoherenceClusterMember member, RemoteCallable<Boolean> remoteCallable)
        {
        try
            {
            return member.submit(remoteCallable).get();
            }
        catch (Exception e)
            {}

        return false;
        }

    /**
     * Submit a {@link RemoteCallable<String>} of a given cluster server
     * and returns its result.
     */
    public String submitString(CoherenceCacheServer server, RemoteCallable<String> remoteCallable)
        {
        try
            {
            return server.submit(remoteCallable).get();
            }
        catch (Exception e)
            {}

        return "";
        }
    }
