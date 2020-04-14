/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.AddressProviderFactory;

import java.net.InetSocketAddress;

/**
 * An AddressProvider which substitutes "unset" portions of an address.
 *
 * @author mf
 *
 * @since Coherence 12.2.1
 */
public class SubstitutionAddressProvider
        implements AddressProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Return a new  SubstitutionAddressProvider
     *
     * @param delegate  the delegate provider
     * @param nPort     the value to replace, zero port values with
     */
    public SubstitutionAddressProvider(AddressProvider delegate, int nPort)
        {
        f_delegate = delegate;
        f_nPort    = nPort;
        }

    // ----- AddressProvider interface --------------------------------------

    @Override
    public InetSocketAddress getNextAddress()
        {
        InetSocketAddress address = f_delegate.getNextAddress();
        if (address != null && f_nPort != 0 && address.getPort() == 0)
            {
            address = new InetSocketAddress(address.getAddress(), f_nPort);
            }

        return address;
        }

    @Override
    public void accept()
        {
        f_delegate.accept();
        }

    @Override
    public void reject(Throwable eCause)
        {
        f_delegate.reject(eCause);
        }


    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return f_delegate.toString();
        }

    @Override
    public int hashCode()
        {
        return f_delegate.hashCode();
        }

    // ----- factory helpers ------------------------------------------------

    /**
     * Return a AddressProviderFactory which substitutes the specified port for any zero ports.
     *
     * @param delegate  the delegate factory
     * @param nPort     the new port value
     *
     * @return the factory
     */
    public static AddressProviderFactory createFactory(AddressProviderFactory delegate, int nPort)
        {
        return new AddressProviderFactory()
            {
            @Override
            public AddressProvider createAddressProvider(ClassLoader loader)
                {
                return new SubstitutionAddressProvider(delegate.createAddressProvider(loader), nPort);
                }
            };
        }

    // ----- data fields ----------------------------------------------------

    /**
     * The delegate provider.
     */
    private final AddressProvider f_delegate;

    /**
     * The port to replace zero port values with
     */
    private final int f_nPort;
    }
