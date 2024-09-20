/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.Project;

import java.util.Objects;

/**
 * Contains various utility methods used by the Coherence Gradle Plugin.
 *
 * @author Gunnar Hillert  2023.03.16
 * @since 22.06.05
 */
public final class PluginUtils
    {
    //----- constructors ----------------------------------------------------

    /**
     * Private constructor for utility class.
     */
    private PluginUtils()
        {
        throw new AssertionError("This is a static utility class.");
        }

    //----- PluginUtils methods ---------------------------------------------

    /**
     * Returns the major Gradle version used by the Gradle {@link Project}.
     *
     * @param project the provided Gradle project must not be null
     *
     * @return the major Gradle version
     */
    static int getGradleMajorVersion(Project project)
        {
        Objects.requireNonNull(project, "The provided project must not be null.");

        String sGradleVersion = project.getGradle().getGradleVersion();
        return Integer.parseInt(sGradleVersion.substring(0, sGradleVersion.indexOf(".")));
        }
    }
