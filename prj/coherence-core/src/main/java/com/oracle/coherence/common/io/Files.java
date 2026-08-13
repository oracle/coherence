/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;

import com.tangosol.coherence.config.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.net.URI;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import java.util.Objects;
import java.util.StringTokenizer;

/**
 * File related utility methods.
 *
 * @author mf  2017.07.26
 */
public class Files
    {
    /**
     * Return a {@link Path} for a native file-system path or a local
     * {@code file:} URI.
     * <p>
     * Absolute native paths are recognized before URI parsing so that a
     * Windows drive letter is not mistaken for a URI scheme. Plain relative
     * paths are also supported. A URI must use the {@code file} scheme and
     * must not have an authority, query, or fragment.
     * <p>
     * This method does not impose locality restrictions on native paths. In
     * particular, a native UNC path may be returned on Windows; callers that
     * require local storage must enforce that policy separately.
     *
     * @param sPath  a native path or local {@code file:} URI
     *
     * @return the corresponding path
     *
     * @throws NullPointerException     if {@code sPath} is {@code null}
     * @throws IllegalArgumentException if {@code sPath} is invalid or uses an
     *                                  unsupported URI form
     */
    public static Path toPath(String sPath)
        {
        Objects.requireNonNull(sPath, "path");

        Path pathNative = null;
        try
            {
            pathNative = Path.of(sPath);
            }
        catch (InvalidPathException ignored)
            {
            }

        if (pathNative != null && pathNative.isAbsolute())
            {
            return pathNative;
            }

        URI    uri     = URI.create(sPath);
        String sScheme = uri.getScheme();
        if (sScheme == null || sScheme.isEmpty())
            {
            return pathNative == null ? Path.of(sPath) : pathNative;
            }

        if (!"file".equalsIgnoreCase(sScheme) || uri.getAuthority() != null
                || uri.getQuery() != null || uri.getFragment() != null)
            {
            throw new IllegalArgumentException("path must be a native path or local file URI");
            }

        return Path.of(uri);
        }

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


    // ----- constants ------------------------------------------------------

    /**
     * The default value to assume if it cannot be determined if a path is local or not.
     */
    protected static final boolean ASSUME_LOCAL = Config.getBoolean(Files.class.getCanonicalName() + ".assumeLocal");
    }
