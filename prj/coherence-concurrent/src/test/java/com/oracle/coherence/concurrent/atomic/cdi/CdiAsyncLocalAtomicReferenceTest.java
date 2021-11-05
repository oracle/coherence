/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic.cdi;

import com.oracle.coherence.concurrent.atomic.AsyncAtomicReference;
import com.oracle.coherence.concurrent.atomic.AsyncAtomicReferenceTest;
import com.oracle.coherence.concurrent.atomic.AsyncLocalAtomicReference;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@link AsyncLocalAtomicReference} CDI tests.
 * 
 * @author Aleks Seovic  2020.12.08
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CdiAsyncLocalAtomicReferenceTest
        extends AsyncAtomicReferenceTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addBeanClass(AsyncAtomicReferenceProducer.class));

    @Inject
    AsyncAtomicReference<String> value;

    protected AsyncAtomicReference<String> asyncValue()
        {
        return value;
        }
    }
