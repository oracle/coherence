/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.*;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;

public class SingleMemberCoherenceClusterResourceTest
{
    /**
     * A {@link CoherenceClusterResource} defining a single cluster member
     */
    @ClassRule
    public static CoherenceClusterResource coherenceResource =
        new CoherenceClusterResource()
            .with(ClusterName.of("SingletonCluster"), LocalHost.only(), Multicast.ttl(0))
            .include(1,
                     RoleName.of("storage"),
                     DisplayName.of("storage"),
                     LocalStorage.enabled(),
                     SystemProperty.of("test.property", "storageMember"));


    /**
     * Ensure that a single member cluster is formed by {@link CoherenceClusterResource}.
     */
    @Test
    public void shouldFormCorrectSizeCluster()
    {
        CoherenceCluster cluster  = coherenceResource.getCluster();
        Set<String>      setNames = new TreeSet<>();

        for (CoherenceClusterMember member : cluster)
        {
            setNames.add(member.getName());
        }

        assertThat(setNames, contains("storage-1"));
    }


    @Test
    public void shouldHaveSetStorageMemberProperty() throws Exception
    {
        for (CoherenceClusterMember member : coherenceResource.getCluster().getAll("storage"))
        {
            assertThat(member.getSystemProperty("test.property"), is("storageMember"));
        }
    }                                                         
}
