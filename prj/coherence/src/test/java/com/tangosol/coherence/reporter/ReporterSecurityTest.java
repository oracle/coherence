/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.reporter;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.lang.reflect.Field;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import java.nio.file.Files;
import java.nio.file.LinkOption;

import java.util.HashSet;
import java.util.Set;

import javax.management.ObjectName;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

/**
 * Unit tests for {@link ReporterSecurity}.
 *
 * @author as 2026.05.15
 * @since 26.04
 */
public class ReporterSecurityTest
    {
    @Before
    public void captureProperties()
        {
        m_sOutputRoot          = System.getProperty(PROP_OUTPUT_ROOT);
        m_sLegacyOutputRoot    = System.getProperty(PROP_LEGACY_OUTPUT_ROOT);
        m_sReportConfig        = System.getProperty(PROP_REPORT_CONFIG);
        m_sLegacyReportConfig  = System.getProperty(PROP_LEGACY_REPORT_CONFIG);
        m_sRemoteReportAllowed = System.getProperty(ReporterSecurity.PROP_REMOTE_REPORT_ALLOWED);
        }

    @After
    public void cleanup()
            throws IOException
        {
        restoreProperty(PROP_OUTPUT_ROOT, m_sOutputRoot);
        restoreProperty(PROP_LEGACY_OUTPUT_ROOT, m_sLegacyOutputRoot);
        restoreProperty(PROP_REPORT_CONFIG, m_sReportConfig);
        restoreProperty(PROP_LEGACY_REPORT_CONFIG, m_sLegacyReportConfig);
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

        System.setProperty(PROP_OUTPUT_ROOT, dirRoot.getCanonicalPath());

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
    public void shouldAllowOutputPathUsingLegacyOutputDirectoryProperty()
            throws Exception
        {
        File dirRoot   = createTempDir();
        File dirOutput = new File(dirRoot, "report-output");

        System.clearProperty(PROP_OUTPUT_ROOT);
        System.setProperty(PROP_LEGACY_OUTPUT_ROOT, dirRoot.getCanonicalPath());

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThat(ReporterSecurity.validateOutputPath(dirOutput.getPath(), "test"),
                    is(dirOutput.getCanonicalPath()));
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
    public void shouldAcceptFileReportResourceFromClassLoader()
            throws Exception
        {
        String      sReport = "reports/report-group.xml";
        ClassLoader loader  = new ReportResourceClassLoader(sReport, "file");

        URL url = ReporterSecurity.resolveTrustedReportUrl(sReport, loader, "setConfigFile", "jmx-direct");

        assertThat(url.getProtocol(), is("file"));
        }

    @Test
    public void shouldRejectInteriorTraversalBeforeClasspathLookup()
            throws Exception
        {
        String      sReport = "reports/../outside.xml";
        ClassLoader loader  = new ReportResourceClassLoader(sReport, "file");

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl(sReport, loader,
                            "setConfigFile", "jmx-direct"));
            }
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
    public void shouldAllowAbsoluteReportPathUnderApprovedRootInHardenedMode()
            throws Exception
        {
        File dir        = createTempDir();
        File fileConfig = createReportFile(dir, "report-group.xml");
        File fileReport = createReportFile(dir, "report.xml");
        System.setProperty(PROP_REPORT_CONFIG, fileConfig.getCanonicalPath());

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            URL urlConfig = ReporterSecurity.resolveTrustedReportUrl(fileConfig.getAbsolutePath(),
                    ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct");
            URL urlPath = ReporterSecurity.resolveTrustedReportUrl(fileReport.getAbsolutePath(),
                    ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct");
            URL urlUri  = ReporterSecurity.resolveTrustedReportUrl(fileReport.toURI().toString(),
                    ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct");

            assertThat(urlConfig.toExternalForm(), is(fileConfig.getCanonicalFile().toURI().toURL().toExternalForm()));
            assertThat(urlPath.toExternalForm(), is(fileReport.getCanonicalFile().toURI().toURL().toExternalForm()));
            assertThat(urlPath.toExternalForm(), is(urlUri.toExternalForm()));
            }
        }

    @Test
    public void shouldAllowAbsoluteReportPathWithSpaceUnderApprovedRootInHardenedMode()
            throws Exception
        {
        File dir        = new File(createTempDir(), "reporter root");
        File fileConfig = createReportFile(dir, "report-group.xml");
        File fileReport = createReportFile(dir, "report.xml");
        System.setProperty(PROP_REPORT_CONFIG, fileConfig.getCanonicalPath());

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            URL url = ReporterSecurity.resolveTrustedReportUrl(fileReport.getAbsolutePath(),
                    ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct");

            assertThat(url.toExternalForm(), is(fileReport.getCanonicalFile().toURI().toURL().toExternalForm()));
            }
        }

    @Test
    public void shouldAllowRelativeReportPathUnderApprovedRootInHardenedMode()
            throws Exception
        {
        File dir        = createTempDirInWorkingDirectory();
        File fileConfig = createReportFile(dir, "report-group.xml");
        File fileReport = createReportFile(dir, "report.xml");
        System.setProperty(PROP_REPORT_CONFIG, fileConfig.getCanonicalPath());

        String sRelativePath = toWorkingDirectoryRelativePath(fileReport);

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            URL urlPath = ReporterSecurity.resolveTrustedReportUrl(sRelativePath,
                    ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct");
            URL urlUri = ReporterSecurity.resolveTrustedReportUrl(fileReport.toURI().toString(),
                    ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct");

            assertThat(urlPath.toExternalForm(), is(urlUri.toExternalForm()));
            }
        }

    @Test
    public void shouldRejectRelativeReportPathOutsideApprovedRootInHardenedMode()
            throws Exception
        {
        File dir         = createTempDirInWorkingDirectory();
        File dirApproved = new File(dir, "approved");
        File fileConfig  = createReportFile(dirApproved, "report-group.xml");
        File fileOutside = createReportFile(dir, "outside.xml");
        System.setProperty(PROP_REPORT_CONFIG, fileConfig.getCanonicalPath());

        String sRelativePath = toWorkingDirectoryRelativePath(fileOutside);

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl(sRelativePath,
                            ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct"));
            }
        }

    @Test
    public void shouldAllowAbsoluteReportPathUsingLegacyConfigurationProperty()
            throws Exception
        {
        File file = createReportFile(createTempDir(), "reports.xml");
        System.clearProperty(PROP_REPORT_CONFIG);
        System.setProperty(PROP_LEGACY_REPORT_CONFIG, file.getCanonicalPath());

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            URL url = ReporterSecurity.resolveTrustedReportUrl(file.getAbsolutePath(),
                    ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct");

            assertThat(url.toExternalForm(), is(file.getCanonicalFile().toURI().toURL().toExternalForm()));
            }
        }

    @Test
    public void shouldAllowReportPathWhenConfiguredReportFileDoesNotExistInHardenedMode()
            throws Exception
        {
        File dir        = createTempDir();
        File fileConfig = new File(dir, "missing-report-group.xml");
        File fileReport = createReportFile(dir, "report.xml");
        System.setProperty(PROP_REPORT_CONFIG, fileConfig.getAbsolutePath());

        assertThat(fileConfig.exists(), is(false));

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            URL url = ReporterSecurity.resolveTrustedReportUrl(fileReport.getAbsolutePath(),
                    ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct");

            assertThat(url.toExternalForm(), is(fileReport.getCanonicalFile().toURI().toURL().toExternalForm()));
            }
        }

    @Test
    public void shouldUseConfiguredReportSymlinkTargetAsApprovedRootInHardenedMode()
            throws Exception
        {
        File dir            = createTempDir();
        File dirLink        = new File(dir, "link-root");
        File dirTarget      = new File(dir, "target-root");
        File fileConfig     = createReportFile(dirTarget, "reports.xml");
        File fileReport     = createReportFile(dirTarget, "report.xml");
        File fileOutside    = createReportFile(dirLink, "outside.xml");
        File fileConfigLink = new File(dirLink, "reports.xml");

        try
            {
            Files.createSymbolicLink(fileConfigLink.toPath(), fileConfig.toPath());
            }
        catch (IOException | UnsupportedOperationException e)
            {
            Assume.assumeNoException(e);
            }

        System.setProperty(PROP_REPORT_CONFIG, fileConfigLink.getAbsolutePath());

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            URL urlConfig = ReporterSecurity.resolveTrustedReportUrl(fileConfigLink.getAbsolutePath(),
                    ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct");
            URL urlReport = ReporterSecurity.resolveTrustedReportUrl(fileReport.getAbsolutePath(),
                    ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct");

            assertThat(urlConfig.toExternalForm(), is(fileConfig.getCanonicalFile().toURI().toURL().toExternalForm()));
            assertThat(urlReport.toExternalForm(), is(fileReport.getCanonicalFile().toURI().toURL().toExternalForm()));
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl(fileOutside.getAbsolutePath(),
                            ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct"));
            }
        }

    @Test
    public void shouldRejectAbsoluteReportPathOutsideApprovedRootInHardenedMode()
            throws Exception
        {
        File dir         = createTempDir();
        File dirApproved = new File(dir, "approved");
        File fileConfig  = createReportFile(dirApproved, "reports.xml");
        File fileOutside = createReportFile(dir, "outside.xml");
        System.setProperty(PROP_REPORT_CONFIG, fileConfig.getCanonicalPath());

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl(fileOutside.getAbsolutePath(),
                            ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct"));
            }
        }

    @Test
    public void shouldAllowAbsoluteReportPathOutsideApprovedRootInCompatibilityMode()
            throws Exception
        {
        File dir         = createTempDir();
        File dirApproved = new File(dir, "approved");
        File fileConfig  = createReportFile(dirApproved, "reports.xml");
        File fileOutside = createReportFile(dir, "outside.xml");
        System.setProperty(PROP_REPORT_CONFIG, fileConfig.getCanonicalPath());

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            URL url = ReporterSecurity.resolveTrustedReportUrl(fileOutside.getAbsolutePath(),
                    ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct");

            assertThat(url.toExternalForm(), is(fileOutside.getCanonicalFile().toURI().toURL().toExternalForm()));
            }
        }

    @Test
    public void shouldRejectAbsoluteReportPathSymlinkOutsideApprovedRootInHardenedMode()
            throws Exception
        {
        assertRejectReportPathSymlinkOutsideApprovedRoot(false);
        }

    @Test
    public void shouldRejectFileUriSymlinkOutsideApprovedRootInHardenedMode()
            throws Exception
        {
        assertRejectReportPathSymlinkOutsideApprovedRoot(true);
        }

    private void assertRejectReportPathSymlinkOutsideApprovedRoot(boolean fFileUri)
            throws Exception
        {
        File dir         = createTempDir();
        File dirApproved = new File(dir, "approved");
        File fileConfig  = createReportFile(dirApproved, "reports.xml");
        File fileOutside = createReportFile(dir, "outside.xml");
        File fileLink    = new File(dirApproved, "link.xml");
        System.setProperty(PROP_REPORT_CONFIG, fileConfig.getCanonicalPath());

        try
            {
            Files.createSymbolicLink(fileLink.toPath(), fileOutside.toPath());
            }
        catch (IOException | UnsupportedOperationException e)
            {
            Assume.assumeNoException(e);
            }

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            String sName = fFileUri ? fileLink.toURI().toString() : fileLink.getAbsolutePath();

            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl(sName,
                            ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct"));
            }
        }

    @Test
    public void shouldRejectNetworkAbsoluteReportPathInHardenedMode()
        {
        String sNetworkPath = "//reporter-host/share/reports.xml";
        System.setProperty(PROP_REPORT_CONFIG, sNetworkPath);

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl(sNetworkPath,
                            ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct"));
            }
        }

    @Test
    public void shouldRejectUnsafeFileUrisInHardenedMode()
        {
        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityHardened())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl("file://reporter-host/share/report.xml",
                            ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct"));
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl("file:/reports/%2e%2e/report.xml",
                            ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct"));
            }

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            assertThrows(ReporterSecurity.ReporterSecurityException.class,
                    () -> ReporterSecurity.resolveTrustedReportUrl("file:////reporter-host/share/report.xml",
                            ReporterSecurityTest.class.getClassLoader(), "setConfigFile", "jmx-direct"));
            }
        }

    @Test
    public void shouldRejectOutputPathOutsideApprovedRootInHardenedMode()
            throws Exception
        {
        File dirRoot = createTempDir();
        File outside = new File(dirRoot.getParentFile(), dirRoot.getName() + "-outside");

        System.setProperty(PROP_OUTPUT_ROOT, dirRoot.getCanonicalPath());

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

        System.setProperty(PROP_OUTPUT_ROOT, dirRoot.getCanonicalPath());

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            assertThat(ReporterSecurity.validateOutputPath(outside.getAbsolutePath(), "test"),
                    is(outside.getCanonicalPath()));
            }
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldLogCompatibilityWarningOncePerCategory()
            throws Exception
        {
        Field field = ReporterSecurity.class.getDeclaredField("LOGGED_COMPATIBILITY_WARNINGS");
        field.setAccessible(true);

        Set<Object> setWarnings      = (Set<Object>) field.get(null);
        Set<Object> setWarningsSaved = new HashSet<>(setWarnings);
        setWarnings.clear();

        try (CoherenceModeHelper.ModeScope ignored = CoherenceModeHelper.securityCompatibility())
            {
            ReporterSecurity.validateObjectNamePattern("java.lang:type=Memory", "test");
            assertThat(setWarnings.size(), is(1));

            ReporterSecurity.validateObjectNamePattern("com.sun.management:type=DiagnosticCommand", "test");
            assertThat(setWarnings.size(), is(1));

            ReporterSecurity.validateObjectNamePattern("*:type=Memory", "test");
            assertThat(setWarnings.size(), is(2));

            ReporterSecurity.validateObjectNamePattern("*:type=Threading", "test");
            assertThat(setWarnings.size(), is(2));
            }
        finally
            {
            setWarnings.clear();
            setWarnings.addAll(setWarningsSaved);
            }
        }

    private File createTempDir()
            throws IOException
        {
        m_dirTemp = Files.createTempDirectory("reporter-security-test").toFile();
        return m_dirTemp;
        }

    private File createTempDirInWorkingDirectory()
            throws IOException
        {
        m_dirTemp = Files.createTempDirectory(new File(".").toPath(), "reporter-security-test").toFile();
        return m_dirTemp;
        }

    private String toWorkingDirectoryRelativePath(File file)
            throws IOException
        {
        return new File(".").getCanonicalFile().toPath()
                .relativize(file.getCanonicalFile().toPath())
                .toString()
                .replace(File.separatorChar, '/');
        }

    private File createReportFile(File dir, String sName)
            throws IOException
        {
        assertThat(dir.mkdirs() || dir.isDirectory(), is(true));
        File file = new File(dir, sName);
        assertThat(file.createNewFile(), is(true));
        return file;
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
        if (file == null || !Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS))
            {
            return;
            }

        if (Files.isSymbolicLink(file.toPath()))
            {
            Files.deleteIfExists(file.toPath());
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

    private static final String PROP_REPORT_CONFIG = "coherence.management.report.configuration";

    private static final String PROP_LEGACY_REPORT_CONFIG = "tangosol.coherence.management.report.configuration";

    private static final String PROP_OUTPUT_ROOT = "coherence.reporter.output.directory";

    private static final String PROP_LEGACY_OUTPUT_ROOT = "tangosol.coherence.reporter.output.directory";

    private String m_sOutputRoot;

    private String m_sLegacyOutputRoot;

    private String m_sReportConfig;

    private String m_sLegacyReportConfig;

    private String m_sRemoteReportAllowed;

    private File m_dirTemp;
    }
