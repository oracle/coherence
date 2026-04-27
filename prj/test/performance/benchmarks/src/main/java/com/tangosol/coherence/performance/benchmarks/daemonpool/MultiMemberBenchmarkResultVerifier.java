/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.LinkedHashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small post-run guard for JMH runs that can exit successfully while omitting
 * result rows after benchmark teardown failures.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public class MultiMemberBenchmarkResultVerifier
    {
    public static void main(String[] asArg)
            throws IOException
        {
        Map<String, String> mapArgs = parseArgs(asArg);
        Path pathResult = Path.of(required(mapArgs, "resultFile"));
        int  cExpected  = Integer.parseInt(required(mapArgs, "expectedRows"));

        if (!Files.isRegularFile(pathResult))
            {
            throw new IllegalStateException("JMH result file does not exist: " + pathResult);
            }

        String sJson = Files.readString(pathResult);
        int    cRows = countBenchmarkRows(sJson);
        if (cRows != cExpected)
            {
            throw new IllegalStateException("JMH result row count mismatch for " + pathResult
                    + ": expectedRows=" + cExpected + ", actualRows=" + cRows);
            }

        String sLogFile = mapArgs.get("logFile");
        if (sLogFile != null)
            {
            Path pathLog = Path.of(sLogFile);
            if (Files.isRegularFile(pathLog))
                {
                String sLog = Files.readString(pathLog);
                if (sLog.contains("Measured-window pinned virtual-thread event detected"))
                    {
                    throw new IllegalStateException("Measured-window pin failure found in " + pathLog);
                    }
                }
            }

        System.out.println("MultiMemberBenchmarkResultVerifier passed: resultFile=" + pathResult
                + ", rows=" + cRows);
        }

    private static int countBenchmarkRows(String sJson)
        {
        Matcher matcher = BENCHMARK_FIELD.matcher(sJson);
        int     cRows   = 0;
        while (matcher.find())
            {
            cRows++;
            }
        return cRows;
        }

    private static Map<String, String> parseArgs(String[] asArg)
        {
        Map<String, String> mapArgs = new LinkedHashMap<>();
        for (String sArg : asArg)
            {
            int of = sArg.indexOf('=');
            if (of > 0)
                {
                mapArgs.put(sArg.substring(0, of), sArg.substring(of + 1));
                }
            }
        return mapArgs;
        }

    private static String required(Map<String, String> mapArgs, String sName)
        {
        String sValue = mapArgs.get(sName);
        if (sValue == null || sValue.isBlank())
            {
            throw new IllegalArgumentException("Missing required argument: " + sName);
            }
        return sValue;
        }

    private static final Pattern BENCHMARK_FIELD = Pattern.compile("\"benchmark\"\\s*:");
    }
