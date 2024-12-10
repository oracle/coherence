/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

import com.tangosol.util.ClassHelper;

/**
 * The DefaultRemoteNameServiceDependencies class provides a default implementation
 * of RemoteNameServiceDependencies.
 *
 * @author welin  2013.05.17
 *
 * @since Coherence 12.1.3
 */
public class DefaultRemoteNameServiceDependencies
        extends DefaultRemoteServiceDependencies
        implements RemoteNameServiceDependencies
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultRemoteNameServiceDependencies object.
     */
    public DefaultRemoteNameServiceDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultRemoteNameServiceDependencies object, copying the values
     * from the specified RemoteNameServiceDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultRemoteNameServiceDependencies(RemoteNameServiceDependencies deps)
        {
        super(deps);
        }
    }
