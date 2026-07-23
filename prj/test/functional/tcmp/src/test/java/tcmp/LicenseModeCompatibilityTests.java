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
import com.oracle.coherence.testing.BedrockInvocationProperties;

import common.AbstractFunctionalTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

/**
 * Tests mixed legacy security mode and existing dev/prod license mode clusters.
 *
 * @author Aleks Seovic  2026.05.13
 */
public class LicenseModeCompatibilityTests
        extends AbstractFunctionalTest
    {
    @Test
    public void shouldRollDevelopmentClusterToLegacyMode()
        {
        assertRollingRestart("dev", "legacy");
        }

    @Test
    public void shouldRollProductionClusterToLegacyMode()
        {
        assertRollingRestart("prod", "legacy");
        }

    @Test
    public void shouldRollLegacyClusterToDevelopmentMode()
        {
        assertRollingRestart("legacy", "dev");
        }

    @Test
    public void shouldRollLegacyClusterToProductionMode()
        {
        assertRollingRestart("legacy", "prod");
        }

    private void assertRollingRestart(String sModeOne, String sModeTwo)
        {
        String sCluster = clusterName(sModeOne + "-to-" + sModeTwo);
        int    nPort    = LocalPlatform.get().getAvailablePorts().next();

        List<CoherenceClusterMember> listMembers = new ArrayList<>();
        try
            {
            CoherenceClusterMember memberOne = startMember(sCluster, nPort, sModeOne, sModeOne + "-one");
            listMembers.add(memberOne);
            assertClusterSize(1, memberOne);

            CoherenceClusterMember memberTwo = startMember(sCluster, nPort, sModeOne, sModeOne + "-two");
            listMembers.add(memberTwo);
            assertClusterSize(2, memberOne, memberTwo);

            closeMember(listMembers, memberTwo);
            assertClusterSize(1, memberOne);

            memberTwo = startMember(sCluster, nPort, sModeTwo, sModeTwo + "-two");
            listMembers.add(memberTwo);
            assertClusterSize(2, memberOne, memberTwo);

            closeMember(listMembers, memberOne);
            assertClusterSize(1, memberTwo);

            memberOne = startMember(sCluster, nPort, sModeTwo, sModeTwo + "-one");
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
