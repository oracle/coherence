/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.client;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Order;

import org.junit.jupiter.api.extension.RegisterExtension;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import org.testcontainers.images.ImagePullPolicy;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.CoreMatchers.is;

/**
 * An integration test that uses the Testcontainers framework to start the
 * server clusters and client in Docker containers.
 */
@SuppressWarnings("resource")
@Testcontainers
@Disabled("Disabled as the images will not have been built in most CI environments")
public class MultiClusterContainerIT
        extends AbstractMultiClusterClientIT {

    public static final String DEFAULT_SERVER_IMAGE = "ghcr.io/coherence-community/multi-cluster-server:latest";

    public static final String SERVER_IMAGE         = System.getProperty("test.server.image", DEFAULT_SERVER_IMAGE);

    public static final String DEFAULT_CLIENT_IMAGE = "ghcr.io/coherence-community/multi-cluster-client:latest";

    public static final String CLIENT_IMAGE         = System.getProperty("test.client.image", DEFAULT_CLIENT_IMAGE);

    static final Network network = Network.newNetwork();

    @RegisterExtension
    @Order(1)
    static final TestLogsExtension testLogs = new TestLogsExtension(MultiClusterContainerIT.class);

    @Container
    @Order(2)
    static GenericContainer<?> tenants = new GenericContainer<>(DockerImageName.parse(SERVER_IMAGE))
            .withImagePullPolicy(NeverPull.INSTANCE)
            .withNetwork(network)
            .withNetworkAliases("webserver")
            .withExposedPorts(20000)
            .withLogConsumer(new ConsoleLogConsumer(testLogs.builder().build("webserver")))
            .withEnv("COHERENCE_WKA", "webserver")
            .withEnv("COHERENCE_CLUSTER", "webserver");

    @Container
    @Order(2)
    static GenericContainer<?> marvel = new GenericContainer<>(DockerImageName.parse(SERVER_IMAGE))
            .withImagePullPolicy(NeverPull.INSTANCE)
            .withNetwork(network)
            .withNetworkAliases("marvel")
            .withExposedPorts(20000)
            .withLogConsumer(new ConsoleLogConsumer(testLogs.builder().build("marvel")))
            .withEnv("COHERENCE_WKA", "marvel")
            .withEnv("COHERENCE_CLUSTER", "marvel");

    @Container
    @Order(2)
    static GenericContainer<?> starWars = new GenericContainer<>(DockerImageName.parse(SERVER_IMAGE))
            .withImagePullPolicy(NeverPull.INSTANCE)
            .withNetwork(network)
            .withNetworkAliases("star-wars")
            .withExposedPorts(1408)
            .withLogConsumer(new ConsoleLogConsumer(testLogs.builder().build("star-wars")))
            .withEnv("COHERENCE_WKA", "star-wars")
            .withEnv("COHERENCE_CLUSTER", "star-wars");

    @Container
    @Order(3)
    static GenericContainer<?> webServer = new GenericContainer<>(DockerImageName.parse(CLIENT_IMAGE))
            .withImagePullPolicy(NeverPull.INSTANCE)
            .withNetwork(network)
            .withNetworkAliases("webserver")
            .withExposedPorts(8080)
            .withLogConsumer(new ConsoleLogConsumer(testLogs.builder().build("webserver")))
            .withEnv("COHERENCE_EXTEND_ADDRESS", "127.0.0.1")
            .withEnv("COHERENCE_EXTEND_PORT", String.valueOf(tenants.getMappedPort(20000)));

    /**
     * Configure the tenants using the admin endpoints.
     * This will create the two tenants, "marvel" and "star-wars".
     *
     * @throws Exception if tenant creation fails
     */
    @BeforeAll
    static void configureTenants() throws Exception {
        Eventually.assertDeferred(marvel::isHealthy, is(true));
        Eventually.assertDeferred(starWars::isHealthy, is(true));
        Eventually.assertDeferred(tenants::isHealthy, is(true));
        Eventually.assertDeferred(webServer::isHealthy, is(true));

        int httpPort = webServer.getMappedPort(8080);
        configureTenants(httpPort, 20000, 1408);
    }

    @Override
    protected int getHttpPort() {
        return webServer.getMappedPort(8080);
    }

    public static class NeverPull
            implements ImagePullPolicy {
        @Override
        public boolean shouldPull(DockerImageName imageName) {
            return false;
        }

        public static final NeverPull INSTANCE = new NeverPull();
    }
}
