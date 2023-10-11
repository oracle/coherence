/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.tracing;

import com.tangosol.util.Base;

import java.util.Objects;

/**
 * Implementations of this interface are responsible for initializing a tracing runtime, and providing a mapping
 * from the Coherence API to the underlying implementation API.
 *
 * @since  14.1.1.0
 * @author rl 11.5.2019
 */
public interface TracingShim
        extends NoopAware
    {
    // ----- interface methods ----------------------------------------------
    /**
     * Initialize the {@link Tracer}, if not already initialized.
     *
     * @param dependencies  the tracing {@link TracingShim.Dependencies dependencies}
     *
     * @return a resource which can be used to terminate tracing or {@code null} if it was already enabled
     */
    public Control initialize(Dependencies dependencies);

    /**
     * Return {@code true} if there is a non no-op registered {@link Tracer}.
     *
     * @return {@code true} if there is a non no-op registered {@link Tracer}.
     */
    public boolean isEnabled();

    /**
     * Return the {@link Tracer}.
     *
     * @return the current {@link Tracer}
     */
    public Tracer getTracer();

    /**
     * Activate the specified {@link Span}.
     *
     * @param span  the {@link Span} to activate.
     *
     * @return the activated {@link Span}
     *
     * @since 24.03
     */
    public Span activateSpan(Span span);

    /**
     * Return a {@link Span} where all operations performed against it will be no-ops.
     *
     * @return a {@link Span} where all operations performed against it will be no-ops
     */
    public Span getNoopSpan();

    /**
     * Return a {@link Span.Builder} where all operations performed against it will be no-ops.
     *
     * @return a {@link Span.Builder} where all operations performed against it will be no-ops
     */
    public Span.Builder getNoopSpanBuilder();

    /**
     * Return the {@link Dependencies} used to configure this {@code TracingShim}.
     *
     * @return the {@link Dependencies} used to configure this {@code TracingShim}.
     */
    public Dependencies getDependencies();

    // ----- inner interface: Control ---------------------------------------

    /**
     * {@code Control} can be used to perform basic control actions against a {@link Tracer tracer}.
     */
    public interface Control
            extends AutoCloseable
        {
        @Override
        public void close();

        /**
         * Return the dependencies for the tracer.
         *
         * @return the dependencies
         */
        public Dependencies getDependencies();
        }

    // ----- inner interface: Dependencies ----------------------------------

    /**
     * TracingShim Dependencies.
     */
    public interface Dependencies
        {
        /**
         * Return the identity for this process.
         *
         * @return the identity
         */
        public String getIdentity();

        /**
         * Return the sampling ratio.
         *
         * @return the sampling ratio
         */
        public float getSamplingRatio();
        }

    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * Default implementation of Dependencies.
     */
    public class DefaultDependencies
            implements Dependencies
        {
        /**
         * Construct a DefaultDependencies instance.
         */
        public DefaultDependencies()
            {
            }

        /**
         * Construct a DefaultDependencies instance, copying values from the supplied instance.
         *
         * @param deps  the {@link Dependencies} to copy
         */
        public DefaultDependencies(Dependencies deps)
            {
            if (deps != null)
                {
                setIdentity(deps.getIdentity());
                setSamplingRatio(deps.getSamplingRatio());
                }
            }

        @Override
        public String getIdentity()
            {
            return m_sIdentity;
            }

        /**
         * Specify the identity for this process.
         *
         * @param sIdentity  the identity
         *
         * @return this object
         */
        public DefaultDependencies setIdentity(String sIdentity)
            {
            m_sIdentity = sIdentity;
            return this;
            }

        @Override
        public float getSamplingRatio()
            {
            return Math.min(m_nSamplingRatio, 1.0f);
            }

        /**
         * Set the sampling ratio.
         *
         * @param nSamplingRatio  the sampling ratio
         *
         * @return this object
         */
        public DefaultDependencies setSamplingRatio(float nSamplingRatio)
            {
            m_nSamplingRatio = nSamplingRatio;
            return this;
            }

        // ----- object methods ---------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }
            DefaultDependencies that = (DefaultDependencies) o;
            return Float.compare(that.m_nSamplingRatio, m_nSamplingRatio) == 0
                   && Base.equals(m_sIdentity, that.m_sIdentity);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_sIdentity, m_nSamplingRatio);
            }

        @Override
        public String toString()
            {
            return "DefaultDependencies("
                   + "Identity='" + m_sIdentity + '\''
                   + ", SamplingRatio=" + m_nSamplingRatio
                   + ')';
            }

        // ----- data members -----------------------------------------------

        /**
         * The identity of the traced process.
         */
        protected String m_sIdentity;

        /**
         * The sampling ratio to use.
         */
        protected float m_nSamplingRatio = -1f;
        }

    // ----- inner class: Noop ------------------------------------------

    /**
     * A no-op implementation of {@link TracingShim}.
     */
    final class Noop
            implements TracingShim
        {
        // ----- constructors -------------------------------------------

        /**
         * @see #INSTANCE
         */
        private Noop()
            {
            }

        // ----- TracingShim interface ----------------------------------

        /**
         * Always returns {@code null}.
         *
         * @return {@code null}.
         */
        @Override
        public Control initialize(Dependencies dependencies)
            {
            return null;
            }

        /**
         * Always returns {@code false}.
         *
         * @return {@code false}.
         */
        @Override
        public boolean isEnabled()
            {
            return false;
            }

        /**
         * Always returns {@link Tracer.Noop#INSTANCE}.
         *
         * @return {@link Tracer.Noop#INSTANCE}
         */
        @Override
        public Tracer getTracer()
            {
            return Tracer.Noop.INSTANCE;
            }

        /**
         * Always returns {@link Span.Noop#INSTANCE}.
         *
         * @return {@link Span.Noop#INSTANCE}
         */
        @Override
        public Span getNoopSpan()
            {
            return Span.Noop.INSTANCE;
            }

        @Override
        public Span activateSpan(Span span)
            {
            return span;
            }

        @Override
        public Span.Builder getNoopSpanBuilder()
            {
            return Span.Builder.Noop.INSTANCE;
            }

        @Override
        public Dependencies getDependencies()
            {
            return new DefaultDependencies();
            }

        // ---- NoopAware interface -------------------------------------

        /**
         * Returns {@code true}.
         *
         * @return {@code true}
         */
        @Override
        public boolean isNoop()
            {
            return true;
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return "TracingShim.Noop";
            }

        // ----- constants --------------------------------------------------

        /**
         * Singleton no-op {@link TracingShim} instance.
         */
        public static final TracingShim INSTANCE = new Noop();
        }
    }
