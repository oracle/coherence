/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.docker;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.client.GrpcSessionConfiguration;
import com.oracle.coherence.concurrent.atomic.Atomics;
import com.oracle.coherence.concurrent.atomic.RemoteAtomicInteger;
import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;
import com.oracle.coherence.io.json.JsonSerializer;
import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.internal.net.management.HttpHelper;
import com.tangosol.internal.net.metrics.MetricsHttpHelper;
import com.tangosol.io.Serializer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * A base class for testing Oracle Coherence Docker images.
 *
 * @author Jonathan Knight 2022.08.02
 */
public abstract class BaseDockerImageTests
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeEach
    void setUp(TestInfo testInfo)
        {
        m_sTestMethod = testInfo.getTestMethod().map(Method::getName).orElse("unknown");
        }

    @AfterEach
    void afterTest()
        {
        // clean up Coherence
        CacheFactory.shutdown();
        }

    // ----- helper methods -------------------------------------------------

    GenericContainer<?> start(GenericContainer<?> container)
        {
        container.start();
        return container;
        }

    protected File getOutputDirectory()
        {
        File fileOutDir = FileUtils.getTestOutputFolder(getClass());
        File fileTests  = new File(fileOutDir, "functional" + File.separator + getClass().getSimpleName());
        File dir        = new File(fileTests, m_sTestMethod);
        dir.mkdirs();
        return dir;
        }

    // ----- inner class NeverPull ------------------------------------------

    /**
     * A Testcontainers pull policy.
     */
    public static class NeverPull
            implements ImagePullPolicy
        {
        @Override
        public boolean shouldPull(DockerImageName imageName)
            {
            return false;
            }

        public static final NeverPull INSTANCE = new NeverPull();
        }

    // ----- constants ------------------------------------------------------

    /**
     * COHERENCE_HOME inside coherence docker image.
     */
    public static final String COHERENCE_HOME = System.getProperty("docker.coherence.home", "/coherence");

    public static final int GRPC_PORT = Integer.getInteger("port.grpc", 1408);
    public static final int MANAGEMENT_PORT = Integer.getInteger("port.management", HttpHelper.DEFAULT_MANAGEMENT_OVER_REST_PORT);
    public static final int METRICS_PORT = Integer.getInteger("port.metrics", MetricsHttpHelper.DEFAULT_PROMETHEUS_METRICS_PORT);
    public static final int EXTEND_PORT = Integer.getInteger("port.extend",20000);

    public static final int CONCURRENT_EXTEND_PORT = Integer.getInteger("port.concurrent.extend",20001);

    // ----- data members ---------------------------------------------------

    /**
     * The name of the current test method.
     */
    protected  String m_sTestMethod = "unknown";
    }
