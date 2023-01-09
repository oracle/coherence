/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.health;

import com.tangosol.net.Coherence;

import com.tangosol.net.management.Registry;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * A CDI producer that produces Coherence {@link HealthCheck health checks}.
 *
 * @author Jonathan Knight  2023.01.06
 * @since 22.06.4
 */
@ApplicationScoped
public class CoherenceHealthChecks
    {
    /**
     * Default constructor required for CDI bean.
     */
    public CoherenceHealthChecks()
        {
        this(Coherence::getInstances);
        }

    /**
     * Create a {@link CoherenceHealthChecks} instance.
     *
     * @param supplier  the {@link Coherence} instances supplier.
     */
    public CoherenceHealthChecks(Supplier<? extends Collection<Coherence>> supplier)
        {
        f_supplier = supplier;
        }

    @Produces
    @Liveness
    public HealthCheck liveness()
        {
        return new CoherenceLiveness(f_supplier);
        }

    @Produces
    @Readiness
    public HealthCheck readiness()
        {
        return new CoherenceReadiness(f_supplier);
        }

    // ----- inner class CoherenceHealth ------------------------------------

    /**
     * A base implementation for Coherence {@link HealthCheck health checks}.
     */
    protected static abstract class CoherenceHealth
            implements HealthCheck
        {
        /**
         * Create a {@link CoherenceHealth} instance.
         *
         * @param sName     the name of the health check
         * @param supplier  the {@link Coherence} instance supplier
         */
        protected CoherenceHealth(String sName, Supplier<? extends Collection<Coherence>> supplier)
            {
            f_sName    = sName;
            f_supplier = supplier;
            }

        @Override
        public HealthCheckResponse call()
            {
            HealthCheckResponseBuilder builder      = HealthCheckResponse.builder().name(f_sName);
            Collection<Coherence>      colCoherence = f_supplier.get();

            if (colCoherence == null || colCoherence.isEmpty())
                {
                return builder.down().build();
                }

            return colCoherence.stream()
                    .findFirst()
                    .map(coherence -> update(coherence, builder))
                    .orElseGet(builder::down)
                    .build();
            }

        /**
         * Update the {@link HealthCheckResponseBuilder builder} with Coherence health information.
         *
         * @param coherence  the Coherence instance
         * @param builder    the {@link HealthCheckResponseBuilder builder} to update
         *
         * @return the updated {@link HealthCheckResponseBuilder builder}
         */
        protected HealthCheckResponseBuilder update(Coherence coherence, HealthCheckResponseBuilder builder)
            {
            Registry management = coherence.getManagement();
            boolean  fUp        = true;

            Collection<com.tangosol.util.HealthCheck> healthChecks = management.getHealthChecks();
            if (healthChecks.isEmpty())
                {
                return builder.down();
                }

            for (com.tangosol.util.HealthCheck healthCheck : healthChecks)
                {
                boolean fHealthy = isUp(healthCheck);
                builder.withData(healthCheck.getName(), fHealthy);
                fUp = fUp && fHealthy;
                }

            return builder.state(fUp);
            }

        /**
         * Returns {@code true} if the Coherence {@link com.tangosol.util.HealthCheck}
         * is healthy, otherwise returns {@code false}.
         *
         * @param healthCheck  the Coherence {@link com.tangosol.util.HealthCheck}
         *
         * @return {@code true} if the Coherence {@link com.tangosol.util.HealthCheck}
         *         is healthy, otherwise returns {@code false}
         */
        protected abstract boolean isUp(com.tangosol.util.HealthCheck healthCheck);

        // ----- data members -----------------------------------------------

        /**
         * The name of the health check.
         */
        private final String f_sName;

        /**
         * The supplier of {@link Coherence} instances.
         */
        private final Supplier<? extends Collection<Coherence>> f_supplier;
        }

    // ----- inner class: CoherenceReadiness --------------------------------

    /**
     * A Coherence readiness {@link HealthCheck health checks}.
     */
    protected static class CoherenceReadiness
            extends CoherenceHealth
        {
        public CoherenceReadiness(Supplier<? extends Collection<Coherence>> supplier)
            {
            super(HEALTH_CHECK_READINESS, supplier);
            }

        @Override
        protected boolean isUp(com.tangosol.util.HealthCheck healthCheck)
            {
            return healthCheck.isReady();
            }
        }

    // ----- inner class: CoherenceReadiness --------------------------------

    /**
     * A Coherence liveness {@link HealthCheck health checks}.
     */
    protected static class CoherenceLiveness
            extends CoherenceHealth
        {
        public CoherenceLiveness(Supplier<? extends Collection<Coherence>> supplier)
            {
            super(HEALTH_CHECK_LIVENESS, supplier);
            }

        @Override
        protected boolean isUp(com.tangosol.util.HealthCheck healthCheck)
            {
            return healthCheck.isLive();
            }
        }

    // ----- inner class: CoherenceStarted ----------------------------------

    /**
     * A Coherence start-up {@link HealthCheck health checks}.
     */
    protected static class CoherenceStarted
            extends CoherenceHealth
        {
        public CoherenceStarted(Supplier<? extends Collection<Coherence>> supplier)
            {
            super(HEALTH_CHECK_STARTUP, supplier);
            }

        @Override
        protected boolean isUp(com.tangosol.util.HealthCheck healthCheck)
            {
            return healthCheck.isStarted();
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the Coherence readiness {@link HealthCheck health checks}
     */
    public static final String HEALTH_CHECK_READINESS = "CoherenceReadiness";

    /**
     * The name of the Coherence liveness {@link HealthCheck health checks}
     */
    public static final String HEALTH_CHECK_LIVENESS = "CoherenceLiveness";

    /**
     * The name of the Coherence start-up {@link HealthCheck health checks}
     */
    public static final String HEALTH_CHECK_STARTUP = "CoherenceStartup";

    // ----- data members ---------------------------------------------------

    /**
     * The supplier of {@link Coherence} instances.
     */
    private final Supplier<? extends Collection<Coherence>> f_supplier;
    }
