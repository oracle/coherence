/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link CheckJDK} helper methods.
 *
 * @author jf  2022.03.22
 */
public class CheckJDKTest
    {
    @Test
    public void testComputeVersionFromJavaHome()
        {
        String[] asJdkHome =
                {
                    "/Library/Java/JavaVirtualMachines/1.5.0.jdk/Contents/Home",
                    "/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home",
                    "/Library/Java/JavaVirtualMachines/1.7.0.jdk/Contents/Home",
                    "/Library/Java/JavaVirtualMachines/1.8.0.jdk/Contents/Home",
                    "/Library/Java/JavaVirtualMachines/1.9.0.jdk/Contents/Home",
                    "/Library/Java/JavaVirtualMachines/1.10.0.jdk/Contents/Home",
                    "/Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home",
                    "/Library/Java/JavaVirtualMachines/jdk-15.jdk/Contents/Home",
                    "/Library/Java/JavaVirtualMachines/jdk-16.jdk/Contents/Home",
                    "/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home",
                    "/Library/Java/JavaVirtualMachines/jdk-18.jdk/jdk-18.jdk/Contents/Home",
                    "/Library/Java/JavaVirtualMachines/jdk-19.jdk/jdk-19.jdk/Contents/Home"
                };

        int[] anExpectedVersion = {5, 6, 7, 8, 9, 10, 11, 15, 16, 17, 18, 19};

        for (int i = 0; i < asJdkHome.length; i++)
            {
            assertEquals("verifying Jdk.of(JAVA_HOME=" + asJdkHome[i] + ")", anExpectedVersion[i], CheckJDK.computeVersionFromJavaHome(asJdkHome[i]));
            }
        }

    @Test
    public void testComputeVersion()
        {
        String[] asJdkVersion =
                {
                    "1.5.0",
                    "1.6.0.",
                    "1.7.0.",
                    "1.8.0.",
                    "1.9.0.",
                    "1.10.0.",
                    "11.0.7",
                    "15.0.9",
                    "16.0",
                    "17.0",
                    "18.0",
                    "19.0.ea"
                };

        int[] anExpectedVersion = {5, 6, 7, 8, 9, 10, 11, 15, 16, 17, 18, 19};

        for (int i = 0; i < asJdkVersion.length; i++)
            {
            assertEquals("verifying java.version=" + asJdkVersion[i] + ")", anExpectedVersion[i], CheckJDK.computeVersion(asJdkVersion[i]));
            }
        }
    }