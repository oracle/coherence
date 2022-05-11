/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cdi;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.CoherenceProducer;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.SessionConfiguration;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoherenceServerIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                        .addPackages(CoherenceExtension.class)
                                                        .addExtension(new CoherenceExtension())
                                                        .addBeanClass(TestServerCoherenceProducer.class)
                                                        .addBeanClass(CoherenceProducer.class)
                                                        .addBeanClass(ConfigProducer.class));

    @Inject
    BeanManager m_beanManager;

    @Test
    void shouldBeServer()
        {
        Coherence coherence = CoherenceExtension.ensureCoherence(m_beanManager);
        assertThat(coherence.getMode(), is(Coherence.Mode.ClusterMember));
        }

    @ApplicationScoped
    public static class ConfigProducer
        {
        @Produces
        @Named(Coherence.DEFAULT_NAME)
        CoherenceConfiguration.Builder createConfiguration()
            {
            return CoherenceConfiguration.builder()
                    .named(Coherence.DEFAULT_NAME)
                    .withSession(SessionConfiguration.defaultSession());
            }
        }
    }
