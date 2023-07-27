/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.bedrock.util.Capture;
import com.oracle.coherence.guides.client.webserver.WebServer;
import com.tangosol.net.grpc.GrpcDependencies;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.extension.RegisterExtension;

public class MultiClusterClientIT
        extends AbstractMultiClusterClientIT {

    /**
     * The port the Extend proxy in the Tenants' admin cluster will listen on.
     * This will effectively be an ephemeral port chosen at runtime.
     */
    static Capture<Integer> adminPort = new Capture<>(LocalPlatform.get().getAvailablePorts());

    /**
     * The port the Extend proxy in the Marvel cluster will listen on.
     * This will effectively be an ephemeral port chosen at runtime.
     */
    static Capture<Integer> marvelPort = new Capture<>(LocalPlatform.get().getAvailablePorts());

    /**
     * The port the gRPC proxy in the Star Wars cluster will listen on.
     * This will effectively be an ephemeral port chosen at runtime.
     */
    static Capture<Integer> starWarsPort = new Capture<>(LocalPlatform.get().getAvailablePorts());

    /**
     * The port the web-server in the client will listen on.
     * This will effectively be an ephemeral port chosen at runtime.
     */
    static Capture<Integer> httpPort = new Capture<>(LocalPlatform.get().getAvailablePorts());

    /**
     * A JUnit extension that will capture logs from the Coherence clusters started below.
     */
    @RegisterExtension
    @Order(1)
    static final TestLogsExtension logs = new TestLogsExtension(MultiClusterClientIT.class);

    /**
     * A Coherence Bedrock JUnit extension to start the Marvel cluster.
     */
    @RegisterExtension
    @Order(2)
    static CoherenceClusterExtension marvel = new CoherenceClusterExtension()
            .include(1, CoherenceClusterMember.class,
                    ClusterName.of("Marvel"),
                    SystemProperty.of("coherence.extend.port", marvelPort),
                    SystemProperty.of(GrpcDependencies.PROP_PORT, 0),
                    SystemProperty.of("webserver.port", 0),
                    RoleName.of("storage"),
                    IPv4Preferred.yes(),
                    LocalHost.only(),
                    WellKnownAddress.of("127.0.0.1"),
                    DisplayName.of("Marvel"),
                    logs);

    /**
     * A Coherence Bedrock JUnit extension to start the Star Wars cluster.
     */
    @RegisterExtension
    @Order(2)
    static CoherenceClusterExtension starWars = new CoherenceClusterExtension()
            .include(1, CoherenceClusterMember.class,
                    ClusterName.of("StarWars"),
                    SystemProperty.of(GrpcDependencies.PROP_PORT, starWarsPort),
                    SystemProperty.of("webserver.port", 0),
                    RoleName.of("storage"),
                    IPv4Preferred.yes(),
                    LocalHost.only(),
                    WellKnownAddress.of("127.0.0.1"),
                    DisplayName.of("StarWars"),
                    logs);

    /**
     * A Coherence Bedrock JUnit extension to start the Tenants Admin cluster.
     */
    @RegisterExtension
    @Order(3)
    static CoherenceClusterExtension tenants = new CoherenceClusterExtension()
            .include(1, CoherenceClusterMember.class,
                    ClusterName.of("Tenants"),
                    SystemProperty.of("coherence.extend.port", adminPort),
                    SystemProperty.of(GrpcDependencies.PROP_PORT, 0),
                    SystemProperty.of("webserver.port", 0),
                    RoleName.of("storage"),
                    IPv4Preferred.yes(),
                    LocalHost.only(),
                    WellKnownAddress.of("127.0.0.1"),
                    DisplayName.of("Tenants"),
                    logs);

    /**
     * A Coherence Bedrock JUnit extension to start the Client Web-Server.
     */
    @RegisterExtension
    @Order(4)
    static CoherenceClusterExtension client = new CoherenceClusterExtension()
            .include(1, CoherenceClusterMember.class,
                    ClusterName.of("client"),
                    SystemProperty.of("webserver.port", httpPort),
                    SystemProperty.of("coherence.client", "remote-fixed"),
                    SystemProperty.of("coherence.extend.address", "127.0.0.1"),
                    SystemProperty.of("coherence.extend.port", adminPort),
                    RoleName.of("client"),
                    ClassPath.ofSystem().excluding(".*coherence-grpc-proxy.*"),
                    IPv4Preferred.yes(),
                    LocalHost.only(),
                    WellKnownAddress.of("127.0.0.1"),
                    DisplayName.of("client"),
                    StabilityPredicate.of(CoherenceCluster.Predicates.isReady(WebServer.HEALTH_CHECK_NAME)),
                    logs);


    /**
     * Configure the tenants using the admin endpoints.
     * This will create the two tenants, "marvel" and "star-wars".
     *
     * @throws Exception if tenant creation fails
     */
    @BeforeAll
    static void configureTenants() throws Exception {
        configureTenants(httpPort.get(), marvelPort.get(), starWarsPort.get());
    }

    @Override
    protected int getHttpPort() {
        return httpPort.get();
    }
}
