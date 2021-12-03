/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.internal.cdi;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.concurrent.executor.RemoteExecutor;

import javax.enterprise.context.ApplicationScoped;

import javax.inject.Inject;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link RemoteExecutorProducer}.
 *
 * @author Vaso Putica  2021.12.01
 */
@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RemoteExecutorProducerTest
    {
    @WeldSetup
    private WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                          .addExtension(new CoherenceExtension())
                                                          .addPackages(CoherenceExtension.class)
                                                          .addBeanClass(TestServerCoherenceProducer.class)
                                                          .addBeanClass(ExecutorBean.class)
                                                          .addBeanClass(RemoteExecutorProducer.class));

    @Inject
    ExecutorBean executorBean;

    @Test
    void testDistributedInjection()
        {
        RemoteExecutor singleThreadExecutor = executorBean.getSingleThreadExecutor();
        RemoteExecutor fixedPoolExecutor = executorBean.getFixedPoolRemoteExecutor();
        assertThat(singleThreadExecutor, notNullValue());
        assertThat(fixedPoolExecutor, notNullValue());
        }

    // ----- test bean ------------------------------------------------------

    @ApplicationScoped
    static class ExecutorBean
        {
        @Inject
        private RemoteExecutor singleThreadExecutor;

        @Inject
        @Name("fixedPoolExecutor")
        private RemoteExecutor fixedPoolRemoteExecutor;

        public RemoteExecutor getSingleThreadExecutor()
            {
            return singleThreadExecutor;
            }

        public void setSingleThreadExecutor(RemoteExecutor singleThreadExecutor)
            {
            this.singleThreadExecutor = singleThreadExecutor;
            }

        public RemoteExecutor getFixedPoolRemoteExecutor()
            {
            return fixedPoolRemoteExecutor;
            }

        public void setFixedPoolRemoteExecutor(RemoteExecutor fixedPoolRemoteExecutor)
            {
            this.fixedPoolRemoteExecutor = fixedPoolRemoteExecutor;
            }
        }
    }

