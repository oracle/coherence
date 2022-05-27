/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package health;

import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.management.Registry;
import com.tangosol.util.HealthCheck;
import com.tangosol.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HealthCheckRegistrationTests
    {
    @BeforeAll
    static void setup() throws Exception
        {
        System.setProperty(ClusterName.PROPERTY, "HealthCheckRegistrationTests");
        System.setProperty(Logging.PROPERTY_LEVEL, "9");

        CoherenceConfiguration config = CoherenceConfiguration.builder()
            .withSession(SessionConfiguration.defaultSession())
            .withSession(SessionConfiguration.builder()
                .named("Test")
                .withScopeName("Test")
                .withConfigUri("test-cache-config.xml")
                .build())
            .build();

        s_coherence = Coherence.clusterMember(config)
                .start()
                .get(5, TimeUnit.MINUTES);
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

        Eventually.assertDeferred(registry::allHealthChecksReady, is(true));
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
