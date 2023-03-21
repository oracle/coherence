/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.gradle;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.schema.ClassFileSchemaSource;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.SchemaBuilder;
import com.oracle.coherence.common.schema.XmlSchemaSource;

import com.tangosol.io.pof.generator.PortableTypeGenerator;

import com.tangosol.io.pof.schema.annotation.PortableType;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;

import org.gradle.api.artifacts.Configuration;

import org.gradle.api.file.Directory;

import org.gradle.api.logging.Logger;

import org.gradle.api.provider.Property;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;

import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;

import static com.oracle.coherence.common.schema.ClassFileSchemaSource.Filters.hasAnnotation;

/**
 * The main Gradle task.
 *
 * @author Gunnar Hillert  2023.03.16
 * @since 22.06.05
 */
abstract class CoherenceTask
        extends DefaultTask
    {
    //----- constructors ----------------------------------------------------

    /**
     * Constructor of the Gradle Task that sets up default values and
     * conventions for class properties.
     *
     * @param project dependency injected Gradle Project
     */
    @Inject
    public CoherenceTask(Project project)
        {
        getLogger().info("Setting up Task property conventions.");
        getDebug().convention(false);
        getInstrumentTestClasses().convention(false);

        Directory mainJavaOutputDir = PluginUtils.getMainJavaOutputDir(project);
        getMainClassesDirectory().convention(mainJavaOutputDir.getAsFile());

        Directory testJavaOutputDir = PluginUtils.getTestJavaOutputDir(project);
        getTestClassesDirectory().convention(testJavaOutputDir.getAsFile());

        File fileMainResourcesOutputDir = PluginUtils.getMainResourcesOutputDir(project);
        if (fileMainResourcesOutputDir != null)
            {
            getMainResourcesDirectory().convention(fileMainResourcesOutputDir);
            }

        File testResourcesOutputDir = PluginUtils.getTestResourcesOutputDir(project);
        if (testResourcesOutputDir != null)
            {
            getTestResourcesDirectory().convention(testResourcesOutputDir);
            }
        }

    //----- CoherenceTask methods -------------------------------------------

    /**
     * Returns a Gradle container object wrapping a Boolean property. If set to {@code true}
     * we instruct the underlying PortableTypeGenerator to generate debug code in regards
     * the instrumented classes. If not specified, this property ultimately defaults to
     * {@code false}.
     *
     * @return Gradle container object wrapping a Boolean property
     */
    @Input
    @Optional
    public abstract Property<Boolean> getDebug();

    /**
     * Returns {@code true} if test classes shall be instrumented as well.
     */
    @Input
    @Optional
    public abstract Property<Boolean> getInstrumentTestClasses();

    /**
     * Sets the project's test classes directory. This is an optional property.
    **/
    @InputFiles
    @Optional
    public abstract Property<File> getTestClassesDirectory();

    /**
     * Sets the project's test resources directory. This is an optional property.
     **/
    @InputFiles
    @Optional
    public abstract Property<File> getTestResourcesDirectory();

    /**
     * Sets the project's main classes directory. This is an optional property.
     **/
    @InputFiles
    @Optional
    public abstract Property<File> getMainClassesDirectory();

    /**
     * Sets the project's main resources directory. This is an optional property.
     **/
    @InputFiles
    @Optional
    public abstract Property<File> getMainResourcesDirectory();

    /**
     * The Gradle action to run when the task is executed.
     */
    @TaskAction
    public void instrumentPofClasses()
        {
        boolean fDebug = getDebug().get();
        Logger  logger = getLogger();

        logger.lifecycle("Start executing Gradle task instrumentPofClasses...");
        logger.info("The following configuration properties are configured:");
        logger.info("Property debug = {}", fDebug);
        logger.info("Property instrumentTestClasses = {}", getInstrumentTestClasses().get());

        Property<File> propTestClassesDirectory = getTestClassesDirectory();
        logger.info("Property testClassesDirectory = {}", propTestClassesDirectory);

        Property<File> propMainClassesDirectory = getMainClassesDirectory();
        logger.info("Property mainClassesDirectory = {}", propMainClassesDirectory);

        ClassFileSchemaSource source                 = new ClassFileSchemaSource();
        List<File>            listInstrument         = new ArrayList<>();
        SchemaBuilder         schemaBuilder          = new SchemaBuilder();
        List<File>            listClassesDirectories = new ArrayList<>();

        addSchemaSourceIfExists(schemaBuilder, getTestResourcesDirectory());
        addSchemaSourceIfExists(schemaBuilder, getMainResourcesDirectory());

        if (propTestClassesDirectory.isPresent() && propTestClassesDirectory.get().exists())
            {
            File fileTestClassesDirectory = propTestClassesDirectory.get();
            listClassesDirectories.add(fileTestClassesDirectory);
            }
        else
            {
            logger.error("PortableTypeGenerator skipping test classes directory as it does not exist.");
            }

        if (propMainClassesDirectory.isPresent() && propMainClassesDirectory.get().exists())
            {
            File fileMainClassesDirectory = propMainClassesDirectory.get();
            listClassesDirectories.add(fileMainClassesDirectory);
            }
        else
            {
            logger.error("PortableTypeGenerator skipping main classes directory as it does not exist.");
            }

        if (!listClassesDirectories.isEmpty()) {
            source.withTypeFilter(hasAnnotation(PortableType.class))
                  .withMissingPropertiesAsObject();
            for (File classesDir : listClassesDirectories)
                {
                source.withClassesFromDirectory(classesDir);
                listInstrument.add(classesDir);
                }
        }

        if (!listInstrument.isEmpty())
            {
            List<File> listDeps = resolveDependencies();
            ClassFileSchemaSource dependencies =
                    new ClassFileSchemaSource()
                            .withTypeFilter(hasAnnotation(PortableType.class))
                            .withPropertyFilter(fieldNode -> false);

            listDeps.stream()
                    .filter(File::isDirectory)
                    .peek(f -> logger.lifecycle("Adding classes from " + f + " to schema"))
                    .forEach(dependencies::withClassesFromDirectory);

            listDeps.stream()
                    .filter(f -> f.isFile() && f.getName().endsWith(".jar"))
                    .peek(f -> logger.lifecycle("Adding classes from " + f + " to schema"))
                    .forEach(dependencies::withClassesFromJarFile);

            Schema schema = schemaBuilder
                    .addSchemaSource(dependencies)
                    .addSchemaSource(source)
                    .build();

            for (File dir : listInstrument)
                {
                try
                    {
                    logger.warn("Running PortableTypeGenerator for classes in " + dir.getCanonicalPath());
                    PortableTypeGenerator.instrumentClasses(dir, schema, fDebug, new GradleLogger(logger));
                    }
                catch (IOException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                }
            }
        }

    /**
     * Determines whether a "schema.xml" file exists. If it exists it will be added as an
     * XmlSchemaSource to the SchemaBuilder.
     *
     * @param builder                Coherence SchemaBuilder
     * @param propResourcesDirectory directory that may contain a schema.xml file
     */
    private void addSchemaSourceIfExists(SchemaBuilder builder, Property<File> propResourcesDirectory)
        {
        Logger logger = getLogger();

        if (propResourcesDirectory.isPresent())
            {
            File fileResourcesDirectory = propResourcesDirectory.get();

            if (fileResourcesDirectory.exists())
                {
                File xmlSchema = Paths.get(fileResourcesDirectory.getPath(), "META-INF", "schema.xml").toFile();
                if (xmlSchema.exists())
                    {
                    logger.lifecycle("Add XmlSchemaSource '{}'.", xmlSchema.getAbsolutePath());
                    builder.addSchemaSource(new XmlSchemaSource(xmlSchema));
                    }
                else
                    {
                    logger.info("No schema.xml file found at {}", xmlSchema.getAbsolutePath());
                    }
                }
            else
                {
                logger.info("The specified resources directory '{}' does not exist.", fileResourcesDirectory.getAbsolutePath());
                }
            }
        else
            {
            logger.info("The resources directory property is not present.");
            }
        }

    /**
     * Resolves project dependencies from the runtimeClasspath.
     * @return list of resolved project dependencies or an empty List
     */
    private List<File> resolveDependencies()
        {
        List<File>    listArtifacts = new ArrayList<>();
        Configuration configuration = getProject().getConfigurations().getByName("runtimeClasspath");

        configuration.forEach(file ->
            {
            getLogger().info("Adding dependency '{}'.", file.getAbsolutePath());
            if (file.exists())
                {
                listArtifacts.add(file);
                }
            else
                {
                getLogger().info("Dependency '{}' does not exist.", file.getAbsolutePath());
                }
            });
        getLogger().lifecycle("Resolved {} dependencies.", listArtifacts.size());
        return listArtifacts;
        }
    }
