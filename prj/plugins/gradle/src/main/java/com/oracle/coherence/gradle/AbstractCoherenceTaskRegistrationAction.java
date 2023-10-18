/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;

/**
 * Contains common task configuration logic.
 *
 * @author Gunnar Hillert  2023.10.05
 * @since 23.09.1
 * @see CoherenceTaskRegistrationAction
 * @see CoherenceTestTaskRegistrationAction
 */
public abstract class AbstractCoherenceTaskRegistrationAction implements Action<CoherenceTask> {

    //----- constructors ----------------------------------------------------

    /**
     * Common constructor for the AbstractCoherenceTaskRegistrationAction
     * @param coherenceExtension configuration for data for the Gradle task
     * @param project the Gradle project
     * @param javaCompileTask the Gradle compile task
     * @param processResourcesDestinationDirProvider The Gradle {@link Provider} for the destination directory of the processResources task
     */
    public AbstractCoherenceTaskRegistrationAction(CoherenceExtension coherenceExtension, Project project,
                                                   JavaCompile javaCompileTask, Provider<File> processResourcesDestinationDirProvider)
        {
        this.coherenceExtension                     = coherenceExtension;
        this.project                                = project;
        this.javaCompileTask                        = javaCompileTask;
        this.processResourcesDestinationDirProvider = processResourcesDestinationDirProvider;
        }

    // ----- AbstractCoherenceTaskRegistrationAction methods ----------------

    @Override
    public void execute(CoherenceTask coherencePofTask)
        {
        coherencePofTask.getClassesDirectory().set(javaCompileTask.getDestinationDirectory());
        applyInitialConfig(project, coherencePofTask);

        if (coherenceExtension.getUsePofSchemaXml().getOrElse(false))
            {
            coherencePofTask.getResourcesDirectory().set(processResourcesDestinationDirProvider);
            coherencePofTask.getUsePofSchemaXml().set(coherenceExtension.getUsePofSchemaXml());
            }
        if (coherenceExtension.getDebug().isPresent())
            {
            coherencePofTask.getDebug().set(coherenceExtension.getDebug());
            }

        if (coherenceExtension.getPofSchemaXmlPath().isPresent())
            {
            coherencePofTask.getPofSchemaXmlPath().set(coherenceExtension.getPofSchemaXmlPath());
            }
        }


    /**
     * Set up default values and
     * conventions for the properties of {@link CoherenceTask}.
     *
     * @param project gradle Project
     * @param coherenceTask the task to configure
     */
    private void applyInitialConfig(Project project, CoherenceTask coherenceTask)
        {
        project.getLogger().info("Setting up Task property conventions.");
        coherenceTask.getDebug().convention(false);
        coherenceTask.getPofSchemaXmlPath().convention(POF_XML_SCHEMA_DEFAULT_LOCATION);
        coherenceTask.getUsePofSchemaXml().convention(false);
        }

    // ----- data members -----------------------------------------------

    /**
     * Reference to the settings and properties for the Coherence Gradle plugin.
     */
    protected final CoherenceExtension coherenceExtension;

    /**
     * The Gradle {@link Project}.
     */
    protected final Project project;

    /**
     * The Gradle {@link JavaCompile} task.
     */
    protected final JavaCompile javaCompileTask;

    /**
     * The Gradle {@link Provider} for the destination directory of the processResources task.
     */
    protected final Provider<File> processResourcesDestinationDirProvider;

    // ----- constants ------------------------------------------------------

    /**
     * The default location of the POF XML Schema file.
     */
    public static final String POF_XML_SCHEMA_DEFAULT_LOCATION = "META-INF/schema.xml";
    }
