/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import java.net.SocketAddress;

/**
 * The SocketAddressProvider is an interface that serves as a means to provide
 * addresses to a consumer.  Simple implementations could be backed by a static
 * list; more complex ones could use dynamic discovery protocols.
 * <p>
 * SocketAddressProvider implementations must exercise extreme caution since any
 * delay with return or unhandled exception will cause a delay or complete
 * shutdown of the corresponding service.
 * <p>
 * Implementations that involve more expensive operations (e.g. network
 * fetch) may choose to do so asynchronously by extending
 * {@link com.tangosol.net.RefreshableAddressProvider RefreshableAddressProvider}.
 *
 * @author gg,jh 2008.08.14
 * @author phf 2012.04.05
 *
 * @since Coherence 12.1.2
 */
public interface SocketAddressProvider
    {
    /**
     * Obtain a next available address to use. If the caller can successfully use
     * the returned address (e.g. a connection was established), it should call
     * the SocketAddressProvider's {@link #accept} method.
     *
     * @return the next available address or null if the list of available
     *         addresses was exhausted
     */
    public SocketAddress getNextAddress();

    /**
     * This method should be called by the client immediately after it determines
     * that it can successfully use an address returned by the
     * {@link #getNextAddress} method.
     */
    public void accept();

    /**
     * This method should be called by the client immediately after it determines
     * that an attempt to use an address returned by the {@link #getNextAddress}
     * method has failed.
     *
     * @param eCause  (optional) an exception that carries the reason why the
     *                the caller rejected the previously returned address
     */
    public void reject(Throwable eCause);

    /**
     * SocketAddressProvider instances are considered equivalent iff they
     * consistently produce the same resulting set of addresses.
     * <p>
     * Note: the general contract of <tt>hashCode</tt> and <tt>equals()</tt>
     *       should be preserved; AddressProviders that are "equal"
     *       should produce the same hashCode.
     *
     * @param o  the Object to compare this SocketAddressProvider to for equality
     *
     * @return true iff this SocketAddressProvider is equal to the specified object
     */
    public boolean equals(Object o);

    /**
     * Return the hash code for this SocketAddressProvider.
     *
     * @return the hash code for this SocketAddressProvider
     */
    public int hashCode();
    }
