/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.util.processor.MethodInvocationProcessor;

/**
 * An interface that should be implemented by the classes that are able to
 * create a client-side proxy for itself.
 *
 * @author as  2014.11.19
 * @since 12.2.1
 */
public interface Proxyable<T>
    {
    /**
     * Return a proxy for a cached value.
     * <p>
     * The proxy returned will typically use {@link MethodInvocationProcessor}
     * to invoke methods on a remote object, but ultimately it is up to the
     * proxy implementor to decide how each individual method will be proxied.
     *
     * @param sCacheName  the name of the cache the proxied object is in
     * @param key         the key associated with the proxied object
     * @param <K>         the type of the cache key
     *
     * @return a proxy for the value with a given key in the specified cache
     */
    <K> T getProxy(String sCacheName, K key);
    }
