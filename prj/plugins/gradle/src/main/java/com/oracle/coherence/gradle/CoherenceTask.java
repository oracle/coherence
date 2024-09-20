/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
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

import com.tangosol.io.pof.PofIndexer;

import com.tangosol.io.pof.generator.PortableTypeGenerator;

import com.tangosol.io.pof.schema.annotation.PortableType;

import org.gradle.api.DefaultTask;
import org.gradle.api.Project;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;

import org.gradle.api.logging.Logger;

import org.gradle.api.plugins.JavaPlugin;

import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import org.gradle.api.tasks.compile.JavaCompile;

import javax.inject.Inject;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
     * Shall {@link PortableType} annotated classes be indexed to an index file at {@code META-INF/pof.idx}?
     * If not specified, this property defaults to {@code true}.
     *
     * @return Gradle container object wrapping a Boolean property
     */
    @Input
    @Optional
    public abstract Property<Boolean> getIndexPofClasses();

    /**
     * Allows you to include one or more packages when indexing {@link PortableType} annotated classes. This is an optional
     * property but limiting the scanning of {@link PortableType} annotated classes to a set of packages may speed up
     * indexing substantially.
     *
     * @return a Gradle SetProperty wrapping a String values
     */
    @Input
    @Optional
    public abstract SetProperty<String> getPofIndexPackages();

    /**
     * Sets the project's resources directory. This is an optional property.
     **/
    @Input
    @Optional
    public abstract Property<FileCollection> getResourcesDirectories();

    /**
     * Sets the project's main classes directory. This is the main input directory.
     **/
    @InputFiles
    public abstract DirectoryProperty getClassesDirectory();

    /**
     * Sets the task's output directory.
     **/
    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory();

    /**
     * The Gradle action to run when the task is executed.
     */
    @TaskAction
    public void instrumentPofClasses() {

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

        final Set<File> resourcesDirectoriesAsFiles;

        if (this.getResourcesDirectories().isPresent())
            {
            resourcesDirectoriesAsFiles = this.getResourcesDirectories().get().getFiles();
            }
        else
            {
            resourcesDirectoriesAsFiles = null;
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

        final boolean indexPofClasses = this.getIndexPofClasses().getOrElse(true);

        logger.info("The following configuration properties are configured:");

        logger.info("Property classesDirectory   = {}", classesDirectory.getAbsolutePath());

        if (resourcesDirectoriesAsFiles == null || resourcesDirectoriesAsFiles.isEmpty())
            {
            logger.info("Property resourcesDirectory = {}", "N/A");
            }
        else
            {
            for (File resourceDirectory : resourcesDirectoriesAsFiles)
                {
                logger.info("Property resourcesDirectory = {}", resourceDirectory.getAbsolutePath());
                }
            }

        logger.info("Property outputDirectory    = {}", outputDirectory.getAbsolutePath());
        logger.info("Property sPofSchemaXmlPath  = {}", sPofSchemaXmlPath);
        logger.info("Property indexPofClasses  = {}", indexPofClasses);
        logger.info("Property debug              = {}", fDebug);

        if (!classesDirectory.equals(outputDirectory))
            {
            logger.info("Copying all classes from '{}' to '{}'.", classesDirectory, outputDirectory);
            final boolean didTheCopyOperationSucceed = getProject()
                    .copy(copy -> copy.from(classesDirectory).into(outputDirectory))
                    .getDidWork();
            logger.debug("Copying of classes {}.", didTheCopyOperationSucceed ? "completed" : "not completed");
            }

        final ClassFileSchemaSource classFileSchemaSource  = new ClassFileSchemaSource();
        final List<File>            listInstrument         = new ArrayList<>();
        final SchemaBuilder         schemaBuilder          = new SchemaBuilder();
        final List<File>            listClassesDirectories = new ArrayList<>();

        if (usePofSchemaXmlPath && sPofSchemaXmlPath != null)
            {
            if (resourcesDirectoriesAsFiles != null && !resourcesDirectoriesAsFiles.isEmpty())
                {
                Set<File> missingLocations = new HashSet<>();
                for (File resourceDirectory : resourcesDirectoriesAsFiles)
                    {
                    final File schemaSourceXmlFile = new File(resourceDirectory, sPofSchemaXmlPath);
                    if (schemaSourceXmlFile.exists())
                        {
                        if (schemaSourceXmlFile.isDirectory())
                            {
                            throw new IllegalStateException(
                                String.format("Declared schemaSource XML file '%s' is a directory,", schemaSourceXmlFile));
                            }
                        addPofXmlSchema(schemaBuilder, schemaSourceXmlFile);
                        }
                    else
                        {
                        missingLocations.add(schemaSourceXmlFile);
                        }
                    }
                    if (missingLocations.size() == resourcesDirectoriesAsFiles.size())
                        {
                        throw new IllegalStateException(
                            String.format("The declared schemaSource XML file '%s' does not exist " +
                                    "in the provided %s resource folder(s).", sPofSchemaXmlPath, resourcesDirectoriesAsFiles.size()));
                        }
                }


            }

        if (classesDirectory.exists())
            {
            listClassesDirectories.add(outputDirectory);
            }
        else
            {
            logger.error("PortableTypeGenerator skipping classes directory as it does not exist.");
            }

        classFileSchemaSource.withTypeFilter(hasAnnotation(PortableType.class))
                             .withMissingPropertiesAsObject();

        if (!listClassesDirectories.isEmpty())
            {
            for (File classesDir : listClassesDirectories)
                {
                classFileSchemaSource.withClassesFromDirectory(classesDir);
                listInstrument.add(classesDir);
                }
            }

        final List<File> classesFromDirectory = new ArrayList<>();
        final List<File> classesFromJarFile = new ArrayList<>();

        if (!listInstrument.isEmpty())
            {
            final List<File>            listDeps     = resolveDependencies();
            final ClassFileSchemaSource dependencies = new ClassFileSchemaSource()
                    .withTypeFilter(hasAnnotation(PortableType.class))
                    .withPropertyFilter(fieldNode -> false);

            listDeps.stream()
                    .filter(File::isDirectory)
                    .peek(f -> logger.lifecycle("Adding classes from " + f + " to schema"))
                    .forEach(classesFromDirectory::add);

            classesFromDirectory.forEach(dependencies::withClassesFromDirectory);

            listDeps.stream()
                    .filter(f -> f.isFile() && f.getName().endsWith(".jar"))
                    .peek(f -> logger.lifecycle("Adding classes from " + f + " to schema"))
                    .forEach(classesFromJarFile::add);

            classesFromJarFile.forEach(dependencies::withClassesFromJarFile);

            final Schema schema = schemaBuilder
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

        if (indexPofClasses)
            {
            try {
                logger.warn("Creating POF index in directory " + outputDirectory.getCanonicalPath());
                final PofIndexer pofIndexer = new PofIndexer(new GradleLogger(logger));
                pofIndexer.ignoreClasspath(true)
                        .withClassesFromDirectory(listInstrument)
                        .withClassesFromJarFile(classesFromJarFile);

                if (!this.getPofIndexPackages().getOrElse(Collections.EMPTY_SET).isEmpty())
                    {
                    pofIndexer.setPackagesToScan(this.getPofIndexPackages().get());
                    }
                pofIndexer.createIndexInDirectory(outputDirectory);
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
     * Resolves project dependencies from the JavaCompile task's classpath.
     * @return list of resolved project dependencies or an empty List
     */
    private List<File> resolveDependencies()
        {
        final List<File>    listArtifacts = new ArrayList<>();

        final JavaCompile javaCompileTask = (JavaCompile) getProject().getTasks().findByName(JavaPlugin.COMPILE_JAVA_TASK_NAME);
        final Set<File> dependencies = javaCompileTask.getClasspath().getFiles();

        for (File file : dependencies)
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
            }
        getLogger().lifecycle("Resolved {} dependencies.", listArtifacts.size());
        return listArtifacts;
        }
    }
