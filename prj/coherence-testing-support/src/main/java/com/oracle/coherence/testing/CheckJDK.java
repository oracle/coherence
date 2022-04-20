/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import org.hamcrest.Matchers;
import org.junit.Assume;

import static org.hamcrest.CoreMatchers.is;

/**
 * A utility to check the JDK vendor
 *
 * @author jk  2020.03.26
 */
public class CheckJDK
    {
    public static void assumeOracleJDK()
        {
        String sVendor = System.getProperty("java.vm.vendor");
        Assume.assumeThat("Skipping test - JVM vendor is not Oracle", sVendor, is(JDK_VENDOR_ORACLE));
        }

    /**
     * Skip test if current Java version is less than required {@code} nMinVersion.
     *
     * @param nMinVersion  required minimum Java version to run test
     */
    public static void assumeJDKVersionEqualOrGreater(int nMinVersion)
        {
        // look up current Java version
        int nCurrentVersion = computeVersion(System.getProperty("java.version"));

        Assume.assumeThat("Skipping test for Java version " + nCurrentVersion + " less than required version of " + nMinVersion,
                          nCurrentVersion, Matchers.greaterThanOrEqualTo(nMinVersion));
        }


    /**
     * Skip test if current Java version is greater than required {@code} nMaxVersion.
     * Use when a tested feature has been deprecated and defaulted to not allowed.
     *
     * @param nMaxVersion  required maximum Java version to run test
     */
    public static void assumeJDKVersionLessThanOrEqual(int nMaxVersion)
        {
        // look up current Java version
        int nCurrentVersion = computeVersion(System.getProperty("java.version"));

        Assume.assumeThat("Skipping test for Java version " + nCurrentVersion + " greater than required max version of " + nMaxVersion,
                          nCurrentVersion, Matchers.lessThanOrEqualTo(nMaxVersion));
        }

    // ----- helpers -----------------------------------------------------------

    /**
     * Return numeric Java version from java.version property.
     *
     * @param sVersion java.version string of 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 11, 15, 16, 17, 18, ...
     *
     * @return numeric Java version 5-11, 15 and on
     */
    public static int computeVersion(String sVersion)
        {
        String[] as = sVersion.split("-|\\.");

        return Integer.parseInt(as[0].equals("1") ? as[1] : as[0]);
        }

    /**
     * Compute java version from JAVA_HOME string.
     *
     * @param sJavaHome  path to JAVA_HOME
     *
     * @return numeric Java version 5-11, 15 and on
     */
    public static int computeVersionFromJavaHome(String sJavaHome)
        {
        // match jdk-11, jdk-15, jdk-16, ...
        int of = sJavaHome.indexOf("jdk-");
        if (of != -1)
            {
            return Integer.parseInt(sJavaHome.substring(of + 4, of + 6));
            }

        // match 1.5, 1.6, 1.7, 1.8, ...
        of = sJavaHome.indexOf("1.");
        if (of != -1)
            {
            String[] as = sJavaHome.substring(of).split("-|\\.");

            if (as.length > 1)
                {
                return Integer.parseInt(as[1]);
                }
            }

        throw new IllegalArgumentException("CheckJDK.computeVersionFromJavaHome: unable to compute java version from provided JAVA_HOME string of " + sJavaHome);
        }

    // ----- constants -------------------------------------------------------

    public static final String JDK_VENDOR_ORACLE = "Oracle Corporation";
    }
