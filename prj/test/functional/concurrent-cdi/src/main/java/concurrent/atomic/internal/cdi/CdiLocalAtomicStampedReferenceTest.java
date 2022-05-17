/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.atomic.internal.cdi;

import com.oracle.coherence.concurrent.atomic.AtomicStampedReference;
import com.oracle.coherence.concurrent.atomic.internal.cdi.AtomicStampedReferenceProducer;
import concurrent.atomic.AtomicStampedReferenceTest;
import com.oracle.coherence.concurrent.atomic.LocalAtomicStampedReference;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@link LocalAtomicStampedReference} CDI tests.
 * 
 * @author Aleks Seovic  2020.12.09
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CdiLocalAtomicStampedReferenceTest
        extends AtomicStampedReferenceTest
    {
    @WeldSetup
    @SuppressWarnings("unused")
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addBeanClass(AtomicStampedReferenceProducer.class));

    @Inject
    AtomicStampedReference<String> value;

    protected AtomicStampedReference<String> value()
        {
        return value;
        }
    }
