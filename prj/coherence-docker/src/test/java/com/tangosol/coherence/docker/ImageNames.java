/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.docker;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.console.NullApplicationConsole;
import com.oracle.bedrock.runtime.options.Argument;
import com.oracle.bedrock.runtime.options.WorkingDirectory;
import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.io.IOException;

/**
 * Test Docker image names utils.
 *
 * @author Jonathan Knight 2022.08.01
 */
public class ImageNames
    {
    /**
     * Verify the assumptions needed to run tests.
     */
    public static void verifyTestAssumptions()
        {
        String[] asImageName = getImageNames();
        Assumptions.assumeTrue(asImageName.length  > 0, "Skipping test, "
                + PROP_IMAGES + " property not set");
        Assumptions.assumeTrue(imagesExist(), "Skipping test, one or more images not present: " + IMAGE_NAMES);
        }

    /**
     * Verify that the images being tested is already present.
     *
     * @return {@code true} if the images being tested is present.
     */
    public static boolean imagesExist()
        {
        if (s_fImagesExist == null)
            {
            boolean fExists = true;

            String[] asImageName = getImageNames();
            if (asImageName.length == 0)
                {
                fExists = false;
                }

            for (String sImage : asImageName)
                {
                if (!imageExists(sImage))
                    {
                    fExists = false;
                    break;
                    }
                }

            s_fImagesExist = fExists;
            }
        return s_fImagesExist;
        }

    /**
     * Return the names of the test images.
     *
     * @return the names of the test images
     */
    public static String[] getImageNames()
        {
        if (s_asImageName == null)
            {
            if (IMAGE_NAMES != null && !IMAGE_NAMES.isEmpty())
                {
                s_asImageName = IMAGE_NAMES.split(",");
                }
            else
                {
                s_asImageName = new String[0];
                }
            }
        return s_asImageName;
        }

    /**
     * Verify that the image being tested is already present.
     *
     * @param sImage  the name of the image
     *
     * @return {@code true} if the image being tested is present.
     */
    public static boolean imageExists(String sImage)
        {
        String sDocker = "docker.io/";
        if (sImage.startsWith(sDocker))
            {
            sImage = sImage.substring(sDocker.length());
            }

        Platform platform = LocalPlatform.get();

        try
            {
            try (Application app = platform.launch("docker",
                                                   Argument.of("inspect"),
                                                   Argument.of(sImage),
                                                   NullApplicationConsole.builder()))
                {
                int exitCode = app.waitFor();
                return exitCode == 0;
                }
            }
        catch (Exception e)
            {
            return true;
            }
        }

    /**
     * The system property containing the image names.
     */
    public static final String PROP_IMAGES = "test.image.names";

    /**
     * The names of the images to test, set by the {@link #PROP_IMAGES} System property.
     */
    private static final String IMAGE_NAMES = System.getProperty(PROP_IMAGES);

    private static String[] s_asImageName;

    private static Boolean s_fImagesExist;
    }
