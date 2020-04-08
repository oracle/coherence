/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance.psr;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * @author jk 2015.11.25
 */
public class ConsoleExtended
        extends Console
    {
    public ConsoleExtended(String sAddr, int nPort, String sCmd)
        {
        super(sAddr, nPort, sCmd);
        }

    public static void main(String[] args)
        {
        try
            {
            final PipedOutputStream inputWriter = new PipedOutputStream();
            final PipedInputStream  inputReader = new PipedInputStream(inputWriter);

            s_printWriter = new PrintWriter(inputWriter);

            s_console = init(args, new ConsoleExtendedSupplier());
            s_console.run(new BufferedReader(new InputStreamReader(inputReader)));
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }


    public static class RunCommand
            implements RemoteCallable<TestResult>
        {
        public RunCommand(String sCommand)
            {
            this(sCommand, true);
            }

        public RunCommand(String sCommand, boolean fStopOnError)
            {
            m_sCommand     = sCommand;
            m_fStopOnError = fStopOnError;
            }

        @Override
        public TestResult call()
            {
            s_console.setStopOnError(true);
            s_console.doCommand(new ArrayList<>(), m_sCommand);
            return s_console.getResult();
            }

        private String m_sCommand;

        private boolean m_fStopOnError = false;
        }

    public static class IsReady
            implements RemoteCallable<Boolean>
        {
        @Override
        public Boolean call() throws Exception
            {
            try
                {
                boolean fReady = s_console.awaitReady(1000);
                return fReady;
                }
            catch (Exception e)
                {
                e.printStackTrace();
                }
            return false;
            }

        }

    public static class ConsoleExtendedSupplier
            implements Supplier<ConsoleExtended>
        {
        @Override
        public ConsoleExtended get(String sAddr, int nPort, String sCmd)
            {
            return new ConsoleExtended(sAddr, nPort, sCmd);
            }
        }

    public static PrintWriter s_printWriter;

    public static ConsoleExtended s_console;

    }
