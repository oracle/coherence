/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.junit;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.*;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.util.Capture;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Set;
import java.util.TreeSet;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class CoherenceClusterExtensionTest
{
    /**
     * Establish a {@link CoherenceClusterExtension} for our test.
     */
    @RegisterExtension
    static CoherenceClusterExtension coherenceResource =
            new CoherenceClusterExtension()
                    .using(LocalPlatform.get())
                    .with(LocalHost.only(),
                          Multicast.ttl(0),
                          ClusterPort.automatic(),
                          SystemProperty.of("coherence.mode", System.getProperty("coherence.mode", "dev")),
                          SystemProperty.of("tangosol.coherence.extend.address", LocalPlatform.get().getLoopbackAddress().getHostAddress()),
                          SystemProperty.of("tangosol.coherence.extend.port", Capture.of(LocalPlatform.get().getAvailablePorts())))
                    .include(2,
                            DisplayName.of("storage"),
                            RoleName.of("storage"),
                            LocalStorage.enabled(),
                            SystemProperty.of("tangosol.coherence.extend.enabled", false),
                            SystemProperty.of("test.property", "storageMember"))
                    .include(1,
                            DisplayName.of("proxy"),
                            RoleName.of("proxy"),
                            LocalStorage.disabled(),
                            SystemProperty.of("tangosol.coherence.extend.enabled", true),
                            SystemProperty.of("test.property", "proxyServer"));


    /**
     * Ensure that the required cluster is formed by a {@link CoherenceClusterResource}.
     */
    @Test
    public void shouldFormCluster()
    {
        // ensure the size of the cluster is as expected
        assertThat(coherenceResource.getCluster().getClusterSize(), is(greaterThanOrEqualTo(3)));

        // collect the names of the members
        CoherenceCluster cluster  = coherenceResource.getCluster();
        Set<String>      setNames = new TreeSet<>();

        for (CoherenceClusterMember member : cluster)
        {
            setNames.add(member.getName());
        }

        // ensure the names of the cluster members are as expected
        assertThat(setNames, contains("proxy-1", "storage-1", "storage-2"));
    }


    /**
     * Ensure that a {@link StorageDisabledMember} session can be created against the {@link CoherenceClusterResource}.
     */
    @Test
    public void shouldCreateStorageDisabledMemberSession()
    {
        ConfigurableCacheFactory session = coherenceResource.createSession(SessionBuilders.storageDisabledMember());

        NamedCache               cache   = session.ensureCache("dist-example", null);

        Eventually.assertThat(coherenceResource.getCluster().getClusterSize(), is(4));

        cache.put("message", "hello world");

        Eventually.assertThat(invoking(coherenceResource.getCluster().getCache("dist-example")).get("message"),
                              is((Object) "hello world"));
    }


    /**
     * Ensure that a {@link ExtendClient} session can be created against the {@link CoherenceClusterResource}.
     */
    @Test
    public void shouldCreateExtendClientSession()
    {
        ConfigurableCacheFactory session =
            coherenceResource.createSession(SessionBuilders.extendClient("client-cache-config.xml"));

        NamedCache cache = session.ensureCache("dist-example", null);

        assertThat(coherenceResource.getCluster().getClusterSize(), is(greaterThanOrEqualTo(3)));

        cache.put("message", "hello world");

        Eventually.assertThat(invoking(coherenceResource.getCluster().getCache("dist-example")).get("message"),
                              is((Object) "hello world"));
    }

    /**
     * Ensure the proxy member(s) have a property set.
     */
    @Test
    public void shouldHaveSetProxyServerProperty()
    {
        for (CoherenceClusterMember member : coherenceResource.getCluster().getAll("proxy"))
        {
            assertThat(member.getSystemProperty("test.property"), is("proxyServer"));
        }
    }


    /**
     * Ensure the storage-enabled member(s) have a property set.
     */
    @Test
    public void shouldHaveSetStorageMemberProperty()
    {
        for (CoherenceClusterMember member : coherenceResource.getCluster().getAll("storage"))
        {
            assertThat(member.getSystemProperty("test.property"), is("storageMember"));
        }
    }


    /**
     * Ensure that a {@link CoherenceClusterResource} can be used to perform a rolling restart.
     */
    @Test
    public void shouldPerformRollingRestart() throws Exception
    {
        CoherenceCluster cluster = coherenceResource.getCluster();

        int memberOneBefore = cluster.get("storage-1").getLocalMemberId();
        int memberTwoBefore = cluster.get("storage-2").getLocalMemberId();

        // only restart storage enabled members
        cluster.filter(member -> member.getName().startsWith("storage")).relaunch();

        CoherenceClusterMember memberOneAfter = cluster.get("storage-1");
        CoherenceClusterMember memberTwoAfter = cluster.get("storage-2");

        assertThat(memberOneAfter, is(notNullValue()));
        assertThat(memberTwoAfter, is(notNullValue()));

        assertThat(memberOneAfter.getLocalMemberId(), is(not(memberOneBefore)));
        assertThat(memberTwoAfter.getLocalMemberId(), is(not(memberTwoBefore)));
    }


    /**
     * Ensure that a {@link CoherenceClusterResource} can be used to perform a rolling restart
     * when there's a storage disabled session.
     */
    @Test
    public void shouldPerformRollingRestartWithStorageDisabledSession() throws Exception
    {
        CoherenceCluster cluster = coherenceResource.getCluster();

        ConfigurableCacheFactory cacheFactory = coherenceResource.createSession(new StorageDisabledMember());

        // only restart storage enabled members
        cluster.filter(member -> member.getName().startsWith("storage")).relaunch();

        CoherenceClusterMember member1 = cluster.get("storage-1");
        CoherenceClusterMember member2 = cluster.get("storage-2");

        assertThat(member1, is(notNullValue()));
        assertThat(member2, is(notNullValue()));
    }
}
