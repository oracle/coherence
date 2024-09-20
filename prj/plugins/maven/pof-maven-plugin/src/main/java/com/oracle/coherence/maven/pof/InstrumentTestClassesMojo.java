/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.pof;


import com.tangosol.io.pof.generator.PortableTypeGenerator;
import java.io.File;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;


/**
 * A Maven plugin that instruments classes to implement evolvable portable types
 * by running the {@link PortableTypeGenerator} class.
 *
 * @author jk  2017.07.25
 */
@Mojo(name = "instrument-tests",
        defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true)
public class InstrumentTestClassesMojo
        extends PortableTypeMojo
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create an {@link InstrumentTestClassesMojo}.
     */
    public InstrumentTestClassesMojo()
        {
        super(true);
        }

    // ----- PortableTypeMojo methods ---------------------------------------

    @Override
    protected File[] getClassesDirectories()
        {
        return new File[]{m_fileClassesDir, m_fileTestClassesDir};
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Set the project classes directory.
     *
     * @param fileClassesDir  the project classes directory
     */
    public void setClassesDirectory(File fileClassesDir)
        {
        m_fileClassesDir = fileClassesDir;
        }

    /**
     * Set the project test classes directory.
     *
     * @param fileTestClassesDir  the project classes directory
     */
    public void setTestClassesDirectory(File fileTestClassesDir)
        {
        m_fileTestClassesDir = fileTestClassesDir;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Location of the project classes.
     */
    @Parameter(name = "classesDirectory", 
               property = "project.build.outputDirectory",
               defaultValue = "${project.build.outputDirectory}",
               required = true)
    private File m_fileClassesDir;

    /**
     * Location of the project test classes.
     */
    @Parameter(name = "testClassesDirectory",
               property = "project.build.testOutputDirectory",
               defaultValue = "${project.build.testOutputDirectory}",
               required = true)
    private File m_fileTestClassesDir;
    }
