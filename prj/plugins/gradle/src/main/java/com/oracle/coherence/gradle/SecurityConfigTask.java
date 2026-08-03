/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import com.tangosol.internal.util.security.SecurityConfigGenerator;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A Gradle task to generate {@code META-INF/coherence/security-config.xml}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
@CacheableTask
public abstract class SecurityConfigTask
        extends DefaultTask
    {
    //----- constructors ----------------------------------------------------

    /**
     * Default constructor for the SecurityConfigTask.
     */
    public SecurityConfigTask()
        {
        }

    //----- SecurityConfigTask methods -------------------------------------

    /**
     * Return the compiled classes directory to scan.
     *
     * @return the compiled classes directory to scan
     */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getClassesDirectory();

    /**
     * Return the generated security config output file.
     *
     * @return the generated security config output file
     */
    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    /**
     * Return whether generation should be skipped.
     *
     * @return whether generation should be skipped
     */
    @Input
    @Optional
    public abstract Property<Boolean> getSkip();

    /**
     * Generate {@code security-config.xml}.
     */
    @TaskAction
    public void generateSecurityConfig()
        {
        if (getSkip().getOrElse(false))
            {
            getLogger().info("security-config generation skipped");
            return;
            }

        if (!getClassesDirectory().isPresent())
            {
            getLogger().info("security-config generation skipped; classes directory is not specified");
            return;
            }

        Path pathClasses = getClassesDirectory().get().getAsFile().toPath();
        try
            {
            if (!Files.isDirectory(pathClasses) || !containsClassFile(pathClasses))
                {
                getLogger().info("security-config generation skipped; classes directory is missing or empty: {}", pathClasses);
                return;
                }

            SecurityConfigGenerator.GeneratedConfig config = SecurityConfigGenerator.generate(pathClasses);
            Path                                    path   = SecurityConfigGenerator.generateAndWrite(pathClasses);

            getLogger().info("wrote {} allowed-classes entries to {}", config.size(), path);
            }
        catch (IOException e)
            {
            throw new GradleException("Failed to generate security-config.xml from " + pathClasses, e);
            }
        }

    private static boolean containsClassFile(Path path)
            throws IOException
        {
        try (java.util.stream.Stream<Path> stream = Files.walk(path))
            {
            return stream.anyMatch(SecurityConfigTask::isClassFile);
            }
        }

    private static boolean isClassFile(Path path)
        {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".class");
        }
    }
