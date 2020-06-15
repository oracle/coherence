/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

/**
 * The DefaultInvocationServiceProxyDependencies class provides a default implementation of
 * InvocationServiceProxyDependencies.
 *
 * @author pfm 2011.07.25
 * @since Coherence 12.1.2
 */
public class DefaultInvocationServiceProxyDependencies
        extends DefaultServiceProxyDependencies
        implements InvocationServiceProxyDependencies
    {
    /**
     * Construct a DefaultInvocationServiceProxyDependencies object.
     */
    public DefaultInvocationServiceProxyDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultInvocationServiceProxyDependencies object, copying the values from
     * the specified InvocationServiceProxyDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultInvocationServiceProxyDependencies(InvocationServiceProxyDependencies deps)
        {
        super(deps);
        }
    }
