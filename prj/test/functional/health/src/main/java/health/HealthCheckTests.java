/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package health;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.runtime.coherence.callables.IsServiceRunning;

import com.oracle.bedrock.runtime.coherence.callables.IsServiceSuspended;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.RoleName;

import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;

import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;

import com.oracle.bedrock.util.Capture;

import com.oracle.coherence.common.base.Exceptions;

import com.tangosol.net.Coherence;

import com.tangosol.util.HealthCheck;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;

import java.net.ConnectException;

import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("unused")
public class HealthCheckTests
    {
    @BeforeEach
    public void cleanupPersistence(TestInfo testInfo)
        {
        File fileBuild = MavenProjectFileUtils.locateBuildFolder(HealthCheckTests.class);
        File fileBase  = new File(fileBuild, "persistence");
        m_filePersistence = new File(fileBase, testInfo.getTestMethod().get().getName());

        if (m_filePersistence.exists())
            {
            MavenProjectFileUtils.recursiveDelete(m_filePersistence);
            }
        assertThat(m_filePersistence.mkdirs(), is(true));
        }

    @Test
    void shouldBeHealthySingleMember()
        {
        LocalPlatform    platform       = LocalPlatform.get();
        Capture<Integer> nHealthPort    = new Capture<>(platform.getAvailablePorts());
        Capture<Integer> managementPort = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember ignored = platform.launch(CoherenceClusterMember.class,
                                                       ClassName.of(Coherence.class),
                                                       CacheConfig.of("test-cache-config.xml"),
                                                       IPv4Preferred.yes(),
                                                       LocalHost.only(),
                                                       Logging.atMax(),
                                                       m_testLogs.builder(),
                                                       DisplayName.of("Storage"),
                                                       SystemProperty.of("coherence.management.http", "inherit"),
                                                       SystemProperty.of("coherence.management.http.port", managementPort),
                                                       SystemProperty.of(PROP_HEALTH_PORT, nHealthPort)))
            {
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, HealthCheck.PATH_STARTED), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, HealthCheck.PATH_LIVE), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, HealthCheck.PATH_HEALTHZ), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, HealthCheck.PATH_READY), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, PATH_HA), is(200));
            }
        }

    @Test
    public void shouldBeHealthyMultipleMembers()
        {
        LocalPlatform    platform     = LocalPlatform.get();
        Capture<Integer> nHealthPort1 = new Capture<>(platform.getAvailablePorts());
        Capture<Integer> nHealthPort2 = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember app1 = platform.launch(CoherenceClusterMember.class,
                                                    ClassName.of(Coherence.class),
                                                    CacheConfig.of("test-cache-config.xml"),
                                                    IPv4Preferred.yes(),
                                                    LocalHost.only(),
                                                    Logging.atMax(),
                                                    m_testLogs.builder(),
                                                    DisplayName.of("Storage-0"),
                                                    SystemProperty.of(PROP_HEALTH_PORT, nHealthPort1))) {
            try (CoherenceClusterMember app2 = platform.launch(CoherenceClusterMember.class,
                                                        ClassName.of(Coherence.class),
                                                        CacheConfig.of("test-cache-config.xml"),
                                                        IPv4Preferred.yes(),
                                                        LocalHost.only(),
                                                        Logging.atMax(),
                                                        m_testLogs.builder(),
                                                        DisplayName.of("Storage-1"),
                                                        SystemProperty.of(PROP_HEALTH_PORT, nHealthPort2))) {

            Eventually.assertDeferred(() -> httpRequest(nHealthPort1, HealthCheck.PATH_STARTED), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort2, HealthCheck.PATH_STARTED), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort1, HealthCheck.PATH_LIVE), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort2, HealthCheck.PATH_LIVE), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort1, HealthCheck.PATH_READY), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort2, HealthCheck.PATH_READY), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort1, PATH_HA), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort2, PATH_HA), is(200));
            }
        }
    }

    @Test
    public void shouldBeHealthyWhenStorageDisabled()
        {
        LocalPlatform    platform       = LocalPlatform.get();
        Capture<Integer> nHealthPort    = new Capture<>(platform.getAvailablePorts());
        Capture<Integer> managementPort = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember ignored = platform.launch(CoherenceClusterMember.class,
                                                       ClassName.of(Coherence.class),
                                                       CacheConfig.of("test-cache-config.xml"),
                                                       IPv4Preferred.yes(),
                                                       LocalHost.only(),
                                                       Logging.atMax(),
                                                       LocalStorage.disabled(),
                                                       m_testLogs.builder(),
                                                       DisplayName.of("Storage"),
                                                       SystemProperty.of("coherence.management.http", "inherit"),
                                                       SystemProperty.of("coherence.management.http.port", managementPort),
                                                       SystemProperty.of(PROP_HEALTH_PORT, nHealthPort)))
            {
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, HealthCheck.PATH_STARTED), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, HealthCheck.PATH_LIVE), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, HealthCheck.PATH_READY), is(200));
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, PATH_HA), is(200));
            }
        }

    @Test
    public void shouldBeStatusHAMultipleMembersStorageEnabledAndDisabledActivePersistence()
        {
        File             buildDir    = MavenProjectFileUtils.ensureTestOutputFolder(getClass(), "shouldBeStatusHAMultipleMembersStorageEnabledAndDisabledActivePersistence");
        File             activeDir   = new File(buildDir, "persistence");
        LocalPlatform    platform     = LocalPlatform.get();
        Capture<Integer> nHealthPort1 = new Capture<>(platform.getAvailablePorts());
        Capture<Integer> nHealthPort2 = new Capture<>(platform.getAvailablePorts());

        activeDir.mkdirs();
        activeDir.deleteOnExit();

        try (CoherenceClusterMember app1 = platform.launch(CoherenceClusterMember.class,
                                                    ClassName.of(Coherence.class),
                                                    CacheConfig.of("test-cache-config.xml"),
                                                    LocalStorage.enabled(),
                                                    IPv4Preferred.yes(),
                                                    LocalHost.only(),
                                                    Logging.atMax(),
                                                    m_testLogs.builder(),
                                                    DisplayName.of("storage"),
                                                    SystemProperty.of("coherence.distributed.persistence-mode", "active"),
                                                    SystemProperty.of("coherence.distributed.persistence.base.dir", activeDir.getAbsolutePath()),
                                                    SystemProperty.of(PROP_HEALTH_PORT, nHealthPort1)))
            {
            try (CoherenceClusterMember app2 = platform.launch(CoherenceClusterMember.class,
                                                        ClassName.of(Coherence.class),
                                                        CacheConfig.of("test-cache-config.xml"),
                                                        LocalStorage.disabled(),
                                                        IPv4Preferred.yes(),
                                                        LocalHost.only(),
                                                        Logging.atMax(),
                                                        m_testLogs.builder(),
                                                        DisplayName.of("storage-disabled"),
                                                        SystemProperty.of(PROP_HEALTH_PORT, nHealthPort2)))
                {
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, HealthCheck.PATH_READY), is(200));
                Eventually.assertDeferred(() -> httpRequest(nHealthPort2, HealthCheck.PATH_READY), is(200));
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, PATH_HA), is(200));
                Eventually.assertDeferred(() -> httpRequest(nHealthPort2, PATH_HA), is(200));
                }
            }
        }

    @Test
    public void shouldBeStatusHAMultipleMemberDifferentServices()
        {
        LocalPlatform    platform     = LocalPlatform.get();
        Capture<Integer> nHealthPort1 = new Capture<>(platform.getAvailablePorts());
        Capture<Integer> nHealthPort2 = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember app1 = platform.launch(CoherenceClusterMember.class,
                                                    ClassName.of(Coherence.class),
                                                    CacheConfig.of("test-cache-config.xml"),
                                                    IPv4Preferred.yes(),
                                                    LocalHost.only(),
                                                    Logging.atMax(),
                                                    m_testLogs.builder(),
                                                    DisplayName.of("storage-0"),
                                                    SystemProperty.of(PROP_HEALTH_PORT, nHealthPort1)))
            {
            try (CoherenceClusterMember app2 = platform.launch(CoherenceClusterMember.class,
                                                        ClassName.of(Coherence.class),
                                                        CacheConfig.of("test-cache-config-two.xml"),
                                                        IPv4Preferred.yes(),
                                                        LocalHost.only(),
                                                        Logging.atMax(),
                                                        m_testLogs.builder(),
                                                        DisplayName.of("storage-1"),
                                                        SystemProperty.of(PROP_HEALTH_PORT, nHealthPort2)))
                {

                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, HealthCheck.PATH_READY), is(200));
                Eventually.assertDeferred(() -> httpRequest(nHealthPort2, HealthCheck.PATH_READY), is(200));
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, HealthCheck.PATH_SAFE), is(200));
                Eventually.assertDeferred(() -> httpRequest(nHealthPort2, HealthCheck.PATH_SAFE), is(200));
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, PATH_HA), is(200));
                Eventually.assertDeferred(() -> httpRequest(nHealthPort2, PATH_HA), is(200));
                }
            }
        }

    @Test
    public void shouldBeStatusHAMultipleMemberWithBackupCountTwoIgnoringService()
        {
        LocalPlatform    platform     = LocalPlatform.get();
        Capture<Integer> nHealthPort1 = new Capture<>(platform.getAvailablePorts());
        Capture<Integer> nHealthPort2 = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember app1 = platform.launch(CoherenceClusterMember.class,
                                                    ClassName.of(Coherence.class),
                                                    CacheConfig.of("test-cache-config.xml"),
                                                    SystemProperty.of("coherence.distributed.backupcount", 2),
                                                    SystemProperty.of("coherence.config.backupcount", 1),
                                                    IPv4Preferred.yes(),
                                                    LocalHost.only(),
                                                    Logging.atMax(),
                                                    m_testLogs.builder(),
                                                    DisplayName.of("storage-0"),
                                                    SystemProperty.of("partitioned.cache.one.allow-endangered", true),
                                                    SystemProperty.of(PROP_HEALTH_PORT, nHealthPort1)))
            {
            try (CoherenceClusterMember app2 = platform.launch(CoherenceClusterMember.class,
                                                        ClassName.of(Coherence.class),
                                                        CacheConfig.of("test-cache-config.xml"),
                                                        IPv4Preferred.yes(),
                                                        LocalHost.only(),
                                                        Logging.atMax(),
                                                        m_testLogs.builder(),
                                                        DisplayName.of("storage-1"),
                                                        SystemProperty.of("partitioned.cache.one.allow-endangered", true),
                                                        SystemProperty.of("coherence.distributed.backupcount", 2),
                                                        SystemProperty.of("coherence.config.backupcount", 1),
                                                        SystemProperty.of(PROP_HEALTH_PORT, nHealthPort2)))
                {
                Eventually.assertDeferred(() -> isServiceOneRunning(app1), is(true));
                Eventually.assertDeferred(() -> isServiceOneRunning(app2), is(true));
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, PATH_HA), is(200));
                Eventually.assertDeferred(() -> httpRequest(nHealthPort2, PATH_HA), is(200));
                }
            }
        }

    @Test
    public void shouldSuspendAllServicesSingleMember()
        {
        LocalPlatform    platform     = LocalPlatform.get();
        Capture<Integer> httpPort = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember app = platform.launch(CoherenceClusterMember.class,
                                                   ClassName.of(Coherence.class),
                                                   CacheConfig.of("test-cache-config-two.xml"),
                                                   IPv4Preferred.yes(),
                                                   LocalHost.only(),
                                                   Logging.atMax(),
                                                   m_testLogs.builder(),
                                                   Logging.atMax(),
                                                   DisplayName.of("storage"),
                                                   SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                   SystemProperty.of("coherence.distributed.persistence-mode", "active"),
                                                   SystemProperty.of("coherence.distributed.persistence.base.dir", m_filePersistence),
                                                   SystemProperty.of(PROP_HEALTH_PORT, httpPort)))
            {
            Eventually.assertDeferred(() -> isServiceOneRunning(app), is(true));
            Eventually.assertDeferred(() -> isServiceTwoRunning(app), is(true));

            // wait for ready
            Eventually.assertDeferred(() -> httpRequest(httpPort, HealthCheck.PATH_READY), is(200));

            // suspend services
            Eventually.assertDeferred(() -> httpRequest(httpPort, PATH_SUSPEND), is(200));

            Eventually.assertDeferred(() -> isServiceOneSuspended(app), is(true));
            Eventually.assertDeferred(() -> isServiceTwoSuspended(app), is(true));

            Eventually.assertDeferred(() -> httpRequest(httpPort, PATH_RESUME), is(200));
            Eventually.assertDeferred(() -> isServiceOneSuspended(app), is(false));
            Eventually.assertDeferred(() -> isServiceTwoSuspended(app), is(false));
            }
        }

    @Test
    public void shouldSuspendAllServicesMultipleMembers()
        {
        LocalPlatform    platform     = LocalPlatform.get();
        Capture<Integer> nHealthPort1 = new Capture<>(platform.getAvailablePorts());
        Capture<Integer> nHealthPort2 = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember app1 = platform.launch(CoherenceClusterMember.class,
                                                    ClassName.of(Coherence.class),
                                                    CacheConfig.of("test-cache-config-two.xml"),
                                                    IPv4Preferred.yes(),
                                                    LocalHost.only(),
                                                    Logging.atMax(),
                                                    m_testLogs.builder(),
                                                    DisplayName.of("storage-0"),
                                                    SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                    SystemProperty.of("coherence.distributed.persistence-mode", "active"),
                                                    SystemProperty.of("coherence.distributed.persistence.base.dir", m_filePersistence),
                                                    SystemProperty.of(PROP_HEALTH_PORT, nHealthPort1)))
            {
            try (CoherenceClusterMember app2 = platform.launch(CoherenceClusterMember.class,
                                                        ClassName.of(Coherence.class),
                                                        CacheConfig.of("test-cache-config-two.xml"),
                                                        IPv4Preferred.yes(),
                                                        LocalHost.only(),
                                                        Logging.atMax(),
                                                        m_testLogs.builder(),
                                                        DisplayName.of("storage-1"),
                                                        SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                        SystemProperty.of("coherence.distributed.persistence-mode", "active"),
                                                        SystemProperty.of("coherence.distributed.persistence.base.dir", m_filePersistence),
                                                        SystemProperty.of(PROP_HEALTH_PORT, nHealthPort2)))
                {
                Eventually.assertDeferred(() -> isServiceOneRunning(app1), is(true));
                Eventually.assertDeferred(() -> isServiceOneRunning(app2), is(true));
                Eventually.assertDeferred(() -> isServiceTwoRunning(app1), is(true));
                Eventually.assertDeferred(() -> isServiceTwoRunning(app2), is(true));

                // wait for ready
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, HealthCheck.PATH_READY), is(200));
                // suspend services
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, PATH_SUSPEND), is(200));

                Eventually.assertDeferred(() -> isServiceOneSuspended(app1), is(true));
                Eventually.assertDeferred(() -> isServiceTwoSuspended(app1), is(true));
                Eventually.assertDeferred(() -> isServiceOneSuspended(app2), is(true));
                Eventually.assertDeferred(() -> isServiceTwoSuspended(app2), is(true));

                Eventually.assertDeferred(() -> httpRequest(nHealthPort2, PATH_RESUME), is(200));
                Eventually.assertDeferred(() -> isServiceOneSuspended(app1), is(false));
                Eventually.assertDeferred(() -> isServiceTwoSuspended(app1), is(false));
                Eventually.assertDeferred(() -> isServiceOneSuspended(app2), is(false));
                Eventually.assertDeferred(() -> isServiceTwoSuspended(app2), is(false));
                }
            }
        }

    @Test
    public void shouldSuspendSpecifiedServicesSingleMember()
        {
        LocalPlatform    platform     = LocalPlatform.get();
        Capture<Integer> httpPort = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember app = platform.launch(CoherenceClusterMember.class,
                                                   ClassName.of(Coherence.class),
                                                   CacheConfig.of("test-cache-config-two.xml"),
                                                   IPv4Preferred.yes(),
                                                   LocalHost.only(),
                                                   Logging.atMax(),
                                                   m_testLogs.builder(),
                                                   DisplayName.of("storage"),
                                                   SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                   SystemProperty.of("coherence.distributed.persistence-mode", "active"),
                                                   SystemProperty.of("coherence.distributed.persistence.base.dir", m_filePersistence),
                                                   SystemProperty.of(PROP_HEALTH_PORT, httpPort)))
            {
            Eventually.assertDeferred(() -> isServiceOneRunning(app), is(true));
            Eventually.assertDeferred(() -> isServiceTwoRunning(app), is(true));

            // wait for ready
            Eventually.assertDeferred(() -> httpRequest(httpPort, HealthCheck.PATH_READY), is(200));
            // suspend services
            String path = PATH_SUSPEND + "/PartitionedCacheOne";
            Eventually.assertDeferred(() -> httpRequest(httpPort, path), is(200));

            Eventually.assertDeferred(() -> isServiceOneSuspended(app), is(true));
            assertThat(isServiceTwoSuspended(app), is(false));

            Eventually.assertDeferred(() -> httpRequest(httpPort, PATH_RESUME), is(200));
            Eventually.assertDeferred(() -> isServiceOneSuspended(app), is(false));
            Eventually.assertDeferred(() -> isServiceTwoSuspended(app), is(false));
            }
        }

    @Test
    public void shouldResumeSpecifiedServicesSingleMember()
        {
        LocalPlatform    platform     = LocalPlatform.get();
        Capture<Integer> httpPort = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember app = platform.launch(CoherenceClusterMember.class,
                                                   ClassName.of(Coherence.class),
                                                   CacheConfig.of("test-cache-config-two.xml"),
                                                   IPv4Preferred.yes(),
                                                   LocalHost.only(),
                                                   Logging.atMax(),
                                                   m_testLogs.builder(),
                                                   DisplayName.of("storage"),
                                                   SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                   SystemProperty.of("coherence.distributed.persistence-mode", "active"),
                                                   SystemProperty.of("coherence.distributed.persistence.base.dir", m_filePersistence),
                                                   SystemProperty.of(PROP_HEALTH_PORT, httpPort)))
            {

            Eventually.assertDeferred(() -> isServiceOneRunning(app), is(true));
            Eventually.assertDeferred(() -> isServiceTwoRunning(app), is(true));

            // wait for ready
            Eventually.assertDeferred(() -> httpRequest(httpPort, HealthCheck.PATH_READY), is(200));
            // suspend services
            Eventually.assertDeferred(() -> httpRequest(httpPort, PATH_SUSPEND), is(200));

            Eventually.assertDeferred(() -> isServiceOneSuspended(app), is(true));
            Eventually.assertDeferred(() -> isServiceTwoSuspended(app), is(true));

            String path = PATH_RESUME + "/PartitionedCacheOne";
            Eventually.assertDeferred(() -> httpRequest(httpPort, path), is(200));
            Eventually.assertDeferred(() -> isServiceOneSuspended(app), is(false));
            assertThat(isServiceTwoSuspended(app), is(true));
            }
        }

    @Test
    public void shouldNotSuspendServicesWithDifferentRoles()
        {
        LocalPlatform    platform     = LocalPlatform.get();
        Capture<Integer> nHealthPort1 = new Capture<>(platform.getAvailablePorts());
        Capture<Integer> nHealthPort2 = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember app1 = platform.launch(CoherenceClusterMember.class,
                                                    ClassName.of(Coherence.class),
                                                    CacheConfig.of("test-cache-config-two.xml"),
                                                    IPv4Preferred.yes(),
                                                    LocalHost.only(),
                                                    Logging.atMax(),
                                                    m_testLogs.builder(),
                                                    DisplayName.of("storage-0"),
                                                    SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                    SystemProperty.of("coherence.distributed.persistence-mode", "active"),
                                                    SystemProperty.of("coherence.distributed.persistence.base.dir", m_filePersistence),
                                                    RoleName.of("foo"),
                                                    SystemProperty.of(PROP_HEALTH_PORT, nHealthPort1)))
            {
            try (CoherenceClusterMember app2 = platform.launch(CoherenceClusterMember.class,
                                                        ClassName.of(Coherence.class),
                                                        CacheConfig.of("test-cache-config-two.xml"),
                                                        IPv4Preferred.yes(),
                                                        LocalHost.only(),
                                                        Logging.atMax(),
                                                        m_testLogs.builder(),
                                                        DisplayName.of("storage-1"),
                                                        SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                        SystemProperty.of("coherence.distributed.persistence-mode", "active"),
                                                        SystemProperty.of("coherence.distributed.persistence.base.dir", m_filePersistence),
                                                        RoleName.of("bar"),
                                                        SystemProperty.of(PROP_HEALTH_PORT, nHealthPort2)))
                {
                Eventually.assertDeferred(() -> isServiceOneRunning(app1), is(true));
                Eventually.assertDeferred(() -> isServiceOneRunning(app2), is(true));
                Eventually.assertDeferred(() -> isServiceTwoRunning(app1), is(true));
                Eventually.assertDeferred(() -> isServiceTwoRunning(app2), is(true));

                // wait for ready
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, HealthCheck.PATH_READY), is(200));
                // suspend services
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, PATH_SUSPEND), is(200));

                assertThat(isServiceOneSuspended(app1), is(false));
                assertThat(isServiceTwoSuspended(app1), is(false));
                assertThat(isServiceOneSuspended(app2), is(false));
                assertThat(isServiceTwoSuspended(app2), is(false));
                }
            }
        }

    @Test
    public void shouldNotSuspendStorageDisabledServices()
        {
        LocalPlatform    platform     = LocalPlatform.get();
        Capture<Integer> nHealthPort1 = new Capture<>(platform.getAvailablePorts());
        Capture<Integer> nHealthPort2 = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember app1 = platform.launch(CoherenceClusterMember.class,
                                                    ClassName.of(Coherence.class),
                                                    CacheConfig.of("test-cache-config-two.xml"),
                                                    LocalStorage.disabled(),
                                                    IPv4Preferred.yes(),
                                                    LocalHost.only(),
                                                    Logging.atMax(),
                                                    m_testLogs.builder(),
                                                    DisplayName.of("server-0"),
                                                    SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                    SystemProperty.of("coherence.distributed.persistence-mode", "active"),
                                                    SystemProperty.of("coherence.distributed.persistence.base.dir", m_filePersistence),
                                                    SystemProperty.of(PROP_HEALTH_PORT, nHealthPort1)))
            {
            try (CoherenceClusterMember app2 = platform.launch(CoherenceClusterMember.class,
                                                        ClassName.of(Coherence.class),
                                                        CacheConfig.of("test-cache-config-two.xml"),
                                                        LocalStorage.disabled(),
                                                        IPv4Preferred.yes(),
                                                        LocalHost.only(),
                                                        Logging.atMax(),
                                                        m_testLogs.builder(),
                                                        DisplayName.of("server-1"),
                                                        SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                        SystemProperty.of("coherence.distributed.persistence-mode", "active"),
                                                        SystemProperty.of("coherence.distributed.persistence.base.dir", m_filePersistence),
                                                        SystemProperty.of(PROP_HEALTH_PORT, nHealthPort2)))
                {
                Eventually.assertDeferred(() -> isServiceOneRunning(app1), is(true));
                Eventually.assertDeferred(() -> isServiceOneRunning(app2), is(true));
                Eventually.assertDeferred(() -> isServiceTwoRunning(app1), is(true));
                Eventually.assertDeferred(() -> isServiceTwoRunning(app2), is(true));

                // wait for ready
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, HealthCheck.PATH_READY), is(200));
                // suspend services
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, PATH_SUSPEND), is(200));

                assertThat(isServiceOneSuspended(app1), is(false));
                assertThat(isServiceTwoSuspended(app1), is(false));
                assertThat(isServiceOneSuspended(app2), is(false));
                assertThat(isServiceTwoSuspended(app2), is(false));
                }
            }
        }

    @Test
    public void shouldNotSuspendNonPersistentServices()
        {
        LocalPlatform    platform     = LocalPlatform.get();
        Capture<Integer> nHealthPort1 = new Capture<>(platform.getAvailablePorts());
        Capture<Integer> nHealthPort2 = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember app1 = platform.launch(CoherenceClusterMember.class,
                                                    ClassName.of(Coherence.class),
                                                    CacheConfig.of("test-cache-config-two.xml"),
                                                    LocalStorage.enabled(),
                                                    IPv4Preferred.yes(),
                                                    LocalHost.only(),
                                                    Logging.atMax(),
                                                    m_testLogs.builder(),
                                                    DisplayName.of("server-0"),
                                                    SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                    SystemProperty.of(PROP_HEALTH_PORT, nHealthPort1)))
            {
            try (CoherenceClusterMember app2 = platform.launch(CoherenceClusterMember.class,
                                                        ClassName.of(Coherence.class),
                                                        CacheConfig.of("test-cache-config-two.xml"),
                                                        LocalStorage.enabled(),
                                                        IPv4Preferred.yes(),
                                                        LocalHost.only(),
                                                        Logging.atMax(),
                                                        m_testLogs.builder(),
                                                        DisplayName.of("server-1"),
                                                        SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                        SystemProperty.of(PROP_HEALTH_PORT, nHealthPort2)))
                {
                Eventually.assertDeferred(() -> isServiceOneRunning(app1), is(true));
                Eventually.assertDeferred(() -> isServiceOneRunning(app2), is(true));
                Eventually.assertDeferred(() -> isServiceTwoRunning(app1), is(true));
                Eventually.assertDeferred(() -> isServiceTwoRunning(app2), is(true));

                // wait for ready
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, HealthCheck.PATH_READY), is(200));
                // suspend services
                Eventually.assertDeferred(() -> httpRequest(nHealthPort1, PATH_SUSPEND), is(200));

                assertThat(isServiceOneSuspended(app1), is(false));
                assertThat(isServiceTwoSuspended(app1), is(false));
                assertThat(isServiceOneSuspended(app2), is(false));
                assertThat(isServiceTwoSuspended(app2), is(false));
                }
            }
        }

    @Test
    public void shouldNotResumeExcludedServices()
        {
        LocalPlatform    platform   = LocalPlatform.get();
        Capture<Integer> nHealthPort = new Capture<>(platform.getAvailablePorts());

        try (CoherenceClusterMember app = platform.launch(CoherenceClusterMember.class,
                                                   ClassName.of(Coherence.class),
                                                   CacheConfig.of("test-cache-config-two.xml"),
                                                   IPv4Preferred.yes(),
                                                   LocalHost.only(),
                                                   m_testLogs.builder(),
                                                   Logging.atMax(),
                                                   DisplayName.of("storage"),
                                                   SystemProperty.of("coherence.distributed.partitioncount", "13"),
                                                   SystemProperty.of("coherence.distributed.persistence-mode", "active"),
                                                   SystemProperty.of("coherence.distributed.persistence.base.dir", m_filePersistence),
                                                   SystemProperty.of(PROP_HEALTH_PORT, nHealthPort)))
            {
            Eventually.assertDeferred(() -> isServiceOneRunning(app), is(true));
            Eventually.assertDeferred(() -> isServiceTwoRunning(app), is(true));

            // wait for ready
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, HealthCheck.PATH_READY), is(200));

            // suspend services
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, PATH_SUSPEND), is(200));

            Eventually.assertDeferred(() -> isServiceOneSuspended(app), is(true));
            Eventually.assertDeferred(() -> isServiceTwoSuspended(app), is(true));

            String sRequestOne = PATH_RESUME + "?exclude=PartitionedCacheOne,PartitionedCacheTwo";
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, sRequestOne), is(200));
            assertThat(isServiceOneSuspended(app), is(true));
            assertThat(isServiceTwoSuspended(app), is(true));

            String sRequestTwo = PATH_RESUME + "?exclude=%20PartitionedCacheTwo%20";
            Eventually.assertDeferred(() -> httpRequest(nHealthPort, sRequestTwo), is(200));
            Eventually.assertDeferred(() -> isServiceOneSuspended(app), is(false));
            assertThat(isServiceTwoSuspended(app), is(true));
            }
        }

    // ----- helper methods -------------------------------------------------

    public int httpRequest(Capture<Integer> nPort, String sRequest)
        {
        try
            {
            if (!sRequest.startsWith("/"))
                {
                sRequest = "/" + sRequest;
                }

            HttpRequest request = HttpRequest.newBuilder()
                  .GET()
                  .uri(URI.create("http://127.0.0.1:" + nPort.get() + sRequest))
                  .build();

            HttpResponse<byte[]> response = m_client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            return response.statusCode();
            }
        catch (ConnectException e)
            {
            return -1;
            }
        catch (Exception e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    private boolean isServiceOneSuspended(CoherenceClusterMember app)
        {
        return isServiceSuspended(app, "PartitionedCacheOne");
        }

    private boolean isServiceTwoSuspended(CoherenceClusterMember app)
        {
        return isServiceSuspended(app, "PartitionedCacheTwo");
        }

    private boolean isServiceSuspended(CoherenceClusterMember app, String sName)
        {
        try
            {
            return app.submit(new IsServiceSuspended(sName)).get();
            }
        catch (Exception e)
            {
            System.err.println("ERROR: isServiceSuspended failed: " + e.getMessage());
            return false;
            }
        }

    private boolean isServiceOneRunning(CoherenceClusterMember app)
        {
        return isServiceRunning(app, "PartitionedCacheOne");
        }

    private boolean isServiceTwoRunning(CoherenceClusterMember app)
        {
        return isServiceRunning(app, "PartitionedCacheTwo");
        }

    private boolean isServiceRunning(CoherenceClusterMember app, String sName)
        {
        try
            {
            return app.submit(new IsServiceRunning(sName)).get();
            }
        catch (Exception e)
            {
            System.err.println("ERROR: isServiceRunning failed: " + e.getMessage());
            return false;
            }
        }

    // ----- constants ------------------------------------------------------

    private static final String PROP_HEALTH_PORT = "coherence.health.http.port";

    private static final String PATH_HA = "/ha";
    private static final String PATH_SUSPEND = "/suspend";
    private static final String PATH_RESUME = "/resume";

    // ----- data members ---------------------------------------------------

    private static File m_filePersistence;

    @RegisterExtension
    static TestLogsExtension m_testLogs = new TestLogsExtension();

    private final HttpClient m_client = HttpClient.newHttpClient();
    }
