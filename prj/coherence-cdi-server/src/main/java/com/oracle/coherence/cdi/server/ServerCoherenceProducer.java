/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.CoherenceProducer;

import com.oracle.coherence.cdi.Name;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;

import javax.annotation.Priority;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;

import javax.inject.Named;
import javax.inject.Singleton;

/**
 * A CDI server-side {@link Coherence} bean producer.
 * <p>
 * This bean is a higher priority alternative to the {@link CoherenceProducer}
 * bean in the CDI common module.
 *
 * @author Jonathan Knight  2020.12.15
 * @since 20.12
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class ServerCoherenceProducer
        extends CoherenceProducer
    {
    /**
     * Create the default {@link Coherence} bean.
     * <p>
     * By default the {@link Coherence} bean will be run in {@link Coherence.Mode#ClusterMember} mode.
     * <p>
     * The configuration can be changed by supplying a {@link CoherenceConfiguration.Builder}
     * bean annotated with {@literal @}{@link Named} with a name {@link Coherence#DEFAULT_NAME}.
     * This configuration bean will then be used to provide the configuration for the
     * {@link Coherence} bean.
     *
     * @param beanManager  the CDI bean manager
     *
     * @return the default {@link Coherence} bean
     */
    @Produces
    @Singleton
    @Name(Coherence.DEFAULT_NAME)
    public Coherence createCoherence(BeanManager beanManager)
        {
        // build and start the Coherence instance
        Coherence coherence = Coherence.clusterMember(createConfiguration(beanManager));

        // wait for start-up to complete
        coherence.start().join();

        return coherence;
        }
    }
