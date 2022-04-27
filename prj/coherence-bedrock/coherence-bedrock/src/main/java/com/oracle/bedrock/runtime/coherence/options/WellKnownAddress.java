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

import java.util.Iterator;
import java.util.Objects;

public class WellKnownAddress
        implements Profile, Option
    {
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
    private final String address;

    /**
     * Constructs a {@link WellKnownAddress}.
     *
     * @param address the address
     */
    private WellKnownAddress(String address)
        {
        this.address = address;
        }

    /**
     * Obtains the address of the {@link WellKnownAddress}.
     *
     * @return the address of the {@link WellKnownAddress}
     */
    public String getAddress()
        {
        return address;
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
            optionsByType.add(SystemProperty.of(PROPERTY, address));
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

        if (!(o instanceof WellKnownAddress))
            {
            return false;
            }

        WellKnownAddress that = (WellKnownAddress) o;

        return !Objects.equals(address, that.address);
        }


    @Override
    public int hashCode()
        {
        return address != null ? address.hashCode() : 0;
        }


    @Override
    public String toString()
        {
        return "WellKnownAddress(" + address + "')";
        }
    }
