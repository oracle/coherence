/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.internal;


import com.tangosol.internal.tracing.TracingHelper;


/**
 * Base class for custom {@link GrpcTracingImplementationLoader}
 * implementations.
 * <p></p>
 * NOTE: This is an internal API and is subject to change without notice.
 *
 * @author rl 9.27.2023
 * @since 24.03
 */
public abstract class AbstractGrpcTracingImplementationLoader
        implements GrpcTracingImplementationLoader
    {
    // ----- abstract methods -----------------------------------------------

    /**
     * Return the class name that should be checked by the
     * {@link #isAvailable()} implementation.
     *
     * @return the class name that should be checked by the
     *         {@link #isAvailable()} implementation
     */
    protected abstract String getSearchClassName();

    /**
     * Returns a new instance of the {@link GrpcTracingImplementation}.
     *
     * @return a new instance of the {@link GrpcTracingImplementation}
     */
    protected abstract GrpcTracingImplementation newInstance();

    // ----- TracingInterceptorLoader interface -----------------------------

    /**
     * Return the {@link GrpcTracingImplementation} or {@code null}
     * if no suitable implementation is found.
     *
     * @return the {@link GrpcTracingImplementation} or {@code null}
     *         if no suitable implementation is found
     */
    public GrpcTracingImplementation getGrpcTracingImplementation()
        {
        return isAvailable() ? newInstance() : null;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Returns {@code true} if the class name returned by
     * {@link #getSearchClassName()} is available on the classpath.
     *
     * @return {@code true} if the class name returned by
     *         {@link #getSearchClassName()} is available on the classpath,
     *         otherwise returns {@code false}
     */
    protected boolean isAvailable()
        {
        try
            {
            Class.forName(getSearchClassName());
            return true;
            }
        catch (ClassNotFoundException e)
            {
            return false;
            }
        }
    }
