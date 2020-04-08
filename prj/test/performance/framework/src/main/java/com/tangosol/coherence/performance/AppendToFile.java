/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.io.PrintWriter;

/**
 * @author jk 2015.11.26
 */
public class AppendToFile
        implements RemoteRunnable
    {

    public AppendToFile(String sFileName, String... asLines)
        {
        m_sFileName = sFileName;
        m_asLines = asLines;
        }

    @Override
    public void run()
        {
        appendToFile(m_sFileName, m_asLines);
        }

    public static void appendToFile(String sFileName, String... asLines)
        {
        appendToFile(new File(sFileName), asLines);
        }

    public static void appendToFile(File file, String... asLines)
        {
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file, true))))
            {
            for (String s : asLines)
                {
                out.println(s);
                }
            out.flush();
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }

        }

    private String m_sFileName;

    private String[] m_asLines;
    }
