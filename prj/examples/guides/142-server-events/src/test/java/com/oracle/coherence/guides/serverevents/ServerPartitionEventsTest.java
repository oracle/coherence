/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.serverevents;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.NamedCache;

import org.hamcrest.Matchers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests to demonstrate various uses of partition events.
 *
 * @author Tim Middleton 2022.05.04
 */
public class ServerPartitionEventsTest
        extends AbstractEventsTest {

    @BeforeAll
    public static void startup() {
        _startup("partition-events");
    }

    // #tag::test1[]
    @Test
    public void testPartitionEvents() {
        System.out.println("testPartitionEvents");
        CoherenceClusterMember member1 = getMember1();
        CoherenceClusterMember member2 = getMember2();

        NamedCache<Integer, String> cache = member1.getCache("test-cache");

        for (int i = 0; i < 10; i++) {
            cache.put(i, "value-" + i);
        }

        // ensure all audit events are received = 10 insert events plus 2 cache created events
        Eventually.assertDeferred(()->auditEvents.size(), Matchers.is(12));
        
        // shutdown the second member
        member2.close();

        // wait for additional partition events to be received
        Eventually.assertDeferred(() -> auditEvents.size(), Matchers.greaterThan(16));

        dumpAuditEvents("testPartitionEvents");
    }
    // #end::test1[]
}
