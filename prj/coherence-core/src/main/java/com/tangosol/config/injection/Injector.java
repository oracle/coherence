/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.injection;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.util.ResourceResolver;

/**
 * An {@link Injector} is responsible for injecting resolved values into Java
 * objects.
 *
 * @see Injectable
 * @see ResourceResolver
 *
 * @author bo 2012.09.17
 * @since Coherence 12.1.2
 */
public interface Injector
    {
    /**
     * Attempts to inject appropriate values provided by a
     * {@link ResourceResolver} into a specified object.
     *
     * @param object    the object in which to inject values
     * @param resolver  the {@link ResourceResolver} providing values to inject
     */
    public <T> T inject(T object, ResourceResolver resolver);
    }
