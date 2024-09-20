/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import org.junit.jupiter.api.Assumptions;

/**
 * A JUnit 5 utility to check the JDK vendor
 */
public class Junit5CheckJDK
    {
    public static void assumeOracleJDK()
        {
        String sVendor = System.getProperty("java.vm.vendor");
        Assumptions.assumeTrue(JDK_VENDOR_ORACLE.equals(sVendor), "Skipping test - JVM vendor is not Oracle");
        }

    /**
     * Skip test if current Java version is less than required {@code} nMinVersion.
     *
     * @param sMinVersion  required minimum Java version to run test
     */
    public static void assumeJDKVersionGreaterThanEqual(String sMinVersion)
        {
        assumeJDKVersionGreaterThanEqual(computeVersion(sMinVersion));
        }

    /**
     * Skip test if current Java version is less than required {@code} nMinVersion.
     *
     * @param nMinVersion  required minimum Java version to run test
     */
    public static void assumeJDKVersionGreaterThanEqual(int nMinVersion)
        {
        // look up current Java version
        int nCurrentVersion = computeVersion(System.getProperty("java.version"));
        System.err.println("Checking test assumption that JDK version " + nCurrentVersion + " >= " + nMinVersion);
        Assumptions.assumeTrue(nCurrentVersion >= nMinVersion, "Skipping test for Java version " + nCurrentVersion + " less than required version of " + nMinVersion);
        }


    /**
     * Skip test if current Java version is greater than required {@code} nMaxVersion.
     * Use when a tested feature has been deprecated and defaulted to not allowed.
     *
     * @param sMaxVersion  required maximum Java version to run test
     */
    public static void assumeJDKVersionLessThanOrEqual(String sMaxVersion)
        {
        assumeJDKVersionLessThanOrEqual(computeVersion(sMaxVersion));
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
        System.err.println("Checking test assumption that JDK version " + nCurrentVersion + " <= " + nMaxVersion);
        Assumptions.assumeTrue(nCurrentVersion <= nMaxVersion, "Skipping test for Java version " + nCurrentVersion + " greater than required max version of " + nMaxVersion);
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
