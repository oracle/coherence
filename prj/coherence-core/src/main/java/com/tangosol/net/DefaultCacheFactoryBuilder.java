/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.internal.net.ScopedUriScopeResolver;

/**
 * {@link DefaultCacheFactoryBuilder} is the default implementation of {@link CacheFactoryBuilder}.  As
 * of Coherence 3.7.2, this class simply extends {@link ScopedCacheFactoryBuilder}.  Prior to Coherence
 * 3.5.1, a single {@link ConfigurableCacheFactory} per cluster member was created.  This behavior
 * is preserved in {@link SingletonCacheFactoryBuilder}.
 * 
 * @author rhl  2009.07.14
 * @author pp   2011.01.20
 *
 * @since Coherence 3.5.1
 */
public class DefaultCacheFactoryBuilder
        extends ScopedCacheFactoryBuilder
    {
    public DefaultCacheFactoryBuilder()
        {
        super(new ScopedUriScopeResolver(false));
        }
    }
