/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;

import com.oracle.coherence.cdi.server.CoherenceServerExtension;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.internal.cdi.AsyncAtomicStampedReferenceProducer;
import concurrent.atomic.AsyncAtomicStampedReferenceTest;
import com.oracle.coherence.concurrent.atomic.AsyncRemoteAtomicStampedReference;

import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.Remote;
import com.oracle.coherence.cdi.SessionName;

import com.tangosol.net.NamedMap;

import java.util.concurrent.atomic.AtomicStampedReference;

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
 * {@link AsyncRemoteAtomicStampedReference} CDI tests.
 * 
 * @author Aleks Seovic  2020.12.09
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CdiAsyncRemoteAtomicStampedReferenceTest
        extends AsyncAtomicStampedReferenceTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addPackages(CoherenceServerExtension.class)
                                                          .addBeanClass(AsyncAtomicStampedReferenceProducer.class));

    @Inject
    @Remote
    AsyncAtomicStampedReference<String> value;

    @Inject
    @SessionName("concurrent")
    @Name("atomic-stamped-ref")
    NamedMap<String, AtomicStampedReference<String>> refs;

    @Test
    @Order(100)
    void testRemoteEntry()
        {
        int[] aStamp = new int[1];
        assertThat(refs.get("value").get(aStamp), is("atomic"));
        assertThat(aStamp[0], is(3));
        }

    protected AsyncAtomicStampedReference<String> asyncValue()
        {
        return value;
        }
    }
