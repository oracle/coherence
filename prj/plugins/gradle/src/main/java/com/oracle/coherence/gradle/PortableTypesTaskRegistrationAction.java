/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates.
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
public class PortableTypesTaskRegistrationAction extends AbstractPortableTypesTaskRegistrationAction
    {

    //----- constructors ----------------------------------------------------

    /**
     * Common constructor for the PortableTypesTaskRegistrationAction
     * @param coherenceExtension configuration for data for the Gradle task
     * @param project the Gradle project
     * @param javaCompileTask the Gradle compile task
     * @param resourcesFolders The Gradle {@link Provider} for the destination directory of the processResources task
     */
    public PortableTypesTaskRegistrationAction(PortableTypesExtension coherenceExtension, Project project, JavaCompile javaCompileTask, FileCollection resourcesFolders)
        {
        super(coherenceExtension, project, javaCompileTask, resourcesFolders);
        }

    // ----- PortableTypesTaskRegistrationAction methods ------------------------

    /**
     * Configure the Coherence Pof {@link org.gradle.api.Task}. Delegates the bulk of setup
     * to {@link AbstractPortableTypesTaskRegistrationAction}.
     * @param portableTypesTask the task to be configured
     */
    @Override
    public void execute(PortableTypesTask portableTypesTask)
        {
        portableTypesTask.getLogger().debug("PortableTypesTaskRegistrationAction.execute() called");

        portableTypesTask.setGroup("Coherence");
        portableTypesTask.setDescription("Generate Pof-instrumented classes.");

        super.execute(portableTypesTask);
        }
    }
