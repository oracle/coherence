/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import java.net.InetSocketAddress;

/**
 * The AddressProvider is a subclass interface of {@link SocketAddressProvider}
 * which returns {@link InetSocketAddress} from {@link #getNextAddress()}.
 *
 * @author gg,jh 2008-08-14
 * @since Coherence 3.4
 */
public interface AddressProvider
        extends SocketAddressProvider
    {
    /**
     * Covariant of {@link SocketAddressProvider#getNextAddress()} which
     * returns an {@link InetSocketAddress}.
     * 
     * @return the next available address or null if the list of available
     *         addresses was exhausted
     */
    public InetSocketAddress getNextAddress();
    }
