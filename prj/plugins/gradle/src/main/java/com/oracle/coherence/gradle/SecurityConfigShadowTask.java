/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

import java.nio.file.Path;

/**
 * Generates the semantically merged Coherence security configuration used by
 * a Gradle Shadow uber JAR.
 *
 * @author Aleks Seovic  2026.08.10
 * @since 26.1
 */
@CacheableTask
public abstract class SecurityConfigShadowTask
        extends DefaultTask
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@code SecurityConfigShadowTask}.
     */
    public SecurityConfigShadowTask()
        {
        }

    // ----- SecurityConfigShadowTask methods ------------------------------

    /**
     * Return the runtime classpath to scan.
     *
     * @return the runtime classpath to scan
     */
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    /**
     * Return the output directory containing the merged resource.
     *
     * @return the output directory
     */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    /**
     * Return the merged security configuration file.
     *
     * @return the merged security configuration file
     */
    @Internal
    public Provider<RegularFile> getOutputFile()
        {
        return getOutputDirectory().file(SecurityConfigResourceMerger.RESOURCE_SECURITY_CONFIG);
        }

    /**
     * Merge all security configuration resources from the runtime classpath.
     */
    @TaskAction
    public void mergeSecurityConfig()
        {
        Path pathOutput = getOutputFile().get().getAsFile().toPath();

        try
            {
            SecurityConfigResourceMerger.MergeResult result = SecurityConfigResourceMerger.merge(
                    getClasspath().getFiles(), pathOutput);

            getLogger().info("merged {} security-config sources containing {} entries into {}",
                    result.getSourceCount(), result.getEntryCount(), pathOutput);
            }
        catch (IOException e)
            {
            throw new GradleException("Failed to merge "
                    + SecurityConfigResourceMerger.RESOURCE_SECURITY_CONFIG, e);
            }
        }
    }
