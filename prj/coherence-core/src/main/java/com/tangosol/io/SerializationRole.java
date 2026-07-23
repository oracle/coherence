/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io;

/**
 * Serialization transport role for per-route gate telemetry.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public enum SerializationRole
    {
    CLUSTER,
    EXTEND_PROXY,
    EXTEND_CLIENT,
    GRPC,
    REST,
    MANAGEMENT_REST,
    JMX,
    SESSION,
    FEDERATION,
    PERSISTENCE,
    CACHE_STORE,
    TOPICS,
    TOOLING,
    UNCLASSIFIED;

    /**
     * Return the current serialization role.
     *
     * @return the current role
     */
    public static SerializationRole current()
        {
        return TL.get();
        }

    /**
     * Set the current serialization role and return a scope that restores the
     * prior role when closed.
     *
     * @param role  the role to set
     *
     * @return a scope that restores the prior role
     */
    public static Scope setAndClose(SerializationRole role)
        {
        SerializationRole rolePrior = TL.get();
        TL.set(role == null ? UNCLASSIFIED : role);
        return new Scope(rolePrior);
        }

    /**
     * Set the current serialization role to {@link #CLUSTER} and return a
     * scope that restores the prior role when closed.
     *
     * @return a scope that restores the prior role
     */
    public static Scope setClusterAndClose()
        {
        return setAndClose(CLUSTER);
        }

    /**
     * Set the current serialization role to the Extend proxy or client role
     * and return a scope that restores the prior role when closed.
     *
     * @param fProxy  {@code true} to set the proxy role, or {@code false} to
     *                set the client role
     *
     * @return a scope that restores the prior role
     */
    public static Scope setExtendAndClose(boolean fProxy)
        {
        return setAndClose(fProxy ? EXTEND_PROXY : EXTEND_CLIENT);
        }

    /**
     * Set the current serialization role to {@link #JMX} and return a scope
     * that restores the prior role when closed.
     *
     * @return a scope that restores the prior role
     */
    public static Scope setJmxAndClose()
        {
        return setAndClose(JMX);
        }

    // ----- inner class: Scope ----------------------------------------------

    /**
     * Role scope.
     */
    public static class Scope
            implements AutoCloseable
        {
        /**
         * Create a role scope.
         *
         * @param prior  the prior role
         */
        protected Scope(SerializationRole prior)
            {
            f_prior = prior;
            }

        @Override
        public void close()
            {
            TL.set(f_prior);
            }

        /**
         * Prior role.
         */
        private final SerializationRole f_prior;
        }

    // ----- constants --------------------------------------------------------

    /**
     * Current role.
     */
    private static final ThreadLocal<SerializationRole> TL =
            ThreadLocal.withInitial(() -> UNCLASSIFIED);
    }
