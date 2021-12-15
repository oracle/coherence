/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package test;

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
        int nCurrentVersion = Integer.valueOf(System.getProperty("java.version").split("-|\\.")[0]);
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
        int nCurrentVersion = Integer.valueOf(System.getProperty("java.version").split("-|\\.")[0]);
        Assume.assumeThat("Skipping test for Java version " + nCurrentVersion + " greater than required max version of " + nMaxVersion,
                          nCurrentVersion, Matchers.lessThanOrEqualTo(nMaxVersion));
        }

    public static final String JDK_VENDOR_ORACLE = "Oracle Corporation";
    }
