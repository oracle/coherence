/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.ConfigurableCacheFactory;

/**
 * A {@link BackingMapManagerBuilder} realizes {@link BackingMapManager}s.
 *
 * @author bo 2012.11.06
 * @since Coherence 12.1.2
 */
public interface BackingMapManagerBuilder
    {
    /**
     * Realize a {@link BackingMapManager} to be scoped by the specified
     * {@link ConfigurableCacheFactory}.
     *
     * @param ccf  the {@link ConfigurableCacheFactory}
     *
     * @return a {@link BackingMapManager}
     */
    public BackingMapManager realizeBackingMapManager(ConfigurableCacheFactory ccf);
    }
