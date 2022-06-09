/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.pof;

import com.oracle.coherence.common.schema.ClassFileSchemaSource;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.SchemaBuilder;
import com.oracle.coherence.common.schema.XmlSchemaSource;

import com.tangosol.io.pof.generator.PortableTypeGenerator;
import com.tangosol.io.pof.schema.annotation.PortableType;

import java.io.File;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;

import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;

import org.eclipse.aether.RepositorySystemSession;

import static com.oracle.coherence.common.schema.ClassFileSchemaSource.Filters.hasAnnotation;

/**
 * A Maven plugin that instruments classes to implement evolvable portable types
 * by running the {@link PortableTypeGenerator} class.
 *
 * @author as  2017.03.14
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class PortableTypeMojo
        extends AbstractMojo
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PortableTypeMojo}.
     *
     * @param fTests  {@code true} if instrumenting test classes
     */
    PortableTypeMojo(boolean fTests)
        {
        f_fTests = fTests;
        }

    // ----- PortableTypeMojo methods ---------------------------------------

    /**
     * Obtain the directories containing the classes to instrument.
     *
     * @return  the directories containing the classes to instrument
     */
    protected abstract File[] getClassesDirectories();

    // ----- AbstractMojo methods -------------------------------------------

    @Override
    public void execute()
            throws MojoExecutionException
        {
        try
            {
            MavenLogger log = new MavenLogger(getLog());

            if (m_fSkip)
                {
                log.info("PortableTypeGenerator code generation is skipped");
                return;
                }
            
            SchemaBuilder         builder        = new SchemaBuilder();
            ClassFileSchemaSource source         = new ClassFileSchemaSource();
            File[]                aDirs          = getClassesDirectories();
            List<File>            listInstrument = new ArrayList<>();

            if (aDirs.length == 0)
                {
                return;
                }

            for (File dir : aDirs)
                {
                if (dir.isDirectory() && dir.exists())
                    {
                    source.withClassesFromDirectory(dir)
                          .withTypeFilter(hasAnnotation(PortableType.class))
                          .withMissingPropertiesAsObject();

                    File xmlSchema = Paths.get(dir.getPath(), "META-INF", "schema.xml").toFile();
                    if (xmlSchema.exists())
                        {
                        builder.addSchemaSource(new XmlSchemaSource(xmlSchema));
                        }

                    listInstrument.add(dir);
                    }
                else
                    {
                    log.info("PortableTypeGenerator skipping " + dir + " as it is not a directory");
                    }
                }

            if (listInstrument.size() > 0)
                {
                List<File> listDeps = resolveDependencies();
                ClassFileSchemaSource dependencies =
                        new ClassFileSchemaSource()
                                .withTypeFilter(hasAnnotation(PortableType.class))
                                .withPropertyFilter(fieldNode -> false);

                listDeps.stream()
                        .filter(File::isDirectory)
                        .peek(f -> log.debug("Adding classes from " + f + " to schema"))
                        .forEach(dependencies::withClassesFromDirectory);

                listDeps.stream()
                        .filter(f -> f.isFile() && f.getName().endsWith(".jar"))
                        .peek(f -> log.debug("Adding classes from " + f + " to schema"))
                        .forEach(dependencies::withClassesFromJarFile);

                Schema schema = builder
                        .addSchemaSource(dependencies)
                        .addSchemaSource(source)
                        .build();

                for (File dir : listInstrument)
                    {
                    log.info("Running PortableTypeGenerator for classes in " + dir.getCanonicalPath());

                    PortableTypeGenerator.instrumentClasses(dir, schema, m_fDebug, log);
                    }
                }
            }
        catch (Exception e)
            {
            throw new MojoExecutionException("Failed to instrument classes", e);
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Set Maven project.
     *
     * @param project  Maven project
     */
    public void setProject(MavenProject project)
        {
        m_project = project;
        }

    /**
     * Set remote repositories.
     *
     * @param listRemoteRepositories  the list of remote repositories
     */
    public void setRemoteRepositories(List<ArtifactRepository> listRemoteRepositories)
        {
        m_listRemoteRepositories = listRemoteRepositories;
        }

    /**
     * Set local repository.
     *
     * @param localRepository  the local repository
     */
    public void setLocalRepository(ArtifactRepository localRepository)
        {
        m_localRepository = localRepository;
        }

    /**
     * Set artifact resolver.
     *
     * @param artifactResolver  the artifact resolver
     */
    public void setArtifactResolver(ArtifactResolver artifactResolver)
        {
        m_artifactResolver = artifactResolver;
        }

    /**
     * Enable or disable debug mode.
     *
     * @param fDebug  flag specifying whether to enable debug mode
     */
    public void setDebug(boolean fDebug)
        {
        this.m_fDebug = fDebug;
        }

    public void setSkip(boolean fSkip)
        {
        m_fSkip = fSkip;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Resolve dependencies.
     *
     * @return a list of dependencies.
     *
     * @throws ArtifactResolverException  if a dependency resolution error occurs
     */
    private List<File> resolveDependencies() throws ArtifactResolverException
        {
        List<File> listArtifacts = new ArrayList<>();
        Log        log           = getLog();

        for (Artifact artifact : m_project.getArtifacts())
            {
            if (artifact.getScope().equals(Artifact.SCOPE_TEST) && !f_fTests)
                {
                continue;
                }

            String sArtifactId = artifact.getArtifactId();

            if (!artifact.isResolved())
                {
                log.debug("Resolving artifact " + artifact);

                ProjectBuildingRequest req = new DefaultProjectBuildingRequest()
                        .setRepositorySession(m_session)
                        .setLocalRepository(m_localRepository)
                        .setRemoteRepositories(m_listRemoteRepositories);

                ArtifactResult result = m_artifactResolver.resolveArtifact(req, artifact);
                artifact = result.getArtifact();
                }

            // The file should exists, but we never know.
            File file = artifact.getFile();
            if (file == null || !file.exists())
                {
                log.warn("Artifact " + sArtifactId
                        + " has no attached file. Its content will not be copied to the target model directory.");
                }
            else
                {
                log.debug("Adding file: artifact=" + artifact + " file=" + file);

                listArtifacts.add(file);
                }
            }

        return listArtifacts;
        }

    // ----- inner class: MavenLogger ---------------------------------------

    /**
     * An implementation of a logger to log to the Maven build output.
     */
    private static class MavenLogger
            implements PortableTypeGenerator.Logger
        {
        /**
         * Create a logger that wraps the specified Maven logger.
         *
         * @param log  the Maven logger
         */
        MavenLogger(Log log)
            {
            f_log = log;
            }

        // ----- PortableTypeGenerator.Logger methods -----------------------
        
        @Override
        public void debug(String message)
            {
            f_log.debug(message);
            }

        @Override
        public void info(String message)
            {
            f_log.info(message);
            }

        // ----- data members -----------------------------------------------

        /**
         * The wrapped Maven logger.
         */
        private final org.apache.maven.plugin.logging.Log f_log;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject m_project;

    /**
     * List of Remote Repositories used by the resolver
     */
    @Parameter(defaultValue = "${project.remoteArtifactRepositories}", readonly = true, required = true)
    private List<ArtifactRepository> m_listRemoteRepositories;

    /**
     * Location of the local repository.
     */
    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository m_localRepository;

    /**
     * Used to look up Artifacts in the remote repository.
     */
    @Component
    private ArtifactResolver m_artifactResolver;

    /**
     * The current repository/network configuration of Maven.
     */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession m_session;

    /**
     * Add debug statements to serialization code.
     */
    @Parameter(name         = "debug",
               property     = "pof.debug",
               defaultValue = "false")
    private boolean m_fDebug;

    /**
     * Whether to skip execution.
     */
    @Parameter(name         = "skip",
               property     = "pof.skip",
               defaultValue = "false")
    private boolean m_fSkip = false;

    /**
     * A flag indicating whether this is test classes instrumentation.
     */
    private final boolean f_fTests;
    }
