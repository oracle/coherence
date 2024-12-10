/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import com.tangosol.net.CacheFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple utility to parse versions.
 */
public class VersionUtils
    {
    /**
     * Private constructor for utility class.
     */
    private VersionUtils()
        {
        }

    /**
     * Check a version.
     *
     * @param v1  the first version to compare
     * @param v2  the second version to compare
     * @return the version comparison
     */
    public static int compare(String v1, String v2)
        {
        String[] v1Parts   = splitVersion(v1);
        String[] v2Parts   = splitVersion(v2);
        int      partCount = Math.min(v1Parts.length, v2Parts.length);

        if (partCount > 0)
            {
            for (int i = 0; i < partCount; i++)
                {
                int nResult = v1Parts[i].compareTo(v2Parts[i]);
                if (nResult != 0)
                    {
                    return nResult;
                    }
                }
            }

        // versions are equal
        return 0;
        }

    private static String[] splitVersion(String version)
        {
        Matcher  matcher = PATTERN.matcher(version);
        String[] anPart;

        if (matcher.matches())
            {
            int groupCount = matcher.groupCount();
            anPart = new String[groupCount];

            for (int i = 1; i <= groupCount; i++)
                {
                try
                    {
                    anPart[i - 1] = matcher.group(i);
                    }
                catch (NumberFormatException e)
                    {
                    anPart[i - 1] = "";
                    }
                }
            }
        else
            {
            anPart = new String[0];
            }

        return anPart;
        }

    // ----- data members ---------------------------------------------------
    private static final Pattern PATTERN = Pattern.compile("(\\d*)\\D*(\\d*)\\D*(\\d*)\\D*(\\d*)\\D*(\\d*)\\D*");

    }
