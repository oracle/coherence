/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.daemonpool.common;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Conservative parser for {@code -Djdk.tracePinnedThreads=full} member output.
 *
 * @author Aleks Seovic  2026.04.25
 * @since 26.04
 */
public final class PinnedThreadParser
    {
    private PinnedThreadParser()
        {
        }

    public static List<String> findPinnedThreadTraces(Path pathLog)
            throws IOException
        {
        if (!Files.isReadable(pathLog))
            {
            return List.of();
            }

        List<String> listLines  = Files.readAllLines(pathLog);
        List<String> listTraces = new ArrayList<>();

        for (int i = 0; i < listLines.size(); i++)
            {
            String sLine = listLines.get(i);
            if (isPinnedThreadLine(sLine))
                {
                StringBuilder sb = new StringBuilder(sLine);
                for (int j = i + 1; j < listLines.size() && j <= i + STACK_CONTEXT_LINES; j++)
                    {
                    String sStackLine = listLines.get(j);
                    if (sStackLine.isBlank())
                        {
                        break;
                        }
                    sb.append(System.lineSeparator()).append(sStackLine);
                    }
                listTraces.add(sb.toString());
                }
            }

        return listTraces;
        }

    private static boolean isPinnedThreadLine(String sLine)
        {
        String sLower = sLine.toLowerCase(Locale.ROOT);
        return (sLower.contains("virtualthread") || sLower.contains("virtual thread"))
                && (sLower.contains("pinned") || sLower.contains("reason:"));
        }

    private static final int STACK_CONTEXT_LINES = 32;
    }
