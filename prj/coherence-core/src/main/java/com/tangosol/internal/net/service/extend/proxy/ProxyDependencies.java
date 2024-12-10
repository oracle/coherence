/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;


import com.tangosol.net.ServiceDependencies;


/**
 * The ProxyDependencies interface provides a Proxy object with its external dependencies.
 *
 * @author pfm  2011.07.25
 * @since Coherence 12.1.2
 */
public interface ProxyDependencies
        extends ServiceDependencies
    {
    /**
     * Return true if the Proxy is enabled.
     *
     * @return true if the Proxy is enabled
     */
    public boolean isEnabled();
    }
