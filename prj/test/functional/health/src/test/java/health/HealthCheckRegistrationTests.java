/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package health;

import com.oracle.coherence.functional.health.WrapperHealthCheck;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.management.Registry;
import com.tangosol.util.HealthCheck;
import com.tangosol.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

public class HealthCheckRegistrationTests
    {
    @BeforeAll
    static void setup() throws Exception
        {
        CoherenceConfiguration config = CoherenceConfiguration.builder()
            .withSession(SessionConfiguration.defaultSession())
            .withSession(SessionConfiguration.builder()
                .named("Test")
                .withScopeName("Test")
                .withConfigUri("test-cache-config-two.xml")
                .build())
            .build();

        s_coherence = Coherence.clusterMember(config)
                .start()
                .get(5, TimeUnit.MINUTES);
        }

    @BeforeEach
    void resetWrappedHealthCheck()
        {
        WrapperHealthCheck.setDelegate(null);
        }

    @Test
    public void shouldDiscoverHealthCheck()
        {
        HealthCheckStub stub = new HealthCheckStub("Test");
        // The WrapperHealthCheck will be discovered using the ServiceLoader and registered
        // as a health check. It wraps the stub created above.
        WrapperHealthCheck.setDelegate(stub);

        Registry                registry       = s_coherence.getCluster().getManagement();
        Collection<HealthCheck> colHealthCheck = registry.getHealthChecks();

        System.err.println("HealthChecks:");
        colHealthCheck.forEach(h -> System.err.println(h.getName() + " " + h.getClass()));

        List<HealthCheck> list = colHealthCheck.stream()
                .filter(h -> WrapperHealthCheck.NAME.equals(h.getName()))
                .collect(Collectors.toList());

        assertThat(list.size(), is(1));

        HealthCheck           healthCheck = list.get(0);
        Optional<HealthCheck> optional    = registry.getHealthCheck(WrapperHealthCheck.NAME);

        assertThat(optional.isPresent(), is(true));

        HealthCheck healthCheckByName = optional.get();
        assertThat(healthCheckByName,  is(sameInstance(healthCheck)));

        assertThat(registry.allHealthChecksReady(), is(true));
        assertThat(healthCheck.isReady(), is(true));
        stub.setReady(false);
        assertThat(registry.allHealthChecksReady(), is(false));
        assertThat(healthCheck.isReady(), is(false));
        stub.setReady(true);
        assertThat(registry.allHealthChecksReady(), is(true));
        assertThat(healthCheck.isReady(), is(true));

        assertThat(registry.allHealthChecksLive(), is(true));
        assertThat(healthCheck.isLive(), is(true));
        stub.setLive(false);
        assertThat(registry.allHealthChecksLive(), is(false));
        assertThat(healthCheck.isLive(), is(false));
        stub.setLive(true);
        assertThat(registry.allHealthChecksLive(), is(true));
        assertThat(healthCheck.isLive(), is(true));

        assertThat(registry.allHealthChecksStarted(), is(true));
        assertThat(healthCheck.isStarted(), is(true));
        stub.setStarted(false);
        assertThat(registry.allHealthChecksStarted(), is(false));
        assertThat(healthCheck.isStarted(), is(false));
        stub.setStarted(true);
        assertThat(registry.allHealthChecksStarted(), is(true));
        assertThat(healthCheck.isStarted(), is(true));

        assertThat(registry.allHealthChecksSafe(), is(true));
        assertThat(healthCheck.isSafe(), is(true));
        stub.setSafe(false);
        assertThat(registry.allHealthChecksSafe(), is(false));
        assertThat(healthCheck.isSafe(), is(false));
        stub.setSafe(true);
        assertThat(registry.allHealthChecksSafe(), is(true));
        assertThat(healthCheck.isSafe(), is(true));
        }

    @Test
    public void shouldRegisterHealthCheck()
        {
        String          sName    = "Dummy-" + new UUID();
        HealthCheckStub stub     = new HealthCheckStub(sName);
        Registry        registry = s_coherence.getCluster().getManagement();

        Set<String> setName = registry.getHealthChecks().stream()
                .map(HealthCheck::getName)
                .collect(Collectors.toSet());

        assertThat(setName.contains(sName), is(false));

        registry.register(stub);

        setName = registry.getHealthChecks().stream()
                .map(HealthCheck::getName)
                .collect(Collectors.toSet());

        assertThat(setName.contains(sName), is(true));

        assertThat(registry.allHealthChecksReady(), is(true));
        stub.setReady(false);
        assertThat(registry.allHealthChecksReady(), is(false));
        stub.setReady(true);
        assertThat(registry.allHealthChecksReady(), is(true));

        assertThat(registry.allHealthChecksLive(), is(true));
        stub.setLive(false);
        assertThat(registry.allHealthChecksLive(), is(false));
        stub.setLive(true);
        assertThat(registry.allHealthChecksLive(), is(true));

        assertThat(registry.allHealthChecksStarted(), is(true));
        stub.setStarted(false);
        assertThat(registry.allHealthChecksStarted(), is(false));
        stub.setStarted(true);
        assertThat(registry.allHealthChecksStarted(), is(true));

        assertThat(registry.allHealthChecksSafe(), is(true));
        stub.setSafe(false);
        assertThat(registry.allHealthChecksSafe(), is(false));
        stub.setSafe(true);
        assertThat(registry.allHealthChecksSafe(), is(true));

        registry.unregister(stub);

        setName = registry.getHealthChecks().stream()
                .map(HealthCheck::getName)
                .collect(Collectors.toSet());

        assertThat(setName.contains(sName), is(false));

        stub.setSafe(false);
        assertThat(registry.allHealthChecksSafe(), is(true));
        }

    // ----- HealthCheckStub ------------------------------------------------

    /**
     * A test {@link HealthCheck} implementation.
     */
    public static class HealthCheckStub
            implements HealthCheck
        {
        public HealthCheckStub(String sName)
            {
            f_sName = sName;
            }

        @Override
        public String getName()
            {
            return f_sName;
            }

        @Override
        public boolean isReady()
            {
            return m_fReady;
            }

        public void setReady(boolean fReady)
            {
            m_fReady = fReady;
            }

        @Override
        public boolean isLive()
            {
            return m_fLive;
            }

        public void setLive(boolean fLive)
            {
            m_fLive = fLive;
            }

        @Override
        public boolean isStarted()
            {
            return m_fStarted;
            }

        public void setStarted(boolean fStarted)
            {
            m_fStarted = fStarted;
            }

        @Override
        public boolean isSafe()
            {
            return m_fSafe;
            }

        public void setSafe(boolean fSafe)
            {
            m_fSafe = fSafe;
            }

        // ----- data members -----------------------------------------------

        private final String f_sName;

        private boolean m_fReady = true;
        private boolean m_fLive = true;
        private boolean m_fStarted = true;
        private boolean m_fSafe = true;
        }

    // ----- data members ---------------------------------------------------

    private static Coherence s_coherence;
    }
