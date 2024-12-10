/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

/**
 * A {@link ResourceResolver} provides a mechanism to lookup and resolve
 * optionally named, strongly typed resources.
 *
 * @see ResourceRegistry
 *
 * @author bo 2012.09.17
 * @since Coherence 12.1.2
 */
public interface ResourceResolver
    {
    /**
     * Attempts to retrieve the resource that was registered with the
     * specified class.
     *
     * @param <R>            the type of the resource
     * @param clsResource    the class of the resource
     *
     * @return the registered resource or <code>null</code> if the resource is
     *         unknown to the {@link ResourceRegistry}
     */
    public <R> R getResource(Class<R> clsResource);

    /**
     * Attempts to retrieve the resource that was registered with the
     * specified class and name.
     *
     * @param <R>            the type of the resource
     * @param clsResource    the class of the resource
     * @param sResourceName  the name of the resource
     *
     * @return the registered resource or <code>null</code> if the resource is
     *         unknown to the {@link ResourceRegistry}
     */
    public <R> R getResource(Class<R> clsResource, String sResourceName);
    }
