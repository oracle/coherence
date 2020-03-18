/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.MapListener;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.ResourceRegistry;

import java.util.Map;

/**
 * An {@link ObservableCachingScheme} is a {@link CachingScheme} that supports
 * defining and adding {@link MapListener}s to realized {@link Map}s and
 * {@link NamedCache}s.
 *
 * @author bo  2012.11.06
 * @since Coherence 12.1.2
 */
public interface ObservableCachingScheme
        extends CachingScheme
    {
    /**
     * Obtains a {@link ParameterizedBuilder} for a {@link MapListener} that
     * can be used for building {@link MapListener}s those of which may be
     * later added to the {@link Map}s or {@link NamedCache}s realized by
     * the {@link CachingScheme}.
     *
     * @return a {@link ParameterizedBuilder} for {@link MapListener}s
     */
    public ParameterizedBuilder<MapListener> getListenerBuilder();

    /**
     * Establishes an appropriate {@link MapListener} (provided by the
     * {@link #getListenerBuilder()}) on the {@link ObservableMap}
     * that was produced by the {@link ObservableCachingScheme}.
     * <p>
     * This method will automatically inject the following types and
     * named values into realized classes that have been annotated with
     * &#64;Injectable.
     * <ol>
     *      <li> {@link com.tangosol.net.BackingMapManagerContext} (optionally named "manager-context")
     *      <li> {@link ConfigurableCacheFactory}
     *      <li> Cache Name (as a {@link String}.class named "cache-name")
     *      <li> Context {@link ClassLoader} (optionally named "class-loader")
     *      <li> {@link ResourceRegistry}
     *      <li> {@link CacheConfig}
     *      <li> together with any other resource, named or otherwise, available
     *           in the {@link ResourceRegistry} provided by the
     *           {@link ConfigurableCacheFactory}.
     * </ol>
     *
     * @see com.tangosol.config.annotation.Injectable
     *
     * @param map           an {@link ObservableMap} to which to add a {@link MapListener}
     *                      (if the map is not observable, no listeners are added)
     * @param resolver      the {@link ParameterResolver} to use for resolving
     *                      builder parameters
     * @param dependencies  the {@link MapBuilder} dependencies from which to
     *                      obtain builder information
     */
    public void establishMapListeners(Map map, ParameterResolver resolver, MapBuilder.Dependencies dependencies);
    }
