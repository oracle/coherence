/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * The default {@link ResourceConfig} implementation that supports
 * management requests.
 *
 * @author hr  2016.07.26
 * @since 12.2.1.4.0
 */
public class ManagementResourceConfig
        extends ResourceConfig
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an instance of <tt>ResourceConfig</tt> that initialize
     * Coherence predefined properties and searches for root resource classes
     * and providers in the specified packages.
     */
    public ManagementResourceConfig()
        {
        RestManagement.configure(this);
        }
    }
