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

import java.util.Objects;

/**
 * An option to configure Coherence well known addresses.
 */
public class WellKnownAddress
        implements Profile, Option
    {
    /**
     * Constructs a {@link WellKnownAddress}.
     *
     * @param address the address
     */
    private WellKnownAddress(String address)
        {
        m_sAddress = address;
        }

    /**
     * Obtains the address of the {@link WellKnownAddress}.
     *
     * @return the address of the {@link WellKnownAddress}
     */
    public String getsAddress()
        {
        return m_sAddress;
        }

    /**
     * Obtains a {@link WellKnownAddress} that uses the loopback address.
     *
     * @return a {@link WellKnownAddress} that uses the loopback address
     */
    public static WellKnownAddress loopback()
        {
        return new WellKnownAddress("127.0.0.1");
        }

    /**
     * Obtains a {@link WellKnownAddress} for a specified address.
     *
     * @param address the address of the {@link WellKnownAddress}
     * @return a {@link WellKnownAddress} for the specified address
     */
    public static WellKnownAddress of(String address)
        {
        return new WellKnownAddress(address);
        }

    @Override
    public void onLaunching(Platform platform, MetaClass metaClass, OptionsByType optionsByType)
        {
        SystemProperties systemProperties = optionsByType.get(SystemProperties.class);

        if (systemProperties != null)
            {
            optionsByType.add(SystemProperty.of(PROPERTY, m_sAddress));
            }
        }

    @Override
    public void onLaunched(Platform platform, Application application, OptionsByType optionsByType)
        {
        }

    @Override
    public void onClosing(Platform platform, Application application, OptionsByType optionsByType)
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
        WellKnownAddress that = (WellKnownAddress) o;
        return Objects.equals(m_sAddress, that.m_sAddress);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_sAddress);
        }

    @Override
    public String toString()
        {
        return "WellKnownAddress(" + m_sAddress + "')";
        }

    // ----- data members ---------------------------------------------------

    /**
     * The tangosol.coherence.wka property.
     */
    public static final String PROPERTY = "coherence.wka";

    /**
     * The tangosol.coherence.wka.port property.
     */
    public static final String PROPERTY_PORT = "coherence.wka.port";

    /**
     * The well known address of an {@link CoherenceClusterMember}.
     */
    private final String m_sAddress;
    }
