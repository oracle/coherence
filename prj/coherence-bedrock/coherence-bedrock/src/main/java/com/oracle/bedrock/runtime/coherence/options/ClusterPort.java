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
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.MetaClass;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.Profile;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.java.JavaVirtualMachine;
import com.oracle.bedrock.runtime.java.options.SystemProperties;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.util.Capture;
import com.oracle.bedrock.util.PerpetualIterator;

import java.util.Iterator;

public class ClusterPort
        implements Profile, Option
    {
    /**
     * The tangosol.coherence.clusterport property.
     */
    public static final String PROPERTY = "coherence.clusterport";

    /**
     * The cluster port for a {@link CoherenceClusterMember}.
     * <p>
     * When <code>null</code> the cluster port will automatically be chosen
     * based on the {@link Platform}.
     */
    private Iterator<Integer> ports;


    /**
     * Constructs a {@link ClusterPort} for the specified port.
     *
     * @param ports the ports
     */
    private ClusterPort(Iterator<Integer> ports)
        {
        this.ports = ports;
        }


    /**
     * Obtains the possible values of the {@link ClusterPort}, returning
     * <code>null</code> if it is to be automatically chosen.
     *
     * @return the possible values of the {@link ClusterPort}
     */
    public Iterator<Integer> getPorts()
        {
        return ports;
        }


    /**
     * Obtains a {@link ClusterPort} for a specified port(s).
     *
     * @param ports the possible ports for a {@link ClusterPort}
     * @return a {@link ClusterPort} for the specified port(s)
     */
    public static ClusterPort from(Iterator<Integer> ports)
        {
        return new ClusterPort(ports);
        }


    /**
     * Obtains a {@link ClusterPort} for a specified port.
     *
     * @param port the  port for a {@link ClusterPort}
     * @return a {@link ClusterPort} for the specified port
     */

    public static ClusterPort of(int port)
        {
        return new ClusterPort(new PerpetualIterator<>(port));
        }


    /**
     * Obtains a {@link ClusterPort} for a specified port(s).
     *
     * @param ports the possible ports for a {@link ClusterPort}
     * @return a {@link ClusterPort} for the specified port(s)
     */
    public static ClusterPort from(AvailablePortIterator ports)
        {
        return new ClusterPort(ports);
        }


    /**
     * Obtains a {@link ClusterPort} that is automatically chosen at runtime.
     *
     * @return a {@link ClusterPort} that is automatically chosen
     */
    public static ClusterPort automatic()
        {
        return new ClusterPort(null);
        }


    /**
     * Obtains a {@link ClusterPort} for a specified port.
     *
     * @param port the  port for a {@link ClusterPort}
     * @return a {@link ClusterPort} for the specified port
     */
    public static ClusterPort of(Capture<Integer> port)
        {
        return new ClusterPort(port);
        }


    @Override
    public void onLaunching(
            Platform platform,
            MetaClass metaClass,
            OptionsByType optionsByType)
        {
        if (ports != null && !ports.hasNext())
            {
            throw new IllegalStateException("Exhausted the available ports for the ClusterPort");
            }
        else
            {
            SystemProperties systemProperties = optionsByType.get(SystemProperties.class);

            if (systemProperties != null)
                {
                int port;

                if (ports == null)
                    {
                    if (platform instanceof LocalPlatform || platform instanceof JavaVirtualMachine)
                        {
                        ports = new Capture<>(LocalPlatform.get().getAvailablePorts());
                        }
                    else
                        {
                        throw new IllegalStateException("Can't automatically determine a ClusterPort for the non-LocalPlatform, non-JavaVirtualMachine Platform :"
                                                                + platform.getName());
                        }
                    }

                // now acquire a port
                port = ports.next();

                optionsByType.add(SystemProperty.of(PROPERTY, port));
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

        if (!(o instanceof ClusterPort))
            {
            return false;
            }

        ClusterPort that = (ClusterPort) o;

        return ports.equals(that.ports);

        }


    @Override
    public int hashCode()
        {
        return ports.hashCode();
        }


    @Override
    public String toString()
        {
        return "ClusterPort(" + ports + ')';
        }
    }
