/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.StringTokenizer;

import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Files.walkFileTree;

/**
 * File related utility methods.
 *
 * @author mf  2017.07.26
 */
public class Files
    {
    /**
     * Return true if the specified file or path appears to be on a locally mounted filesystem.
     *
     * Note: By default the implementation will return a value of <tt>false</tt> if it cannot determine that
     * the file is local, this can be overridden by setting the <tt>com.oracle.common.io.Files.assumeLocal</tt>
     * system property to <tt>true</tt>.
     *
     * @param file  the file or path to query
     *
     * @return true if local
     */
    public static boolean isLocal(File file)
        {
        return isLocal(file, ASSUME_LOCAL);
        }

    /**
     * Return true if the specified file or path appears to be on a locally mounted filesystem.
     *
     * @param file          the file or path to query
     * @param fAssumeLocal  the value to return if a definitive answer cannot be determined
     *
     * @return true if local
     */
    public static boolean isLocal(File file, final boolean fAssumeLocal)
        {
        try
            {
            file.exists(); // trigger auto-mounter if any so that the mount point will be resolvable

            String sDir = file.isDirectory() ? file.getCanonicalPath() : file.getCanonicalFile().getParent();
            String sOS  = new StringTokenizer(System.getProperty("os.name").toLowerCase().trim()).nextToken();

            String sMountTable;
            switch (sOS)
                {
                case "windows":
                    // we can at least assume that anything on the boot drive is local
                    String sSystemDrive = System.getenv("SystemDrive");
                    if (sSystemDrive != null)
                        {
                        int ofDrive = sDir.indexOf(":");
                        if (ofDrive != -1)
                            {
                            String sDrive = sDir.substring(0, ofDrive + 1);
                            if (sDrive.equalsIgnoreCase(sSystemDrive))
                                {
                                return true;
                                }
                            }
                        }
                    return fAssumeLocal;

                case "mac": // Mac OS
                    // we can at least assume that anything on the boot drive is local
                    // Note: we could execute "mount" and parse the output for a more accurate result, but this
                    // really should be sufficient
                    return !sDir.startsWith("/Volumes/") || fAssumeLocal;

                case "linux":
                    sMountTable = "/proc/mounts";
                    break;

                default: // solaris, hpux, other unixes
                    sMountTable = "/etc/mnttab";
                    break;
                }

            String  sPointBest = "";
            boolean fLocal     = fAssumeLocal;
            try (BufferedReader in = new BufferedReader(new FileReader(new File(sMountTable))))
                {
                if (!sDir.endsWith("/"))
                    {
                    sDir += "/";
                    }

                for (;;)
                    {
                    String sLine = in.readLine();
                    if (sLine == null)
                        {
                        break;
                        }

                    sLine = sLine.trim();
                    if (sLine.isEmpty() || sLine.startsWith("#"))
                        {
                        continue;
                        }

                    StringTokenizer sMount  = new StringTokenizer(sLine);
                    String          sDevice = sMount.nextToken();
                    String          sPoint  = sMount.nextToken();
                    String          sFs     = sMount.nextToken();

                    if (!sPoint.endsWith("/"))
                        {
                        sPoint += "/";
                        }

                    if (sDir.startsWith(sPoint) && sPoint.length() >= sPointBest.length()) // mountpoint can appear multiple times
                        {
                        sPointBest = sPoint;
                        fLocal     = sDevice.equals    ("rootfs") ||  // linux systemd root mount
                                     sDevice.startsWith("/dev/")  ||  // linux/solaris normal local mounts
                                     sDevice.startsWith("rpool/");    // solaris root pool local mounts

                        if (sFs.equals("lofs"))
                            {
                            // solaris loopback filesystems, sDevice is the source mount; common for /export/home -> /home
                            fLocal = isLocal(new File(sDevice), fAssumeLocal);
                            }
                        else if (sFs.equals("ocfs2"))
                            {
                            // assume that fs type ocfs2 is sharable remote block storage.
                            return false;
                            }
                        }
                    }
                }

            return fLocal;
            }
        catch (Throwable e)
            {
            return fAssumeLocal;
            }
        }

    /**
     * Recursively delete directory and all the files and subdirectories within it.
     *
     * @param path  the path of a directory to delete
     *
     * @throws IOException if an I/O error occurs
     */
    public static void deleteDirectory(Path path) throws IOException
        {
        if (!exists(path))
            {
            return;
            }

        walkFileTree(path, new SimpleFileVisitor<>()
            {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
                {
                deleteIfExists(file);
                return FileVisitResult.CONTINUE;
                }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException
                {
                if (exc != null)
                    {
                    throw exc;
                    }

                // Retry logic for Windows
                for (int i = 0; i < DELETE_RETRY_COUNT; i++)
                    {
                    try
                        {
                        deleteIfExists(dir);
                        return FileVisitResult.CONTINUE;
                        }
                    catch (DirectoryNotEmptyException e)
                        {
                        try
                            {
                            Thread.sleep(100L * (1L << i)); // 100ms, 200ms, 400ms, ...
                            }
                        catch (InterruptedException ie)
                            {
                            Thread.currentThread().interrupt();
                            throw new IOException("Interrupted while retrying directory deletion", ie);
                            }
                        }
                    }

                // Final attempt (will log the list of files and throw if still not empty)
                try
                    {
                    deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                    }
                catch (DirectoryNotEmptyException e)
                    {
                    try (DirectoryStream<Path> stream = newDirectoryStream(dir))
                        {
                        for (Path entry : stream)
                            {
                            Logger.fine("Unable to delete: " + entry);
                            }
                        }
                    catch (IOException suppressed)
                        {
                        Logger.fine("Failed to list directory: " + dir + " → " + suppressed);
                        }
                    throw e;
                    }
                }
            });
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default value to assume if it cannot be determined if a path is local or not.
     */
    protected static final boolean ASSUME_LOCAL = Config.getBoolean(Files.class.getCanonicalName() + ".assumeLocal");

    /**
     * The maximum number of attempts for directory deletion.
     * <p/>
     * This is needed primarily on Windows, which tends to lock the files more
     * aggressively, and may not allow deletion of a file that's open, or has been
     * recently closed.
     */
    protected static final int DELETE_RETRY_COUNT = 5;
    }
