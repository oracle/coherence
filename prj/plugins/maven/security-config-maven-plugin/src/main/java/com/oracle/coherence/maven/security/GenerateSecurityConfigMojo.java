/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.security;

import com.tangosol.internal.util.security.SecurityConfigGenerator;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generate {@code META-INF/coherence/security-config.xml}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
@Mojo(name = "generate",
      defaultPhase = LifecyclePhase.PROCESS_CLASSES,
      requiresDependencyResolution = ResolutionScope.NONE,
      threadSafe = true)
public class GenerateSecurityConfigMojo
        extends AbstractMojo
    {
    @Override
    public void execute()
            throws MojoExecutionException
        {
        if (skip)
            {
            getLog().info("security-config generation skipped");
            return;
            }

        try
            {
            Path pathClasses = classesDirectory.toPath();

            if (!Files.isDirectory(pathClasses) || !containsClassFile(pathClasses))
                {
                getLog().info("security-config generation skipped; classes directory is missing or empty: " + pathClasses);
                return;
                }

            Path path = SecurityConfigGenerator.generateAndWrite(pathClasses);
            getLog().info("wrote " + SecurityConfigGenerator.OUTPUT_RESOURCE + " to " + path);
            }
        catch (IOException | IllegalStateException e)
            {
            throw new MojoExecutionException("Failed to generate security-config.xml", e);
            }
        }

    /**
     * Set the classes directory.
     *
     * @param fileClassesDirectory  the classes directory
     */
    public void setClassesDirectory(File fileClassesDirectory)
        {
        classesDirectory = fileClassesDirectory;
        }

    /**
     * Set whether generation should be skipped.
     *
     * @param fSkip  {@code true} to skip generation
     */
    public void setSkip(boolean fSkip)
        {
        skip = fSkip;
        }

    private static boolean containsClassFile(Path path)
            throws IOException
        {
        try (java.util.stream.Stream<Path> stream = Files.walk(path))
            {
            return stream.anyMatch(GenerateSecurityConfigMojo::isClassFile);
            }
        }

    private static boolean isClassFile(Path path)
        {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".class");
        }

    // ----- data members ---------------------------------------------------

    /**
     * The compiled classes directory.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File classesDirectory;

    /**
     * Whether generation is skipped.
     */
    @Parameter(property = "coherence.security-config.skip", defaultValue = "false")
    private boolean skip;
    }
