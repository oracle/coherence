/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.MachineName;
import com.oracle.bedrock.runtime.coherence.options.MemberName;
import com.oracle.bedrock.runtime.coherence.options.RackName;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.SiteName;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.component.util.SafeCluster;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MemberIdentityTests
    {
    @Test
    public void shouldUseSystemProperties()
        {
        try (CoherenceClusterMember member = s_platform.launch(CoherenceClusterMember.class,
                                                               serverClasspath(),
                                                               ClusterName.of("test-cluster"),
                                                               MachineName.of("test-machine"),
                                                               MemberName.of("test-member"),
                                                               RackName.of("test-rack"),
                                                               RoleName.of("test-role"),
                                                               SiteName.of("test-site"),
                                                               Logging.atMax(),
                                                               m_testLogs))
            {
            Eventually.assertDeferred(member::isReady, is(true));
            assertThat(member.getClusterName(), is("test-cluster"));
            assertThat(member.getMachineName(), is("test-machine"));
            assertThat(member.getMemberName(), is("test-member"));
            assertThat(member.getRackName(), is("test-rack"));
            assertThat(member.getRoleName(), is("test-role"));
            assertThat(member.getSiteName(), is("test-site"));
            }
        }

    @Test
    public void shouldUseSpecifiedProvider()
        {
        try (CoherenceClusterMember member = s_platform.launch(CoherenceClusterMember.class,
                                                               SystemProperty.of(MemberIdentityProvider.PROPERTY, TestIdentity.class.getName()),
                                                               ClusterName.of("test-cluster"),
                                                               Logging.atMax(),
                                                               m_testLogs))
            {
            TestIdentity identity = new TestIdentity();

            Eventually.assertDeferred(member::isReady, is(true));
            assertThat(member.getClusterName(), is("test-cluster"));
            assertThat(member.getMachineName(), is(identity.getMachineName()));
            assertThat(member.getMemberName(), is(identity.getMemberName()));
            assertThat(member.getRackName(), is(identity.getRackName()));
            assertThat(member.getRoleName(), is(identity.getRoleName()));
            assertThat(member.getSiteName(), is(identity.getSiteName()));
            }
        }

    @Test
    public void shouldUseDiscoveredProvider()
        {
        try (CoherenceClusterMember member = s_platform.launch(CoherenceClusterMember.class,
                                                               ClusterName.of("test-cluster"),
                                                               Logging.atMax(),
                                                               m_testLogs))
            {
            DiscoveredIdentity identity = new DiscoveredIdentity();

            Eventually.assertDeferred(member::isReady, is(true));
            assertThat(member.getClusterName(), is("test-cluster"));
            assertThat(member.getMachineName(), is(identity.getMachineName()));
            assertThat(member.getMemberName(), is(identity.getMemberName()));
            assertThat(member.getRackName(), is(identity.getRackName()));
            assertThat(member.getRoleName(), is(identity.getRoleName()));
            assertThat(member.getSiteName(), is(identity.getSiteName()));
            }
        }

    @Test
    public void shouldUseSystemPropertiesOverProvider()
        {
        try (CoherenceClusterMember member = s_platform.launch(CoherenceClusterMember.class,
                                                               ClusterName.of("test-cluster"),
                                                               MachineName.of("test-machine"),
                                                               MemberName.of("test-member"),
                                                               RackName.of("test-rack"),
                                                               RoleName.of("test-role"),
                                                               SiteName.of("test-site"),
                                                               Logging.atMax(),
                                                               m_testLogs))
            {
            Eventually.assertDeferred(member::isReady, is(true));
            assertThat(member.getClusterName(), is("test-cluster"));
            assertThat(member.getMachineName(), is("test-machine"));
            assertThat(member.getMemberName(), is("test-member"));
            assertThat(member.getRackName(), is("test-rack"));
            assertThat(member.getRoleName(), is("test-role"));
            assertThat(member.getSiteName(), is("test-site"));
            }
        }

    private ClassPath serverClasspath()
        {
        try
            {
            return ClassPath.of(ClassPath.ofClass(Coherence.class), ClassPath.ofClass(SafeCluster.class));
            }
        catch (IOException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    // ----- inner class: TestIdentity --------------------------------------

    public static class TestIdentity
            implements MemberIdentityProvider
        {
        @Override
        public String getMachineName()
            {
            Logger.finer("****** Called TestIdentity.getMachineName()");
            return "machine-one";
            }

        @Override
        public String getMemberName()
            {
            Logger.finer("****** Called TestIdentity.getMemberName()");
            return "member-one";
            }

        @Override
        public String getRackName()
            {
            Logger.finer("****** Called TestIdentity.getRackName()");
            return "rack-one";
            }

        @Override
        public String getRoleName()
            {
            Logger.finer("****** Called TestIdentity.getRoleName()");
            return "role-one";
            }

        @Override
        public String getSiteName()
            {
            Logger.finer("****** Called TestIdentity.getSiteName()");
            return "site-one";
            }
        }

    // ----- inner class: TestIdentity --------------------------------------

    public static class DiscoveredIdentity
            implements MemberIdentityProvider
        {
        @Override
        public String getMachineName()
            {
            return "machine-two";
            }

        @Override
        public String getMemberName()
            {
            return "member-two";
            }

        @Override
        public String getRackName()
            {
            return "rack-two";
            }

        @Override
        public String getRoleName()
            {
            return "role-two";
            }

        @Override
        public String getSiteName()
            {
            return "site-two";
            }
        }

    // ----- data members ---------------------------------------------------

    public static final LocalPlatform s_platform = LocalPlatform.get();

    @Rule
    public TestLogs m_testLogs = new TestLogs(MemberIdentityTests.class);
    }
