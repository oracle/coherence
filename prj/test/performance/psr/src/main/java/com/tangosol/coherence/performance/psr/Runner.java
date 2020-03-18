/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.psr;


import com.tangosol.dev.tools.CommandLineTool;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.OperationalContext;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Connection;
import com.tangosol.net.messaging.ConnectionEvent;
import com.tangosol.net.messaging.ConnectionInitiator;
import com.tangosol.net.messaging.ConnectionListener;
import com.tangosol.net.messaging.Message;
import com.tangosol.net.messaging.Protocol;
import com.tangosol.net.messaging.Request;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
* Worker process used to run tests using one or more execute threads.
* <p/>
* A Runner connects to a Console process using Extend TCP/IP and waits for
* tests commands to be sent by the Console. After receiving a test command,
* the Runner executes the command and sends a summary of the results back
* to the Console process.
*
* @author jh  2007.02.13
*/
public class Runner
        extends Base
        implements Runnable, Channel.Receiver, ConnectionListener
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new Runner that connects to a Console process listening on the
    * specified address and port.
    *
    * @param sAddr  the hostname or IP address to connect to
    * @param nPort  the port to connect to
    */
    public Runner(String sAddr, int nPort)
        {
        if (sAddr == null || sAddr.length() == 0)
            {
            sAddr = Console.DEFAULT_ADDRESS;
            }

        if (nPort <= 0)
            {
            nPort = Console.DEFAULT_PORT;
            }

        // load the template TCP/IP initiator configuration file
        XmlDocument xmlConfig = XmlHelper.loadXml(Console.class,
                "tcp-initiator-config.xml", null);

        XmlElement xml;

        // set the listen address and port
        xml = xmlConfig.ensureElement("tcp-initiator/remote-addresses/socket-address/address");
        xml.setString(sAddr);

        xml = xmlConfig.ensureElement("tcp-initiator/remote-addresses/socket-address/port");
        xml.setInt(nPort);

        // create and configure the TCP/IP initiator
        ConnectionInitiator initiator;
        try
            {
            initiator = (ConnectionInitiator) Class.forName(TCP_INITIATOR_CLASS).newInstance();
            initiator.setOperationalContext((OperationalContext) CacheFactory.getCluster());
            initiator.configure(xmlConfig);
            initiator.addConnectionListener(this);
            initiator.registerProtocol(RunnerProtocol.INSTANCE);
            }
        catch (Throwable e)
            {
            throw new RuntimeException("Error creating TCP/IP initiator", e);
            }

        m_initiator = initiator;
        }


    // ----- Runnable interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void run()
        {
        ConnectionInitiator initiator = getInitiator();
        initiator.start();

        m_connection = initiator.ensureConnection();
        m_connection.openChannel(RunnerProtocol.INSTANCE, "Runner", null, this, null);

        out("Runner started.");
        try
            {
            while (m_connection.isOpen())
                {
                synchronized (this)
                    {
                    this.wait();
                    }
                }
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            }
        finally
            {
            try
                {
                initiator.stop();
                }
            catch (RuntimeException e)
                {
                }
            m_connection = null;
            }
        out("Runner exited.");
        }


    // ----- Receiver interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String getName()
        {
        return "Runner";
        }

    /**
    * {@inheritDoc}
    */
    public Protocol getProtocol()
        {
        return RunnerProtocol.INSTANCE;
        }

    /**
    * {@inheritDoc}
    */
    public void registerChannel(Channel channel)
        {
        }

    /**
    * {@inheritDoc}
    */
    public void onMessage(Message message)
        {
        // REVIEW
        // obviously, if all clients had an execute thread pool this wouldn't
        // be necessary; once C++ and .NET have this feature, configure a
        // two execute threads and replace the following with message.run()
        if (message instanceof Request)
            {
            message.run();
            }
        else
            {
            Thread thread = new Thread(message, "Runner$ExecuteThread");
            thread.setDaemon(true);
            thread.start();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void unregisterChannel(Channel channel)
        {
        }


    // ----- ConnectionListener interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public synchronized void connectionOpened(ConnectionEvent evt)
        {
        notifyAll();
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void connectionClosed(ConnectionEvent evt)
        {
        notifyAll();
        }

    /**
    * {@inheritDoc}
    */
    public synchronized void connectionError(ConnectionEvent evt)
        {
        notifyAll();
        }


    // ----- entry point ----------------------------------------------------

    /**
    * Application entry point. Usage:
    * <pre>
    * java com.tangosol.coherence.psr.Runner -?
    * </pre>
    *
    * @param asArg  command line arguments
    */
    public static void main(String[] asArg)
        {
        String sAddr;
        int    nPort;

        try
            {
            // extract switches from the command line
            List<String> listArg      = new ArrayList<>(Arrays.asList(asArg));
            List<String> listSwitches = CommandLineTool.extractSwitches(listArg, VALID_SWITCHES);

            // show help, if necessary
            if (listSwitches.contains(SWITCH_HELP))
                {
                showInstructions();
                System.exit(-1);
                return;
                }

            // parse remainder of command line options
            Map mapCmd = CommandLineTool.parseArguments(asArg, null, true /*case sensitive*/);

            if (mapCmd.containsKey(0))
                {
                sAddr = (String) mapCmd.get(0);
                }
            else
                {
                throw new IllegalArgumentException("missing Console address");
                }

            if (mapCmd.containsKey(1))
                {
                nPort = Integer.valueOf((String) mapCmd.get(1));
                }
            else
                {
                throw new IllegalArgumentException("missing Console port");
                }
            }
       catch (Throwable e)
            {
            err(e);
            showInstructions();
            System.exit(-1);
            return;
            }

        // output "run" parameters
        out("Runner configured as follows:");
        out("Address: " + sAddr);
        out("Port:    " + nPort);
        out();

        // create and start a new Runner
        s_runner = new Runner(sAddr, nPort);
        s_runner.run();
        }


    // ----- helper methods -------------------------------------------------

    public boolean isRunning()
        {
        return m_connection != null && m_connection.isOpen();
        }

    /**
    * Display the command-line instructions.
    */
    protected static void showInstructions()
        {
        out("Runner [options] [Console address] [Console port]");
        out("where valid options are:");
        out("\t-" + SWITCH_HELP + "\t\t show help text");
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the TCP/IP ConnectionInitiator used by this Runner.
    *
    * @return the TCP/IP ConnectionInitiator used by this Runner
    */
    protected ConnectionInitiator getInitiator()
        {
        return m_initiator;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The TCP/IP initiator.
    */
    private final ConnectionInitiator m_initiator;


    // ----- constants ------------------------------------------------------

    /**
    * Command line switch for outputing help text.
    */
    public static final String SWITCH_HELP = "?";

    /**
    * The array of all valid command line switches.
    */
    public static final String[] VALID_SWITCHES =
            {
            SWITCH_HELP
            };

    /**
    * The class name of a TCP/IP-based ConnectionInitiator implementation.
    */
    private static final String TCP_INITIATOR_CLASS =
            "com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator";

    public static Runner s_runner;

    private Connection m_connection;

    }
