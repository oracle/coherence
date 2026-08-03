/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.reporter;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link ReporterSecurity}.
 *
 * @author as 2026.05.15
 * @since 26.07
 */
public class ReporterSecurityTest
    {
    @After
    public void cleanup()
            throws IOException
        {
        restoreProperty("coherence.reporter.output.directory", m_sOutputRoot);
        deleteDir(m_dirTemp);
        }

    @Test
    public void shouldAllowBackslashOutputPathUnderApprovedRoot()
            throws Exception
        {
        File dirRoot   = createTempDir();
        File dirOutput = new File(dirRoot, "member\\report-output");

        m_sOutputRoot = System.getProperty("coherence.reporter.output.directory");
        System.setProperty("coherence.reporter.output.directory", dirRoot.getCanonicalPath());

        assertThat(ReporterSecurity.validateOutputPath(dirOutput.getPath(), "test"),
                is(dirOutput.getCanonicalPath()));
        }

    private File createTempDir()
            throws IOException
        {
        m_dirTemp = Files.createTempDirectory("reporter-security-test").toFile();
        return m_dirTemp;
        }

    private void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    private void deleteDir(File file)
            throws IOException
        {
        if (file == null || !file.exists())
            {
            return;
            }

        File[] aFiles = file.listFiles();
        if (aFiles != null)
            {
            for (File child : aFiles)
                {
                deleteDir(child);
                }
            }

        Files.deleteIfExists(file.toPath());
        }

    private String m_sOutputRoot;

    private File m_dirTemp;
    }
