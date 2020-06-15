/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.psr;


import com.tangosol.dev.tools.CommandLineTool;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.DefaultCacheServer;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.InvocationService;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
* A machine agent process that can launch Runner processes in response to
* commands from a Console.
* <p/>
* A Console process sends commands to one or more Agent processes via an
* InvocationService (@see #INVOCATION_SERVICE). The two commands supported
* by an Agent are as follows:
* <ul>
*   <li>{@link StartRunnerInvocable}: starts one or more Runner processes</li>
*   <li>{@link StopRunnerInvocable}: stops one or more Runner processes</li>
* </ul>
* Each Runner process connects back to the Console process over Extend TCP/IP
* and exectutes tests commands on behalf of the Console. The Runner process
* then reports statistics back to the Console one the test command completes.
*
* @author jh  2008.06.13
*/
public class Agent
        extends Base
        implements Runnable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a new Agent that connects to a Console process listening on the
    * specified address and port.
    *
    * @param sCmd      the command used to launch a Runner process; if null,
    *                  a Runner will be launched using the same JVM that is
    *                  running the Agent
    * @param sWorkDir  the working directory of launched Runner processes;
    *                  if null, the working directory of the Agent process
    *                  will be used
    * @param sLogDir   the directory that will contain Runner log files; if
    *                  null, the working directory of the Runner process
    *                  will be used
    */
    public Agent(String sCmd, String sWorkDir, String sLogDir)
        {
        // construct the Runner launch command, if necessary
        if (sCmd == null || sCmd.length() == 0)
            {
            // Surround classpath with double quotes on Windows, but not
            // Unix, as Runtime.exec will not include a shell to process
            // them
            String sClasspath = File.pathSeparatorChar == ';' ?
                    '"' + System.getProperty("java.class.path") + '"' :
                    System.getProperty("java.class.path");

            StringBuffer sb = new StringBuffer(System.getProperty("java.home"));
            sb.append(File.separatorChar);
            sb.append("bin");
            sb.append(File.separatorChar);
            sb.append("java -server -Xms32m -Xmx32m");

            // append any "tangosol.*" properties
            for (Iterator iter = System.getProperties().entrySet().iterator();
                 iter.hasNext(); )
                {
                Map.Entry entry  = (Map.Entry) iter.next();
                String    sKey   = (String) entry.getKey();
                String    sValue = (String) entry.getValue();
                if (sKey.startsWith("tangosol."))
                    {
                    sb.append(" -D");
                    sb.append(sKey);
                    if (sValue != null)
                        {
                        sb.append('=');
                        sb.append(sValue);
                        }
                    }
                }

            if (sClasspath != null)
                {
                sb.append(" -cp ");
                sb.append(sClasspath);
                }

            sb.append(' ');
            sb.append(Runner.class.getName());

            sCmd = sb.toString();
            }
        m_sRunnerCmd = sCmd;

        File file;

        // initialize the working directory
        if (sWorkDir == null || sWorkDir.length() == 0)
            {
            file = new File(".");
            }
        else
            {
            file = new File(sWorkDir);
            }

        if (!file.exists() || !file.isDirectory())
            {
            throw new IllegalArgumentException("Invalid working directory: " + file);
            }
        m_fileRunnerDir = file;

        // initialize the log directory
        if (sLogDir == null || sLogDir.length() == 0)
            {
            // use Runner work dir
            }
        else
            {
            file = new File(sLogDir);
            }

        if (!file.exists() || !file.isDirectory())
            {
            throw new IllegalArgumentException("Invalid log directory: " + file);
            }
        m_fileLogDir = file;

        // configure the ConfigurableCacheFactory
        DefaultConfigurableCacheFactory factory = new DefaultConfigurableCacheFactory();
        factory.setConfig(DefaultConfigurableCacheFactory.loadConfig(CACHE_CONFIG));
        CacheFactory.setConfigurableCacheFactory(factory);
        }


    // ----- Runnable interface ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void run()
        {
        // start the DefaultCacheServer daemon
        DefaultCacheServer.startDaemon();

        // set the Agent as the user context object
        getInvocationService().setUserContext(this);

        out("Agent started.");
        try
            {
            List list = getRunnerList();
            while (true)
                {
                ProcessInfo[] aInfo = getRunnerArray();
                if (aInfo != null)
                    {
                    for (int i = 0, c = aInfo.length; i < c; ++i)
                        {
                        ProcessInfo  info    = aInfo[i];
                        Process      process = info.getProcess();
                        OutputStream out     = info.getOutStream();
                        OutputStream err     = info.getErrStream();

                        try
                            {
                            InputStream in;

                            // drain output
                            in = process.getInputStream();
                            while (in.available() > 0)
                                {
                                out.write(in.read());
                                }

                            // drain error
                            in = process.getErrorStream();
                            while (in.available() > 0)
                                {
                                err.write(in.read());
                                }
                            }
                        catch (Exception e)
                            {
                            info.stop();
                            synchronized (list)
                                {
                                list.remove(info);
                                setRunnerArray((ProcessInfo[]) list.toArray(
                                        new ProcessInfo[list.size()]));
                                }
                            }
                        }
                    }

                synchronized (this)
                    {
                    this.wait(50L);
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
                DefaultCacheServer.shutdown();
                }
            catch (RuntimeException e)
                {
                }

            List list = getRunnerList();
            synchronized (list)
                {
                for (Iterator iter = list.iterator(); iter.hasNext(); )
                    {
                    ProcessInfo info = (ProcessInfo) iter.next();
                    iter.remove();
                    info.stop();
                    }

                setRunnerArray(null);
                }
            }
        out("Agent exited.");
        }


    // ----- entry point ----------------------------------------------------

    /**
    * Application entry point. Usage:
    * <pre>
    * java com.tangosol.coherence.psr.Agent -?
    * </pre>
    *
    * @param asArg  command line arguments
    */
    public static void main(String[] asArg)
        {
        String sCmd;
        String sWorkDir;
        String sLogDir;

        try
            {
            // extract switches from the command line
            List listArg      = new ArrayList(Arrays.asList(asArg));
            List listSwitches = CommandLineTool.extractSwitches(listArg,
                    VALID_SWITCHES);

            // show help, if necessary
            if (listSwitches.contains(SWITCH_HELP))
                {
                showInstructions();
                System.exit(-1);
                return;
                }

            // parse remainder of command line options
            Map mapCmd = CommandLineTool.parseArguments(asArg, VALID_OPTIONS,
                    true /*case sensitive*/);

            sCmd     = (String) CommandLineTool.processCommand(mapCmd,
                       OPTION_COMMAND, null);
            sWorkDir = (String) CommandLineTool.processCommand(mapCmd,
                       OPTION_WORK_DIR, null);
            sLogDir  = (String) CommandLineTool.processCommand(mapCmd,
                       OPTION_LOG_DIR, null);
            }
       catch (Throwable e)
            {
            err(e);
            showInstructions();
            System.exit(-1);
            return;
            }

        // output "run" parameters
        out("Agent configured as follows:");
        if (sCmd != null)
            {
            out("Command:  " + sCmd);
            }
        if (sWorkDir != null)
            {
            out("Work Dir: " + sWorkDir);
            }
        if (sLogDir != null)
            {
            out("Log Dir:  " + sLogDir);
            }
        out();

        // create and start a new Agent
        Agent agent = new Agent(sCmd, sWorkDir, sLogDir);
        agent.run();
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Display the command-line instructions.
    */
    protected static void showInstructions()
        {
        out("Agent [options]");
        out("where valid options are:");
        out("\t-" + OPTION_COMMAND  + " the command used to launch Runner processes");
        out("\t-" + OPTION_WORK_DIR + " the working directory of Runner processes");
        out("\t-" + OPTION_LOG_DIR  + "\t the directory of Runner log files");
        out("\t-" + SWITCH_HELP     + "\t\t show help text");
        }


    /**
    * Start a new Runner process that will connect to the Console process
    * listening on the given address and port.
    *
    * @param sAddr  the hostname or IP address of the Console process
    * @param nPort  the listen port of the Console process
    *
    * @throws IOException on process execution error
    */
    protected void startRunner(String sAddr, int nPort)
            throws IOException
        {
        assert sAddr != null && sAddr.length() > 0;
        assert nPort >= 0;

        List list = getRunnerList();
        synchronized (list)
            {
            // create log files
            File fileDir = getLogDir();
            File fileOut = new File(fileDir, "runner-" + list.size() + ".out");
            File fileErr = new File(fileDir, "runner-" + list.size() + ".err");

            // open the log files
            OutputStream out = new BufferedOutputStream(
                    new FileOutputStream(fileOut, false));
            OutputStream err = new BufferedOutputStream(
                    new FileOutputStream(fileErr, false));

            // append the Console address and port to the exec command
            String sCmd = getRunnerCommand() + ' ' + sAddr + ' ' + nPort;

            // launch the Runner
            Process process;
            try
                {
                process = Runtime.getRuntime().exec(sCmd, null, getRunnerDir());
                out("Launched: " + sCmd);
                list.add(new ProcessInfo(process, out, err));
                }
            catch (IOException e)
                {
                try
                    {
                    out.close();
                    }
                catch (IOException ee)
                    {
                    }

                try
                    {
                    err.close();
                    }
                catch (IOException ee)
                    {
                    }

                throw e;
                }

            setRunnerArray((ProcessInfo[]) list.toArray(
                    new ProcessInfo[list.size()]));
            }

        synchronized (this)
            {
            notifyAll();
            }
        }

    /**
    * Stop a Runner processes.
    *
    * @return true iff a Runner process was stopped
    */
    protected boolean stopRunner()
        {
        boolean fStopped = false;
        List    list     = getRunnerList();
        synchronized (list)
            {
            for (Iterator iter = list.iterator(); iter.hasNext(); )
                {
                ProcessInfo info = (ProcessInfo) iter.next();
                iter.remove();
                if (info.isRunning())
                    {
                    info.stop();
                    fStopped = true;
                    break;
                    }
                }

            ProcessInfo[] aInfo = getRunnerArray();
            if (aInfo == null || aInfo.length != list.size())
                {
                setRunnerArray((ProcessInfo[]) list.toArray(
                        new ProcessInfo[list.size()]));
                }
            }

        if (fStopped)
            {
            synchronized (this)
                {
                notifyAll();
                }

            out("Stopped Runner.");
            }

        return fStopped;
        }


    // ----- ProcessInfo inner class ----------------------------------------

    /**
    * A ProcessInfo encapsulates information about a Runner process.
    */
    public static class ProcessInfo
            extends Base
        {
        // --------------------------------------------------------------

        /**
        * Create a new ProcessInfo.
        *
        * @param process  the wrapped Process; must not be null
        * @param out      the Process output file stream; must not be null
        * @param err      the Process error file stream; must not be null
        */
        public ProcessInfo(Process process, OutputStream out, OutputStream err)
            {
            super();
            assert process != null;
            assert out     != null;
            assert err     != null;

            m_process = process;
            m_out     = out;
            m_err     = err;
            }

        // ----- lifecycle ----------------------------------------------

        /**
        * Determine if the Process is still running.
        *
        * @return true iff the Process is still running
        */
        public boolean isRunning()
            {
            try
                {
                getProcess().exitValue();
                return false;
                }
            catch (IllegalThreadStateException e)
                {
                return true;
                }
            }

        /**
        * Stop the wrapped Process and close its log file.
        */
        public void stop()
            {
            Process      process = getProcess();
            OutputStream out     = getOutStream();
            OutputStream err     = getErrStream();

            // drain output
            try
                {
                InputStream in = process.getInputStream();
                while (in.available() > 0)
                    {
                    out.write(in.read());
                    }
                }
            catch (Exception e)
                {
                }

            // drain error
            try
                {
                InputStream in = process.getErrorStream();
                while (in.available() > 0)
                    {
                    err.write(in.read());
                    }
                }
            catch (Exception e)
                {
                }

            try
                {
                out.close(); // also flushes the stream
                }
            catch (Exception e)
                {
                }
            try
                {
                err.close(); // also flushes the stream
                }
            catch (Exception e)
                {
                }

            try
                {
                process.destroy();
                }
            catch (Exception e)
                {
                }
            }

        // ----- accessors ----------------------------------------------

        /**
        * Return the wrapped Process.
        *
        * @return the wrapped Process
        */
        public Process getProcess()
            {
            return m_process;
            }

        /**
        * Return the OutputStream of the wrapped Process output file.
        *
        * @return the OutputStream of the wrapped Process output file
        */
        public OutputStream getOutStream()
            {
            return m_out;
            }

        /**
        * Return the OutputStream of the wrapped Process error file.
        *
        * @return the OutputStream of the wrapped Process error file
        */
        public OutputStream getErrStream()
            {
            return m_err;
            }

        // ----- data members -------------------------------------------

        /**
        * The wrapped Process.
        */
        private final Process m_process;

        /**
        * The OutputStream of the wrapped Process output file.
        */
        private final OutputStream m_out;

        /**
        * The OutputStream of the wrapped Process error file.
        */
        private final OutputStream m_err;
        }


    // ----- StartRunnerInvocable inner class -------------------------------

    /**
    * Invocable implementation used to start one or more Runner processes.
    */
    public static class StartRunnerInvocable
            extends AbstractInvocable
            implements ExternalizableLite
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        public StartRunnerInvocable()
            {
            }

        // ----- Invocable interface ------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            Agent agent = (Agent) getService().getUserContext();
            assert agent != null;

            int cRunner = 0;
            try
                {
                for (int i = 0, c = getRunnerCount(); i < c; ++i, ++cRunner)
                    {
                    agent.startRunner(getConsoleAddress(), getConsolePort());
                    }
                }
            catch (IOException e)
                {
                log(e);
                }

            setResult(cRunner);
            }

        // ----- ExternalizableLite interface ---------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in)
                throws IOException
            {
            setConsoleAddress(ExternalizableHelper.readSafeUTF(in));
            setConsolePort(ExternalizableHelper.readInt(in));
            setRunnerCount(ExternalizableHelper.readInt(in));
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            ExternalizableHelper.writeSafeUTF(out, getConsoleAddress());
            ExternalizableHelper.writeInt(out, getConsolePort());
            ExternalizableHelper.writeInt(out, getRunnerCount());
            }

        // ----- accessors ----------------------------------------------

        /**
        * Return the hostname or IP address of the Console process.
        *
        * @return the hostname of IP address of the Console process
        */
        public String getConsoleAddress()
            {
            return m_sConsoleAddr;
            }

        /**
        * Configure the hostname or IP address of the Console process.
        *
        * @param sAddr  the hostname of IP address of the Console process
        */
        public void setConsoleAddress(String sAddr)
            {
            assert sAddr != null && sAddr.length() > 0;
            m_sConsoleAddr = sAddr;
            }

        /**
        * Return the port that the Console process is listening on.
        *
        * @return the listen port of the Console process
        */
        public int getConsolePort()
            {
            return m_nConsolePort;
            }

        /**
        * Configure the port that the Console process is listening on.
        *
        * @param nPort  the listen port of the Console process
        */
        public void setConsolePort(int nPort)
            {
            assert nPort > 0;
            m_nConsolePort = nPort;
            }

        /**
        * The number of Runner processes to start.
        *
        * @return the number of Runner processes to start
        */
        public int getRunnerCount()
            {
            return m_cRunner;
            }

        /**
        * Configure the number of Runner processes to start.
        *
        * @param cRunner  the number of Runner processes to start
        */
        public void setRunnerCount(int cRunner)
            {
            assert cRunner > 0;
            m_cRunner = cRunner;
            }

        // ----- data members -------------------------------------------

        /**
        * The hostname or IP address of the Console process.
        */
        private String m_sConsoleAddr;

        /**
        * The port that the Console process is listening on.
        */
        private int m_nConsolePort;

        /**
        * The number of Runner processes to start.
        */
        private int m_cRunner;
        }


    // ----- StopRunnerInvocable inner class --------------------------------

    /**
    * Invocable implementation used to stop one or more Runner processes.
    */
    public static class StopRunnerInvocable
            extends AbstractInvocable
            implements ExternalizableLite
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        public StopRunnerInvocable()
            {
            }

        // ----- Invocable interface ------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            Agent agent = (Agent) getService().getUserContext();
            assert agent != null;

            int cRunner = 0;
            for (int i = 0, c = getRunnerCount(); i < c; ++i)
                {
                if (agent.stopRunner())
                    {
                    ++cRunner;
                    }
                }

            setResult(cRunner);
            }

        // ----- ExternalizableLite interface ---------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(DataInput in)
                throws IOException
            {
            setRunnerCount(ExternalizableHelper.readInt(in));
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            ExternalizableHelper.writeInt(out, getRunnerCount());
            }

        // ----- accessors ----------------------------------------------

        /**
        * The number of Runner processes to stop.
        *
        * @return the number of Runner processes to stop
        */
        public int getRunnerCount()
            {
            return m_cRunner;
            }

        /**
        * Configure the number of Runner processes to stop.
        *
        * @param cRunner  the number of Runner processes to stop
        */
        public void setRunnerCount(int cRunner)
            {
            assert cRunner > 0;
            m_cRunner = cRunner;
            }

        // ----- data members -------------------------------------------

        /**
        * The number of Runner processes to stop.
        */
        private int m_cRunner;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the InvocationService used by the Agent to communicate with a
    * Console process.
    *
    * @return the InvocationService used by the Agent
    */
    protected InvocationService getInvocationService()
        {
        return (InvocationService) CacheFactory.getService(
                INVOCATION_SERVICE);
        }

    /**
    * Return the command used to launch a Runner.
    *
    * @return the command used to launch a Runner
    */
    protected String getRunnerCommand()
        {
        return m_sRunnerCmd;
        }

    /**
    * Return the working directory of launched Runner processes.
    *
    * @return the working directory of launched Runner processes
    */
    protected File getRunnerDir()
        {
        return m_fileRunnerDir;
        }

    /**
    * Return the log file directory.
    *
    * @return the log file directory
    */
    protected File getLogDir()
        {
        return m_fileLogDir;
        }

    /**
    * Return the list of ProcessInfo objects, one per Runner process.
    *
    * @return the list of ProcessInfo objects, one per Runner process
    */
    protected List getRunnerList()
        {
        return m_listInfo;
        }

    /**
    * Return the array of ProcessInfo objects, one per Runner process.
    *
    * @return the array of ProcessInfo objects, one per Runner process
    */
    protected ProcessInfo[] getRunnerArray()
        {
        return m_aInfo;
        }

    /**
    * Set the array of ProcessInfo objects, one per Runner process.
    *
    * @param aInfo  the array of ProcessInfo objects, one per Runner process
    */
    protected void setRunnerArray(ProcessInfo[] aInfo)
        {
        m_aInfo = aInfo;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The command used to launch a Runner process.
    */
    private final String m_sRunnerCmd;

    /**
    * The working directory of launched Runner processes.
    */
    private final File m_fileRunnerDir;

    /**
    * The log directory.
    */
    private final File m_fileLogDir;

    /**
    * A list of ProcessInfo objects, one per Runner process.
    */
    private final List m_listInfo = new ArrayList();

    /**
    * An array of ProcessInfo objects, one per Runner process.
    */
    private ProcessInfo[] m_aInfo;


    // ----- constants ------------------------------------------------------

    /**
    * Command line option for specifying the command used to launch Runner
    * processes.
    */
    public static final String OPTION_COMMAND = "command";

    /**
    * Command line option for specifying the working directory of Runners.
    */
    public static final String OPTION_WORK_DIR = "workDir";

    /**
    * Command line option for specifying the directory of Runner log files.
    */
    public static final String OPTION_LOG_DIR = "logDir";

    /**
    * The array of all valid command line options.
    */
    public static final String[] VALID_OPTIONS =
            {
            OPTION_COMMAND,
            OPTION_WORK_DIR,
            OPTION_LOG_DIR
            };

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
    * The cache configuration used by the Agent.
    */
    public static final String CACHE_CONFIG =
            "/com/tangosol/coherence/performance/psr/agent-cache-config.xml";

    /**
    * The name of the InvocationService used by the Agent.
    */
    public static final String INVOCATION_SERVICE
            = "AgentInvocationService";
    }
