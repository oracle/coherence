/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ssl;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.OperationalOverride;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.coherence.testing.util.KeyTool;
import com.tangosol.net.SocketProviderFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ClusterOneWayServerOnlyCertsTests
    {
    @BeforeClass
    public static void _setup() throws Exception
        {
        KeyTool.assertCanCreateKeys();

        File fileBuild = MavenProjectFileUtils.locateBuildFolder(SSLKeysAndCertsTests.class);

        s_serverCACert     = KeyTool.createCACert(fileBuild,"server-ca", "PKCS12");
        s_serverKeyAndCert = KeyTool.createKeyCertPair(fileBuild, s_serverCACert, "server", "serverAuth");
        }

    @Before
    public void setPort()
        {
        System.setProperty("coherence.security.server.keystore", s_serverKeyAndCert.getKeystoreURI());
        System.setProperty("coherence.security.server.keystore.password", s_serverKeyAndCert.storePasswordString());
        System.setProperty("coherence.security.server.key.password", s_serverKeyAndCert.keyPasswordString());
        System.setProperty("coherence.security.server.truststore", s_serverCACert.getKeystoreURI());
        System.setProperty("coherence.security.server.truststore.password", s_serverCACert.storePasswordString());
        }

    @Test
    public void shouldStartCluster()
        {
        LocalPlatform         platform = LocalPlatform.get();
        AvailablePortIterator ports    = platform.getAvailablePorts();

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder()
                .include(3, CoherenceClusterMember.class,
                        OperationalOverride.of("global-server-only-one-way-override.xml"),
                        SystemProperty.of(SocketProviderFactory.PROP_GLOBAL_PROVIDER, "one"),
                        SystemProperty.of("coherence.security.server.keystore", s_serverKeyAndCert.getKeystoreURI()),
                        SystemProperty.of("coherence.security.server.keystore.password", s_serverKeyAndCert.storePasswordString()),
                        SystemProperty.of("coherence.security.server.key.password", s_serverKeyAndCert.keyPasswordString()),
                        SystemProperty.of("coherence.security.server.truststore", s_serverCACert.getKeystoreURI()),
                        SystemProperty.of("coherence.security.server.truststore.password", s_serverCACert.storePasswordString()),
                        ClusterName.of("ClusterOneWayServerOnlyCertsTests"),
                        ClusterPort.of(ports.next()),
                        WellKnownAddress.loopback(),
                        LocalHost.loopback(),
                        IPv4Preferred.yes(),
                        m_logs,
                        DisplayName.of("storage"));

        try (CoherenceCluster cluster = builder.build(platform))
            {
            Eventually.assertDeferred(cluster::isReady, is(true));

            for (CoherenceClusterMember member : cluster)
                {
                assertThat(member.getClusterSize(), is(3));
                }
            }
        }

    // ----- data members ---------------------------------------------------

    @Rule
    public final TestLogs m_logs = new TestLogs(ClusterOneWayServerOnlyCertsTests.class);

    protected static KeyTool.KeyAndCert s_serverCACert;

    protected static KeyTool.KeyAndCert s_serverKeyAndCert;
    }
