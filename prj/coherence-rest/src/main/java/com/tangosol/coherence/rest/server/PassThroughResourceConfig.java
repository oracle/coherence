/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.server;

import com.tangosol.coherence.rest.PassThroughRootResource;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * An alternate {@link ResourceConfig} implementation that supports pass-through
 * access to all the caches defined by the cache configuration.
 *
 * @author as  2015.09.07
 * @since 12.2.1
 */
public class PassThroughResourceConfig
        extends DefaultResourceConfig
    {
    @Override
    protected void registerRootResource()
        {
        register(PassThroughRootResource.class);
        }
    }
