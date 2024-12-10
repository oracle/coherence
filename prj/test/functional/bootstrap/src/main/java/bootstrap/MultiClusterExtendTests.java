/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package bootstrap;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.bedrock.util.Capture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class MultiClusterExtendTests
    {
    @Test
    public void shouldConnectToDifferentClusters()
        {
        LocalPlatform    platform  = LocalPlatform.get();
        Capture<Integer> extendOne = new Capture<>(platform.getAvailablePorts());
        Capture<Integer> extendTwo = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember memberOne = platform.launch(CoherenceClusterMember.class,
                                                                ClusterName.of("One"),
                                                                DisplayName.of("one"),
                                                                SystemProperty.of("coherence.extend.port", extendOne),
                                                                m_testLogs);
             CoherenceClusterMember memberTwo = platform.launch(CoherenceClusterMember.class,
                                                                ClusterName.of("Two"),
                                                                DisplayName.of("two"),
                                                                SystemProperty.of("coherence.extend.port", extendTwo),
                                                                m_testLogs))
            {
            Eventually.assertDeferred(memberOne::isReady, is(true));
            Eventually.assertDeferred(memberTwo::isReady, is(true));

            Eventually.assertDeferred(() -> memberOne.getServiceStatus("Proxy"), is(ServiceStatus.RUNNING));
            Eventually.assertDeferred(() -> memberTwo.getServiceStatus("Proxy"), is(ServiceStatus.RUNNING));

            String sCacheName = "test";
            String sKey       = "Key";

            ExtendApp appOne = new ExtendApp("ExtendOne", "127.0.0.1", extendOne.get());
            appOne.setValue(sKey, "ValueOne", sCacheName);
            assertThat(appOne.getValue(sKey, sCacheName), is("ValueOne"));

            ExtendApp appTwo = new ExtendApp("ExtendTwo", "127.0.0.1", extendTwo.get());
            appTwo.setValue(sKey, "ValueTwo", sCacheName);
            assertThat(appTwo.getValue(sKey, sCacheName), is("ValueTwo"));

            // Make sure Bank of America data is unchanged
            assertThat(appOne.getValue(sKey, sCacheName), is("ValueOne"));
            }
        }

    // ----- data members ---------------------------------------------------

    @RegisterExtension
    static final TestLogsExtension m_testLogs = new TestLogsExtension(MultiClusterExtendTests.class);
    }
