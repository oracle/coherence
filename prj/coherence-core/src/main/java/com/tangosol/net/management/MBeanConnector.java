/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.dev.tools.CommandLineTool;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

import com.tangosol.util.ClassHelper;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import com.oracle.coherence.common.base.Blocking;
import javax.management.MBeanServer;

import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;


/**
* Utility class to expose Coherence JMX MBeans via the Sun JMX reference
* implementation <tt>HtmlAdaptorServer</tt> or a JMX Remote
* {@link JMXConnectorServer}.
* <p>
* In order to use this class, the following system property may need to be set
* on the monitored cluster nodes (i.e. cache servers):
* <pre>
*   -Dcoherence.management.remote=true
* </pre>
*
* The following properties may be used for configuration of the monitoring node:
* <table>
*   <caption>Monitoring properties</caption>
*   <tr>
*     <td valign="top"><tt>coherence.management</tt></td>
*     <td valign="top">Specifies if a cluster node's JVM has an in-process MBeanServer
 *    and if this node allows management of other nodes' managed objects. Use
 *    <b>all</b> to specify management of this and all other remotely manageable
 *    cluster nodes</td>
*   </tr>
*   <tr>
*     <td valign="top"><tt>coherence.management.remote.host</tt></td>
*     <td valign="top">The host that the JMX server will bind to. Default is localhost.</td>
*   </tr>
*   <tr>
*     <td valign="top"><tt>coherence.management.remote.registryport</tt></td>
*     <td valign="top">The port used for the JMX RMI registry. Default is 9000.</td>
*   </tr>
*   <tr>
*     <td valign="top"><tt>coherence.management.remote.connectionport</tt></td>
*     <td valign="top">The port used for the JMX RMI connection. Default is ephemeral.</td>
*   </tr>
*   <tr>
*     <td valign="top"><tt>coherence.management.remote.httpport</tt></td>
*     <td valign="top">The port used for the HTTP connection. Default is 8888.</td>
*   </tr>
* </table>
*
* @since Coherence 3.3
* @author pperalta 2007.02.01
*/
public class MBeanConnector
        extends MBeanHelper
    {
    /**
    * Start a Coherence cluster node that enables JMX agents such as a web
    * browser or JConsole to connect to the in-process JMX MBeanServer and
    * manage Coherence MBeans.
    * <p>
    * Command line usage:
    * <p>
    *  <tt>java com.tangosol.net.management.MBeanConnector
    * [-rmi] and/or [-http]</tt>
    * <p>
    * Use the following command to connect via JConsole when using the
    * <tt>-rmi</tt> flag:
    * <p>
    * <tt> jconsole service:jmx:rmi://[host]:[RMI Connection
    * port]/jndi/rmi://[host]:[RMI Registry port]/server </tt>
    * <p>
    * To connect via a web browser when using the <tt>-http</tt> flag:
    * <p>
    * <tt>http://[host]:[HTTP port]</tt>
    *
    * @param asArg  the command line arguments
    *
    * @throws Exception if an error occurs
    */
    public static void main(String[] asArg)
            throws Exception
        {
        Map mapArgs = CommandLineTool.parseArguments(asArg, VALID_COMMANDS, false);

        boolean fRmi  = mapArgs.keySet().contains(RMI_COMMAND_LINE_ARG);
        boolean fHttp = mapArgs.keySet().contains(HTTP_COMMAND_LINE_ARG);
        boolean fHelp = mapArgs.keySet().contains(HELP_COMMAND_LINE_ARG);

        if (!fRmi && !fHttp || fHelp)
            {
            showUsage();
            }
        else
            {
            new MBeanConnector().start(fRmi, fHttp);
            }
        }

    /**
    * Start the RMI and/or HTTP agents.
    *
    * @param fRmi   if true starts the RMI agent
    * @param fHttp  if true starts the HTTP agent
    */
    public void start(boolean fRmi, boolean fHttp)
        {
        Cluster            cluster    = CacheFactory.ensureCluster();
        JMXConnectorServer rmiServer  = null;
        Object             httpServer = null;

        if (fRmi)
            {
            try
                {
                out("Starting RMI Connector...");
                rmiServer = startRmiConnector();
                out("RMI Connector started");
                }
            catch (Exception e)
                {
                out("Could not start RMI connector");
                out(e);
                }
            }

        if (fHttp)
            {
            try
                {
                out("Starting HTTP Connector...");
                httpServer = startHttpConnector();
                out("HTTP Connector started");
                }
            catch (Exception e)
                {
                out("Could not start HTTP Connector");
                out(e);
                }
            }

        if (rmiServer == null && httpServer == null)
            {
            // neither service started up, so return
            return;
            }

        while (cluster.isRunning())
            {
            try
                {
                Blocking.sleep(1000);
                }
            catch (InterruptedException e)
                {
                // we don't know what effect setting the interrupt flag will
                // have on the stop() methods below; since this is the main
                // thread it would be safer not to set the flag
                // Thread.currentThread().interrupt();
                break;
                }
            }

        if (rmiServer != null)
            {
            try
                {
                rmiServer.stop();
                }
            catch (IOException e)
                {
                // this exception can be ignored
                }
            }

        if (httpServer != null)
            {
            try
                {
                ClassHelper.invoke(httpServer, "stop", ClassHelper.VOID_PARAMS);
                }
            catch (Exception e)
                {
                // this exception can be ignored
                }
            }
        }

    /**
    * Starts the RMI connector using the values of the <tt>RMI_*</tt> system
    * properties.
    *
    * @return a JMXConnectorServer that has been started
    *
    * @see MBeanHelper#startRmiConnector(String, int, int, MBeanServer, Map)
    * @see #RMI_HOST_PROPERTY
    * @see #RMI_REGISTRY_PORT_PROPERTY
    * @see #RMI_CONNECTION_PORT_PROPERTY
    * @see #DEFAULT_RMI_HOST
    * @see #DEFAULT_RMI_REGISTRY_PORT
    * @see #DEFAULT_RMI_CONNECTION_PORT
    *
    * @throws IOException if an I/O error occurs
    */
    public JMXConnectorServer startRmiConnector()
            throws IOException
        {
        return startRmiConnector(getHostName(), getRegistryPort(), getConnectionPort(),
                findMBeanServer(), getRMIConnectorAttributes());
        }

    /**
     * Get the configured host name for the RMI Connector Server.
     *
     * @return the configured host name for the RMI Connector Server
     */
   public static String getHostName()
       {
       return Config.getProperty(RMI_HOST_PROPERTY, DEFAULT_RMI_HOST);
       }

    /**
     * Get the configured registry port for the RMI Connector Server.
     *
     * @return the configured registry port for the RMI Connector Server
     */
    public static int getRegistryPort()
        {
        String sConPort = Config.getProperty(RMI_REGISTRY_PORT_PROPERTY);
        try
            {
            return sConPort == null ? DEFAULT_RMI_REGISTRY_PORT : Integer.parseInt(sConPort);
            }
        catch (NumberFormatException e)
            {
            throw ensureRuntimeException(e, "Illegal " +
                RMI_REGISTRY_PORT_PROPERTY + " system property value " + sConPort);
            }
        }

    /**
     * Get the configured connection port for the RMI Connector Server.
     *
     * @return  the configured connection port for the RMI Connector Server
     */
    public static int getConnectionPort()
        {
        String sConPort = Config.getProperty(RMI_CONNECTION_PORT_PROPERTY);
        try
            {
            return sConPort == null ? DEFAULT_RMI_CONNECTION_PORT : Integer.parseInt(sConPort);
            }
        catch (NumberFormatException e)
            {
            throw ensureRuntimeException(e, "Illegal " +
                RMI_CONNECTION_PORT_PROPERTY + " system property value " + sConPort);
            }
        }

    /**
     * Get the max configured connection port for the RMI Connector Server.
     *
     * @return  the max configured connection port for the RMI Connector Server
     */
    public static int getConnectionPortMax()
        {
        String sConPort = Config.getProperty(RMI_CONNECTION_PORT_ADJUST_PROPERTY);
        try
            {
            return sConPort == null ? 65535 : Integer.parseInt(sConPort);
            }
        catch (NumberFormatException e)
            {
            throw ensureRuntimeException(e, "Illegal " +
                    RMI_CONNECTION_PORT_ADJUST_PROPERTY + " system property value " + sConPort);
            }
        }

    /**
     * Get the attributes for the RMI Connector Server.
     *
     * @return a set of attributes to control the RMI connector server's
     *         behavior
     */
    public static Map getRMIConnectorAttributes()
        {
        String sAuthenticator = Config.getProperty(RMI_CUSTOM_AUTHENTICATOR_PROPERTY);
        if (sAuthenticator != null)
            {
            try
                {
                Class            cls              = Class.forName(sAuthenticator);
                JMXAuthenticator jmxAuthenticator = (JMXAuthenticator) cls.newInstance();
                Map              mapAttr          = new HashMap();
                mapAttr.put(JMXConnectorServer.AUTHENTICATOR, jmxAuthenticator);
                return mapAttr;
                }
            catch (Throwable e)
                {
                Logger.err("Failed to instantiate custom JMXAuthenticator class " + sAuthenticator, e);
                }
            }
        return null;
        }

    /**
    * Starts the HTTP connector using the values of the <tt>HTTP_*</tt> system
    * properties.
    *
    * @return a <tt>com.sun.jdmk.comm.HtmlAdaptorServer</tt> that has been started
    *
    * @see MBeanHelper#startHttpConnector(int, MBeanServer)
    * @see #HTTP_PORT_PROPERTY
    * @see #DEFAULT_HTTP_PORT
    */
    public Object startHttpConnector()
        {
        String sPort = Config.getProperty(HTTP_PORT_PROPERTY);
        int    nPort;

        try
            {
            nPort = sPort == null ? DEFAULT_HTTP_PORT : Integer.parseInt(sPort);
            }
        catch (NumberFormatException e)
            {
            throw ensureRuntimeException(e, "Illegal " + DEFAULT_HTTP_PORT +
                " system property value " + sPort);
            }

        MBeanServer mbs = findMBeanServer();

        return startHttpConnector(nPort, mbs);
        }

    /**
    * Output usage instructions.
    */
    public static void showUsage()
        {
        out();
        out("java com.tangosol.net.management.MBeanConnector [-rmi] [-http]");
        out();
        out("command option descriptions:");
        out("\t-rmi  (optional) start a JMX RMI server. The following properties may be set to configure ports:");
        out("\t -Dcoherence.management.remote.registryport (default is 9000)");
        out("\t -Dcoherence.management.remote.connectionport (default is 3000)");
        out();
        out("\t-http (optional) start a JMX HTTP server. The following property may be set to configure the HTTP port:");
        out("\t -Dcoherence.management.remote.httpport (default is 8888)");
        out();
        out("Note that at least one protocol (either rmi or http) must be selected.");
        }


    // ----- constants ------------------------------------------------------

    /**
    * Command line argument used to output usage.
    */
    public static final String HELP_COMMAND_LINE_ARG = "help";

    /**
    * Command line argument to start RMI server.
    */
    public static final String RMI_COMMAND_LINE_ARG = "rmi";

    /**
    * Command line argument to start HTTP server.
    */
    public static final String HTTP_COMMAND_LINE_ARG = "http";

    /**
    * Array that contains all valid command line arguments.
    */
    public static final String[] VALID_COMMANDS =
        {
        RMI_COMMAND_LINE_ARG,
        HTTP_COMMAND_LINE_ARG,
        HELP_COMMAND_LINE_ARG
        };

    /**
    * Property name to specify the RMI host.
    */
    public static final String RMI_HOST_PROPERTY =
            "coherence.management.remote.host";

    /**
    * Property name to specify RMI connection port.
    */
    public static final String RMI_CONNECTION_PORT_PROPERTY =
            "coherence.management.remote.connectionport";

    /**
     * Property name to specify RMI connection max port.
     */
    public static final String RMI_CONNECTION_PORT_ADJUST_PROPERTY =
            "coherence.management.remote.connectionport.adjust";

    /**
    * Property name to specify RMI registry port.
    */
    public static final String RMI_REGISTRY_PORT_PROPERTY =
            "coherence.management.remote.registryport";

    /**
    * Property name to specify custom RMI Authenticator.
    */
    public static final String RMI_CUSTOM_AUTHENTICATOR_PROPERTY =
             "coherence.management.remote.authenticator";

    /**
    * Property name to specify HTTP port.
    */
    public static final String HTTP_PORT_PROPERTY =
            "coherence.management.remote.httpport";

    /**
    * Default RMI host (wildcard).
    */
    public static final String DEFAULT_RMI_HOST = "0.0.0.0";

    /**
    * Default RMI connection port (0 ephemeral).
    */
    public static final int DEFAULT_RMI_CONNECTION_PORT = 0;

    /**
    * Default RMI registry port (9000).
    */
    public static final int DEFAULT_RMI_REGISTRY_PORT = 9000;

    /**
    * Default HTTP port (8888).
    */
    public static final int DEFAULT_HTTP_PORT = 8888;
    }
