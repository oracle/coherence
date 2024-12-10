/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.server;

/**
 * An extension of {@link PassThroughResourceConfig} that should be used for
 * container deployments of Coherence REST when pass-through is required.
 *
 * @author tam  2016.10.11
 */
public class ContainerPassThroughResourceConfig
        extends PassThroughResourceConfig
    {

    // ----- constructors ---------------------------------------------------

    public ContainerPassThroughResourceConfig()
        {
        super();
        }

    // ---- DefaultResourceConfig methods -----------------------------------

    @Override
    protected boolean isRunningInContainer()
        {
        return true;
        }
    }
