/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.bedrock.runtime.coherence.options;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.util.Capture;
import com.oracle.bedrock.util.PerpetualIterator;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Objects;

/**
 * An option to configure Coherence to run in localhost mode.
 */
public class LocalHost
        implements Profile, Option
    {
    /**
     * Constructs a {@link LocalHost} for the specified address and ports.
     * <p>
     * When both the address and ports are <code>null</code>, the {@link LocalHost}
     * will be configured for "local only mode".
     * </p>
     *
     * @param address the address of the {@link LocalHost}
     * @param ports   the possible ports for the {@link LocalHost}
     */
    private LocalHost(String address, Iterator<Integer> ports)
        {
        m_sAddress = address;
        m_ports    = ports;
        }


    /**
     * Obtains the address of the {@link LocalHost}, <code>null</code> if not defined
     *
     * @return the address of the {@link LocalHost}
     */
    public String getAddress()
        {
        return m_sAddress;
        }

    /**
     * Obtains the possible ports of the {@link LocalHost}, <code>null</code>  if not defined.
     *
     * @return the possible ports of the {@link LocalHost}
     */
    public Iterator<Integer> getPorts()
        {
        return m_ports;
        }

    /**
     * Obtains a {@link LocalHost} using the loopback address.
     *
     * @return a {@link LocalHost} using the loopback address
     */
    public static LocalHost loopback()
        {
        return new LocalHost("127.0.0.1", null);
        }

    /**
     * Obtains a {@link LocalHost} for a specified address.
     *
     * @param address the address of the {@link LocalHost}
     * @return a {@link LocalHost} for the specified address
     */
    public static LocalHost of(String address)
        {
        return new LocalHost(address, null);
        }

    /**
     * Obtains a {@link LocalHost} for a specified address and port.
     *
     * @param address the address of the {@link LocalHost}
     * @param port    the port of the {@link LocalHost}
     * @return a {@link LocalHost} for the specified address and port
     */
    public static LocalHost of(
            String address,
            int port)
        {
        return new LocalHost(address, new PerpetualIterator<>(port));
        }

    /**
     * Obtains a {@link LocalHost} for a specified address and port.
     *
     * @param address the address of the {@link LocalHost}
     * @param port    the port of the {@link LocalHost}
     * @return a {@link LocalHost} for the specified address and port
     */
    public static LocalHost of(
            String address,
            Capture<Integer> port)
        {
        return new LocalHost(address, port);
        }

    /**
     * Obtains a {@link LocalHost} for a specified address and ports.
     *
     * @param address the address of the {@link LocalHost}
     * @param ports   the ports of the {@link LocalHost}
     * @return a {@link LocalHost} for the specified address and ports
     */
    public static LocalHost of(
            String address,
            Iterator<Integer> ports)
        {
        return new LocalHost(address, ports);
        }

    /**
     * Obtains a {@link LocalHost} for a specified address and ports.
     *
     * @param address the address of the {@link LocalHost}
     * @param ports   the ports of the {@link LocalHost}
     * @return a {@link LocalHost} for the specified address and ports
     */
    public static LocalHost of(
            String address,
            AvailablePortIterator ports)
        {
        return new LocalHost(address, ports);
        }

    /**
     * Obtains a {@link LocalHost} configured for "local host only" mode.
     *
     * @return a {@link LocalHost} for local-only mode
     */
    public static LocalHost only()
        {
        return new LocalHost(null, null);
        }

    @Override
    public void onLaunching(
            Platform platform,
            MetaClass metaClass,
            OptionsByType optionsByType)
        {
        if (m_ports != null && !m_ports.hasNext())
            {
            throw new IllegalStateException("Exhausted the available ports for the LocalHost");
            }
        else
            {
            SystemProperties systemProperties = optionsByType.get(SystemProperties.class);

            if (systemProperties != null)
                {
                if (m_sAddress == null && m_ports == null)
                    {
                    // setup local-only mode
                    optionsByType.add(SystemProperty.of(PROPERTY, InetAddress.getLoopbackAddress().getHostAddress()));

                    // set TTL to 0
                    optionsByType.add(SystemProperty.of("coherence.ttl", "0"));
                    }
                else
                    {
                    if (m_sAddress != null)
                        {
                        optionsByType.add(SystemProperty.of(PROPERTY, m_sAddress));
                        }

                    if (m_ports != null)
                        {
                        optionsByType.add(SystemProperty.of(PROPERTY_PORT, m_ports.next()));
                        }
                    }
                }
            }
        }

    @Override
    public void onLaunched(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        }

    @Override
    public void onClosing(
            Platform platform,
            Application application,
            OptionsByType optionsByType)
        {
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        LocalHost localHost = (LocalHost) o;
        return Objects.equals(m_sAddress, localHost.m_sAddress) && Objects.equals(m_ports, localHost.m_ports);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_sAddress, m_ports);
        }

    @Override
    public String toString()
        {
        return "LocalHost(" +
                "address='" + m_sAddress + '\'' +
                ", ports=" + m_ports +
                ')';
        }

    // ----- data members ---------------------------------------------------
    
    /**
     * The coherence.localhost property.
     */
    public static final String PROPERTY = "coherence.localhost";

    /**
     * The coherence.localport property.
     */
    public static final String PROPERTY_PORT = "coherence.localport";

    /**
     * The local address of a {@link CoherenceClusterMember}, or
     * {@code null} when not defined.
     */
    private final String m_sAddress;

    /**
     * The local address port for a {@link CoherenceClusterMember}, or
     * {@code null} when not defined.
     */
    private final Iterator<Integer> m_ports;
    }
