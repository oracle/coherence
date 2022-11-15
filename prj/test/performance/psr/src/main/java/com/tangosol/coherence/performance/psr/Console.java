/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance.psr;

import com.tangosol.dev.tools.CommandLineTool;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.OperationalContext;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.ConnectionAcceptor;
import com.tangosol.net.messaging.Message;
import com.tangosol.net.messaging.Protocol;
import com.tangosol.net.messaging.Request;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.SafeHashSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Command line utility for coordinating distributed tests.
 * <p/>
 * The Console uses an InvocationService to communicate with one or more Agent processes to start and stop desitributed
 * Runner processes. Once started, the Runner processes connect to the Console via Extend TCP/IP and can be used to
 * execute distributed tests. The Runner processes then send test results back to the Console where they are aggregated
 * and summarized.
 *
 * @author jh  2007.02.12
 */
public class Console
        extends Base
        implements Runnable, Channel.Receiver
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new Console that listens for Agent connections on the specified address and port.
     *
     * @param sAddr the hostname or IP address on which to listen
     * @param nPort the listen port
     * @param sCmd  the optional command that the Console should run on startup
     */
    public Console(String sAddr, int nPort, String sCmd)
        {
        if (sAddr == null || sAddr.length() == 0)
            {
            sAddr = DEFAULT_ADDRESS;
            }

        if (nPort <= 0)
            {
            nPort = DEFAULT_PORT;
            }

        // load the template TCP/IP acceptor configuration file
        XmlDocument xmlConfig = XmlHelper.loadXml(Console.class,
                                                  "tcp-acceptor-config.xml", null);

        XmlElement xml;

        // set the listen address and port
        xml = xmlConfig.ensureElement("tcp-acceptor/local-address/address");
        xml.setString(sAddr);

        xml = xmlConfig.ensureElement("tcp-acceptor/local-address/port");
        xml.setInt(nPort);

        // create and configure the TCP/IP acceptor
        ConnectionAcceptor acceptor;
        try
            {
            acceptor = (ConnectionAcceptor) Class.forName(TCP_ACCEPTOR_CLASS).newInstance();
            acceptor.setOperationalContext((OperationalContext) CacheFactory.getCluster());
            acceptor.configure(xmlConfig);
            acceptor.registerProtocol(RunnerProtocol.INSTANCE);
            acceptor.registerReceiver(this);
            }
        catch (Throwable e)
            {
            throw new RuntimeException("Error creating TCP/IP acceptor", e);
            }

        m_sAddr = sAddr;
        m_nPort = nPort;
        m_sCmd = sCmd;
        m_acceptor = acceptor;

        // configure the ConfigurableCacheFactory
        DefaultConfigurableCacheFactory factory = new DefaultConfigurableCacheFactory();
        factory.setConfig(DefaultConfigurableCacheFactory.loadConfig(Agent.CACHE_CONFIG));
        CacheFactory.setConfigurableCacheFactory(factory);
        }

    // ----- Runnable interface ---------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void run()
        {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        run(in);
        }

    /**
     * {@inheritDoc}
     */
    public void run(BufferedReader in)
        {
        // start the DefaultCacheServer daemon
        DefaultCacheServer.startDaemon();

        // set the Console as the user context object
        getInvocationService().setUserContext(this);

        // start the TCP/IP acceptor
        ConnectionAcceptor acceptor = getAcceptor();
        acceptor.start();

        // create a list of pending commands to execute
        List listCmd = new LinkedList();
        String sCmd = getCommand();
        if (sCmd != null && sCmd.length() > 0)
            {
            listCmd.add(sCmd);
            }

        while (acceptor.isRunning())
            {
            try
                {
                out("?");

                // read the next command
                boolean fEcho;
                if (listCmd.isEmpty())
                    {
                    synchronized (this)
                        {
                        m_fReady.set(true);
                        this.notifyAll();
                        }

                    sCmd = in.readLine();

                    System.out.println("************ sCmd=" + sCmd);

                    synchronized (this)
                        {
                        m_fReady.set(false);
                        this.notifyAll();
                        }

                    fEcho = false;
                    }
                else
                    {
                    sCmd = (String) listCmd.remove(0);
                    fEcho = true;
                    }

                if (sCmd == null)
                    {
                    continue;
                    }

                sCmd = sCmd.trim();
                if (fEcho)
                    {
                    out(sCmd);
                    }

                if (doCommand(listCmd, sCmd))
                    {
                    continue;
                    }


                // output command information
                out(COMMAND_EXIT + ":\t  exit the console");
                out(COMMAND_AGENTS + ":\t  output a list of connected Agent processes");
                out(COMMAND_RUNNERS + ":  output a list of connected Runner processes");
                out(COMMAND_START + ":\t  [Runner count]");
                out(COMMAND_STOP + ":\t  [Runner count]");
                out(COMMAND_WAIT + ":\t  [Runner count]");
                out(COMMAND_SLEEP + ":\t  [milliseconds]");
                out(COMMAND_CLEAR + ":\t  [cache name]");
                out(COMMAND_LOAD + ":\t  [cache name] [start key] [job size] [batch size] [type]");
                out(COMMAND_INDEX + ":\t  [cache name] [accessor name] [add]");
                out(COMMAND_PUT + ":\t  [cache name] [iterations] [threads] [start key] [job size] [batch size] [value size]");
                out(COMMAND_PUT2SERV + ":\t  [cache name] [iterations] [threads] [start key] [job size] [batch size] [value size]");
                out(COMMAND_PUTMIXED + ":\t [cache name] [iterations] [threads] [start key] [job size] [batch size] [avg value size]");
                out(COMMAND_PUTMIXEDCONTENT + ":\t [cache name] [iterations] [threads] [start key] [job size] [batch size] [value size] [% of content change]");
                out(COMMAND_PUTMIXEDCOMPLEXCONTENT + ":\t [cache name] [iterations] [threads] [start key] [job size] [batch size] [nested object size('0' for parent parameter change)] [%nested object change][%parameter content change]");
                out(COMMAND_GET + ":\t  [cache name] [iterations] [threads] [start key] [job size] [batch size]");
                out(COMMAND_GET2SERV + ":\t  [cache name] [iterations] [threads] [start key] [job size] [batch size]");
                out(COMMAND_RUN + ":\t  [cache name] [iterations] [threads] [start key] [job size] [batch size] [cost] [latency]");
                out(COMMAND_BENCH + ":\t  [cache name] [iterations] [threads] [start key] [job size] [batch size] [type] [% get] [% put] [% remove]");
                out(COMMAND_QUERY + ":\t  [cache name] [iterations] [threads] [accessor name] [value]");
                out(COMMAND_DISTINCT + ": [cache name] [iterations] [threads] [accessor name]");
                out(COMMAND_SAVE + ":\t  [file name]");
                out(COMMAND_SCRIPT + ":\t  [file name]");
                }
            catch (Exception e)
                {
                out(e);
                }
            }
        }

    public boolean doCommand(List listCmd, String sCmd)
        {
        // COMMAND_EXIT: quit the console
        if (COMMAND_EXIT.equals(sCmd))
            {
            doExit();
            return true;
            }

        // COMMAND_AGENTS : print information on all connected Agents
        if (COMMAND_AGENTS.equals(sCmd))
            {
            doAgents();
            return true;
            }

        // COMMAND_RUNNERS : print information on all connected Runners
        if (COMMAND_RUNNERS.equals(sCmd))
            {
            doRunners();
            return true;
            }

        // COMMAND_START : start one or more Runner processes
        if (sCmd.startsWith(COMMAND_START) && sCmd.length()
                > COMMAND_START.length())
            {
            try
                {
                doStart(Integer.parseInt(sCmd.substring(COMMAND_START.length() + 1)));
                return true;
                }
            catch (NumberFormatException e)
                {
                }
            }

        // COMMAND_STOP : stop one or more Runner processes
        if (sCmd.startsWith(COMMAND_STOP) && sCmd.length()
                > COMMAND_STOP.length())
            {
            try
                {
                doStop(Integer.parseInt(sCmd.substring(COMMAND_STOP.length() + 1)));
                return true;
                }
            catch (NumberFormatException e)
                {
                }
            }

        // COMMAND_WAIT : wait for one or more Runner processes
        if (sCmd.startsWith(COMMAND_WAIT) && sCmd.length()
                > COMMAND_WAIT.length())
            {
            try
                {
                doWait(Integer.parseInt(sCmd.substring(COMMAND_WAIT.length() + 1)));
                return true;
                }
            catch (NumberFormatException e)
                {
                }
            }

        // COMMAND_SLEEP : wait for a specified number of milliseconds
        if (sCmd.startsWith(COMMAND_SLEEP) && sCmd.length()
                > COMMAND_SLEEP.length())
            {
            try
                {
                doSleep(Long.parseLong(sCmd.substring(COMMAND_SLEEP.length() + 1)));
                return true;
                }
            catch (NumberFormatException e)
                {
                }
            }

        // COMMAND_CLEAR : clear a NamedCache
        if (sCmd.startsWith(COMMAND_CLEAR) && sCmd.length()
                > COMMAND_CLEAR.length())
            {
            String sName = sCmd.substring(COMMAND_CLEAR.length() + 1);
            if (sName.length() > 0)
                {
                doClear(sName);
                return true;
                }
            }

        // COMMAND LOAD: populate a NamedCache
        if (sCmd.startsWith(COMMAND_LOAD) && sCmd.length()
                > COMMAND_LOAD.length())
            {
            List list = new ArrayList(6);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 6)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int nStart = Integer.parseInt((String) list.get(2));
                    int cJob = Integer.parseInt((String) list.get(3));
                    int cBatch = Integer.parseInt((String) list.get(4));
                    int nType = Integer.parseInt((String) list.get(5));

                    doLoad(sName, nStart, cJob, cBatch, nType);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }

        // COMMAND INDEX: add/remove an index to/from a NamedCache
        if (sCmd.startsWith(COMMAND_INDEX) && sCmd.length()
                > COMMAND_INDEX.length())
            {
            List list = new ArrayList(4);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 4)
                {
                String sName = (String) list.get(1);
                String sAccessor = (String) list.get(2);
                boolean fAdd = Boolean.valueOf((String) list.get(3));

                doIndex(sName, sAccessor, fAdd);
                return true;
                }
            }

        // COMMAND PUT: update a NamedCache
        if (sCmd.startsWith(COMMAND_PUT) && sCmd.length()
                > COMMAND_PUT.length())
            {
            List list = new ArrayList(8);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 8)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int cIter = Integer.parseInt((String) list.get(2));
                    int cThread = Integer.parseInt((String) list.get(3));
                    int nStart = Integer.parseInt((String) list.get(4));
                    int cJob = Integer.parseInt((String) list.get(5));
                    int cBatch = Integer.parseInt((String) list.get(6));
                    int cbValue = Integer.parseInt((String) list.get(7));

                    doPut(sName, cIter, cThread, nStart, cJob, cBatch, cbValue);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }
        // COMMAND PUT2SERV: update a NamedCache
        if (sCmd.startsWith(COMMAND_PUT2SERV) && sCmd.length()
                > COMMAND_PUT2SERV.length())
            {
            List list = new ArrayList(8);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 8)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int cIter = Integer.parseInt((String) list.get(2));
                    int cThread = Integer.parseInt((String) list.get(3));
                    int nStart = Integer.parseInt((String) list.get(4));
                    int cJob = Integer.parseInt((String) list.get(5));
                    int cBatch = Integer.parseInt((String) list.get(6));
                    int cbValue = Integer.parseInt((String) list.get(7));

                    doPut(sName, cIter, cThread, nStart, cJob, cBatch, cbValue);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }
        // COMMAND PUTMIXED: update a NamedCache with mixed sizes of entries
        if (sCmd.startsWith(COMMAND_PUTMIXED) && sCmd.length()
                > COMMAND_PUTMIXED.length())
            {
            List list = new ArrayList(8);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 8)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int cIter = Integer.parseInt((String) list.get(2));
                    int cThread = Integer.parseInt((String) list.get(3));
                    int nStart = Integer.parseInt((String) list.get(4));
                    int cJob = Integer.parseInt((String) list.get(5));
                    int cBatch = Integer.parseInt((String) list.get(6));
                    int cbValue = Integer.parseInt((String) list.get(7));

                    doPutMixed(sName, cIter, cThread, nStart, cJob, cBatch, cbValue);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }
        // COMMAND PUTMIXEDVALUESIZE: update a NamedCache with mixed sizes of entries
        if (sCmd.startsWith(COMMAND_PUTMIXEDCONTENT) && sCmd.length()
                > COMMAND_PUTMIXEDCONTENT.length())
            {
            List list = new ArrayList(8);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 9)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int cIter = Integer.parseInt((String) list.get(2));
                    int cThread = Integer.parseInt((String) list.get(3));
                    int nStart = Integer.parseInt((String) list.get(4));
                    int cJob = Integer.parseInt((String) list.get(5));
                    int cBatch = Integer.parseInt((String) list.get(6));
                    int cbValue = Integer.parseInt((String) list.get(7));
                    int cbContentValue = Integer.parseInt((String) list.get(8));
                    doPutMixedValue(sName, cIter, cThread, nStart, cJob, cBatch, cbValue, cbContentValue);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }
        // COMMAND PUTMIXEDCOMPLEXVALUESIZE: update a NamedCache with mixed sizes of entries
        if (sCmd.startsWith(COMMAND_PUTMIXEDCOMPLEXCONTENT) && sCmd.length()
                > COMMAND_PUTMIXEDCOMPLEXCONTENT.length())
            {
            List list = new ArrayList(9);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 10)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int cIter = Integer.parseInt((String) list.get(2));
                    int cThread = Integer.parseInt((String) list.get(3));
                    int nStart = Integer.parseInt((String) list.get(4));
                    int cJob = Integer.parseInt((String) list.get(5));
                    int cBatch = Integer.parseInt((String) list.get(6));
                    int cbValue = Integer.parseInt((String) list.get(7));
                    int cbContentValue = Integer.parseInt((String) list.get(8));
                    int cbNoParam = Integer.parseInt((String) list.get(9));
                    doPutMixedComplexValue(sName, cIter, cThread, nStart, cJob, cBatch, cbValue, cbContentValue, cbNoParam);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }
        // COMMAND GET: access a NamedCache
        if (sCmd.startsWith(COMMAND_GET) && sCmd.length()
                > COMMAND_GET.length())
            {
            List list = new ArrayList(7);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 7)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int cIter = Integer.parseInt((String) list.get(2));
                    int cThread = Integer.parseInt((String) list.get(3));
                    int nStart = Integer.parseInt((String) list.get(4));
                    int cJob = Integer.parseInt((String) list.get(5));
                    int cBatch = Integer.parseInt((String) list.get(6));

                    doGet(sName, cIter, cThread, nStart, cJob, cBatch);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }
        // COMMAND GET: access a NamedCache
        if (sCmd.startsWith(COMMAND_GET2SERV) && sCmd.length()
                > COMMAND_GET2SERV.length())
            {
            List list = new ArrayList(7);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 7)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int cIter = Integer.parseInt((String) list.get(2));
                    int cThread = Integer.parseInt((String) list.get(3));
                    int nStart = Integer.parseInt((String) list.get(4));
                    int cJob = Integer.parseInt((String) list.get(5));
                    int cBatch = Integer.parseInt((String) list.get(6));

                    doGet(sName, cIter, cThread, nStart, cJob, cBatch);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }

        // COMMAND RUN: test a NamedCache
        if (sCmd.startsWith(COMMAND_RUN) && sCmd.length()
                > COMMAND_RUN.length())
            {
            List list = new ArrayList(9);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 9)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int cIter = Integer.parseInt((String) list.get(2));
                    int cThread = Integer.parseInt((String) list.get(3));
                    int nStart = Integer.parseInt((String) list.get(4));
                    int cJob = Integer.parseInt((String) list.get(5));
                    int cBatch = Integer.parseInt((String) list.get(6));
                    int cb = Integer.parseInt((String) list.get(7));
                    int cMillis = Integer.parseInt((String) list.get(8));

                    doRun(sName, cIter, cThread, nStart, cJob, cBatch, cb, cMillis);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }

        // COMMAND BENCH: benchmark a NamedCache
        if (sCmd.startsWith(COMMAND_BENCH) && sCmd.length()
                > COMMAND_BENCH.length())
            {
            List list = new ArrayList(11);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 11)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int cIter = Integer.parseInt((String) list.get(2));
                    int cThread = Integer.parseInt((String) list.get(3));
                    int nStart = Integer.parseInt((String) list.get(4));
                    int cJob = Integer.parseInt((String) list.get(5));
                    int cBatch = Integer.parseInt((String) list.get(6));
                    int nType = Integer.parseInt((String) list.get(7));
                    int nPctGet = Integer.parseInt((String) list.get(8));
                    int nPctPut = Integer.parseInt((String) list.get(9));
                    int nPctRemove = Integer.parseInt((String) list.get(10));

                    doBench(sName, cIter, cThread, nStart, cJob, cBatch,
                            nType, nPctGet, nPctPut, nPctRemove);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }

        // COMMAND QUERY: query a NamedCache
        if (sCmd.startsWith(COMMAND_QUERY) && sCmd.length()
                > COMMAND_QUERY.length())
            {
            List list = new ArrayList(6);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 6)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int cIter = Integer.parseInt((String) list.get(2));
                    int cThread = Integer.parseInt((String) list.get(3));
                    String sAccessor = (String) list.get(4);
                    String sValue = (String) list.get(5);

                    doQuery(sName, cIter, cThread, sAccessor, sValue);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }

        // COMMAND DISTINCT: aggregate a NamedCache
        if (sCmd.startsWith(COMMAND_DISTINCT) && sCmd.length()
                > COMMAND_DISTINCT.length())
            {
            List list = new ArrayList(5);
            for (StringTokenizer tok = new StringTokenizer(sCmd);
                 tok.hasMoreElements(); )
                {
                list.add(tok.nextElement());
                }

            if (list.size() == 5)
                {
                try
                    {
                    String sName = (String) list.get(1);
                    int cIter = Integer.parseInt((String) list.get(2));
                    int cThread = Integer.parseInt((String) list.get(3));
                    String sAccessor = (String) list.get(4);

                    doDistinct(sName, cIter, cThread, sAccessor);
                    return true;
                    }
                catch (NumberFormatException e)
                    {
                    }
                }
            }

        // COMMAND SAVE: output results to a file
        if (sCmd.startsWith(COMMAND_SAVE) && sCmd.length()
                > COMMAND_SAVE.length())
            {
            String sFile = sCmd.substring(COMMAND_SAVE.length() + 1);
            sFile = sFile.trim();

            if (sFile.length() > 0)
                {
                doSave(sFile);
                return true;
                }
            }

        // COMMAND SCRIPT: execute a squence of commands
        if (sCmd.startsWith(COMMAND_SCRIPT) && sCmd.length()
                > COMMAND_SCRIPT.length())
            {
            String sFile = sCmd.substring(COMMAND_START.length() + 1);
            sFile = sFile.trim();

            if (sFile.length() > 0)
                {
                File file = new File(sFile);
                if (file.exists() && file.isFile())
                    {
                    BufferedReader inFile = null;
                    try
                        {
                        inFile = new BufferedReader(new FileReader(file));
                        while ((sCmd = inFile.readLine()) != null)
                            {
                            listCmd.add(sCmd);
                            }
                        return true;
                        }
                    catch (IOException e)
                        {
                        out("Error processing script: " + e);
                        }
                    finally
                        {
                        try
                            {
                            inFile.close();
                            }
                        catch (Exception e)
                            {
                            }
                        }
                    }
                else
                    {
                    out("No such file: " + file);
                    }
                }
            }
        return false;
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
        getRunnerSet().add(channel);
        }

    /**
     * {@inheritDoc}
     */
    public void onMessage(Message message)
        {
        message.run();
        }

    /**
     * {@inheritDoc}
     */
    public void unregisterChannel(Channel channel)
        {
        getRunnerSet().remove(channel);

        TestMonitor monitor = getTestMonitor();
        if (monitor != null)
            {
            monitor.notify(null);
            }
        }

    // ----- entry point ----------------------------------------------------

    /**
     * Application entry point. Usage:
     * <pre>
     * java com.tangosol.coherence.psr.Console -?
     * </pre>
     *
     * @param asArg command line arguments
     */
    public static void main(String[] asArg)
        {
        Console console = init(asArg, new Supplier<Console>()
            {
            @Override
            public Console get(String sAddr, int nPort, String sCmd)
                {
                return new Console(sAddr, nPort, sCmd);
                }
            });

        console.run();
        }


    public static <C extends Console> C init(String[] asArg, Supplier<C> supplier)
        {
        String sAddr = null;
        int nPort = 0;
        String sCmd = null;

        try
            {
            // extract switches from the command line
            List listArg = new ArrayList<>(Arrays.asList(asArg));
            List listSwitches = CommandLineTool.extractSwitches(listArg,
                                                                VALID_SWITCHES);

            // show help, if necessary
            if (listSwitches.contains(SWITCH_HELP))
                {
                showInstructions();
                System.exit(-1);
                }

            // parse remainder of command line options
            Map mapCmd = CommandLineTool.parseArguments(asArg, VALID_OPTIONS,
                                                        true /*case sensitive*/);

            sAddr = CommandLineTool.processCommand(mapCmd, OPTION_ADDRESS,
                                                   DEFAULT_ADDRESS).toString();
            nPort = CommandLineTool.processIntCommand(mapCmd, OPTION_PORT,
                                                      DEFAULT_PORT);

            sCmd = (String) mapCmd.get(0);
            if (sCmd != null)
                {
                for (int i = 1; ; ++i)
                    {
                    if (mapCmd.containsKey(i))
                        {
                        sCmd = sCmd + " " + mapCmd.get(i);
                        }
                    else
                        {
                        break;
                        }
                    }
                }
            }
        catch (Throwable e)
            {
            err(e);
            showInstructions();
            System.exit(-1);
            }

        // output "run" parameters
        out("Console configured as follows:");
        out("Address: " + sAddr);
        out("Port:    " + nPort);
        out();

        // create and start a new Console
        CacheFactory.getCluster();

        return supplier.get(sAddr, nPort, sCmd);
        }

    public interface Supplier<C extends Console>
        {
        C get(String sAddr, int nPort, String sCmd);
        }

    // ----- helper methods -------------------------------------------------


    public boolean isStopOnError()
        {
        return m_fStopOnError;
        }

    public void setStopOnError(boolean fStopOnError)
        {
        m_fStopOnError = fStopOnError;
        }

    /**
     * Display the command-line instructions.
     */
    protected static void showInstructions()
        {
        out("Console [options] [command]");
        out("where valid options are:");
        out("\t-" + OPTION_ADDRESS + " the listen address, default " + DEFAULT_ADDRESS);
        out("\t-" + OPTION_PORT + "\t the listen port, default " + DEFAULT_PORT);
        out("\t-" + SWITCH_HELP + "\t\t show help text");
        }

    /**
     * Exit the Console.
     */
    protected void doExit()
        {
        try
            {
            getAcceptor().stop();
            DefaultCacheServer.shutdown();
            }
        catch (Exception e)
            {
            }
        }

    /**
     * Output the list of Agent Connections.
     */
    protected void doAgents()
        {
        Set set = getAgentSet();
        int c = set.size();

        if (c == 0)
            {
            out("<None>");
            }
        else
            {
            out(c + " Agent(s):");

            for (Iterator iter = set.iterator(); iter.hasNext(); )
                {
                Object o = iter.next();
                String s;
                try
                    {
                    s = (String) ClassHelper.invoke(o, "toString",
                                                    new Object[]{-1});
                    }
                catch (Exception e)
                    {
                    s = o.toString();
                    }
                out(s);
                }
            }
        }

    /**
     * Output the list of Runner Connections.
     */
    protected void doRunners()
        {
        Set set = getRunnerSet();
        int c = set.size();

        if (c == 0)
            {
            out("<None>");
            }
        else
            {
            out(c + " Runner(s):");

            for (Iterator iter = set.iterator(); iter.hasNext(); )
                {
                Channel channel = (Channel) iter.next();
                out(channel.getConnection());
                }
            }
        }

    /**
     * Start the specified number of remote Runner processes.
     *
     * @param cRunner the number of Runner processes to start
     */
    protected void doStart(int cRunner)
        {
        Set set = getAgentSet();
        if (set.isEmpty())
            {
            out("No available Agents.");
            }
        else if (cRunner < 1)
            {
            out("Illegal Runner count: " + cRunner);
            }
        else
            {
            try
                {
                // divide the new runners evenly across all agents
                int cRunnerNew = getRunnerSet().size() + cRunner;
                int cRunnerDiv = Math.max(cRunner / set.size(), 1);

                Agent.StartRunnerInvocable task = new Agent.StartRunnerInvocable();
                task.setConsoleAddress(getAddress());
                task.setConsolePort(getPort());
                task.setRunnerCount(cRunnerDiv);

                for (Iterator iter = set.iterator(); iter.hasNext() && cRunner > 0; )
                    {
                    Member member = (Member) iter.next();
                    if (!iter.hasNext())
                        {
                        task.setRunnerCount(cRunner);
                        }

                    Map map = getInvocationService().query(task,
                                                           Collections.singleton(member));

                    Integer IRunner = (Integer) map.values().iterator().next();
                    for (int i = 0, c = IRunner.intValue(); i < c; ++i)
                        {
                        out("Started Runner.");
                        --cRunner;
                        }
                    }

                // give the runners a chance to connect
                long ldtStart = System.currentTimeMillis();
                do
                    {
                    if (getRunnerSet().size() == cRunnerNew)
                        {
                        break;
                        }
                    Thread.sleep(100L);
                    } while (System.currentTimeMillis() - ldtStart < 30000L);
                }
            catch (Exception e)
                {
                out(e);
                }
            }
        }

    /**
     * Stop the specified number of remote Runner processes.
     *
     * @param cRunner the number of Runner processes to stop
     */
    protected void doStop(int cRunner)
        {
        Set set = getAgentSet();
        if (set.isEmpty())
            {
            out("No available Agents.");
            }
        else if (cRunner < 1)
            {
            out("Illegal Runner count.");
            }
        else
            {
            try
                {
                cRunner = Math.min(getRunnerSet().size(), cRunner);

                // stop the runners evenly across all agents
                int cRunnerNew = getRunnerSet().size() - cRunner;
                int cRunnerDiv = Math.max(cRunner / set.size(), 1);

                Agent.StopRunnerInvocable task = new Agent.StopRunnerInvocable();
                task.setRunnerCount(cRunnerDiv);

                for (Iterator iter = set.iterator(); iter.hasNext() && cRunner > 0; )
                    {
                    Member member = (Member) iter.next();
                    if (!iter.hasNext())
                        {
                        task.setRunnerCount(cRunner);
                        }

                    Map map = getInvocationService().query(task,
                                                           Collections.singleton(member));

                    Integer IRunner = (Integer) map.values().iterator().next();
                    for (int i = 0, c = IRunner.intValue(); i < c; ++i)
                        {
                        out("Stopped Runner.");
                        --cRunner;
                        }
                    }

                // give the runners a chance to disconnect
                long ldtStart = System.currentTimeMillis();
                do
                    {
                    if (getRunnerSet().size() == cRunnerNew)
                        {
                        break;
                        }
                    Thread.sleep(100L);
                    } while (System.currentTimeMillis() - ldtStart < 60000L);
                }
            catch (Exception e)
                {
                out(e);
                }
            }
        }

    /**
     * Wait for the specified number of remote Runner processes to connect.
     *
     * @param cRunner the number of Runner processes to wait for
     */
    protected void doWait(int cRunner)
        {
        if (cRunner < 0)
            {
            out("Illegal Runner count: " + cRunner);
            }
        else
            {
            try
                {
                // give the runners a chance to connect
                long ldtStart = System.currentTimeMillis();
                do
                    {
                    if (getRunnerSet().size() == cRunner)
                        {
                        break;
                        }
                    Thread.sleep(100L);
                    } while (System.currentTimeMillis() - ldtStart < 120000L);
                }
            catch (Exception e)
                {
                log(e);
                }

            out(getRunnerSet().size() + " Runner(s) started.");
            }
        }

    /**
     * Wait for the specified number of milliseconds before continuing.
     *
     * @param cMillis the number of milliseconds to sleep for
     */
    protected void doSleep(long cMillis)
        {
        if (cMillis < 0)
            {
            out("Illegal sleep time: " + cMillis);
            }
        else
            {
            try
                {
                // give the runners a chance to connect
                long ldtEnd = System.currentTimeMillis() + cMillis;
                do
                    {
                    Thread.sleep(ldtEnd - System.currentTimeMillis());
                    } while (System.currentTimeMillis() < ldtEnd);
                }
            catch (Exception e)
                {
                log(e);
                }
            }
        }

    /**
     * Clear the specified NamedCache.
     *
     * @param sName the name of the target NamedCache
     */
    protected void doClear(String sName)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else
            {
            out("Clearing " + sName + " ...");
            try
                {
                Channel channel = (Channel) set.iterator().next();

                // create and send a ClearRequest
                RunnerProtocol.ClearRequest req = (RunnerProtocol.ClearRequest) channel.getMessageFactory().createMessage(
                        RunnerProtocol.ClearRequest.TYPE_ID);
                req.withCacheName(sName);

                channel.request(req);
                }
            catch (Exception e)
                {
                out(e);
                }
            }
        }

    /**
     * Populate the specified NamedCache.
     *
     * @param sName  the name of the target NamedCache
     * @param nStart the starting key
     * @param cJob   the job size
     * @param cBatch the batch size
     * @param nType  the POF type of the objects to populate the cache with
     */
    public void doLoad(String sName, int nStart, int cJob, int cBatch, int nType)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cJob < 1)
            {
            out("Illegal job size.");
            }
        else if (cBatch < 1)
            {
            out("Illegal batch size.");
            }
        else if (nType < 0)
            {
            out("Illegal type ID.");
            }
        else
            {
            out("Populating " + cJob + " value(s) (starting with #" + nStart
                        + ") of type " + nType + " in batches of " + cBatch + " ...");

            try
                {
                // sub-divide the job equally among all Runners
                int cRunner = set.size();
                int cJobDiv = Math.max(cJob / (Math.max(cRunner, 1)), 1);

                Collection colReq = new ArrayList(cRunner);
                for (Iterator iter = set.iterator(); iter.hasNext() && cJob > 0; )
                    {
                    Channel channel = (Channel) iter.next();

                    // calculate the job size for this Runner
                    int cJobCur = iter.hasNext() ? cJobDiv : cJob;

                    // create and send a LoadRequest
                    RunnerProtocol.LoadRequest req = (RunnerProtocol.LoadRequest) channel.getMessageFactory().createMessage(
                            RunnerProtocol.LoadRequest.TYPE_ID);

                    req.withCacheName(sName);
                    req.withStartKey(nStart);
                    req.withJobSize(cJobCur);
                    req.withBatchSize(cBatch);
                    req.withType(nType);

                    channel.send(req);
                    colReq.add(req);

                    cJob -= cJobCur;
                    nStart += cJobCur;
                    }

                // wait for all requests to complete
                for (Iterator iter = colReq.iterator(); iter.hasNext(); )
                    {
                    Request req = (Request) iter.next();
                    req.getStatus().waitForResponse();
                    }
                }
            catch (Exception e)
                {
                out(e);
                }
            }
        }

    /**
     * Add/remove an index to/from the specified NamedCache.
     *
     * @param sName     the name of the target NamedCache
     * @param sAccessor the name of an accessor for objects in the cache that returns an indexable object
     * @param fAdd      true to add an index, false to remove an index
     */
    public void doIndex(String sName, String sAccessor, boolean fAdd)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (sAccessor == null || sAccessor.length() == 0)
            {
            out("Illegal accessor name.");
            }
        else
            {
            if (fAdd)
                {
                out("Adding an index to " + sName + " ...");
                }
            else
                {
                out("Removing an index from " + sName + " ...");
                }
            try
                {
                Channel channel = (Channel) set.iterator().next();

                // create and send an IndexRequest
                RunnerProtocol.IndexRequest req = (RunnerProtocol.IndexRequest) channel.getMessageFactory().createMessage(
                        RunnerProtocol.IndexRequest.TYPE_ID);
                req.withCacheName(sName);
                req.withExtractor(sAccessor);
                req.add(fAdd);

                channel.request(req);
                }
            catch (Exception e)
                {
                out(e);
                }
            }
        }

    /**
     * Update the specified NamedCache.
     *
     * @param sName   the name of the target NamedCache
     * @param cIter   the number of iterations
     * @param cThread the number of threads to use
     * @param nStart  the starting key
     * @param cJob    the job size
     * @param cBatch  the batch size
     * @param cbValue the size of the value in bytes
     */
    public void doPut2Serv(String sName, int cIter, int cThread, int nStart, int cJob, int cBatch, int cbValue)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cIter < 1)
            {
            out("Illegal iteration count.");
            }
        else if (cThread < 1)
            {
            out("Illegal thread count.");
            }
        else if (cJob < 1)
            {
            out("Illegal job size.");
            }
        else if (cBatch < 1)
            {
            out("Illegal batch size.");
            }
        else if (cbValue < 0)
            {
            out("Illegal value size.");
            }
        else
            {
            out("Updating 2 Serv" + cJob + " value(s) (starting with #" + nStart
                        + ") each of " + cbValue + " bytes in batches of "
                        + cBatch + " using " + cThread + " thread(s) " + cIter
                        + " time(s) ...");

            try
                {
                TestMonitor monitor = new TestMonitor();
                setTestMonitor(monitor);

                // sub-divide the job equally among all Runners
                int cRunner = set.size();
                int cJobDiv = Math.max(cJob / (Math.max(cRunner, 1)), 1);

                Collection colRunner = new ArrayList(cRunner);
                for (Iterator iter = set.iterator(); iter.hasNext() && cJob > 0; )
                    {
                    Channel channel = (Channel) iter.next();

                    // calculate the job size for this Runner
                    int cJobCur = iter.hasNext() ? cJobDiv : cJob;

                    // create and send a PutMessage
                    RunnerProtocol.PutMessage2Serv msg = (RunnerProtocol.PutMessage2Serv) channel.getMessageFactory().createMessage(
                            RunnerProtocol.PutMessage2Serv.TYPE_ID);

                    msg.withCacheName(sName);
                    msg.withIterationCount(cIter);
                    msg.withThreadCount(cThread);
                    msg.withStartKey(nStart);
                    msg.withJobSize(cJobCur);
                    msg.withBatchSize(cBatch);
                    msg.withValueSize(cbValue);

                    channel.send(msg);
                    colRunner.add(channel);

                    cJob -= cJobCur;
                    nStart += cJobCur;
                    }

                waitForResult(monitor, colRunner);
                }
            catch (Exception e)
                {
                out(e);
                }
            finally
                {
                setTestMonitor(null);
                }
            }
        }

    /**
     * Update the specified NamedCache.
     *
     * @param sName   the name of the target NamedCache
     * @param cIter   the number of iterations
     * @param cThread the number of threads to use
     * @param nStart  the starting key
     * @param cJob    the job size
     * @param cBatch  the batch size
     * @param cbValue the size of the value in bytes
     */
    public void doPut(String sName, int cIter, int cThread, int nStart, int cJob, int cBatch, int cbValue)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cIter < 1)
            {
            out("Illegal iteration count.");
            }
        else if (cThread < 1)
            {
            out("Illegal thread count.");
            }
        else if (cJob < 1)
            {
            out("Illegal job size.");
            }
        else if (cBatch < 1)
            {
            out("Illegal batch size.");
            }
        else if (cbValue < 0)
            {
            out("Illegal value size.");
            }
        else
            {
            out("Updating " + cJob + " value(s) (starting with #" + nStart
                        + ") each of " + cbValue + " bytes in batches of "
                        + cBatch + " using " + cThread + " thread(s) " + cIter
                        + " time(s) ...");

            try
                {
                TestMonitor monitor = new TestMonitor();
                setTestMonitor(monitor);

                // sub-divide the job equally among all Runners
                int cRunner = set.size();
                int cJobDiv = Math.max(cJob / (Math.max(cRunner, 1)), 1);

                Collection colRunner = new ArrayList(cRunner);
                for (Iterator iter = set.iterator(); iter.hasNext() && cJob > 0; )
                    {
                    Channel channel = (Channel) iter.next();

                    // calculate the job size for this Runner
                    int cJobCur = iter.hasNext() ? cJobDiv : cJob;

                    // create and send a PutMessage
                    RunnerProtocol.PutMessage msg = (RunnerProtocol.PutMessage) channel.getMessageFactory().createMessage(
                            RunnerProtocol.PutMessage.TYPE_ID);

                    msg.withStopOnError(isStopOnError());
                    msg.withCacheName(sName);
                    msg.withIterationCount(cIter);
                    msg.withThreadCount(cThread);
                    msg.withStartKey(nStart);
                    msg.withJobSize(cJobCur);
                    msg.withBatchSize(cBatch);
                    msg.withValueSize(cbValue);

                    channel.send(msg);
                    colRunner.add(channel);

                    cJob -= cJobCur;
                    nStart += cJobCur;
                    }

                waitForResult(monitor, colRunner);
                }
            catch (Exception e)
                {
                out(e);
                }
            finally
                {
                setTestMonitor(null);
                }
            }
        }

    /**
     * Update the specified NamedCache with mixed size values.
     *
     * @param sName   the name of the target NamedCache
     * @param cIter   the number of iterations
     * @param cThread the number of threads to use
     * @param nStart  the starting key
     * @param cJob    the job size
     * @param cBatch  the batch size
     * @param cbValue the average size of the value in bytes
     */
    public void doPutMixed(String sName, int cIter, int cThread, int nStart, int cJob, int cBatch, int cbValue)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cIter < 1)
            {
            out("Illegal iteration count.");
            }
        else if (cThread < 1)
            {
            out("Illegal thread count.");
            }
        else if (cJob < 1)
            {
            out("Illegal job size.");
            }
        else if (cBatch < 1)
            {
            out("Illegal batch size.");
            }
        else if (cbValue < 0)
            {
            out("Illegal value size.");
            }
        else
            {
            out("doPutMixed : Updating " + cJob + " values (starting with #" + nStart
                        + ") with average size of " + cbValue + " bytes in batches of "
                        + cBatch + " using " + cThread + " threads " + cIter
                        + " times ...");

            try
                {
                TestMonitor monitor = new TestMonitor();
                setTestMonitor(monitor);

                // sub-divide the job equally among all Runners
                int cRunner = set.size();
                int cJobDiv = Math.max(cJob / (Math.max(cRunner, 1)), 1);

                Collection colRunner = new ArrayList(cRunner);
                for (Iterator iter = set.iterator(); iter.hasNext() && cJob > 0; )
                    {
                    Channel channel = (Channel) iter.next();

                    // calculate the job size for this Runner
                    int cJobCur = iter.hasNext() ? cJobDiv : cJob;

                    // create and send a PutMixedMessage
                    RunnerProtocol.PutMixedMessage msg = (RunnerProtocol.PutMixedMessage) channel.getMessageFactory().createMessage(
                            RunnerProtocol.PutMixedMessage.TYPE_ID);

                    msg.withCacheName(sName);
                    msg.withIterationCount(cIter);
                    msg.withThreadCount(cThread);
                    msg.withStartKey(nStart);
                    msg.withJobSize(cJobCur);
                    msg.withBatchSize(cBatch);
                    msg.withValueSize(cbValue);

                    channel.send(msg);
                    colRunner.add(channel);

                    cJob -= cJobCur;
                    nStart += cJobCur;
                    }

                waitForResult(monitor, colRunner);
                }
            catch (Exception e)
                {
                out(e);
                }
            finally
                {
                setTestMonitor(null);
                }
            }
        }

    /**
     * Update the specified NamedCache with mixed value size values.
     *
     * @param sName                the name of the target NamedCache
     * @param cIter                the number of iterations
     * @param cThread              the number of threads to use
     * @param nStart               the starting key
     * @param cJob                 the job size
     * @param cBatch               the batch size
     * @param cbValue              the average size of the value in bytes
     * @param cbValuePercentChange the percentage of value changes in bytes
     */
    public void doPutMixedValue(String sName, int cIter, int cThread, int nStart, int cJob, int cBatch, int cbValue, int cbValuePercentChange)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cIter < 1)
            {
            out("Illegal iteration count.");
            }
        else if (cThread < 1)
            {
            out("Illegal thread count.");
            }
        else if (cJob < 1)
            {
            out("Illegal job size.");
            }
        else if (cBatch < 1)
            {
            out("Illegal batch size.");
            }
        else if (cbValue < 0)
            {
            out("Illegal value size.");
            }
        else if ((cbValuePercentChange < 0) || (cbValuePercentChange > 100))
            {
            out("Illegal value size.Please enter percent value between 0 and 100");
            }
        else
            {
            out("doPutMixedContent..: Updating " + cJob + " values (starting with #" + nStart
                        + ") with size of " + cbValue + " and " + cbValuePercentChange + "% value change " + "in batches of "
                        + cBatch + " using " + cThread + " threads " + cIter
                        + " times ...");

            try
                {
                TestMonitor monitor = new TestMonitor();
                setTestMonitor(monitor);

                // sub-divide the job equally among all Runners
                int cRunner = set.size();
                int cJobDiv = Math.max(cJob / (Math.max(cRunner, 1)), 1);

                Collection colRunner = new ArrayList(cRunner);
                for (Iterator iter = set.iterator(); iter.hasNext() && cJob > 0; )
                    {
                    Channel channel = (Channel) iter.next();

                    // calculate the job size for this Runner
                    int cJobCur = iter.hasNext() ? cJobDiv : cJob;

                    RunnerProtocol.PutMixedContentValueMessage msg = (RunnerProtocol.PutMixedContentValueMessage) channel.getMessageFactory().createMessage(
                            RunnerProtocol.PutMixedContentValueMessage.TYPE_ID);

                    msg.withCacheName(sName);
                    msg.withIterationCount(cIter);
                    msg.withThreadCount(cThread);
                    msg.withStartKey(nStart);
                    msg.withJobSize(cJobCur);
                    msg.withBatchSize(cBatch);
                    msg.withValueSize(cbValue);
                    msg.withPercentContentValueChange(cbValuePercentChange);
                    channel.send(msg);
                    colRunner.add(channel);

                    cJob -= cJobCur;
                    nStart += cJobCur;
                    }

                waitForResult(monitor, colRunner);
                }
            catch (Exception e)
                {
                out(e);
                }
            finally
                {
                setTestMonitor(null);
                }
            }
        }

    /**
     * Update the specified NamedCache with mixed value size values.
     *
     * @param sName                the name of the target NamedCache
     * @param cIter                the number of iterations
     * @param cThread              the number of threads to use
     * @param nStart               the starting key
     * @param cJob                 the job size
     * @param cBatch               the batch size
     * @param cbValue              the average size of the value in bytes
     * @param cbValuePercentChange the percentage of value changes in bytes
     */
    public void doPutMixedComplexValue(String sName, int cIter, int cThread, int nStart, int cJob, int cBatch, int cbValue, int cbValuePercentChange, int cno_param)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cIter < 1)
            {
            out("Illegal iteration count.");
            }
        else if (cThread < 1)
            {
            out("Illegal thread count.");
            }
        else if (cJob < 1)
            {
            out("Illegal job size.");
            }
        else if (cBatch < 1)
            {
            out("Illegal batch size.");
            }
        else if (cbValue < 0)
            {
            out("Illegal value size.");
            }
        else if ((cbValuePercentChange < 0) || (cbValuePercentChange > 100))
            {
            out("Illegal value size.Please enter percent value between 0 and 100");
            }
        else
            {
            out("doPutMixedContentComplex...: Updating " + cJob + " complex values (starting with #" + nStart
                        + ") with size of " + cbValue + " and " + cbValuePercentChange + "% value change " + " with " + cno_param + "parameters in batches of "
                        + cBatch + " using " + cThread + " threads " + cIter
                        + " times ...");

            try
                {
                TestMonitor monitor = new TestMonitor();
                setTestMonitor(monitor);

                // sub-divide the job equally among all Runners
                int cRunner = set.size();
                int cJobDiv = Math.max(cJob / (Math.max(cRunner, 1)), 1);

                Collection colRunner = new ArrayList(cRunner);
                for (Iterator iter = set.iterator(); iter.hasNext() && cJob > 0; )
                    {
                    Channel channel = (Channel) iter.next();

                    // calculate the job size for this Runner
                    int cJobCur = iter.hasNext() ? cJobDiv : cJob;

                    RunnerProtocol.PutMixedContentComplexValueMessage msg = (RunnerProtocol.PutMixedContentComplexValueMessage) channel.getMessageFactory().createMessage(
                            RunnerProtocol.PutMixedContentComplexValueMessage.TYPE_ID);
                    msg.withCacheName(sName);
                    msg.withIterationCount(cIter);
                    msg.withThreadCount(cThread);
                    msg.withStartKey(nStart);
                    msg.withJobSize(cJobCur);
                    msg.withBatchSize(cBatch);
                    msg.withValueSize(cbValue);
                    msg.withPercentContentValueChange(cbValuePercentChange);
                    msg.withM_number_param(cno_param);
                    channel.send(msg);
                    colRunner.add(channel);

                    cJob -= cJobCur;
                    nStart += cJobCur;
                    }

                waitForResult(monitor, colRunner);
                }
            catch (Exception e)
                {
                out(e);
                }
            finally
                {
                setTestMonitor(null);
                }
            }
        }

    /**
     * Access the specified NamedCache.
     *
     * @param sName   the name of the target NamedCache
     * @param cIter   the number of iterations
     * @param cThread the number of threads to use
     * @param nStart  the starting key
     * @param cJob    the job size
     * @param cBatch  the batch size
     */
    public void doGet(String sName, int cIter, int cThread, int nStart, int cJob, int cBatch)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cIter < 1)
            {
            out("Illegal iteration count.");
            }
        else if (cThread < 1)
            {
            out("Illegal thread count.");
            }
        else if (cJob < 1)
            {
            out("Illegal job size.");
            }
        else if (cBatch < 1)
            {
            out("Illegal batch size.");
            }
        else
            {
            out("Accessing " + cJob + " value(s) (starting with #" + nStart
                        + ") in batches of " + cBatch + " using " + cThread
                        + " thread(s) " + cIter + " time(s) ...");

            try
                {
                TestMonitor monitor = new TestMonitor();
                setTestMonitor(monitor);

                // sub-divide the job equally among all Runners
                int cRunner = set.size();
                int cJobDiv = Math.max(cJob / (Math.max(cRunner, 1)), 1);

                Collection colRunner = new ArrayList(cRunner);
                for (Iterator iter = set.iterator(); iter.hasNext() && cJob > 0; )
                    {
                    Channel channel = (Channel) iter.next();

                    // calculate the job size for this Runner
                    int cJobCur = iter.hasNext() ? cJobDiv : cJob;

                    // create and send a GetMessage
                    RunnerProtocol.GetMessage msg = (RunnerProtocol.GetMessage) channel.getMessageFactory().createMessage(
                            RunnerProtocol.GetMessage.TYPE_ID);

                    msg.withCacheName(sName);
                    msg.withIterationCount(cIter);
                    msg.withThreadCount(cThread);
                    msg.withStartKey(nStart);
                    msg.withJobSize(cJobCur);
                    msg.withBatchSize(cBatch);

                    channel.send(msg);
                    colRunner.add(channel);

                    cJob -= cJobCur;
                    nStart += cJobCur;
                    }

                waitForResult(monitor, colRunner);
                }
            catch (Exception e)
                {
                out(e);
                }
            finally
                {
                setTestMonitor(null);
                }
            }
        }

    /**
     * Access the specified two NamedCaches.
     *
     * @param sName   the name of the target NamedCache
     * @param cIter   the number of iterations
     * @param cThread the number of threads to use
     * @param nStart  the starting key
     * @param cJob    the job size
     * @param cBatch  the batch size
     */
    public void doGet2Serv(String sName, int cIter, int cThread, int nStart, int cJob, int cBatch)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cIter < 1)
            {
            out("Illegal iteration count.");
            }
        else if (cThread < 1)
            {
            out("Illegal thread count.");
            }
        else if (cJob < 1)
            {
            out("Illegal job size.");
            }
        else if (cBatch < 1)
            {
            out("Illegal batch size.");
            }
        else
            {
            out("Accessing " + cJob + " value(s) (starting with #" + nStart
                        + ") in batches of " + cBatch + " using " + cThread
                        + " thread(s) " + cIter + " time(s) ...");

            try
                {
                TestMonitor monitor = new TestMonitor();
                setTestMonitor(monitor);

                // sub-divide the job equally among all Runners
                int cRunner = set.size();
                int cJobDiv = Math.max(cJob / (Math.max(cRunner, 1)), 1);

                Collection colRunner = new ArrayList(cRunner);
                for (Iterator iter = set.iterator(); iter.hasNext() && cJob > 0; )
                    {
                    Channel channel = (Channel) iter.next();

                    // calculate the job size for this Runner
                    int cJobCur = iter.hasNext() ? cJobDiv : cJob;

                    // create and send a GetMessage
                    RunnerProtocol.Get2ServMessage msg = (RunnerProtocol.Get2ServMessage) channel.getMessageFactory().createMessage(
                            RunnerProtocol.Get2ServMessage.TYPE_ID);

                    msg.withCacheName(sName);
                    msg.withIterationCount(cIter);
                    msg.withThreadCount(cThread);
                    msg.withStartKey(nStart);
                    msg.withJobSize(cJobCur);
                    msg.withBatchSize(cBatch);

                    channel.send(msg);
                    colRunner.add(channel);

                    cJob -= cJobCur;
                    nStart += cJobCur;
                    }

                waitForResult(monitor, colRunner);
                }
            catch (Exception e)
                {
                out(e);
                }
            finally
                {
                setTestMonitor(null);
                }
            }
        }

    /**
     * Simulate an arbitrary test on the specified NamedCache.
     *
     * @param sName   the name of the target NamedCache
     * @param cIter   the number of iterations
     * @param cThread the number of threads to use
     * @param nStart  the starting key
     * @param cJob    the job size
     * @param cBatch  the batch size
     * @param cb      the cost of each operation, in bytes
     * @param cMillis the latency of each batch, in milliseconds
     */
    public void doRun(String sName, int cIter, int cThread, int nStart, int cJob, int cBatch, int cb, int cMillis)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cIter < 1)
            {
            out("Illegal iteration count.");
            }
        else if (cThread < 1)
            {
            out("Illegal thread count.");
            }
        else if (cJob < 1)
            {
            out("Illegal job size.");
            }
        else if (cBatch < 1)
            {
            out("Illegal batch size.");
            }
        else if (cb < 0)
            {
            out("Illegal cost.");
            }
        else if (cMillis < 0)
            {
            out("Illegal latency");
            }
        else
            {
            out("Simulating " + cJob + " operation(s) (starting with #"
                        + nStart + ") each of " + cb + " bytes in batches of "
                        + cBatch + " (with latency " + cMillis + " ms) using "
                        + cThread + " thread(s) " + cIter + " time(s) ...");

            try
                {
                TestMonitor monitor = new TestMonitor();
                setTestMonitor(monitor);

                // sub-divide the job equally among all Runners
                int cRunner = set.size();
                int cJobDiv = Math.max(cJob / (Math.max(cRunner, 1)), 1);

                Collection colRunner = new ArrayList(cRunner);
                for (Iterator iter = set.iterator(); iter.hasNext() && cJob > 0; )
                    {
                    Channel channel = (Channel) iter.next();

                    // calculate the job size for this Runner
                    int cJobCur = iter.hasNext() ? cJobDiv : cJob;

                    // create and send a RunMessage
                    RunnerProtocol.RunMessage msg = (RunnerProtocol.RunMessage) channel.getMessageFactory().createMessage(
                            RunnerProtocol.RunMessage.TYPE_ID);

                    msg.withCacheName(sName);
                    msg.withIterationCount(cIter);
                    msg.withThreadCount(cThread);
                    msg.withStartKey(nStart);
                    msg.withJobSize(cJobCur);
                    msg.withBatchSize(cBatch);
                    msg.withCost(cb);
                    msg.withLatency(cMillis);

                    channel.send(msg);
                    colRunner.add(channel);

                    cJob -= cJobCur;
                    nStart += cJobCur;
                    }

                waitForResult(monitor, colRunner);
                }
            catch (Exception e)
                {
                out(e);
                }
            finally
                {
                setTestMonitor(null);
                }
            }
        }

    /**
     * Perform a benchmark against the specified NamedCache.
     *
     * @param sName      the name of the target NamedCache
     * @param cIter      the number of iterations
     * @param cThread    the number of threads to use
     * @param nStart     the starting key
     * @param cJob       the job size
     * @param cBatch     the batch size
     * @param nType      the POF type of the objects to populate the cache with
     * @param nPctGet    the percentage of operations that will be gets [0-100]
     * @param nPctPut    the percentage of operations that will be puts [0-100]
     * @param nPctRemove the percentage of operations that will be removes [0-100]
     */
    public void doBench(
            String sName, int cIter, int cThread, int nStart,
            int cJob, int cBatch, int nType, int nPctGet, int nPctPut, int nPctRemove)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cIter < 1)
            {
            out("Illegal iteration count.");
            }
        else if (cThread < 1)
            {
            out("Illegal thread count.");
            }
        else if (cJob < 1)
            {
            out("Illegal job size.");
            }
        else if (cBatch < 1)
            {
            out("Illegal batch size.");
            }
        else if (nType < 0)
            {
            out("Illegal type ID.");
            }
        else if (nPctGet < 0 || nPctPut < 0 || nPctRemove < 0
                || (nPctGet + nPctPut + nPctRemove) != 100)
            {
            out("Illegal operation distribution.");
            }
        else
            {
            out("Benchmarking " + cJob + " operation(s) (starting with #"
                        + nStart + ", " + nPctGet + "% get, " + nPctPut + "% put, "
                        + nPctRemove + "% remove) with objects of type "
                        + nType + " in batches of " + cBatch + " using "
                        + cThread + " thread(s) " + cIter + " time(s) ...");

            try
                {
                TestMonitor monitor = new TestMonitor();
                setTestMonitor(monitor);

                // sub-divide the job equally among all Runners
                int cRunner = set.size();
                int cJobDiv = Math.max(cJob / (Math.max(cRunner, 1)), 1);

                Collection colRunner = new ArrayList(cRunner);
                for (Iterator iter = set.iterator(); iter.hasNext() && cJob > 0; )
                    {
                    Channel channel = (Channel) iter.next();

                    // calculate the job size for this Runner
                    int cJobCur = iter.hasNext() ? cJobDiv : cJob;

                    // create and send a BenchMessage
                    RunnerProtocol.BenchMessage msg = (RunnerProtocol.BenchMessage) channel.getMessageFactory().createMessage(
                            RunnerProtocol.BenchMessage.TYPE_ID);

                    msg.withCacheName(sName);
                    msg.withIterationCount(cIter);
                    msg.withThreadCount(cThread);
                    msg.withStartKey(nStart);
                    msg.withJobSize(cJobCur);
                    msg.withBatchSize(cBatch);
                    msg.withType(nType);
                    msg.withPercentGet(nPctGet);
                    msg.withPercentPut(nPctPut);
                    msg.withPercentRemove(nPctRemove);

                    channel.send(msg);
                    colRunner.add(channel);

                    cJob -= cJobCur;
                    nStart += cJobCur;
                    }

                waitForResult(monitor, colRunner);
                }
            catch (Exception e)
                {
                out(e);
                }
            finally
                {
                setTestMonitor(null);
                }
            }
        }

    /**
     * Perform a query against the specified NamedCache.
     *
     * @param sName     the name of the target NamedCache
     * @param cIter     the number of iterations
     * @param cThread   the number of threads to use
     * @param sAccessor the name of an accessor for objects in the cache that returns an object
     * @param sValue    the value to compare with extracted values
     */
    public void doQuery(
            String sName, int cIter, int cThread, String sAccessor,
            String sValue)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cIter < 1)
            {
            out("Illegal iteration count.");
            }
        else if (cThread < 1)
            {
            out("Illegal thread count.");
            }
        else if (sAccessor == null || sAccessor.length() == 0)
            {
            out("Illegal accessor name.");
            }
        else
            {
            out("Querying using " + cThread + " thread(s) " + cIter + " time(s) ...");

            try
                {
                TestMonitor monitor = new TestMonitor();
                setTestMonitor(monitor);

                Collection colRunner = new ArrayList(set.size());
                for (Iterator iter = set.iterator(); iter.hasNext(); )
                    {
                    Channel channel = (Channel) iter.next();

                    // create and send a QueryMessage
                    RunnerProtocol.QueryMessage msg = (RunnerProtocol.QueryMessage) channel.getMessageFactory().createMessage(
                            RunnerProtocol.QueryMessage.TYPE_ID);

                    msg.withCacheName(sName);
                    msg.withIterationCount(cIter);
                    msg.withThreadCount(cThread);
                    msg.withExtractor(sAccessor);
                    msg.withValue(sValue);

                    channel.send(msg);
                    colRunner.add(channel);
                    }

                waitForResult(monitor, colRunner);
                }
            catch (Exception e)
                {
                out(e);
                }
            finally
                {
                setTestMonitor(null);
                }
            }
        }

    /**
     * Perform an aggregation against the specified NamedCache.
     *
     * @param sName     the name of the target NamedCache
     * @param cIter     the number of iterations
     * @param cThread   the number of threads to use
     * @param sAccessor the name of an accessor for objects in the cache that returns an object
     */
    public void doDistinct(String sName, int cIter, int cThread, String sAccessor)
        {
        Set set = getRunnerSet();
        if (set.isEmpty())
            {
            out("No available Runners.");
            }
        else if (sName == null || sName.length() == 0)
            {
            out("Illegal NamedCache name.");
            }
        else if (cIter < 1)
            {
            out("Illegal iteration count.");
            }
        else if (cThread < 1)
            {
            out("Illegal thread count.");
            }
        else if (sAccessor == null || sAccessor.length() == 0)
            {
            out("Illegal accessor name.");
            }
        else
            {
            out("Aggregating using " + cThread + " thread(s) " + cIter + " time(s) ...");

            try
                {
                TestMonitor monitor = new TestMonitor();
                setTestMonitor(monitor);

                Collection colRunner = new ArrayList(set.size());
                for (Iterator iter = set.iterator(); iter.hasNext(); )
                    {
                    Channel channel = (Channel) iter.next();

                    // create and send a DistinctMessage
                    RunnerProtocol.DistinctMessage msg = (RunnerProtocol.DistinctMessage) channel.getMessageFactory().createMessage(
                            RunnerProtocol.DistinctMessage.TYPE_ID);

                    msg.withCacheName(sName);
                    msg.withIterationCount(cIter);
                    msg.withThreadCount(cThread);
                    msg.withExtractor(sAccessor);

                    channel.send(msg);
                    colRunner.add(channel);
                    }

                waitForResult(monitor, colRunner);
                }
            catch (Exception e)
                {
                out(e);
                }
            finally
                {
                setTestMonitor(null);
                }
            }
        }

    /**
     * Save the last test result to the file with the given name.
     *
     * @param sFile the name of the file to output a report to
     */
    public void doSave(String sFile)
        {
        doSave(sFile, getResult());
        }

    /**
     * Save the last test result to the file with the given name.
     *
     * @param sFile the name of the file to output a report to
     */
    public static void doSave(String sFile, TestResult result)
        {
        if (sFile == null || sFile.length() == 0)
            {
            out("Illegal file name.");
            }
        else
            {
            doSave(new File(sFile), result);
            }
        }

    /**
     * Save the last test result to the file with the given name.
     *
     * @param file the file to output a report to
     */
    public static void doSave(File file, TestResult result)
        {
        if (result == null)
            {
            out("No available result.");
            }
        else if (file == null)
            {
            out("Illegal file parameter.");
            }
        else
            {
            try
                {
                if (file.exists())
                    {
                    if (!file.canWrite())
                        {
                        throw new IOException("Read-only file: " + file);
                        }
                    }
                else
                    {
                    file.createNewFile();
                    }

                PrintStream out = new PrintStream(
                        new FileOutputStream(file, true));
                try
                    {
                    result.writeReport(out);
                    }
                finally
                    {
                    out.close();
                    }
                }
            catch (IOException e)
                {
                out(e);
                }
            }
        }

    /**
     * Wait for the specified runners to return completed test results, outputing sampled results while waiting, and
     * then output summary information after all test results have been received.
     *
     * @param monitor   the TestMonitor used to wait for all results
     * @param colRunner the collection of Channel objects, one per runner that was asked to execute a test
     *
     * @throws InterruptedException if the current thread is interrupted while waiting for results.
     */
    protected void waitForResult(TestMonitor monitor, Collection colRunner)
            throws InterruptedException
        {
        TestResult resultLast = null;
        long       nPeriod    = Long.getLong("test.sample.period", SAMPLE_PERIOD);

        long ldtStart = System.currentTimeMillis();
        long ldtLast = ldtStart;
        int cLast = 0;

        monitor.setResultCount(colRunner.size());
        while (!monitor.waitAll(nPeriod))
            {
            TestResult resultAll = new TestResult();

            // collect samples from all runners
            int cNow = 0;
            for (Iterator iter = colRunner.iterator(); iter.hasNext(); )
                {
                Channel channel = (Channel) iter.next();

                // create and send a SampleRequest
                RunnerProtocol.SampleRequest req = (RunnerProtocol.SampleRequest) channel.getMessageFactory().createMessage(
                        RunnerProtocol.SampleRequest.TYPE_ID);

                Collection colResult = (Collection) channel.request(req);
                if (colResult != null)
                    {
                    cNow += colResult.size();
                    resultAll.add(colResult);
                    }
                }
            if (cNow < cLast)
                {
                // runners have finished; we cannot output lifetime and
                // delta results once one or more runners have finished
                continue;
                }

            // compute lifetime results
            long ldtNow = System.currentTimeMillis();
            resultAll.setDuration(ldtNow - ldtStart);

            // compute current results
            TestResult resultDelta = resultAll.computeDelta(resultLast);
            resultDelta.setDuration(ldtNow - ldtLast);

            // prepare for next sample
            resultLast = resultAll;
            ldtLast = ldtNow;
            cLast = cNow;

            // output results
            out();
            out("*** Lifetime:");
            outputResult(resultAll);
            out();
            out("*** Current:");
            outputResult(resultDelta);
            }

        // output final results
        TestResult resultFinal = monitor.getResult();
        out();
        out("*** Summary:");
        outputResult(resultFinal);
        setResult(resultFinal);
        }

    /**
     * Output summary information about the given TestResult.
     *
     * @param result the TestResult
     */
    protected void outputResult(TestResult result)
        {
        out("Duration:              " + result.getDuration() + "ms");
        out("Successful Operations: " + result.getSuccessCount());
        out("Failed Operations:     " + result.getFailureCount());
        out("Total Rate:            " + result.getRate() + "ops");
        out("Total Throughput:      " + toBandwidthString(
                result.getThroughput(), false));
        out("Latency:               " + result.getLatency());
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the InvocationService used by the Console to communicate with Agent processes.
     *
     * @return the InvocationService used by the Console
     */
    protected InvocationService getInvocationService()
        {
        return (InvocationService) CacheFactory.getService(
                Agent.INVOCATION_SERVICE);
        }

    /**
     * Return the hostname or IP address of the Console process.
     *
     * @return the hostname of IP address of the Console process
     */
    public String getAddress()
        {
        return m_sAddr;
        }

    /**
     * Return the port that the Console process is listening on.
     *
     * @return the listen port of the Console process
     */
    public int getPort()
        {
        return m_nPort;
        }

    /**
     * Return the optional command that the Console should run on startup.
     *
     * @return the option command that the Console should run on startup
     */
    public String getCommand()
        {
        return m_sCmd;
        }

    /**
     * Return the TCP/IP ConnectionAcceptor used by the Console to communicate with Runner processes.
     *
     * @return the TCP/IP ConnectionAcceptor used by the Console
     */
    protected ConnectionAcceptor getAcceptor()
        {
        return m_acceptor;
        }

    /**
     * Return the set of running Agent processes.
     *
     * @return the set of running Agent processes
     */
    protected Set getAgentSet()
        {
        InvocationService service = getInvocationService();

        // return the set of Members running the InvocationService, minus
        // the local Member
        Set set = new HashSet(service.getInfo().getServiceMembers());
        //set.remove(CacheFactory.ensureCluster().getLocalMember());
        set.remove(CacheFactory.getCluster().getLocalMember());

        return set;
        }

    /**
     * Return the set of running Runner processes.
     *
     * @return the set of running Runner processes
     */
    protected Set getRunnerSet()
        {
        return m_setRunner;
        }

    /**
     * Return the TestMonitor used by this Console to wait for the completion of the current test.
     *
     * @return the TestMonitor
     */
    protected TestMonitor getTestMonitor()
        {
        return m_monitor;
        }

    /**
     * Set the TestMonitor used by this Console to wait for the completion of the current test
     *
     * @param monitor the TestMonitor
     */
    protected void setTestMonitor(TestMonitor monitor)
        {
        m_monitor = monitor;
        }

    /**
     * Return the result of the last test.
     *
     * @return the result of the last test or null if a test hasn't been run
     */
    protected TestResult getResult()
        {
        return m_result;
        }

    /**
     * Set the result of the last test.
     *
     * @param result the result of the last test
     */
    protected void setResult(TestResult result)
        {
        m_result = result;
        }


    public boolean awaitReady(long nTimeout)
            throws Exception
        {
        if (!m_fReady.get())
            {
            synchronized (this)
                {
                if (!m_fReady.get())
                    {
                    this.wait(nTimeout);
                    }
                }
            }


        return m_fReady.get();
        }

    // ----- data members ---------------------------------------------------
    /**
     * The hostname or IP address of the Console process.
     */
    private final String m_sAddr;
    /**
     * The port that the Console process is listening on.
     */
    private final int m_nPort;
    /**
     * The optional initial command to run.
     */
    private final String m_sCmd;
    /**
     * The TCP/IP acceptor.
     */
    private final ConnectionAcceptor m_acceptor;
    /**
     * The set of open Runner Channels.
     */
    private final Set m_setRunner = new SafeHashSet();
    /**
     * The current TestMonitor.
     */
    private volatile TestMonitor m_monitor;
    /**
     * The result of the last test.
     */
    private TestResult m_result;

    /**
     * Flag indicating that the task should stop on an error.
     */
    private boolean m_fStopOnError = false;

    // ----- constants ------------------------------------------------------
    /**
     * Command line option for specifying the listen address.
     */
    public static final String OPTION_ADDRESS = "address";
    /**
     * Command line option for specifying the listen port.
     */
    public static final String OPTION_PORT = "port";
    /**
     * Default value of the {@link #OPTION_ADDRESS} option.
     */
    public static final String DEFAULT_ADDRESS = "localhost";
    /**
     * Default value of the {@link #OPTION_PORT} option.
     */
    public static final int DEFAULT_PORT = 7778;
    /**
     * The array of all valid command line options.
     */
    public static final String[] VALID_OPTIONS = {
            OPTION_ADDRESS,
            OPTION_PORT
    };
    /**
     * Command line switch for outputing help text.
     */
    public static final String SWITCH_HELP = "?";
    /**
     * The array of all valid command line switches.
     */
    public static final String[] VALID_SWITCHES = {
            SWITCH_HELP
    };
    /**
     * Command to exit the Console.
     */
    public static final String COMMAND_EXIT = "bye";
    /**
     * Command to output the list of Agent connections.
     */
    public static final String COMMAND_AGENTS = "agents";
    /**
     * Command to output the list of Runner connections.
     */
    public static final String COMMAND_RUNNERS = "runners";
    /**
     * Command to start Runner processes.
     */
    public static final String COMMAND_START = "start";
    /**
     * Command to stop Runner processes.
     */
    public static final String COMMAND_STOP = "stop";
    /**
     * Command to wait for Runner processes to start.
     */
    public static final String COMMAND_WAIT = "wait";
    /**
     * Command to wait for a specified amount of time.
     */
    public static final String COMMAND_SLEEP = "sleep";
    /**
     * Command to clear a NamedCache.
     */
    public static final String COMMAND_CLEAR = "clear";
    /**
     * Command to populate a NamedCache.
     */
    public static final String COMMAND_LOAD = "load";
    /**
     * Command to add/remove an index to/from a NamedCache.
     */
    public static final String COMMAND_INDEX = "index";
    /**
     * Command to update a NamedCache.
     */
    public static final String COMMAND_PUT = "put";
    /**
     * Command to update a NamedCache with two distributed services.
     */
    public static final String COMMAND_PUT2SERV = "put2serv";
    /**
     * Command to update a NamedCache with mixed value sizes.
     */
    public static final String COMMAND_PUTMIXED = "putmixed";
    /**
     * Command to update a NamedCache with mixed values and  sizes.
     */
    public static final String COMMAND_PUTMIXEDCONTENT = "putmixedcontent";
    /**
     * Command to update a NamedCache with mixed complex values and  sizes.
     */
    public static final String COMMAND_PUTMIXEDCOMPLEXCONTENT = "putmixedcomplexcontent";
    /**
     * Command to retrieve values from a NamedCache.
     */
    public static final String COMMAND_GET = "get";
    /**
     * Command to retrieve values from a two NamedCache services.
     */
    public static final String COMMAND_GET2SERV = "get2serv";
    /**
     * Command to simluate an arbitrary test on a NamedCache.
     */
    public static final String COMMAND_RUN = "run";
    /**
     * Command to perform a benchmark on a NamedCache.
     */
    public static final String COMMAND_BENCH = "bench";
    /**
     * Command to query a NamedCache.
     */
    public static final String COMMAND_QUERY = "query";
    /**
     * Command to perform an aggregation against a NamedCache.
     */
    public static final String COMMAND_DISTINCT = "distinct";
    /**
     * Command to save the result of the last test to a file.
     */
    public static final String COMMAND_SAVE = "save";
    /**
     * Command to execute a sequence of commands stored in a file.
     */
    public static final String COMMAND_SCRIPT = "script";
    /**
     * The class name of a TCP/IP-based ConnectionAcceptor implementation.
     */
    private static final String TCP_ACCEPTOR_CLASS =
            "com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor";
    /**
     * The time in milliseconds between samples.
     */
    private static final long SAMPLE_PERIOD = 10000L;

    private AtomicBoolean m_fReady = new AtomicBoolean(false);
    }
