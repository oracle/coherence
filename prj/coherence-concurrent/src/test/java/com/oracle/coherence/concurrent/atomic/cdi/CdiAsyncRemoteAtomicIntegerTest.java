/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicInteger;
import com.oracle.coherence.concurrent.atomic.AsyncAtomicIntegerTest;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicInteger;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.SessionName;

import com.tangosol.net.NamedMap;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link AsyncRemoteAtomicInteger} CDI tests.
 *
 * @author Aleks Seovic  2020.12.07
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CdiAsyncRemoteAtomicIntegerTest
        extends AsyncAtomicIntegerTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicIntegerProducer.class));

    @Inject
    @Remote
    AsyncAtomicInteger value;

    @Inject
    @SessionName("coherence-concurrent-services")
    @Name("atomic-int")
    NamedMap<String, AtomicInteger> ints;

    @Test
    @Order(100)
    void testRemoteEntry()
        {
        assertThat(ints.get("value").get(), is(1));
        }

    protected AsyncAtomicInteger asyncValue()
        {
        return value;
        }
    }
