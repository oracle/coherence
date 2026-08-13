/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.Action;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.tasks.AbstractCopyTask;
import org.gradle.api.tasks.TaskProvider;

import java.io.File;

import java.nio.file.Path;

/**
 * Configures a Gradle Shadow task to replace all contributing Coherence
 * security configuration resources with one semantically merged resource.
 * <p>
 * This transformer intentionally depends only on Gradle's stable copy-task
 * API. It therefore works with both the legacy
 * {@code com.github.johnrengelman.shadow} plugin and the current
 * {@code com.gradleup.shadow} plugin without binding the Coherence Gradle
 * plugin to one version of Shadow's transformer SPI.
 *
 * @author Aleks Seovic  2026.08.10
 * @since 26.1
 */
public class SecurityConfigShadowTransformer
        implements Action<AbstractCopyTask>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a security configuration Shadow transformer.
     *
     * @param taskMerge  the task that produces the merged resource
     */
    public SecurityConfigShadowTransformer(TaskProvider<SecurityConfigShadowTask> taskMerge)
        {
        f_taskMerge = taskMerge;
        }

    // ----- Action methods -------------------------------------------------

    @Override
    public void execute(AbstractCopyTask task)
        {
        if (task.getExtensions().getExtraProperties().has(CONFIGURED_MARKER))
            {
            return;
            }

        task.dependsOn(f_taskMerge);
        task.from(f_taskMerge.flatMap(SecurityConfigShadowTask::getOutputDirectory));
        task.eachFile(this::excludeUnmergedSecurityConfig);
        task.getExtensions().getExtraProperties().set(CONFIGURED_MARKER, Boolean.TRUE);
        }

    // ----- helper methods -------------------------------------------------

    private void excludeUnmergedSecurityConfig(FileCopyDetails details)
        {
        if (!SecurityConfigResourceMerger.RESOURCE_SECURITY_CONFIG.equals(details.getPath()))
            {
            return;
            }

        File fileMerged = f_taskMerge.get().getOutputFile().get().getAsFile();
        if (!sameFile(details.getFile(), fileMerged))
            {
            details.exclude();
            }
        }

    private static boolean sameFile(File fileOne, File fileTwo)
        {
        Path pathOne = fileOne.toPath().toAbsolutePath().normalize();
        Path pathTwo = fileTwo.toPath().toAbsolutePath().normalize();
        return pathOne.equals(pathTwo);
        }

    // ----- data members ---------------------------------------------------

    private static final String CONFIGURED_MARKER = "coherence.securityConfig.shadow.task.configured";

    private final TaskProvider<SecurityConfigShadowTask> f_taskMerge;
    }
