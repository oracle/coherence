/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package test;

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

    public static final String JDK_VENDOR_ORACLE = "Oracle Corporation";
    }
