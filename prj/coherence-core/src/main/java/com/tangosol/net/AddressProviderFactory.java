/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

/**
 * A factory for {@link AddressProvider} objects.
 *
 * @author wl  2012.04.04
 *
 * @since Coherence 12.1.2
 */
public interface AddressProviderFactory
    {
    /**
     * Create a new AddressProvider using the specified class loader.
     *
     * @param loader  the optional ClassLoader with which to configure the
     *                new AddressProvider
     *
     * @return a new AddressProvider
     */
    public AddressProvider createAddressProvider(ClassLoader loader);
    }
