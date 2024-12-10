/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.concurrent.runnable.RuntimeExit;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.LocalJavaApplicationLauncher;
import com.oracle.bedrock.runtime.java.options.JavaAgent;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.runtime.options.Console;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.util.Capture;
import org.jacoco.agent.rt.RT;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.testsupport.deferred.Eventually.assertThat;
import static org.hamcrest.CoreMatchers.is;

@Disabled
public class LocalCoherenceClusterBuilderIT
        extends AbstractCoherenceClusterBuilderTest
    {
    @Override
    public Platform getPlatform()
        {
        return LocalPlatform.get();
        }


    /**
     * Ensure we can build and close a {@link CoherenceCluster}
     * of storage enabled members with a proxy server when using code-coverage tools.
     */
    @Test
    public void shouldEstablishStorageAndProxyClusterWithCodeCoverage() throws Exception
        {
        // determine the classpath of the JaCoCo runtime agent jar (should be something like jacocoagent-x.y.z.jar)
        ClassPath jacocoPath = ClassPath.ofClass(RT.class);

        // define a temp file name pattern for JaCoCo code coverage reports
        String jacocoDestinationFileName = "jacoco-${bedrock.runtime.id}.exec";
        File destinationFile = new File(System.getProperty("java.io.tmpdir"), jacocoDestinationFileName);

        // define the JavaAgent for JaCoCo
        JavaAgent javaAgent = JavaAgent.using(jacocoPath.toString(),
                                              "destfile=" + destinationFile
                                                      + ",output=file,sessionid=${bedrock.runtime.id},dumponexit=true");

        AvailablePortIterator availablePorts = LocalPlatform.get().getAvailablePorts();
        Capture<Integer> clusterPort = new Capture<>(availablePorts);

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        builder.include(2,
                        CoherenceClusterMember.class,
                        DisplayName.of("storage"),
                        ClusterPort.of(clusterPort),
                        LocalStorage.enabled(),
                        CacheConfig.of("test-cache-config.xml"),
                        LocalHost.only(),
                        ClusterName.of("Storage-Proxy"),
                        javaAgent,
                        RuntimeExit.withExitCode(0));

        builder.include(1,
                        CoherenceClusterMember.class,
                        DisplayName.of("extend"),
                        ClusterPort.of(clusterPort),
                        LocalStorage.disabled(),
                        CacheConfig.of("test-extend-proxy-config.xml"),
                        LocalHost.only(),
                        ClusterName.of("Storage-Proxy"),
                        javaAgent,
                        RuntimeExit.withExitCode(0));

        try (CoherenceCluster cluster = builder.build(Console.system()))
            {
            // ensure the cluster size is as expected
            assertThat(invoking(cluster).getClusterSize(), is(3));

            CoherenceClusterMember extendMember = cluster.get("extend-1");

            assertThat(invoking(extendMember).isServiceRunning("ExtendTcpProxyService"), is(true));

            for (CoherenceClusterMember storageMember : cluster.getAll("storage"))
                {
                assertThat(invoking(storageMember).isServiceRunning("ExtendTcpProxyService"), is(false));
                }
            }
        catch (Exception e)
            {
            e.printStackTrace();
            Assertions.fail();
            }
        }
    }
