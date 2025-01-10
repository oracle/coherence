/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

/**
 * The DefaultRemoteTopicServiceDependencies class provides a default implementation of
 * RemoteTopicServiceDependencies.
 *
 * @author Jonathan Knight  2025.01.01
 */
public class DefaultRemoteTopicServiceDependencies
        extends DefaultRemoteServiceDependencies
        implements RemoteTopicServiceDependencies
    {
    /**
     * Construct a DefaultRemoteTopicServiceDependencies object.
     */
    public DefaultRemoteTopicServiceDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultRemoteTopicServiceDependencies object, copying the values from
     * the specified RemoteTopicServiceDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultRemoteTopicServiceDependencies(RemoteTopicServiceDependencies deps)
        {
        super(deps);
        }

    // ----- RemoteTopicServiceDependencies methods -------------------------

    /**
     * Validate the supplied dependencies.
     *
     * @throws IllegalArgumentException if the dependencies are not valid
     *
     * @return this object
     */
    @Override
    public DefaultRemoteServiceDependencies validate()
        {
        super.validate();

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString()
               + "{"
               + "}";
        }
    }
