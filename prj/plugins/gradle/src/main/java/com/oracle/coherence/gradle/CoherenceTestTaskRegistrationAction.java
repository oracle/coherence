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
 * Gradle Task Configuration logic for the test classes.
 *
 * @author Gunnar Hillert  2023.10.05
 * @since 23.09.1
 */
public class CoherenceTestTaskRegistrationAction extends AbstractCoherenceTaskRegistrationAction {

    //----- constructors ----------------------------------------------------

    /**
     * Common constructor for the CoherenceTestTaskRegistrationAction
     * @param coherenceExtension configuration for data for the Gradle task
     * @param project the Gradle project
     * @param javaCompileTask the Gradle compile task
     * @param testResourcesFolders The Gradle {@link Provider} for the destination directory of the processTestResources task
     */
    public CoherenceTestTaskRegistrationAction(CoherenceExtension coherenceExtension, Project project,
                                               JavaCompile javaCompileTask, FileCollection testResourcesFolders)
        {
        super(coherenceExtension, project, javaCompileTask, testResourcesFolders);
        }

    // ----- CoherenceTestTaskRegistrationAction methods --------------------

    /**
     * Configure the Coherence Pof {@link org.gradle.api.Task} for Test classes. Delegates the bulk of setup
     * to {@link AbstractCoherenceTaskRegistrationAction}.
     * @param coherencePofTask the task to be configured
     */
    @Override
    public void execute(CoherenceTask coherencePofTask)
        {
        coherencePofTask.getLogger().debug("CoherenceTestTaskRegistrationAction.execute() called");
        coherencePofTask.onlyIf(task ->
                coherenceExtension.getInstrumentTestClasses().getOrElse(false));

        boolean isEnabled = coherenceExtension.getInstrumentTestClasses().getOrElse(false);
        if (isEnabled)
            {
            coherencePofTask.setGroup("Coherence");
            coherencePofTask.setDescription("Generate Pof-instrumented test classes.");
            }

        super.execute(coherencePofTask);
        }
    }
