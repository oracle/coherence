/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.oracle.coherence.common.net.InetAddresses;

import com.tangosol.coherence.config.ParameterList;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.AddressProvider;

import com.tangosol.run.xml.XmlElement;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * This class builds an AddressProviderBuilder from a local address.
 * All ConfigurationExceptions are deferred until realization time.
 *
 * @author jf  2015.02.26
 * @since Coherence 12.2.1
 */
public class LocalAddressProviderBuilder
        implements AddressProviderBuilder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Helper constructor for all public constructors to use
     * @param addr       resolved {@link InetAddress}
     * @param sAddr      IP address or hostname
     * @param nPortMin   minimum port value, or -1 to use ephemeral sub-ports
     * @param nPortMax   maximum port value
     * @param xmlConfig  optional xml config
     */
    private LocalAddressProviderBuilder(final InetAddress addr, final String sAddr,
                                        final int nPortMin, final int nPortMax, final XmlElement xmlConfig)
        {
        assert(m_addr == null || m_sAddr == null);
        m_addr             = addr;
        m_sAddr            = sAddr;
        m_nPortMin         = nPortMin;
        m_nPortMinOriginal = nPortMin;
        m_nPortMax         = nPortMax;
        m_xmlConfig        = xmlConfig;
        }

    /**
     * Constructs {@link LocalAddressProviderBuilder} with a resolved address.
     *
     * @param addr      the local address, or null
     * @param nPortMin  the minimum port to use, or -1 to use ephemeral sub-ports
     * @param nPortMax  the maximum port to use
     */
    public LocalAddressProviderBuilder(final InetAddress addr, final int nPortMin, final int nPortMax)
        {
        this(addr, null, nPortMin, nPortMax, null);
        }

    /**
     * Constructs {@link LocalAddressProviderBuilder} deferring address resolution until realized.
     *
     * @param sAddr      the local address, or null
     * @param nPortMin   the minimum port to use, or -1 to use ephemeral sub-ports
     * @param nPortMax   the maximum port to use
     * @param xmlConfig  optional xml to used in ConfigurationException if this is invalid.
     */
    public LocalAddressProviderBuilder(final String sAddr, final int nPortMin, final int nPortMax, XmlElement xmlConfig)
        {
        this(null, sAddr, nPortMin, nPortMax, xmlConfig);
        }

    /**
     * Constructs {@link LocalAddressProviderBuilder} deferring address resolution until realized.
     *
     * @param sAddr     the local address, or null
     * @param nPortMin  the minimum port to use, or -1 to use ephemeral sub-ports
     * @param nPortMax  the maximum port to use
     */
    public LocalAddressProviderBuilder(final String sAddr, final int nPortMin, final int nPortMax)
        {
        this(sAddr, nPortMin, nPortMax, null);
        }

    // ----- ParameterizedBuilder methods -----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public AddressProvider realize(ParameterResolver resolver, ClassLoader loader, ParameterList listParameters)
        {
        resolveAddress();

        validate();

        return new AddressProvider()
            {
            @Override
            public InetSocketAddress getNextAddress()
                {
                if (m_nPort > m_nPortMax)
                    {
                    m_nPort = m_nPortMin;

                    return null;
                    }

                return new InetSocketAddress(m_addr, m_nPort++);
                }

            @Override
            public void accept()
                {
                m_nPort = m_nPortMin;
                }

            @Override
            public void reject(Throwable eCause)
                {
                // no-op
                }

            @Override
            public String toString()
                {
                return "LocalAddressProvider[" + m_addr + ":" + m_nPortMin
                       + (m_nPortMin == m_nPortMax ? "]" : " .. " + m_nPortMax + "]");
                }

            int m_nPort = m_nPortMin;
            };
        }

    // ----- AddressProviderFactory methods ---------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public AddressProvider createAddressProvider(ClassLoader loader)
        {
        return realize(null, loader, null);
        }

    // ----- LocalAddressProviderBuilder methods ----------------------------

    /**
     * Resolve the host address if provided as a string and cache the value.
     * <p>
     * May return null.
     *
     * @return the resolved host address
     */
    public InetAddress resolveAddress()
        {
        if (m_addr == null && m_sAddr != null && !m_sAddr.isEmpty())
            {
            try
                {
                m_addr = InetAddresses.getLocalAddress(m_sAddr);
                }
            catch (UnknownHostException e)
                {
                throw new IllegalArgumentException(e);
                }
            }
        return m_addr;
        }

    /**
     * Set the address to use for any realized {@link AddressProvider}'s
     *
     * @param addr  the {@link InetAddress}
     *
     * @return this {@link LocalAddressProviderBuilder}
     */
    public LocalAddressProviderBuilder setAddress(InetAddress addr)
        {
        m_addr = addr;
        return this;
        }

    /**
     * Return the minimum port number.
     *
     * @return the minimum port number
     */
    public int getPortMin()
        {
        return m_nPortMin;
        }

    /**
     * Set the minimum port number.
     *
     * @param nPort  the minimum port number
     *
     * @return this {@link LocalAddressProviderBuilder}
     */
    public LocalAddressProviderBuilder setPortMin(int nPort)
        {
        m_nPortMin = nPort;
        return this;
        }

    /**
     * Return the minimum port number provided in the constructor.
     *
     * @return the minimum port number
     */
    public int getPortMinOriginal()
        {
        return m_nPortMinOriginal;
        }

    /**
     * Set the maximum port number.
     *
     * @param nPort  the maximum port number
     *
     * @return this {@link LocalAddressProviderBuilder}
     */
    public LocalAddressProviderBuilder setPortMax(int nPort)
        {
        m_nPortMax = nPort;
        return this;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Validate builder values.
     */
    private void validate()
        {
        if (m_nPortMin < MIN_PORT || m_nPortMin > MAX_PORT)
            {
            throw new ConfigurationException("Invalid <local-address> configuration [" + m_xmlConfig + "]",
                                             "Please specify a valid <port>" + ", invalid value is " + m_nPortMin);
            }
        else if (m_nPortMax < m_nPortMin || m_nPortMax > MAX_PORT)
            {
            throw new ConfigurationException("Invalid <local-address> configuration [" + m_xmlConfig + "]",
                                             "Please specify a valid <port-auto-adjust>, port-auto-adjust is " +
                                                     m_nPortMax + " and port value is " + m_nPortMin);
            }
        }

    // ----- constants ------------------------------------------------------

    public static final int MIN_PORT = 0;

    public static final int MAX_PORT = 0xFFFF;

    // ----- data members ---------------------------------------------------

    /**
     * The address in string format or unresolved hostname.
     */
    private String      m_sAddr;

    /**
     * The resolved address.
     */
    private InetAddress m_addr;

    /**
     * The maximum port number.
     */
    private int         m_nPortMax;

    /**
     * The minimum port number.
     */
    private int         m_nPortMin;

    /**
     * The minimum port number as specified in the constructor.
     */
    private int         m_nPortMinOriginal;

    /**
     * An optional XML configuration info used only to report configuration exceptions.
     */
    private XmlElement  m_xmlConfig = null;
    }
