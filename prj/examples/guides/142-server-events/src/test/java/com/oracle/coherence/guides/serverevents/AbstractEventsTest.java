/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.serverevents;

import com.oracle.bedrock.OptionsByType;

import com.oracle.bedrock.runtime.LocalPlatform;

import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.JMXManagementMode;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.ClusterPort;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.MachineName;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;

import com.oracle.bedrock.runtime.java.profiles.JmxProfile;

import com.oracle.bedrock.runtime.network.AvailablePortIterator;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.guides.serverevents.model.AuditEvent;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.NamedCache;
import com.tangosol.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.Comparator;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.coherence.guides.serverevents.interceptors.AbstractAuditingInterceptor.AUDIT_CACHE;
import static org.hamcrest.CoreMatchers.is;


/**
 * Base test class for server events.
 *
 * @author Tim Middleton 2022.05.04
 */
public abstract class AbstractEventsTest {

    private static final String SERVICE_NAME1 = "DistributedCache";
    private static final String SERVICE_NAME2 = "DistributedCacheAudit";
    private static final String CACHE_CONFIG  = "cache-config-events.xml";

    protected static AvailablePortIterator availablePortIteratorWKA;
    protected static CoherenceCacheServer  member1 = null;
    protected static CoherenceCacheServer  member2 = null;
    protected static NamedCache<UUID, AuditEvent> auditEvents;

    public static void _startup(String testName) {
        LocalPlatform platform = LocalPlatform.get();
        availablePortIteratorWKA = platform.getAvailablePorts();
        availablePortIteratorWKA.next();

        int clusterPort = availablePortIteratorWKA.next();

        OptionsByType member1Options = createCacheServerOptions(clusterPort, testName, 1);
        OptionsByType member2Options = createCacheServerOptions(clusterPort, testName, 2);

        member1 = platform.launch(CoherenceCacheServer.class, member1Options.asArray());
        member2 = platform.launch(CoherenceCacheServer.class, member2Options.asArray());

        Eventually.assertDeferred(() -> member1.getServiceStatus(SERVICE_NAME1), is(ServiceStatus.MACHINE_SAFE));
        Eventually.assertDeferred(() -> member1.getServiceStatus(SERVICE_NAME2), is(ServiceStatus.MACHINE_SAFE));

        auditEvents = getMember1().getCache(AUDIT_CACHE);
    }

    protected static CoherenceClusterMember getMember1() {
        return member1;
    }

    protected static CoherenceClusterMember getMember2() {
        return member2;
    }

    @BeforeEach
    public void resetAuditCache() {
        auditEvents.clear();
    }


    @AfterAll
    public static void _shutdown() {
        CacheFactory.shutdown();
        destroyMember(member1);
        destroyMember(member2);
    }

    protected static OptionsByType createCacheServerOptions(int clusterPort, String testName, int memberId) {
        String        hostName      = LocalPlatform.get().getLoopbackAddress().getHostAddress();
        OptionsByType optionsByType = OptionsByType.empty();

        optionsByType.addAll(JMXManagementMode.ALL,
                JmxProfile.enabled(),
                LocalStorage.enabled(),
                WellKnownAddress.of(hostName),
                Multicast.ttl(0),
                CacheConfig.of(CACHE_CONFIG),
                Logging.at(2),
                MachineName.of(testName + "-" + memberId),
                ClusterName.of("server-events"),
                ClusterPort.of(clusterPort));

        return optionsByType;
    }

    private static void destroyMember(CoherenceClusterMember member) {
        try {
            if (member != null) {
                member.close();
            }
        }
        catch (Throwable thrown) {
            // ignored
        }
    }

    protected void dumpAuditEvents(String message) {
        System.out.printf("Dumping the audit events %s\n\n", message);
        auditEvents.values()
                   .stream()
                   .sorted(Comparator.comparing(AuditEvent::getEventTime))
                   .forEach(System.out::println);
        System.out.println("\n");
    }
}
