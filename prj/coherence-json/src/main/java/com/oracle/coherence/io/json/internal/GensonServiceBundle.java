/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json.internal;

import com.oracle.coherence.io.json.GensonBundleProvider;

import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.oracle.coherence.io.json.genson.ext.GensonBundle;

import java.util.ServiceLoader;

/**
 * Service that registers {@link GensonBundle}s provided by {@link GensonBundleProvider} implementations
 * on the classpath.
 * <p>
 * Bundles are registered in iteration order.
 *
 * @since 14.1.2
 */
public class GensonServiceBundle
        extends GensonBundle
    {
    // ----- GensonBundle methods -------------------------------------------

    @Override
    public void configure(GensonBuilder builder)
        {
        ServiceLoader<GensonBundleProvider> loader = ServiceLoader.load(GensonBundleProvider.class);
        loader.forEach(p -> builder.withBundle(p.provide()));
        }
    }
