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

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileType;

import org.gradle.api.logging.Logger;

import org.gradle.api.provider.Property;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.Incremental;
import org.gradle.work.InputChanges;

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;

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
     * The path specified by this property is used to determine the XML file containing Portable Type definitions.
     * @return the relative path to the POF XML definition file, defaults to {@code META-INF/schema.xml}
     */
    @Input
    @Optional
    public abstract Property<String> getPofSchemaXmlPath();

    /**
     * Shall an existing POF XML Schema file be used for instrumentation? If not specified, this property
     * defaults to {@code false}.
     *
     * @return Gradle container object wrapping a Boolean property
     */
    @Input
    @Optional
    public abstract Property<Boolean> getUsePofSchemaXml();

    /**
     * Sets the project's resources directory. This is an optional property.
     **/
    @Input
    @Optional
    public abstract Property<File> getResourcesDirectory();

    /**
     * Sets the project's main classes directory. This is an optional property.
     **/
    @InputFiles
    @Incremental
    public abstract DirectoryProperty getClassesDirectory();

    /**
     * Sets the task's output directory.
     **/
    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory();

    /**
     * The Gradle action to run when the task is executed.
     * @param inputChanges the input changes for incremental builds
     */
    @TaskAction
    public void instrumentPofClasses(InputChanges inputChanges) {

        final boolean fDebug = getDebug().get();
        final Logger  logger = getLogger();

        final File classesDirectory;

        if (this.getClassesDirectory().isPresent())
            {
            classesDirectory = this.getClassesDirectory().get().getAsFile();
            }
        else
            {
            throw new IllegalStateException("The classesDirectory is not specified.");
            }

        final File resourcesDirectory;

        if (this.getResourcesDirectory().isPresent())
            {
            resourcesDirectory = this.getResourcesDirectory().get();
            }
        else
            {
            resourcesDirectory = null;
            }

        final File outputDirectory;

        if (this.getOutputDirectory().isPresent())
            {
            outputDirectory = this.getOutputDirectory().get().getAsFile();
            }
        else
            {
            throw new IllegalStateException("The outputDirectory is not specified.");
            }

        final String sPofSchemaXmlPath;

        if (this.getPofSchemaXmlPath().isPresent())
            {
            sPofSchemaXmlPath = this.getPofSchemaXmlPath().get();
            }
        else
            {
            sPofSchemaXmlPath = null;
            }

        final boolean usePofSchemaXmlPath;

        if (this.getUsePofSchemaXml().isPresent())
            {
            usePofSchemaXmlPath = this.getUsePofSchemaXml().get();
            }
        else
            {
            usePofSchemaXmlPath = false;
            }

        final String executionType = inputChanges.isIncremental() ? "incrementally" : "non-incrementally";
        logger.lifecycle("Start executing Gradle task instrumentPofClasses..." + executionType);

        logger.info("The following configuration properties are configured:");

        logger.info("Property classesDirectory   = {}", classesDirectory.getAbsolutePath());
        logger.info("Property resourcesDirectory = {}", resourcesDirectory != null ? resourcesDirectory.getAbsolutePath() : "N/A");
        logger.info("Property outputDirectory    = {}", outputDirectory.getAbsolutePath());
        logger.info("Property sPofSchemaXmlPath  = {}", sPofSchemaXmlPath);
        logger.info("Property debug              = {}", fDebug);

        final List<File> incrementallyChangedFiles = new ArrayList<>();

        if (!inputChanges.isIncremental())
            {
            logger.info("Copying all classes from '{}' to '{}'.", classesDirectory, outputDirectory);
            final boolean didTheCopyOperationSucceed = getProject()
                    .copy(copy -> copy.from(classesDirectory).into(outputDirectory))
                    .getDidWork();
            logger.debug("Copying of classes {}.", didTheCopyOperationSucceed ? "completed" : "not completed");
            }
        else
            {
            final Iterable<FileChange> changes = inputChanges.getFileChanges(this.getClassesDirectory());

            for (FileChange fileChange : changes)
                {
                final File       sourceFile       = fileChange.getFile();
                final FileType   fileType         = fileChange.getFileType();
                final ChangeType changeType       = fileChange.getChangeType();
                final String     relativeFilePath = sourceFile.getAbsolutePath().substring(classesDirectory.getAbsolutePath().length());
                final String     targetBasePath   = outputDirectory.getAbsolutePath();
                final File       targetFile       = new File(targetBasePath + File.separator + relativeFilePath);

                logger.info("sourceFile: '{}'.", sourceFile.getAbsolutePath());
                logger.info("fileType: '{}'.", fileType.name());
                logger.info("changeType: {}.", changeType.name());
                logger.info("relativeFilePath: '{}'.", relativeFilePath);
                logger.info("targetBasePath: '{}'.", targetBasePath);
                logger.info("targetFile: '{}'.", targetFile.getAbsolutePath());

                switch (changeType)
                    {
                    case ADDED ->
                        {
                        if (FileType.DIRECTORY.equals(fileType))
                            {
                            targetFile.mkdirs();
                            continue;
                            }
                        final boolean didTheCopyOperationSucceed = getProject().copy(copy ->
                            {
                            copy.from(sourceFile)
                                .into(targetFile.getParent());
                            }).getDidWork();
                        logger.info("Copy added file '{}' to '{}'. Success: '{}'.", sourceFile.getAbsolutePath(),
                                outputDirectory, didTheCopyOperationSucceed);
                        incrementallyChangedFiles.add(targetFile);
                        }
                    case MODIFIED ->
                        {
                        final boolean didTheCopyOperationSucceed = getProject().copy(copy ->
                        {
                            copy.from(sourceFile)
                                .into(outputDirectory);
                        }).getDidWork();
                        logger.info("Copy modified file '{}' to '{}'. Success: '{}'.", sourceFile.getAbsolutePath(),
                                outputDirectory, didTheCopyOperationSucceed);
                        incrementallyChangedFiles.add(targetFile);
                        }
                    case REMOVED ->
                        {
                        getProject().delete(delete ->
                            {
                            logger.info("Delete file '{}'.", targetFile.getAbsolutePath());
                            delete.delete(targetFile);
                            });
                        }
                    }
                }
            }

        final ClassFileSchemaSource classFileSchemaSource  = new ClassFileSchemaSource();
        final List<File>            listInstrument         = new ArrayList<>();
        final SchemaBuilder         schemaBuilder          = new SchemaBuilder();
        final List<File>            listClassesDirectories = new ArrayList<>();

        if (usePofSchemaXmlPath && sPofSchemaXmlPath != null)
            {
            File schemaSourceXmlFile = new File(this.getResourcesDirectory().get(), sPofSchemaXmlPath);
            addPofXmlSchema(schemaBuilder, schemaSourceXmlFile);
            }

        if (classesDirectory.exists())
            {
            if (!inputChanges.isIncremental())
                {
                listClassesDirectories.add(outputDirectory);
                }
            }
        else
            {
            logger.error("PortableTypeGenerator skipping classes directory as it does not exist.");
            }

        classFileSchemaSource.withTypeFilter(hasAnnotation(PortableType.class))
                             .withMissingPropertiesAsObject();

        if (!incrementallyChangedFiles.isEmpty())
            {
            for (File classFile : incrementallyChangedFiles)
                {
                classFileSchemaSource.withClassFile(classFile);
                }
            }
        if (!listClassesDirectories.isEmpty())
            {
            for (File classesDir : listClassesDirectories)
                {
                classFileSchemaSource.withClassesFromDirectory(classesDir);
                listInstrument.add(classesDir);
                }
            }

        if (!listInstrument.isEmpty() || !incrementallyChangedFiles.isEmpty())
            {
            final List<File>            listDeps     = resolveDependencies();
            final ClassFileSchemaSource dependencies = new ClassFileSchemaSource()
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
                    .addSchemaSource(classFileSchemaSource)
                    .build();
            try
                {
                logger.warn("Running PortableTypeGenerator for classes in " + outputDirectory.getCanonicalPath());
                PortableTypeGenerator.instrumentClasses(outputDirectory, schema, fDebug, new GradleLogger(logger));
                }
            catch (IOException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        }

    /**
     * Determines whether a "schema.xml" file exists. If it exists it will be added as an
     * XmlSchemaSource to the SchemaBuilder.
     *
     * @param builder             Coherence SchemaBuilder
     * @param schemaSourceXmlFile the schema.xml file
     */
    private void addPofXmlSchema(SchemaBuilder builder, File schemaSourceXmlFile)
        {
        Logger logger = getLogger();

        if (schemaSourceXmlFile.exists())
            {
            logger.lifecycle("Add XmlSchemaSource '{}'.", schemaSourceXmlFile.getAbsolutePath());
            builder.addSchemaSource(new XmlSchemaSource(schemaSourceXmlFile));
            }
        else
            {
            throw new IllegalStateException(String.format("The specified POF XML Schema file does not exist at: '%s'.", schemaSourceXmlFile.getAbsolutePath()));
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
