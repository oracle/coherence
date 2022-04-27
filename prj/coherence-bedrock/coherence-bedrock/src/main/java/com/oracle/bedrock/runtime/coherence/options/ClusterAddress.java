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

public class ClusterAddress
        implements Profile, Option
    {
    /**
     * The tangosol.coherence.clusteraddress property.
     */
    public static final String PROPERTY = "coherence.clusteraddress";

    /**
     * The cluster address for a {@link CoherenceClusterMember}.
     * <p>
     * When <code>null</code> the cluster port will automatically be chosen
     * based on the {@link Platform}.
     */
    private String address;


    /**
     * Constructs a {@link ClusterAddress} using the specified address.
     *
     * @param address the address
     */
    private ClusterAddress(String address)
        {
        this.address = address;
        }


    /**
     * Obtains a {@link ClusterAddress} for a specified address.
     *
     * @param address the address for a {@link ClusterAddress}
     * @return a {@link ClusterAddress} for the specified address
     */

    public static ClusterAddress of(String address)
        {
        return new ClusterAddress(address);
        }


    /**
     * Obtains a {@link ClusterAddress} that is automatically chosen at runtime.
     *
     * @return a {@link ClusterAddress} that is automatically chosen
     */
    @OptionsByType.Default
    public static ClusterAddress automatic()
        {
        return new ClusterAddress(null);
        }


    @Override
    public void onLaunching(
            Platform platform,
            MetaClass metaClass,
            OptionsByType optionsByType)
        {
        if (address != null)
            {
            SystemProperties systemProperties = optionsByType.get(SystemProperties.class);

            if (systemProperties != null)
                {
                optionsByType.add(SystemProperty.of(PROPERTY, address));
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

        if (!(o instanceof ClusterAddress))
            {
            return false;
            }

        ClusterAddress that = (ClusterAddress) o;

        return address.equals(that.address);

        }


    @Override
    public int hashCode()
        {
        return address.hashCode();
        }

    @Override
    public String toString()
        {
        return "ClusterAddress(" +
                "'" + address + '\'' +
                ')';
        }
    }
