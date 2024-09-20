/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.hnswlib;

import com.oracle.coherence.common.base.Classes;
import com.oracle.coherence.common.base.Logger;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.tangosol.util.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Factory for the hnswlib JNA implementation.
 */
final class HnswlibFactory
    {
    private static Hnswlib instance;

    private HnswlibFactory()
        {
        }

    /**
     * Return a single instance of the loaded library.
     *
     * @return hnswlib JNA instance.
     */
    static synchronized Hnswlib getInstance()
        {
        if (instance == null)
            {
            try
                {
                checkIfLibraryProvidedNeedsToBeLoadedIntoSO();
                instance = Native.load(Hnswlib.LIBRARY_NAME, Hnswlib.class);
                }
            catch (UnsatisfiedLinkError | IOException | NullPointerException e)
                {
                Logger.err("Failed to load Coherence HNSW library", e);
                throw new UnsatisfiedLinkError("It's not possible to use the pre-generated dynamic libraries on your system. "
                                               + "Please compile it yourself (if not done yet) and set the \""
                                               + Hnswlib.JNA_LIBRARY_PATH_PROPERTY + "\" property with correct path to where \""
                                               + getLibraryFileName() + "\" is located.");
                }
            }
        return instance;
        }

    private static String getLibraryFileName()
        {
        String extension;
        if (Platform.isLinux())
            {
            extension = "so";
            }
        else if (Platform.isWindows())
            {
            extension = "dll";
            }
        else
            {
            extension = "dylib";
            }
        return String.format("libhnswlib-%s.%s", Platform.ARCH, extension);
        }

    private static void copyPreGeneratedLibraryFiles(Path folder, String fileName)
            throws IOException
        {
        URL         url           = Resources.findFileOrResource(fileName, Classes.getContextClassLoader());
        InputStream libraryStream = url.openStream();
        /* windows seems to be blocking manipulation of .lib files; we store as .libw for now. */
        Files.copy(libraryStream, folder.resolve(fileName.replace(".libw", ".lib")), StandardCopyOption.REPLACE_EXISTING);
        }

    private static void checkIfLibraryProvidedNeedsToBeLoadedIntoSO()
            throws IOException
        {
        String property = System.getProperty(Hnswlib.JNA_LIBRARY_PATH_PROPERTY);
        if (property == null)
            {
            Path libraryFolder = Files.createTempDirectory(Hnswlib.LIBRARY_NAME);
            copyPreGeneratedLibraryFiles(libraryFolder, getLibraryFileName());
            if (Platform.isWindows())
                {
                copyPreGeneratedLibraryFiles(libraryFolder, "libhnswlib-x86-64.exp");
                copyPreGeneratedLibraryFiles(libraryFolder, "libhnswlib-x86-64.libw");
                }
            System.setProperty(Hnswlib.JNA_LIBRARY_PATH_PROPERTY, libraryFolder.toString());
            libraryFolder.toFile().deleteOnExit();
            }
        else
            {
            Logger.info("Coherence HNSW: " + Hnswlib.JNA_LIBRARY_PATH_PROPERTY + " set to: " + property);
            }
        }
    }
