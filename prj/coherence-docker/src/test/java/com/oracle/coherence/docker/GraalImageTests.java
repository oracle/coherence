/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.docker;

import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.oracle.coherence.common.base.Classes;
import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;

import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.Session;

import com.tangosol.util.Resources;
import com.tangosol.util.processor.ScriptProcessor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for the polyglot Graal image.
 *
 * @author Jonathan Knight 2022.08.02
 */
@SuppressWarnings("resource")
public class GraalImageTests
        extends BaseDockerImageTests
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        ImageNames.verifyGraalTestAssumptions();
        }

    /**
     * Return the image names for paramterized tests.
     *
     * @return the image names for paramterized tests
     */
    static String[] getImageNames()
        {
        return ImageNames.getGraalImageNames();
        }

    // ----- test methods ---------------------------------------------------

    @ParameterizedTest
    @MethodSource("getImageNames")
    void shouldRunServersideJS(String sImageName) throws Exception
        {
        ImageNames.verifyGraalTestAssumptions();

        URL  url       = Resources.findFileOrResource("scripts/js/processors.mjs", Classes.getContextClassLoader());
        File dirScript = new File(url.toURI()).getParentFile();

        try (GenericContainer<?> container = start(new GenericContainer<>(DockerImageName.parse(sImageName))
                .withImagePullPolicy(NeverPull.INSTANCE)
                .withLogConsumer(new ConsoleLogConsumer(m_testLogs.builder().build("Storage")))
                .withFileSystemBind(dirScript.getAbsolutePath(), "/app/classes/scripts/js", BindMode.READ_ONLY)
                .withExposedPorts(EXTEND_PORT, CONCURRENT_EXTEND_PORT)))
            {
            Eventually.assertDeferred(container::isHealthy, is(true), Timeout.after(5, TimeUnit.MINUTES));

            LocalPlatform platform       = LocalPlatform.get();
            int           extendPort     = container.getMappedPort(EXTEND_PORT);
            int           concurrentPort = container.getMappedPort(CONCURRENT_EXTEND_PORT);

            try (CoherenceClusterMember client = platform.launch(CoherenceClusterMember.class,
                                                                 SystemProperty.of("coherence.client", "remote-fixed"),
                                                                 SystemProperty.of("coherence.extend.address", "127.0.0.1"),
                                                                 SystemProperty.of("coherence.extend.port", extendPort),
                                                                 SystemProperty.of("coherence.concurrent.extend.address", "127.0.0.1"),
                                                                 SystemProperty.of("coherence.concurrent.extend.port", concurrentPort),
                                                                 IPv4Preferred.yes(),
                                                                 LocalHost.only(),
                                                                 DisplayName.of("client"),
                                                                 m_testLogs))
                {
                Eventually.assertDeferred(client::isCoherenceRunning, is(true));

                RemoteCallable<Void> testExtend = () ->
                    {
                    Session session = Coherence.getInstance().getSession();
                    NamedCache<String, String> cache   = session.getCache("test-cache");

                    Service cacheService = cache.getService();
                    if (cacheService instanceof SafeService)
                        {
                        cacheService = ((SafeService) cacheService).getService();
                        }
                    assertThat(cacheService, is(instanceOf(RemoteCacheService.class)));

                    String sResult = cache.invoke("key-1", new ScriptProcessor<>("js", "TestProcessor"));
                    assertThat(sResult, is("ok"));
                    return null;
                    };

                client.invoke(testExtend);
                }
            }
        }

    // ----- data members ---------------------------------------------------

    @RegisterExtension
    protected static final TestLogsExtension m_testLogs = new TestLogsExtension(GraalImageTests.class);
    }
