/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.reporter;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import java.nio.file.Files;

import javax.management.ObjectName;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for {@link ReporterSecurity}.
 *
 * @author as 2026.05.15
 * @since 26.07
 */
public class ReporterSecurityTest
    {
    @Before
    public void captureProperties()
        {
        m_sOutputRoot          = System.getProperty("coherence.reporter.output.directory");
        m_sReportConfig        = System.getProperty("coherence.management.report.configuration");
        m_sRemoteReportAllowed = System.getProperty(ReporterSecurity.PROP_REMOTE_REPORT_ALLOWED);
        }

    @After
    public void cleanup()
            throws IOException
        {
        restoreProperty("coherence.reporter.output.directory", m_sOutputRoot);
        restoreProperty("coherence.management.report.configuration", m_sReportConfig);
        restoreProperty(ReporterSecurity.PROP_REMOTE_REPORT_ALLOWED, m_sRemoteReportAllowed);
        CoherenceModeHelper.reset();
        deleteDir(m_dirTemp);
        }

    @Test
    public void shouldAllowBackslashOutputPathUnderApprovedRoot()
            throws Exception
        {
        File dirRoot   = createTempDir();
        File dirOutput = new File(dirRoot, "member\\report-output");

        System.setProperty("coherence.reporter.output.directory", dirRoot.getCanonicalPath());

        assertThat(ReporterSecurity.validateOutputPath(dirOutput.getPath(), "test"),
                is(dirOutput.getCanonicalPath()));
        }

    @Test
    public void shouldRejectRemoteReportUrlInHardenedMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl("http://127.0.0.1:1/report.xml",
                            ReporterSecurityTest.class.getClassLoader(), "runTabularReport", "test"));
            }
        }

    @Test
    public void shouldAllowApprovedRemoteReportUrlInHardenedMode()
            throws Exception
        {
        String sBase = "http://127.0.0.1:12345";
        System.setProperty(ReporterSecurity.PROP_REMOTE_REPORT_ALLOWED, sBase);

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            URL url = ReporterSecurity.resolveTrustedReportUrl(sBase + "/report.xml",
                    ReporterSecurityTest.class.getClassLoader(), "runTabularReport", "test");

            assertThat(url.toExternalForm(), is(sBase + "/report.xml"));
            }
        }

    @Test
    public void shouldAcceptWildFlyVfsReportResource()
            throws Exception
        {
        String      sReport = "reports/report-group.xml";
        ClassLoader loader  = new ReportResourceClassLoader(sReport, "vfs");

        URL url = ReporterSecurity.resolveTrustedReportUrl(sReport, loader, "setConfigFile", "jmx-direct");

        assertThat(url.getProtocol(), is("vfs"));
        }

    @Test
    public void shouldAcceptOpenLibertyWsjarReportResource()
            throws Exception
        {
        String      sReport = "reports/report-group.xml";
        ClassLoader loader  = new ReportResourceClassLoader(sReport, "wsjar:file");

        URL url = ReporterSecurity.resolveTrustedReportUrl(sReport, loader, "setConfigFile", "jmx-direct");

        assertThat(url.getProtocol(), is("wsjar"));
        }

    @Test
    public void shouldRejectUnknownReportResourceProtocolInCompatibilityMode()
            throws Exception
        {
        String      sReport = "reports/report-group.xml";
        ClassLoader loader  = new ReportResourceClassLoader(sReport, "unknown");

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl(sReport, loader, "setConfigFile", "jmx-direct"));
            }
        }

    @Test
    public void shouldRejectOutOfRootFileUrlInHardenedMode()
            throws Exception
        {
        File dir  = createTempDir();
        File file = new File(dir, "unapproved-reporter-input.xml");
        assertThat(file.createNewFile(), is(true));

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl(file.toURI().toString(),
                            ReporterSecurityTest.class.getClassLoader(), "runTabularReport", "test"));
            }
        }

    @Test
    public void shouldRejectOutputPathOutsideApprovedRootInHardenedMode()
            throws Exception
        {
        File dirRoot = createTempDir();
        File outside = new File(dirRoot.getParentFile(), dirRoot.getName() + "-outside");

        System.setProperty("coherence.reporter.output.directory", dirRoot.getCanonicalPath());

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.validateOutputPath(outside.getAbsolutePath(), "test"));
            }
        }

    @Test
    public void shouldRejectUntrustedMethodColumnInHardenedMode()
            throws Exception
        {
        ObjectName name = new ObjectName("Coherence:type=Federation,responsibility=Coordinator");
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.validateMBeanOperation(name, "retrievePendingIncomingMessages",
                            ReporterSecurity.ReportSource.INLINE));
            }
        }

    @Test
    public void shouldRejectDiagnosticCommandOperationInHardenedMode()
            throws Exception
        {
        ObjectName name = new ObjectName("com.sun.management:type=DiagnosticCommand");
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.validateMBeanOperation(name, "vmVersion",
                            ReporterSecurity.ReportSource.TRUSTED));
            }
        }

    @Test
    public void shouldRejectUnsupportedColumnClassInHardenedMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.rejectColumnClass("class", "java.lang.Runtime"));
            }
        }

    @Test
    public void shouldAllowOutputPathOutsideApprovedRootInCompatibilityMode()
            throws Exception
        {
        File dirRoot = createTempDir();
        File outside = new File(dirRoot.getParentFile(), dirRoot.getName() + "-outside");

        System.setProperty("coherence.reporter.output.directory", dirRoot.getCanonicalPath());

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            assertThat(ReporterSecurity.validateOutputPath(outside.getAbsolutePath(), "test"),
                    is(outside.getCanonicalPath()));
            }
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

    // ----- helper classes ------------------------------------------------

    /**
     * ClassLoader that exposes a report resource using the specified protocol.
     */
    private static class ReportResourceClassLoader
            extends ClassLoader
        {
        ReportResourceClassLoader(String sReport, String sProtocol)
                throws IOException
            {
            super(null);
            m_sReport = sReport;
            m_url     = new URL(null,
                    sProtocol + ":/content/web.war/WEB-INF/lib/coherence.jar/" + sReport,
                    new URLStreamHandler()
                        {
                        @Override
                        protected URLConnection openConnection(URL url)
                            {
                            throw new UnsupportedOperationException();
                            }
                        });
            }

        @Override
        public URL getResource(String sName)
            {
            return m_sReport.equals(sName) ? m_url : null;
            }

        // ----- data members ---------------------------------------------

        private final String m_sReport;

        private final URL m_url;
        }

    private String m_sOutputRoot;

    private String m_sReportConfig;

    private String m_sRemoteReportAllowed;

    private File m_dirTemp;
    }
