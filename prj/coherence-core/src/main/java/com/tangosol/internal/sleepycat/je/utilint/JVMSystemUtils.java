/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.sleepycat.je.utilint;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class JVMSystemUtils
    {
    private static final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    /*
     * Get the system load average for the last minute.
     */
    public static double getSystemLoad()
        {
        // back port of BDBJE #25383 (7.2.0) for shaded BDB JE 3.2.61, see COH-28458 for details.
        return osBean.getSystemLoadAverage();
        }
    }
