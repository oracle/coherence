/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.util;


import java.nio.file.Files;
import java.nio.file.Path;
import javax.management.MBeanServer;
import java.io.File;
import java.lang.management.ManagementFactory;

import com.sun.management.HotSpotDiagnosticMXBean;

import java.util.Iterator;

/**
 * HeapDump allows for collection of hprof based heap dumps.
 *
 * @author hr  2013.08.08
 */
public class HeapDump
    {
    /**
     * Optionally collect a heap dump of live objects for the specified bug id.
     *
     * The dumps collected by this method can be enabled via <tt>-Dcom.oracle.coherence.common.internal.util.HeapDump=true</tt>
     * or more narrowly via <tt>-Dcom.oracle.coherence.common.internal.util.HeapDump.bugid=true</tt>.  The location of the dumps
     * can be controlled via <tt>-Dcom.oracle.coherence.common.internal.util.HeapDump.dir=path</tt>, which if left unset will
     * default ot the temp directory. <tt>-Dcom.oracle.coherence.common.internal.util.HeapDump.bugid.limit=n</tt> or
     * <tt>-Dcom.oracle.coherence.common.internal.util.HeapDump.limit=n</tt> can be used to limit the total number of heap dumps
     * collected for a specific bug id, the default limit is <tt>3</tt>.
     *
     * @param sBugId  the bug id, this will become part of the generated file name and system properties
     *
     * @return the name of the store file or null if dumps have been disabled
     */
    public static String dumpHeapForBug(String sBugId)
        {
        String sClass   = HeapDump.class.getName();
        String sPropBug = sClass + "." + sBugId;

        if (Boolean.parseBoolean(System.getProperty(sPropBug, System.getProperty(sClass, "false"))))
            {
            String sFileName;
            try
                {
                String sDirName;
                try
                    {
                    sDirName = System.getProperty(sClass + ".dir", System.getProperty("java.io.tmpdir", "."));
                    }
                catch (Throwable t)
                    {
                    sDirName = ".";
                    }

                File fileDir = new File(sDirName);
                int  cLimit  = Integer.parseInt(System.getProperty(sPropBug + ".limit",
                                                System.getProperty(sClass   + ".limit", "3")));

                if (cLimit <= 0)
                    {
                    return null;
                    }

                String sPrefix = sBugId + "-";
                for (Iterator<Path> iter = Files.list(fileDir.toPath()).iterator(); iter.hasNext(); )
                    {
                    Path path = iter.next();
                    if (path.getFileName().toFile().getName().startsWith(sPrefix))
                        {
                        if (--cLimit <= 0)
                            {
                            return null;
                            }
                        }
                    }
                File file = File.createTempFile(sPrefix, ".hprof", fileDir);
                sFileName = file.getCanonicalPath();
                file.delete();
                }
            catch (Exception e)
                {
                throw new RuntimeException(e);
                }

            return dumpHeap(sFileName, true);
            }

        return null;
        }

    /**
     * Collect a heap dump of live objects and write it into a dynamically named file in the temp directory,
     * or <tt>-Dcom.oracle.coherence.common.internal.util.HeapDump.dir=path</tt> if defined.
     *
     * @return the name of the stored file
     */
    public static String dumpHeap()
        {
        return dumpHeap(null, true);
        }

    /**
     * Collect a heap dump and write it into the specified file.
     *
     * @param sFileName  the file in which to store the heap dump, or directory in which a dynamically named
     *                   hprof file will be saved, or null for a dynamic file in the system temp directory
     * @param fLive      true if only live/reachable objects should be represented in the dump.
     *
     * @return the name of the stored file
     */
    public static String dumpHeap(String sFileName, boolean fLive)
        {
        // initialize hotspot diagnostic MBean
        initHotspotMBean();
        try
            {
            if (sFileName == null)
                {
                try
                    {
                    sFileName = System.getProperty(HeapDump.class.getName() + ".dir",
                                System.getProperty("java.io.tmpdir", "."));
                    }
                catch (Throwable t)
                    {
                    sFileName = ".";
                    }
                }

            File file = new File(sFileName);
            if (file.isDirectory())
                {
                file = File.createTempFile("heapdump-", ".hprof", file);
                sFileName = file.getCanonicalPath().startsWith("/private") ? file.getAbsolutePath() : file.getCanonicalPath();
                file.delete();
                }
            s_hotspotDiagMBean.dumpHeap(sFileName, fLive);
            }
        catch (RuntimeException re)
            {
            throw re;
            }
        catch (Exception exp)
            {
            throw new RuntimeException(exp);
            }

        return sFileName;
        }

    // initialize the hotspot diagnostic MBean field
    private static void initHotspotMBean()
        {
        if (s_hotspotDiagMBean == null)
            {
            synchronized (HeapDump.class)
                {
                if (s_hotspotDiagMBean == null)
                    {
                    try
                        {
                        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                        HotSpotDiagnosticMXBean bean =
                                ManagementFactory.newPlatformMXBeanProxy(server,
                                        HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
                        s_hotspotDiagMBean = bean;
                        }
                    catch (RuntimeException re)
                        {
                        throw re;
                        }
                    catch (Exception exp)
                        {
                        throw new RuntimeException(exp);
                        }
                    }
                }
            }
        }


    // ----- data members ---------------------------------------------------

    /**
     * The name of the HotSpot Diagnostic MBean
     */
    private static final String HOTSPOT_BEAN_NAME = "com.sun.management:type=HotSpotDiagnostic";

    /**
     * The hotspot diagnostic MBean
     */
    private static volatile HotSpotDiagnosticMXBean s_hotspotDiagMBean;
    }