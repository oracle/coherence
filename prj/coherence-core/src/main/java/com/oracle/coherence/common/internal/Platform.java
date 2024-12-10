/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

import java.io.File;


/**
 * Platform provides information about the environment in which the JVM executes.
 *
 * @author mf 2013.03.21
 */
public class Platform
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Blocked constructor.
     */
    private Platform()
        {
        boolean fExa = false;

        try
            {
            fExa = new File("/opt/exalogic").exists() ||
                   new File("/opt/exalogic-java").exists();
            }
        catch (SecurityException e)
            {
            try
                {
                fExa = Class.forName("com.oracle.exalogic.ExaManager") != null; // identify if we are on Exalogic; TODO: and SSC detection
                }
            catch (Exception e2) {}
            }

        // the system property ultimately controls if we are exa or not, the presence of the ExaManager just sets the
        // default.  This allows us to enable exa on platforms which we don't detect as such, i.e. SSC, but also allows
        // us to easily disable exa optimizations on exa platforms if desired
        f_fExaEnabled = Boolean.parseBoolean(System.getProperty("com.oracle.exa", fExa ? "true" : "false")) ||
                        Boolean.parseBoolean(System.getProperty("com.oracle.exalogic")) ||
                        Boolean.parseBoolean(System.getProperty("com.oracle.exadata")) ||
                        Boolean.parseBoolean(System.getProperty("com.oracle.ssc"));

        // TODO: would be nice to identify if the JVM has been pinned to a limited number of CPUs in which
        // case the proper answer is just Runtime.availableProcessors()
        int cProcFair = Math.max(1, (int) Math.ceil((double) Runtime.getRuntime().availableProcessors() *
                Math.min(1.0, (double) Runtime.getRuntime().maxMemory() / (double) getTotalPhysicalMemorySize())));

        f_cProcFair = Integer.parseInt(System.getProperty(Platform.class.getName() + ".fairShareProcessors",
                                                          String.valueOf(cProcFair)));
        }

    /**
     * Return the Platform singleton instance.
     *
     * @return the Platform singleton instance.
     */
    public static Platform getPlatform()
        {
        return INSTANCE;
        }

    /**
     * Return true iff the platform is part of the Oracle exa family.
     * <p>
     * This property can be set directly via the <tt>com.oracle.exa</tt> system property.
     *
     * @return true iff the platform is part of the Oracle exa family.
     */
    public final boolean isExaEnabled()
        {
        return f_fExaEnabled;
        }

    /**
     * Return the total amount of physical memory present on the machine.
     *
     * @return the machine memory size in bytes, or Runtime.maxMemory() if the value cannot be determined
     */
    public long getTotalPhysicalMemorySize()
        {
        try
            {
            // TODO: Starting with Java 1.7 this method also exists on java.lang.management.OperatingSystemMXBean
            // as well as the legacy com.sun.management.OperatingSystemMXBean. When we drop 1.6 support, we can safely
            // switch to the "public" version
            return ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
            }
        catch (Throwable e)
            {
            return Runtime.getRuntime().maxMemory();
            }
        }

    /**
     * Return a fair share of processors for this process based on other JVM sizing limitations.
     *
     * This value can be set directly via the <tt>com.oracle.coherence.common.internal.Platform.fairShareProcessors</tt> system property.
     *
     * @return this processes fair share of processors
     */
    public int getFairShareProcessors()
        {
        return f_cProcFair;
        }

    // ----- data members ---------------------------------------------------

    /**
     * True iff this is an exa* platform.
     */
    private final boolean f_fExaEnabled;

    /**
     * This processes fair share processor count
     */
    private final int f_cProcFair;

    /**
     * Singleton Platform instance.
     */
    private static Platform INSTANCE = new Platform();
    }
