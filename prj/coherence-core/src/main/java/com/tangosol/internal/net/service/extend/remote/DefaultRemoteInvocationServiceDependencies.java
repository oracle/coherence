/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

/**
 * The DefaultRemoteInvocationServiceDependencies class provides a default implementation
 * of RemoteInvocationServiceDependencies.
 *
 * @author pfm 2011.09.05
 * @since Coherence 12.1.2
 */
public class DefaultRemoteInvocationServiceDependencies
        extends DefaultRemoteServiceDependencies
        implements RemoteInvocationServiceDependencies
    {
    /**
     * Construct a DefaultRemoteInvocationServiceDependencies object.
     */
    public DefaultRemoteInvocationServiceDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultRemoteInvocationServiceDependencies object, copying the values from
     * the specified RemoteInvocationServiceDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultRemoteInvocationServiceDependencies(RemoteInvocationServiceDependencies deps)
        {
        super(deps);
        }
    }
