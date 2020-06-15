/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.net.InetAddresses;
import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.builder.AddressProviderBuilder;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.run.xml.XmlElement;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import java.util.Iterator;


/**
 * ConfigurableLocalAddressProvider is an AddressProvider which is only
 * capable of returning local addresses.
 * <p>
 * As is it known that only local addresses can be returned, this provider
 * supports some formats not supported by the ConfigurableAddressProvider,
 * namely empty and null hostnames are assumed to be equal to "localhost".
 * Additionally, the hostname string may be in CIDR format (e.g. subnet/mask)
 * allowing the provider to choose a matching local address.
 *
 * @author mf  2011.06.13
 *
 * @since Coherence 3.7.1
 */
public class ConfigurableLocalAddressProvider
        extends ConfigurableAddressProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an instance of ConfigurableLocalAddressProvider based on the
     * specified XML element.
     * <p>
     * Unresolvable addresses will be skipped.
     *
     * @param xmlConfig  the XML element that contains the configuration info
     */
    @Deprecated
    public ConfigurableLocalAddressProvider(XmlElement xmlConfig)
        {
        super(xmlConfig);
        }

    /**
     * Constructs a ConfigurableLocalAddressProvider using the specified
     * {@link ConfigurableAddressProvider.AddressHolder}s.
     *
     * @param addressHolders  the {@link ConfigurableAddressProvider.AddressHolder}s
     * @param fSafe           true if the provider skips unresolved addresses
     */
    public ConfigurableLocalAddressProvider(Iterable<AddressHolder> addressHolders, boolean fSafe)
        {
        super(addressHolders, fSafe);
        }

    /**
     * Constructs a ConfigurableLocalAddressProvider for all local IPs and the given port.
     */
    public ConfigurableLocalAddressProvider(int nPort)
        {
        super(new Iterable<AddressHolder>()
            {
            @Override
            public Iterator<AddressHolder> iterator()
                {
                return new Iterator<AddressHolder>()
                    {
                    @Override
                    public boolean hasNext()
                        {
                        return f_delegate.hasNext();
                        }

                    @Override
                    public AddressHolder next()
                        {
                        return new AddressHolder(f_delegate.next().getHostAddress(), nPort);
                        }

                    final Iterator<InetAddress> f_delegate = InetAddresses.getAllLocalAddresses().iterator();
                    };
                }
            }, true);
        }

    /**
     * Construct an instance of ConfigurableLocalAddressProvider based on the
     * specified XML element.
     *
     * @param xmlConfig  the XML element that contains the configuration info
     * @param fSafe      true if the provider is skips unresolved addresses
     */
    @Deprecated
    public ConfigurableLocalAddressProvider(XmlElement xmlConfig, boolean fSafe)
        {
        super(xmlConfig, fSafe);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected Iterator<InetSocketAddress> resolveAddress(final String sHost, final int nPort)
        {
        try
            {
            return super.resolveAddress(InetAddressHelper.getLocalAddress(sHost).getHostAddress(), nPort);
            }
        catch (UnknownHostException e)
            {
            throw new IllegalArgumentException(e.getMessage(), e);
            }
        }

    /**
     * Return a builder which will produce Providers for all local addresses and the specified port
     *
     * @param nPort  the port
     *
     * @return the builder
     */
    public static AddressProviderBuilder builder(int nPort)
        {
        return new AddressProviderBuilder()
            {
            @Override
            public AddressProvider createAddressProvider(ClassLoader loader)
                {
                return new ConfigurableLocalAddressProvider(nPort);
                }

            @Override
            public AddressProvider realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
                {
                return new ConfigurableLocalAddressProvider(nPort);
                }
            };
        }
    }
