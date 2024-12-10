/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package coherence.mp.health.testing;

import com.oracle.coherence.mp.health.CoherenceHealthChecks;
import com.tangosol.net.Coherence;
import com.tangosol.net.management.Registry;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CoherenceHealthChecksTest
    {
    @Test
    public void shouldNotBeReadyIfNullCoherenceInstances()
        {
        Supplier<Collection<Coherence>> supplier    = () -> null;
        CoherenceHealthChecks           checks      = new CoherenceHealthChecks(supplier);
        HealthCheck                     healthCheck = checks.readiness();
        assertDown(healthCheck, CoherenceHealthChecks.HEALTH_CHECK_READINESS);
        }

    @Test
    public void shouldNotBeReadyIfEmptyCoherenceInstances()
        {
        Supplier<Collection<Coherence>> supplier    = Collections::emptyList;
        CoherenceHealthChecks           checks      = new CoherenceHealthChecks(supplier);
        HealthCheck                     healthCheck = checks.readiness();
        assertDown(healthCheck, CoherenceHealthChecks.HEALTH_CHECK_READINESS);
        }

    @Test
    public void shouldNotBeReadyIfNoCoherenceHealthChecks()
        {
        Coherence coherence  = mock(Coherence.class);
        Registry  management = mock(Registry.class);

        when(coherence.getManagement()).thenReturn(management);
        when(management.getHealthChecks()).thenReturn(Collections.emptyList());

        Supplier<Collection<Coherence>> supplier              = () -> Collections.singleton(coherence);
        CoherenceHealthChecks           coherenceHealthChecks = new CoherenceHealthChecks(supplier);
        HealthCheck                     healthCheck           = coherenceHealthChecks.readiness();

        assertDown(healthCheck, CoherenceHealthChecks.HEALTH_CHECK_READINESS);
        }

    @Test
    public void shouldBeReady()
        {
        Coherence                     coherence  = mock(Coherence.class);
        Registry                      management = mock(Registry.class);
        com.tangosol.util.HealthCheck checkOne   = mock(com.tangosol.util.HealthCheck.class);
        com.tangosol.util.HealthCheck checkTwo   = mock(com.tangosol.util.HealthCheck.class);

        Collection<com.tangosol.util.HealthCheck> checks = List.of(checkOne, checkTwo);

        when(checkOne.getName()).thenReturn("One");
        when(checkOne.isReady()).thenReturn(true);
        when(checkTwo.getName()).thenReturn("Two");
        when(checkTwo.isReady()).thenReturn(true);
        when(coherence.getManagement()).thenReturn(management);
        when(management.getHealthChecks()).thenReturn(checks);

        Supplier<Collection<Coherence>> supplier              = () -> Collections.singleton(coherence);
        CoherenceHealthChecks           coherenceHealthChecks = new CoherenceHealthChecks(supplier);
        HealthCheck                     healthCheck           = coherenceHealthChecks.readiness();

        assertUp(healthCheck, CoherenceHealthChecks.HEALTH_CHECK_READINESS);
        }

    @Test
    public void shouldNotBeReadyIfCoherenceHealthCheckDown()
        {
        Coherence                     coherence  = mock(Coherence.class);
        Registry                      management = mock(Registry.class);
        com.tangosol.util.HealthCheck checkOne   = mock(com.tangosol.util.HealthCheck.class);
        com.tangosol.util.HealthCheck checkTwo   = mock(com.tangosol.util.HealthCheck.class);

        Collection<com.tangosol.util.HealthCheck> checks = List.of(checkOne, checkTwo);

        when(checkOne.getName()).thenReturn("One");
        when(checkOne.isReady()).thenReturn(true);
        when(checkTwo.getName()).thenReturn("Two");
        when(checkTwo.isReady()).thenReturn(false);
        when(coherence.getManagement()).thenReturn(management);
        when(management.getHealthChecks()).thenReturn(checks);

        Supplier<Collection<Coherence>> supplier              = () -> Collections.singleton(coherence);
        CoherenceHealthChecks           coherenceHealthChecks = new CoherenceHealthChecks(supplier);
        HealthCheck                     healthCheck           = coherenceHealthChecks.readiness();

        assertDown(healthCheck, CoherenceHealthChecks.HEALTH_CHECK_READINESS);
        }

    @Test
    public void shouldNotBeLiveIfNullCoherenceInstances()
        {
        Supplier<Collection<Coherence>> supplier    = () -> null;
        CoherenceHealthChecks           checks      = new CoherenceHealthChecks(supplier);
        HealthCheck                     healthCheck = checks.liveness();
        assertDown(healthCheck, CoherenceHealthChecks.HEALTH_CHECK_LIVENESS);
        }

    @Test
    public void shouldNotBeLiveIfEmptyCoherenceInstances()
        {
        Supplier<Collection<Coherence>> supplier    = Collections::emptyList;
        CoherenceHealthChecks           checks      = new CoherenceHealthChecks(supplier);
        HealthCheck                     healthCheck = checks.liveness();
        assertDown(healthCheck, CoherenceHealthChecks.HEALTH_CHECK_LIVENESS);
        }

    @Test
    public void shouldNotBeLiveIfNoCoherenceHealthChecks()
        {
        Coherence coherence  = mock(Coherence.class);
        Registry  management = mock(Registry.class);

        when(coherence.getManagement()).thenReturn(management);
        when(management.getHealthChecks()).thenReturn(Collections.emptyList());

        Supplier<Collection<Coherence>> supplier              = () -> Collections.singleton(coherence);
        CoherenceHealthChecks           coherenceHealthChecks = new CoherenceHealthChecks(supplier);
        HealthCheck                     healthCheck           = coherenceHealthChecks.liveness();

        assertDown(healthCheck, CoherenceHealthChecks.HEALTH_CHECK_LIVENESS);
        }

    @Test
    public void shouldBeLive()
        {
        Coherence                     coherence  = mock(Coherence.class);
        Registry                      management = mock(Registry.class);
        com.tangosol.util.HealthCheck checkOne   = mock(com.tangosol.util.HealthCheck.class);
        com.tangosol.util.HealthCheck checkTwo   = mock(com.tangosol.util.HealthCheck.class);

        Collection<com.tangosol.util.HealthCheck> checks = List.of(checkOne, checkTwo);

        when(checkOne.getName()).thenReturn("One");
        when(checkOne.isLive()).thenReturn(true);
        when(checkTwo.getName()).thenReturn("Two");
        when(checkTwo.isLive()).thenReturn(true);
        when(coherence.getManagement()).thenReturn(management);
        when(management.getHealthChecks()).thenReturn(checks);

        Supplier<Collection<Coherence>> supplier              = () -> Collections.singleton(coherence);
        CoherenceHealthChecks           coherenceHealthChecks = new CoherenceHealthChecks(supplier);
        HealthCheck                     healthCheck           = coherenceHealthChecks.liveness();

        assertUp(healthCheck, CoherenceHealthChecks.HEALTH_CHECK_LIVENESS);
        }

    @Test
    public void shouldNotBeLiveIfCoherenceHealthCheckDown()
        {
        Coherence                     coherence  = mock(Coherence.class);
        Registry                      management = mock(Registry.class);
        com.tangosol.util.HealthCheck checkOne   = mock(com.tangosol.util.HealthCheck.class);
        com.tangosol.util.HealthCheck checkTwo   = mock(com.tangosol.util.HealthCheck.class);

        Collection<com.tangosol.util.HealthCheck> checks = List.of(checkOne, checkTwo);

        when(checkOne.getName()).thenReturn("One");
        when(checkOne.isLive()).thenReturn(true);
        when(checkTwo.getName()).thenReturn("Two");
        when(checkTwo.isLive()).thenReturn(false);
        when(coherence.getManagement()).thenReturn(management);
        when(management.getHealthChecks()).thenReturn(checks);

        Supplier<Collection<Coherence>> supplier              = () -> Collections.singleton(coherence);
        CoherenceHealthChecks           coherenceHealthChecks = new CoherenceHealthChecks(supplier);
        HealthCheck                     healthCheck           = coherenceHealthChecks.liveness();

        assertDown(healthCheck, CoherenceHealthChecks.HEALTH_CHECK_LIVENESS);
        }

    void assertUp(HealthCheck healthCheck, String sExpectedName)
        {
        assertThat(healthCheck, is(notNullValue()));

        HealthCheckResponse response = healthCheck.call();
        assertThat(response, is(notNullValue()));
        assertThat(response.getState(), is(HealthCheckResponse.State.UP));
        }

    void assertDown(HealthCheck healthCheck, String sExpectedName)
        {
        assertThat(healthCheck, is(notNullValue()));

        HealthCheckResponse response = healthCheck.call();
        assertThat(response, is(notNullValue()));
        assertThat(response.getName(), is(sExpectedName));
        assertThat(response.getState(), is(HealthCheckResponse.State.DOWN));
        }
    }
