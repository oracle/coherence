/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

/**
 * {@link BundlingScheme}s define how the bundling (batching)
 * of operations will occur and the {@link BundleManager} used
 * to configure said bundling.
 *
 * @author bko  2013.10.21
 * @since Coherence 12.1.3
 */
public interface BundlingScheme
    {
    /**
     * Obtains the {@link BundleManager}.
     *
     * @return the BundleManager
     */
    public BundleManager getBundleManager();
    }
