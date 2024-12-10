/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.pof;

import com.oracle.coherence.common.schema.ClassFileSchemaSource;
import com.oracle.coherence.common.schema.Schema;
import com.oracle.coherence.common.schema.SchemaBuilder;
import com.oracle.coherence.common.schema.XmlSchemaSource;

import com.tangosol.io.pof.PofIndexer;
import com.tangosol.io.pof.generator.PortableTypeGenerator;
import com.tangosol.io.pof.schema.annotation.PortableType;

import java.io.File;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
 * @author Gunnar Hillert 2024.03.13
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

            File[]      aDirs          = getClassesDirectories();
            List<File>  listInstrument = validateClassesDirectories(aDirs, log);
            List<File>  listDeps       = new ArrayList<>();

            listDeps.addAll(resolveDependencies());

            if (m_fInstrumentPofClasses)
                {
                SchemaBuilder         builder        = new SchemaBuilder();
                ClassFileSchemaSource source         = new ClassFileSchemaSource();

                for (File dir : listInstrument)
                    {
                    source.withClassesFromDirectory(dir)
                          .withTypeFilter(hasAnnotation(PortableType.class))
                          .withMissingPropertiesAsObject();

                    File xmlSchema = Paths.get(dir.getPath(), "META-INF", "schema.xml").toFile();
                    if (xmlSchema.exists())
                        {
                        builder.addSchemaSource(new XmlSchemaSource(xmlSchema));
                        }
                    }

                if (listInstrument.size() > 0)
                    {
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
            else
                {
                log.info("PortableTypeGenerator class instrumentation is skipped");
                }

            if (m_fIndexPofClasses)
                {
                PofIndexer pofIndexer = new PofIndexer(log)
                    .ignoreClasspath(true)
                    .withClassesFromDirectory(listInstrument)
                    .withClassesFromJarFile(listDeps)
                    .withIndexFileName(m_sPofIndexFileName);

                if (m_sPofIndexIncludeFilterPatterns != null && !m_sPofIndexIncludeFilterPatterns.isEmpty())
                    {
                    pofIndexer.setIncludeFilterPatterns(m_sPofIndexIncludeFilterPatterns);
                    }

                if (m_sPofIndexPackages != null && !m_sPofIndexPackages.isEmpty())
                    {
                    pofIndexer.setPackagesToScan(m_sPofIndexPackages);
                    }

                pofIndexer.createIndexInDirectory(getClassesDirectories()[0]);
                }
            else
                {
                log.info("PortableTypeGenerator indexing of classess is skipped");
                }
            }
        catch (Exception e)
            {
            throw new MojoExecutionException("Failed to instrument classes", e);
            }
        }

        List<File> validateClassesDirectories(File[] aDirs, MavenLogger log)
            {
            final List<File>  listInstrument = new ArrayList<>();

            if (aDirs.length == 0)
                {
                return listInstrument;
                }

            for (File dir : aDirs)
                {
                if (dir.isDirectory() && dir.exists())
                    {
                    listInstrument.add(dir);
                    }
                else
                    {
                    log.info("PortableTypeGenerator skipping " + dir + " as it is not a directory");
                    }
                }
                return listInstrument;
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

    public void setInstrumentPofClasses(boolean fInstrumentPofClasses)
        {
        m_fInstrumentPofClasses = fInstrumentPofClasses;
        }

    /**
     * If set to true, create an index of all classes that are annotated with the {@link PortableType} annotation. By
     * default, the index will be created at {@code META-INF/pof.idx}.
     *
     * @param fIndexPofClasses if true, index POF classes
     */
    public void setIndexPofClasses(boolean fIndexPofClasses)
        {
            m_fIndexPofClasses = fIndexPofClasses;
        }

    public void setPofIndexFileName(String m_sPofIndexFileName) {
        this.m_sPofIndexFileName = m_sPofIndexFileName;
    }

        /**
     * Optionally, you can provide one or more regular expressions to only include classes you need. For example, if you
     * only need classes that end in {@code MyClass}, you can provide the following regular expresssion as a String:
     * {@code ".*MyClass$"}.
     *
     * @param pofIndexIncludes a Set of regular expression Strings
     */
    public void setPofIndexIncludes(Set<String> pofIndexIncludes)
        {
        m_sPofIndexIncludeFilterPatterns = pofIndexIncludes;
        }

    /**
     * Allows you to include one or more packages when indexing {@link PortableType} annotated classes. This is an optional
     * property but limiting the scanning of {@link PortableType} annotated classes to a set of packages may speed up
     * indexing substantially.
     * <p>
     * Important: The filter is applied as part of scanning process for {@link PortableType} annotated classes and as such
     * this property DOES affect scanning performance.
     */
    public void setPofIndexPackages(Set<String> pofIndexPackages)
        {
        m_sPofIndexPackages = pofIndexPackages;
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
     * Whether to execute POF instrumentation. This is enabled by default but may be disabled in situation where POF classes
     * shall not be instrumented but POF indices may still be needed. This feature may be useful in testing scenarios
     * where a Maven build instruments all relevant classes but users may still wish to create separate indices for only
     * specific packages/classes.
     */
    @Parameter(name         = "instrumentPofClasses",
            property        = "pof.instrument",
            defaultValue    = "true")
    private boolean m_fInstrumentPofClasses = true;

    /**
     * Whether to index {@link PortableType} annotated classes in an index file at {@code META-INF/pof.idx}.
     * If not specified, this property defaults to {@code true}.
     */
    @Parameter(name         = "indexPofClasses",
               property     = "pof.index",
               defaultValue = "true")
    private boolean m_fIndexPofClasses = true;

    /**
     * Whether to index {@link PortableType} annotated classes in an index file at {@code META-INF/pof.idx}.
     * If not specified, this property defaults to {@code true}.
     */
    @Parameter(name      = "pofIndexFileName",
            property     = "pof.index.name",
            defaultValue = "META-INF/pof.idx")
    private String m_sPofIndexFileName = "META-INF/pof.idx";

    /**
     * Allows to set regular expressions for classes to be included. E.g. if you want to have only classes included
     * in the POF that end in Address, specify a regular expression string of {code .*Address$}.
     * <p>
     * Important: The filter is applied AFTER the retrieval of {@link PortableType} annotated classes and as such
     * this property does not affect scanning performance.
     */
    @Parameter(name      = "pofIndexIncludes",
               property  = "pof.index.includes")
    private Set<String> m_sPofIndexIncludeFilterPatterns;

    /**
     * Allows you to include one or more packages when indexing {@link PortableType} annotated classes. This is an optional
     * property but limiting the scanning of {@link PortableType} annotated classes to a set of packages may speed up
     * indexing substantially.
     * <p>
     * Important: The filter is applied as part of scanning process for {@link PortableType} annotated classes and as such
     * this property DOES affect scanning performance.
     */
    @Parameter(name      = "pofIndexPackages",
               property  = "pof.index.packages")
    private Set<String> m_sPofIndexPackages;

    /**
     * A flag indicating whether this is test classes instrumentation.
     */
    private final boolean f_fTests;
    }
