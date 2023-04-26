/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.HeapSize;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.common.base.Exceptions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * A base class for topics compatibility tests.
 */
public abstract class BaseTopicsTests
    {
    /**
     * Create a running cluster.
     *
     * @param sClusterName  the cluster name
     * @param version       the version of Coherence to use
     *
     * @return  the running {@link ClosableCluster}
     */
    static ClosableCluster createRunningCluster(String sClusterName, Version version)
        {
        return createRunningCluster(sClusterName, version, 3);
        }

    /**
     * Create a running cluster.
     *
     * @param sClusterName  the cluster name
     * @param version       the version of Coherence to use
     * @param cMember       the number of cluster members to start
     *
     * @return  the running {@link ClosableCluster}
     */
    static ClosableCluster createRunningCluster(String sClusterName, Version version, int cMember)
        {
        try
            {
            ClosableCluster clusterExtension = createCluster(sClusterName, version, cMember);
            clusterExtension.start();
            return clusterExtension;
            }
        catch (Exception e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Create a cluster.
     *
     * @param sClusterName  the cluster name
     * @param version       the version of Coherence to use
     *
     * @return  the running {@link ClosableCluster}
     */
    static ClosableCluster createCluster(String sClusterName, Version version)
        {
        return createCluster(sClusterName, version, 3);
        }

    /**
     * Create a cluster.
     *
     * @param sClusterName  the cluster name
     * @param version       the version of Coherence to use
     * @param cMember       the number of cluster members to start
     *
     * @return  the running {@link ClosableCluster}
     */
    static ClosableCluster createCluster(String sClusterName, Version version, int cMember)
        {
        if (sClusterName == null || sClusterName.isBlank())
            {
            throw new IllegalArgumentException("Must provide a cluster name");
            }

        if (cMember <= 0)
            {
            cMember = 3;
            }

        ClosableCluster closableCluster = new ClosableCluster();

        closableCluster.getExtension()
                    .with(CacheConfig.of("coherence-cache-config.xml"),
                          version.getClassPath(),
                          WellKnownAddress.loopback(),
                          ClusterName.of(sClusterName),
                          DisplayName.of("storage"),
                          RoleName.of("storage"),
                          Logging.atFinest(),
                          LocalHost.only(),
                          IPv4Preferred.yes(),
                          HeapSize.of(64, HeapSize.Units.MB, 64, HeapSize.Units.MB, true),
                          m_testLogs)
                    .include(cMember, CoherenceClusterMember.class);

        return closableCluster;
        }

    // ----- data members ---------------------------------------------------

    /**
     * A JUnit extension to capture Bedrock process logs under the target folder.
     */
    @RegisterExtension
    @Order(1)
    static final TestLogsExtension m_testLogs = new TestLogsExtension(PubSubTests.class);
    }
