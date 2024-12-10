/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.executor.internal.cdi;

import com.oracle.coherence.cdi.CoherenceProducer;
import com.oracle.coherence.cdi.Name;

import com.tangosol.net.Coherence;

import javax.annotation.Priority;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;

import javax.inject.Singleton;

/**
 * A bean that produces the server-side Coherence instance.
 *
 * @author Jonathan Knight  2020.12.10
 */
@ApplicationScoped
@Alternative
@Priority(1)
public class TestServerCoherenceProducer
        extends CoherenceProducer
    {
    @Produces
    @Singleton
    @Name(Coherence.DEFAULT_NAME)
    public Coherence createCoherence(BeanManager beanManager)
        {
        // build and start the Coherence instance
        System.setProperty("coherence.cacheconfig", "executor.xml");
        Coherence coherence = Coherence.clusterMember(createConfiguration(beanManager));

        // wait for start-up to complete
        coherence.start().join();

        return coherence;
        }
    }
