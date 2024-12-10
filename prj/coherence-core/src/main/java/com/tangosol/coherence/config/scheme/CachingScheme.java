/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.BackingMapManagerBuilder;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.NamedCacheBuilder;

import com.tangosol.net.BackingMapManager;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import java.util.Map;

/**
 * The {@link CachingScheme} is a multi-builder for cache-based infrastructure.
 * In particular it defines mechanisms to build {@link NamedCache}s, {@link Map}s
 * that are used as the basis for {@link NamedCache}s, and where appropriate,
 * {@link Service}s and {@link BackingMapManager}s.
 *
 * @author pfm  2011.12.28
 * @since Coherence 12.1.2
 */
public interface CachingScheme
        extends ServiceScheme, NamedCacheBuilder, MapBuilder, BackingMapManagerBuilder
    {
    }
