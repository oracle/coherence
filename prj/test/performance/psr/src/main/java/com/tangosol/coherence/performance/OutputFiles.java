/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.performance;

import org.junit.rules.TestName;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.File;
import java.net.URL;

/**
 * @author jk 2016.03.10
 */
public class OutputFiles extends TestName
    {
    // ----- constructors ---------------------------------------------------

    public OutputFiles()
        {
        }

    // ----- TestName methods -----------------------------------------------

    @Override
    public Statement apply(Statement base, Description description)
        {
        try
            {
            m_testClass = description.getTestClass();

            URL    url  = m_testClass.getProtectionDomain().getCodeSource().getLocation();
            File   file = new File(url.toURI());

            m_fileBuild = file.getParentFile();

            m_fileOutputBase = new File(m_fileBuild.getCanonicalPath() + File.separator + "test-output"
                            + File.separator + "performance" + File.separator + m_testClass.getName());

            return super.apply(base, description);
            }
        catch (Exception e)
            {
            throw new RuntimeException(e);
            }
        }

    // ----- OutputFiles methods --------------------------------------------

    public File getBuildFolder()
            throws Exception
        {
        return m_fileBuild;
        }

    public File getOutputFolder()
            throws Exception
        {
        File folder = new File(m_fileOutputBase, getMethodName());

        folder.mkdirs();

        return folder;
        }

    // ----- data members ---------------------------------------------------

    private Class<?> m_testClass;

    private File m_fileBuild;

    private File m_fileOutputBase;
    }
