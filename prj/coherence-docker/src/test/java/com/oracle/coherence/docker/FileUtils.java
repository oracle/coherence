/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.docker;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility methods for file operations.
 *
 * @author jk  2017.06.13
 */
public class FileUtils
    {
    /**
     * checkstyle requires private constructor.
     */
    private FileUtils()
        {
        }

    /**
     * Attempt to obtain the build output folder, typically the {@code target} folder in a Maven build.
     *
     * @param testClass the class to use to locate the folder if the {@code project.build.directory} property is not
     *                  set
     *
     * @return build output folder
     */
    public static File getBuildOutputFolder(Class testClass)
        {
        try
            {
            String dir = System.getProperty("project.build.directory");
            File file;

            if (dir == null || dir.trim().isEmpty())
                {
                URL url = testClass.getProtectionDomain().getCodeSource().getLocation();

                file = new File(url.toURI()).getParentFile();

                while (file != null)
                    {
                    if (file.getName().equals("target"))
                        {
                        break;
                        }

                    file = file.getParentFile();
                    }
                }
            else
                {
                file = new File(dir);
                }

            if (file == null)
                {
                file = new File(".");
                }

            file.mkdirs();

            return file;
            }
        catch (URISyntaxException e)
            {
            throw new RuntimeException(e);
            }
        }


    /**
     * Attempt to obtain the build output folder, typically the {@code target/test-output} folder in a Maven build.
     *
     * @param testClass the class to use to locate the folder if the {@code project.build.directory} property is not
     *                  set
     *
     * @return build output folder
     */
    public static File getTestOutputFolder(Class testClass)
        {
        File fileTarget = getBuildOutputFolder(testClass);

        return ensureFolders(new File(fileTarget, "test-output"));
        }

    /**
     * Attempt to obtain the build output folder, typically the {@code target/test-output} folder in a Maven build.
     *
     * @param testClass the class to use to locate the folder if the {@code project.build.directory} property is not
     *                  set
     * @param suffix    the suffix to use for the folder name
     *
     * @return build output folder
     */
    public static File getTestOutputFolder(Class testClass, String suffix)
        {
        File fileTestOutput = getTestOutputFolder(testClass);
        File fileTest = new File(fileTestOutput, testClass.getSimpleName());

        if (suffix != null && !suffix.trim().isEmpty())
            {
            fileTest = new File(fileTest, suffix);
            }

        return ensureFolders(fileTest);
        }

    private static File ensureFolders(File file)
        {
        if (!file.exists())
            {
            file.mkdirs();
            }

        return file;
        }
    }
