/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.server;

/**
 * An extension of {@link DefaultResourceConfig} that should be used for
 * container deployments of Coherence REST.
 *
 * @author jh 2012.02.03
 */
public class ContainerResourceConfig
        extends DefaultResourceConfig
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ContainerResourceConfig()
        {
        super();
        }

    @Override
    protected boolean isRunningInContainer()
        {
        return true;
        }
    }
