/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import javax.management.MBeanServer;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import java.io.IOException;

import java.lang.management.ManagementFactory;

/**
 * An implementation of a Coherence {@link MBeanServerFinder}
 * that creates a {@link JMXConnectorServer} server that uses
 * JMXMP as its transport rather than RMI. This allows JMX
 * to be visible from inside a container that uses NAT'ing.
 * <p>
 * The JMXMP server can be enabled by setting the
 * {@code coherence.management.serverfactory} system property
 * to the fully qualified name of this class, or by specifically
 * configuring it in the management section of the operational
 * configuration file.
 *
 * @author Jonathan Knight 2022.04.22
 * @since 22.06
 */
public class JmxmpServer
        implements MBeanServerFinder
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link JmxmpServer} that binds to the any local address.
     */
    public JmxmpServer()
        {
        this("0.0.0.0");
        }

    /**
     * Create a {@link JmxmpServer} that binds to the specified address.
     *
     * @param address the address to listen on
     */
    public JmxmpServer(String address)
        {
        this.address = address;
        }

    // ----- MBeanServerFinder methods --------------------------------------

    @Override
    public MBeanServer findMBeanServer(String s)
        {
        return ensureServer(address).getMBeanServer();
        }

    @Override
    public JMXServiceURL findJMXServiceUrl(String s)
        {
        return jmxServiceURL;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the JMXMP protocol {@link JMXConnectorServer} instance, creating the instance of the connector server if
     * one does not already exist.
     *
     * @param address the address to listen on
     * @return the JMXMP protocol {@link JMXConnectorServer} instance.
     */
    private static synchronized JMXConnectorServer ensureServer(String address)
        {
        try
            {
            if (connectorServer == null)
                {
                MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                int         nPort  = Config.getInteger("coherence.jmxmp.port", 9000);

                jmxServiceURL   = new JMXServiceURL("jmxmp", address, nPort);
                connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(jmxServiceURL, null, server);

                connectorServer.start();

                Logger.info("Started JMXMP connector " + connectorServer.getAddress());
                }

            return connectorServer;
            }
        catch (IOException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The JMXServiceURL for the MBeanConnector used by the Coherence JMX framework.
     */
    private static JMXServiceURL jmxServiceURL;

    /**
     * The {@link JMXConnectorServer} using the JMXMP protocol.
     */
    private static JMXConnectorServer connectorServer;

    /**
     * The address to bind to.
     */
    private final String address;
    }
