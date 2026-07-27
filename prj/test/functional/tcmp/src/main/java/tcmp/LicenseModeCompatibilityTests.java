/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package tcmp;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.java.options.JavaHome;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.BedrockInvocationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

/**
 * Tests released license mode rolling compatibility.
 *
 * @author Aleks Seovic  2026.05.13
 */
public class LicenseModeCompatibilityTests
        extends AbstractFunctionalTest
    {
    @Test
    public void shouldRollDevelopmentClusterWithUnsetSecurityMode()
        {
        assertRollingRestart("dev");
        }

    @Test
    public void shouldRollProductionClusterWithUnsetSecurityMode()
        {
        assertRollingRestart("prod");
        }

    private void assertRollingRestart(String sMode)
        {
        String sCluster = clusterName(sMode);
        int    nPort    = LocalPlatform.get().getAvailablePorts().next();

        List<CoherenceClusterMember> listMembers = new ArrayList<>();
        try
            {
            CoherenceClusterMember memberOne = startMember(sCluster, nPort, sMode, sMode + "-one");
            listMembers.add(memberOne);
            assertClusterSize(1, memberOne);

            CoherenceClusterMember memberTwo = startMember(sCluster, nPort, sMode, sMode + "-two");
            listMembers.add(memberTwo);
            assertClusterSize(2, memberOne, memberTwo);

            closeMember(listMembers, memberTwo);
            assertClusterSize(1, memberOne);

            memberTwo = startMember(sCluster, nPort, sMode, sMode + "-two-restarted");
            listMembers.add(memberTwo);
            assertClusterSize(2, memberOne, memberTwo);

            closeMember(listMembers, memberOne);
            assertClusterSize(1, memberTwo);

            memberOne = startMember(sCluster, nPort, sMode, sMode + "-one-restarted");
            listMembers.add(memberOne);
            assertClusterSize(2, memberOne, memberTwo);
            }
        finally
            {
            closeAll(listMembers);
            }
        }

    private CoherenceClusterMember startMember(String sCluster, int nPort, String sMode, String sName)
        {
        OptionsByType options = OptionsByType.of(
                ClusterName.of(sCluster),
                DisplayName.of(sName),
                SystemProperty.of("coherence.localhost", "127.0.0.1"),
                SystemProperty.of("coherence.ttl", 0),
                SystemProperty.of("coherence.wka", "127.0.0.1"),
                SystemProperty.of("test.multicast.port", nPort),
                SystemProperty.of("coherence.mode", sMode));

        String sJavaHome = System.getProperty("server.java.home");
        if (sJavaHome != null)
            {
            options.add(JavaHome.at(sJavaHome));
            }

        return LocalPlatform.get().launch(CoherenceClusterMember.class,
                BedrockInvocationProperties.inherit(options).asArray());
        }

    private void assertClusterSize(int cMembers, CoherenceClusterMember... aMember)
        {
        for (CoherenceClusterMember member : aMember)
            {
            Eventually.assertDeferred(member::getClusterSize, is(cMembers), Timeout.after(2, TimeUnit.MINUTES));
            }
        }

    private String clusterName(String sSuffix)
        {
        return getClass().getSimpleName() + '-' + sSuffix + '-' + System.nanoTime();
        }

    private void closeMember(List<CoherenceClusterMember> listMembers, CoherenceClusterMember member)
        {
        listMembers.remove(member);
        member.close();
        }

    private void closeAll(List<CoherenceClusterMember> listMembers)
        {
        for (CoherenceClusterMember member : listMembers)
            {
            member.close();
            }
        listMembers.clear();
        }
    }
