/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * Contains common task configuration logic.
 *
 * @author Gunnar Hillert  2023.10.05
 * @since 23.09.1
 * @see PortableTypesTaskRegistrationAction
 * @see PortableTypesTestTaskRegistrationAction
 */
public abstract class AbstractPortableTypesTaskRegistrationAction implements Action<PortableTypesTask>
    {

    //----- constructors ----------------------------------------------------

    /**
     * Common constructor for the AbstractPortableTypesTaskRegistrationAction
     * @param coherenceExtension configuration for data for the Gradle task
     * @param project the Gradle project
     * @param javaCompileTask the Gradle compile task
     * @param resourcesFolders The Gradle {@link Provider} for the destination directory of the processResources task
     */
    public AbstractPortableTypesTaskRegistrationAction(PortableTypesExtension coherenceExtension, Project project,
                                                   JavaCompile javaCompileTask, FileCollection resourcesFolders)
        {
        this.coherenceExtension = coherenceExtension;
        this.project            = project;
        this.javaCompileTask    = javaCompileTask;
        this.resourcesFolders   = resourcesFolders;
        }

    // ----- AbstractPortableTypesTaskRegistrationAction methods ----------------

    @Override
    public void execute(PortableTypesTask portableTypesTask)
        {
        portableTypesTask.getClassesDirectory().set(javaCompileTask.getDestinationDirectory());
        portableTypesTask.getOutputDirectory().set(javaCompileTask.getDestinationDirectory());
        applyInitialConfig(project, portableTypesTask);

        if (coherenceExtension.getUsePofSchemaXml().getOrElse(false))
            {
            if (!resourcesFolders.isEmpty())
                {
                portableTypesTask.getResourcesDirectories().set(resourcesFolders);
                portableTypesTask.getUsePofSchemaXml().set(coherenceExtension.getUsePofSchemaXml());
                }
            }
        if (coherenceExtension.getDebug().isPresent())
            {
            portableTypesTask.getDebug().set(coherenceExtension.getDebug());
            }

        if (coherenceExtension.getPofSchemaXmlPath().isPresent())
            {
            portableTypesTask.getPofSchemaXmlPath().set(coherenceExtension.getPofSchemaXmlPath());
            }
        if (coherenceExtension.getIndexPofClasses().isPresent())
            {
            portableTypesTask.getIndexPofClasses().set(coherenceExtension.getIndexPofClasses());
            }
        if (coherenceExtension.getPofIndexPackages().isPresent())
            {
            portableTypesTask.getPofIndexPackages().set(coherenceExtension.getPofIndexPackages());
            }
        }


    /**
     * Set up default values and
     * conventions for the properties of {@link PortableTypesTask}.
     *
     * @param project gradle Project
     * @param coherenceTask the task to configure
     */
    private void applyInitialConfig(Project project, PortableTypesTask coherenceTask)
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
    protected final PortableTypesExtension coherenceExtension;

    /**
     * The Gradle {@link Project}.
     */
    protected final Project project;

    /**
     * The Gradle {@link JavaCompile} task.
     */
    protected final JavaCompile javaCompileTask;

    /**
     * The Gradle resources folders of the Gradle project.
     */
    protected final FileCollection resourcesFolders;

    // ----- constants ------------------------------------------------------

    /**
     * The default location of the POF XML Schema file.
     */
    public static final String POF_XML_SCHEMA_DEFAULT_LOCATION = "META-INF/schema.xml";
    }
