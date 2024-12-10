/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * Gradle Task Configuration logic for the main classes.
 *
 * @author Gunnar Hillert  2023.10.05
 * @since 23.09.1
 */
public class CoherenceTaskRegistrationAction extends AbstractCoherenceTaskRegistrationAction
    {

    //----- constructors ----------------------------------------------------

    /**
     * Common constructor for the CoherenceTaskRegistrationAction
     * @param coherenceExtension configuration for data for the Gradle task
     * @param project the Gradle project
     * @param javaCompileTask the Gradle compile task
     * @param resourcesFolders The Gradle {@link Provider} for the destination directory of the processResources task
     */
    public CoherenceTaskRegistrationAction(CoherenceExtension coherenceExtension, Project project, JavaCompile javaCompileTask, FileCollection resourcesFolders)
        {
        super(coherenceExtension, project, javaCompileTask, resourcesFolders);
        }

    // ----- CoherenceTaskRegistrationAction methods ------------------------

    /**
     * Configure the Coherence Pof {@link org.gradle.api.Task}. Delegates the bulk of setup
     * to {@link AbstractCoherenceTaskRegistrationAction}.
     * @param coherencePofTask the task to be configured
     */
    @Override
    public void execute(CoherenceTask coherencePofTask)
        {
        coherencePofTask.getLogger().debug("CoherenceTaskRegistrationAction.execute() called");

        coherencePofTask.setGroup("Coherence");
        coherencePofTask.setDescription("Generate Pof-instrumented classes.");

        super.execute(coherencePofTask);
        }
    }
