/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicReference;
import com.oracle.coherence.concurrent.atomic.internal.cdi.AsyncAtomicReferenceProducer;
import concurrent.atomic.AsyncAtomicReferenceTest;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.SessionName;

import com.tangosol.net.NamedMap;

import java.util.concurrent.atomic.AtomicReference;

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
 * {@link AsyncRemoteAtomicReference} CDI tests.
 * 
 * @author Aleks Seovic  2020.12.08
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CdiAsyncRemoteAtomicReferenceTest
        extends AsyncAtomicReferenceTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicReferenceProducer.class));

    @Inject
    @Remote
    AsyncAtomicReference<String> value;

    @Inject
    @SessionName("concurrent")
    @Name("atomic-ref")
    NamedMap<String, AtomicReference<String>> refs;

    @Test
    @Order(100)
    void testRemoteEntry()
        {
        assertThat(refs.get("value").get(), is("ATOMIC"));
        }

    protected AsyncAtomicReference<String> asyncValue()
        {
        return value;
        }
    }
