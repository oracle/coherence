/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cdi;

import com.oracle.coherence.cdi.CoherenceExtension;
import com.oracle.coherence.cdi.CoherenceProducer;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SessionInitializer;
import com.tangosol.net.Coherence;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(WeldJunit5Extension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CoherenceClientIT
    {
    @WeldSetup
    private final WeldInitiator weld = WeldInitiator.of(WeldInitiator.createWeld()
                                                        .addPackages(CoherenceExtension.class)
                                                        .addExtension(new CoherenceExtension())
                                                        .addBeanClass(ClientSessionBean.class)
                                                        .addBeanClass(ServerSessionBean.class)
                                                        .addBeanClass(CoherenceProducer.class));

    @Inject
    BeanManager m_beanManager;

    @Test
    void shouldBeClientByDefault()
        {
        Coherence coherence = CoherenceExtension.ensureCoherence(m_beanManager);
        assertThat(coherence.getMode(), is(Coherence.Mode.Client));

        assertThat(coherence.hasSession("ClientSession"), is(true));
        assertThat(coherence.hasSession("ServerSession"), is(true));
        }

    @ApplicationScoped
    @Named("ClientSession")
    @Scope("Client")
    public static class ClientSessionBean
            implements SessionInitializer
        {
        }

    @ApplicationScoped
    @Named("ServerSession")
    @Scope("Server")
    public static class ServerSessionBean
            implements SessionInitializer
        {
        }
    }
