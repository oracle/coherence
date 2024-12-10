/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package management;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.JavaModules;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogs;
import com.oracle.bedrock.util.Capture;

import com.oracle.coherence.common.base.Reads;

import com.oracle.coherence.io.json.JsonSerializer;

import com.oracle.coherence.io.json.internal.GensonMapJsonBodyHandler;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.net.Coherence;

import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This test verifies that Management over REST will start
 * with the bare minimum required dependencies on the server
 */
public class ManagementMinimalDependenciesTests
    {
    @Test
    @SuppressWarnings("unchecked")
    public void shouldStartManagement() throws Exception
        {
        LocalPlatform    platform = LocalPlatform.get();
        Capture<Integer> port     = new Capture<>(platform.getAvailablePorts());
        String           sCluster = "Storage";

        if (JavaModules.useModules())
            {
            // Java Modules would include bedrock and its dependencies.
            // So, this test would fail with those dependencies modules
            // not found errors.
            return;
            }

        // We only need Coherence and Coherence JSON on the classpath
        // We add Coherence and PartitionedCache so that we can run this in an IDE
        // In Maven both classes would be in coherence.jar
        ClassPath classPath = ClassPath.of(ClassPath.ofClass(Coherence.class),
                                           ClassPath.ofClass(PartitionedCache.class),
                                           ClassPath.ofClass(JsonSerializer.class));

        try (CoherenceClusterMember member = platform.launch(CoherenceClusterMember.class, classPath,
                                    LocalHost.only(),
                                    WellKnownAddress.of("127.0.0.1"),
                                    SystemProperty.of("coherence.management", "dynamic"),
                                    SystemProperty.of("coherence.cluster", "Storage"),
                                    SystemProperty.of("coherence.management.extendedmbeanname", true),
                                    SystemProperty.of("coherence.management.http", "inherit"),
                                    SystemProperty.of("coherence.management.http.override-port", port),
                                    f_testLogs))
            {
            URI uri = URI.create("http://localhost:" + port.get() + "/management/coherence/cluster");

            Eventually.assertDeferred(() -> this.canConnect(uri), is(true));

            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            assertThat(connection.getResponseCode(), is(200));

            try (InputStream in = connection.getInputStream())
                {
                byte[] abBody = Reads.read(in);
                assertThat(abBody.length, is(not(0)));

                HashMap<String, Object> map = GensonMapJsonBodyHandler.s_genson.deserialize(abBody, HashMap.class);
                assertThat(map.get("clusterName"), is(sCluster));
                }
            }
        }

    private boolean canConnect(URI uri)
        {
        try
            {
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            return connection.getResponseCode() == 200;
            }
        catch (IOException e)
            {
            return false;
            }
        }

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static final TestLogs f_testLogs = new TestLogs(ManagementMinimalDependenciesTests.class);
    }
