/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.reporter;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Resources;

import java.io.File;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Central Reporter input policy for management/JMX reachable report paths.
 *
 * @author as 2026.05.14
 * @since 26.07
 */
public final class ReporterSecurity
    {
    /**
     * Reporter policy rejection.
     */
    public static class ReporterSecurityException
            extends IllegalArgumentException
        {
        /**
         * Construct a ReporterSecurityException.
         *
         * @param sMessage  the message
         */
        public ReporterSecurityException(String sMessage)
            {
            super(sMessage);
            }
        }

    /**
     * The provenance of the report definition currently being executed.
     */
    public enum ReportSource
        {
        TRUSTED,
        INLINE,
        UNKNOWN
        }

    /**
     * Load a trusted Reporter XML resource after validating the caller supplied name.
     *
     * @param sName       the report resource name
     * @param sDescr      the resource description
     * @param loader      the class loader
     * @param sOperation  the Reporter operation
     * @param sScope      the caller scope
     *
     * @return the loaded XML document
     */
    public static XmlDocument loadTrustedReportXml(String sName, String sDescr, ClassLoader loader,
            String sOperation, String sScope)
        {
        URL url = resolveTrustedReportUrl(sName, loader, sOperation, sScope);
        return "file".equalsIgnoreCase(url.getProtocol())
                ? XmlHelper.loadXml(url)
                : XmlHelper.loadXml(url, "ISO-8859-1");
        }

    /**
     * Validate a Reporter resource name before generic resource loading.
     *
     * @param sName       the report resource name
     * @param sOperation  the Reporter operation
     * @param sScope      the caller scope
     */
    public static void validateReportResourceName(String sName, String sOperation, String sScope)
        {
        resolveTrustedReportUrl(sName, ReporterSecurity.class.getClassLoader(), sOperation, sScope);
        }

    /**
     * Validate a Reporter resource name before generic resource loading.
     *
     * @param sName       the report resource name
     * @param loader      the class loader
     * @param sOperation  the Reporter operation
     * @param sScope      the caller scope
     *
     * @return the approved resource URL
     */
    public static URL resolveTrustedReportUrl(String sName, ClassLoader loader, String sOperation, String sScope)
        {
        if (sName == null || sName.trim().isEmpty())
            {
            reject(sScope, sOperation, "reporter-resource-allowlist", "empty-resource", sName);
            }

        if (containsControl(sName) || sName.indexOf('\\') >= 0 || sName.indexOf('%') >= 0)
            {
            reject(sScope, sOperation, "reporter-resource-allowlist", "unsafe-resource-name", sName);
            }

        URI uri;
        try
            {
            uri = new URI(sName);
            }
        catch (URISyntaxException e)
            {
            reject(sScope, sOperation, "reporter-resource-allowlist", "invalid-resource-uri", sName);
            return null;
            }

        String sScheme = uri.getScheme();
        if (sScheme == null)
            {
            validateRelativePathName(sName, sScope, sOperation, "reporter-resource-allowlist");
            return resolveRelativeReportResource(sName, loader, sOperation, sScope);
            }

        if (!"file".equalsIgnoreCase(sScheme))
            {
            return resolveRemoteReportUrl(uri, sName, sOperation, sScope);
            }

        String sHost = uri.getHost();
        if (sHost != null && !sHost.isEmpty() && !"localhost".equalsIgnoreCase(sHost))
            {
            reject(sScope, sOperation, "reporter-resource-allowlist", "unsupported-file-host", sName);
            }

        try
            {
            File file = Paths.get(uri).toFile().getCanonicalFile();
            if (!isUnderAny(file, getApprovedReportFileRoots()))
                {
                if (!CoherenceMode.isSecurityHardeningEnabled())
                    {
                    shadow(sScope, sOperation, "reporter-resource-allowlist", "file-outside-root", sName);
                    return file.toURI().toURL();
                    }
                reject(sScope, sOperation, "reporter-resource-allowlist", "file-outside-root", sName);
                }
            return file.toURI().toURL();
            }
        catch (ReporterSecurityException e)
            {
            throw e;
            }
        catch (IllegalArgumentException | IOException e)
            {
            reject(sScope, sOperation, "reporter-resource-allowlist", "invalid-file-resource", sName);
            return null;
            }
        }

    /**
     * Validate a management REST report name segment.
     *
     * @param sReportName  the report name path parameter
     */
    public static void validateRestReportName(String sReportName)
        {
        if (sReportName == null || !sReportName.matches("[A-Za-z0-9_-]+"))
            {
            reject("member", "runReport", "reporter-resource-allowlist", "unsafe-report-name", sReportName);
            }
        }

    /**
     * Validate Reporter update fields before generic MBean dispatch.
     *
     * @param entity  the REST entity
     * @param sScope  the route scope
     */
    public static void validateReporterUpdate(Map<String, Object> entity, String sScope)
        {
        if (entity == null)
            {
            return;
            }

        Object oConfig = entity.get("configFile");
        if (oConfig != null)
            {
            validateReportResourceName(String.valueOf(oConfig), "setConfigFile", sScope);
            }

        Object oOutputPath = entity.get("outputPath");
        if (oOutputPath != null)
            {
            validateOutputPath(String.valueOf(oOutputPath), sScope);
            }
        }

    /**
     * Validate a configured Reporter output path.
     *
     * @param sPath   the output path
     * @param sScope  the caller scope
     */
    public static String validateOutputPath(String sPath, String sScope)
        {
        if (sPath == null || containsControl(sPath))
            {
            reject(sScope, "setOutputPath", "reporter-output-allowlist", "invalid-output-path", sPath);
            }

        try
            {
            File root = getApprovedOutputRoot();
            if (sPath.trim().isEmpty() || ".".equals(sPath.trim()))
                {
                return root.getCanonicalPath();
                }

            if (sPath.indexOf('%') >= 0)
                {
                reject(sScope, "setOutputPath", "reporter-output-allowlist", "unsafe-output-path", sPath);
                }

            if (hasOutputUriScheme(sPath))
                {
                reject(sScope, "setOutputPath", "reporter-output-allowlist", "unsupported-output-scheme", sPath);
                }

            Path path = Paths.get(sPath).normalize();
            for (Path part : path)
                {
                if ("..".equals(part.toString()))
                    {
                    reject(sScope, "setOutputPath", "reporter-output-allowlist", "path-traversal", sPath);
                    }
                }

            File file = path.isAbsolute()
                    ? path.toFile().getCanonicalFile()
                    : root.toPath().resolve(path).toFile().getCanonicalFile();
            if (!isUnder(file, root))
                {
                if (!CoherenceMode.isSecurityHardeningEnabled())
                    {
                    shadow(sScope, "setOutputPath", "reporter-output-allowlist", "output-outside-root", sPath);
                    return file.getCanonicalPath();
                    }
                reject(sScope, "setOutputPath", "reporter-output-allowlist", "output-outside-root", sPath);
                }

            return file.getCanonicalPath();
            }
        catch (ReporterSecurityException e)
            {
            throw e;
            }
        catch (URISyntaxException e)
            {
            reject(sScope, "setOutputPath", "reporter-output-allowlist", "invalid-output-path", sPath);
            return null;
            }
        catch (IOException | RuntimeException e)
            {
            reject(sScope, "setOutputPath", "reporter-output-allowlist", "invalid-output-path", sPath);
            return null;
            }
        }

    private static boolean hasOutputUriScheme(String sPath)
            throws URISyntaxException
        {
        if (isWindowsAbsolutePath(sPath))
            {
            return false;
            }

        if (sPath.indexOf('\\') >= 0)
            {
            return false;
            }

        URI uri = new URI(sPath);
        return uri.getScheme() != null;
        }

    private static boolean isWindowsAbsolutePath(String sPath)
        {
        return sPath.length() >= 3
                && Character.isLetter(sPath.charAt(0))
                && sPath.charAt(1) == ':'
                && (sPath.charAt(2) == '\\' || sPath.charAt(2) == '/');
        }

    /**
     * Validate a report XML file-name template before it can be combined with an output path.
     *
     * @param sFileName  the file-name template
     * @param sScope     the caller scope
     */
    public static void validateOutputFileNameTemplate(String sFileName, String sScope)
        {
        if (sFileName == null || sFileName.isEmpty())
            {
            return;
            }

        if (containsControl(sFileName) || sFileName.indexOf('/') >= 0 || sFileName.indexOf('\\') >= 0
                || sFileName.indexOf(':') >= 0 || sFileName.indexOf('%') >= 0
                || ".".equals(sFileName) || "..".equals(sFileName))
            {
            reject(sScope, "report-file", "reporter-output-allowlist", "unsafe-file-name", sFileName);
            }
        }

    /**
     * Validate the final output file before creation or open.
     *
     * @param sFileName  the final file name
     * @param sRoot      the configured output root
     * @param sScope     the caller scope
     *
     * @return the canonical output file
     */
    public static File validateOutputFile(String sFileName, String sRoot, String sScope)
        {
        if (sFileName == null || sFileName.isEmpty())
            {
            return null;
            }

        try
            {
            File root = new File(validateOutputPath(sRoot == null ? "" : sRoot, sScope)).getCanonicalFile();
            File file = new File(sFileName).getCanonicalFile();
            if (!isUnder(file, root))
                {
                reject(sScope, "report-file", "reporter-output-allowlist", "output-outside-root", sFileName);
                }
            return file;
            }
        catch (IOException e)
            {
            reject(sScope, "report-file", "reporter-output-allowlist", "invalid-output-file", sFileName);
            return null;
            }
        }

    /**
     * Validate a Reporter ObjectName query pattern.
     *
     * @param sPattern  the ObjectName pattern
     * @param sScope    the caller scope
     */
    public static void validateObjectNamePattern(String sPattern, String sScope)
        {
        try
            {
            validateObjectName(new ObjectName(sPattern), sScope);
            }
        catch (MalformedObjectNameException e)
            {
            reject(sScope, "query", "reporter-objectname-allowlist", "invalid-object-name", sPattern);
            }
        }

    /**
     * Validate a concrete Reporter ObjectName and operation before invocation.
     *
     * @param name     the ObjectName
     * @param sMethod  the operation name
     * @param source   the report source
     */
    public static void validateMBeanOperation(ObjectName name, String sMethod, ReportSource source)
        {
        validateObjectName(name, "reporter-core");
        if (source != ReportSource.TRUSTED)
            {
            reject("reporter-core", sMethod, "reporter-operation-allowlist", "untrusted-method-column",
                    name == null ? null : name.getCanonicalName());
            }
        if (sMethod == null || sMethod.trim().isEmpty() || containsControl(sMethod))
            {
            reject("reporter-core", sMethod, "reporter-operation-allowlist", "unsafe-operation-name", sMethod);
            }
        }

    /**
     * Reject a non-built-in Reporter column locator class.
     *
     * @param sTypeValue  the column type value
     * @param sClass      the resolved class name
     */
    public static void rejectColumnClass(String sTypeValue, String sClass)
        {
        reject("reporter-core", "column", "reporter-column-class-allowlist", "unsupported-column-class",
                sClass == null || sClass.isEmpty() ? sTypeValue : sClass);
        }

    /**
     * Enter a report-source scope.
     *
     * @param source  the source to enter
     *
     * @return the previous source
     */
    public static ReportSource enterReportSource(ReportSource source)
        {
        ReportSource previous = CURRENT_SOURCE.get();
        CURRENT_SOURCE.set(source == null ? ReportSource.UNKNOWN : source);
        return previous;
        }

    /**
     * Restore a previous report-source scope.
     *
     * @param source  the previous source
     */
    public static void restoreReportSource(ReportSource source)
        {
        CURRENT_SOURCE.set(source == null ? ReportSource.UNKNOWN : source);
        }

    /**
     * Return the current report source.
     *
     * @return the current report source
     */
    public static ReportSource currentReportSource()
        {
        return CURRENT_SOURCE.get();
        }

    // ----- helper methods -------------------------------------------------

    private static URL resolveRelativeReportResource(String sName, ClassLoader loader, String sOperation, String sScope)
        {
        URL url = Resources.findResource(sName, loader, null);
        if (url != null)
            {
            String sProtocol = url.getProtocol();
            if (isTrustedResourceProtocol(sProtocol))
                {
                return url;
                }

            reject(sScope, sOperation, "reporter-resource-allowlist", "unsupported-resource-protocol", sName);
            }

        try
            {
            File file = new File(sName).getCanonicalFile();
            if (file.exists() && isUnderAny(file, getApprovedReportFileRoots()))
                {
                return file.toURI().toURL();
                }
            if (file.exists() && !CoherenceMode.isSecurityHardeningEnabled())
                {
                shadow(sScope, sOperation, "reporter-resource-allowlist", "file-outside-root", sName);
                return file.toURI().toURL();
                }
            }
        catch (IOException | RuntimeException e)
            {
            reject(sScope, sOperation, "reporter-resource-allowlist", "invalid-file-resource", sName);
            }

        reject(sScope, sOperation, "reporter-resource-allowlist", "file-outside-root", sName);
        return null;
        }

    private static boolean isTrustedResourceProtocol(String sProtocol)
        {
        return "file".equalsIgnoreCase(sProtocol)
                || "jar".equalsIgnoreCase(sProtocol)
                || "bundle".equalsIgnoreCase(sProtocol)
                || "bundleresource".equalsIgnoreCase(sProtocol)
                || "vfs".equalsIgnoreCase(sProtocol)
                || "wsjar".equalsIgnoreCase(sProtocol);
        }

    private static URL resolveRemoteReportUrl(URI uri, String sName, String sOperation, String sScope)
        {
        String sScheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(sScheme) && !"https".equalsIgnoreCase(sScheme))
            {
            if (!CoherenceMode.isSecurityHardeningEnabled())
                {
                shadow(sScope, sOperation, "reporter-resource-allowlist", "unsupported-uri-scheme", sName);
                return toUrl(uri, sName, sOperation, sScope);
                }
            reject(sScope, sOperation, "reporter-resource-allowlist", "unsupported-uri-scheme", sName);
            }

        if (isApprovedRemoteReportUrl(uri))
            {
            return toUrl(uri, sName, sOperation, sScope);
            }

        if (!CoherenceMode.isSecurityHardeningEnabled())
            {
            shadow(sScope, sOperation, "reporter-resource-allowlist", "remote-source-not-approved", sName);
            return toUrl(uri, sName, sOperation, sScope);
            }

        reject(sScope, sOperation, "reporter-resource-allowlist", "remote-source-not-approved", sName);
        return null;
        }

    private static URL toUrl(URI uri, String sName, String sOperation, String sScope)
        {
        try
            {
            return uri.toURL();
            }
        catch (IllegalArgumentException | IOException e)
            {
            reject(sScope, sOperation, "reporter-resource-allowlist", "invalid-resource-uri", sName);
            return null;
            }
        }

    private static boolean isApprovedRemoteReportUrl(URI uri)
        {
        String sAllowed = System.getProperty(PROP_REMOTE_REPORT_ALLOWED);
        if (sAllowed == null || sAllowed.trim().isEmpty() || uri.getHost() == null)
            {
            return false;
            }

        String sScheme = uri.getScheme();
        String sHost   = uri.getHost();
        int    nPort   = normalizePort(sScheme, uri.getPort());
        for (String sEntry : sAllowed.split(","))
            {
            String sTrimmed = sEntry.trim();
            if (sTrimmed.isEmpty())
                {
                continue;
                }

            try
                {
                URI uriAllowed = new URI(sTrimmed);
                if (sScheme.equalsIgnoreCase(uriAllowed.getScheme())
                        && sHost.equalsIgnoreCase(uriAllowed.getHost())
                        && nPort == normalizePort(uriAllowed.getScheme(), uriAllowed.getPort()))
                    {
                    return true;
                    }
                }
            catch (URISyntaxException e)
                {
                // invalid admin configuration entry is ignored
                }
            }
        return false;
        }

    private static int normalizePort(String sScheme, int nPort)
        {
        if (nPort >= 0)
            {
            return nPort;
            }
        return "https".equalsIgnoreCase(sScheme) ? 443 : 80;
        }

    private static void validateRelativePathName(String sName, String sScope, String sOperation, String sGate)
        {
        if (sName.startsWith("/") || sName.startsWith(".") || sName.contains("//"))
            {
            reject(sScope, sOperation, sGate, "unsafe-relative-path", sName);
            }

        try
            {
            Path path = Paths.get(sName).normalize();
            if (path.isAbsolute())
                {
                reject(sScope, sOperation, sGate, "absolute-path", sName);
                }

            for (Path part : path)
                {
                if ("..".equals(part.toString()))
                    {
                    reject(sScope, sOperation, sGate, "path-traversal", sName);
                    }
                }
            }
        catch (RuntimeException e)
            {
            reject(sScope, sOperation, sGate, "invalid-path", sName);
            }
        }

    private static void validateObjectName(ObjectName name, String sScope)
        {
        if (name == null || name.isDomainPattern())
            {
            if (!CoherenceMode.isSecurityHardeningEnabled())
                {
                shadow(sScope, "query", "reporter-objectname-allowlist", "object-name-pattern-too-broad",
                        name == null ? null : name.getDomain());
                return;
                }
            reject(sScope, "query", "reporter-objectname-allowlist", "object-name-pattern-too-broad",
                    name == null ? null : name.getCanonicalName());
            }

        String sDomain = name.getDomain();
        String sType   = name.getKeyProperty("type");
        if (!ALLOWED_DOMAINS.contains(sDomain) || !ALLOWED_TYPES.contains(sType))
            {
            if (!CoherenceMode.isSecurityHardeningEnabled())
                {
                shadow(sScope, "query", "reporter-objectname-allowlist", "object-name-not-allowed", sDomain);
                return;
                }
            reject(sScope, "query", "reporter-objectname-allowlist", "object-name-not-allowed",
                    name.getCanonicalName());
            }
        }

    private static boolean containsControl(String sValue)
        {
        if (sValue == null)
            {
            return false;
            }

        for (int i = 0, c = sValue.length(); i < c; i++)
            {
            if (Character.isISOControl(sValue.charAt(i)))
                {
                return true;
                }
            }
        return false;
        }

    private static File getApprovedOutputRoot()
            throws IOException
        {
        String sRoot = System.getProperty("coherence.reporter.output.directory", ".");
        return new File(sRoot == null || sRoot.trim().isEmpty() ? "." : sRoot).getCanonicalFile();
        }

    private static Set<File> getApprovedReportFileRoots()
            throws IOException
        {
        Set<File> setRoots = new HashSet<>();
        String    sConfig  = System.getProperty("coherence.management.report.configuration");

        if (sConfig != null && !sConfig.trim().isEmpty())
            {
            try
                {
                URI    uri    = new URI(sConfig);
                String sScheme = uri.getScheme();
                File   file;
                if (sScheme == null)
                    {
                    file = new File(sConfig);
                    }
                else if ("file".equalsIgnoreCase(sScheme))
                    {
                    file = Paths.get(uri).toFile();
                    }
                else
                    {
                    file = null;
                    }

                if (file != null)
                    {
                    File parent = file.getCanonicalFile().getParentFile();
                    if (parent != null)
                        {
                        setRoots.add(parent.getCanonicalFile());
                        }
                    }
                }
            catch (IllegalArgumentException | URISyntaxException e)
                {
                // invalid admin configuration means no local file root is approved
                }
            }

        return setRoots;
        }

    private static boolean isUnderAny(File file, Set<File> setRoots)
            throws IOException
        {
        for (File root : setRoots)
            {
            if (isUnder(file, root))
                {
                return true;
                }
            }
        return false;
        }

    private static boolean isUnder(File file, File root)
            throws IOException
        {
        Path pathFile = file.getCanonicalFile().toPath();
        Path pathRoot = root.getCanonicalFile().toPath();
        return pathFile.startsWith(pathRoot);
        }

    private static void reject(String sScope, String sOperation, String sGate, String sReason, String sValue)
        {
        if (!CoherenceMode.isSecurityHardeningEnabled() && isCompatibilityShadowReason(sReason))
            {
            shadow(sScope, sOperation, sGate, sReason, sValue);
            return;
            }

        Logger.warn("Rejected Reporter request: route=reporter"
                + ", scope=" + sanitize(sScope)
                + ", operation=" + sanitize(sOperation)
                + ", resource-name=" + sanitize(sValue)
                + ", principal=unknown"
                + ", gate=" + sanitize(sGate)
                + ", reason=" + sanitize(sReason));

        throw new ReporterSecurityException("Unsupported Reporter input");
        }

    private static void shadow(String sScope, String sOperation, String sGate, String sReason, String sValue)
        {
        Logger.warn("Allowed compatibility Reporter request that security hardening would reject:"
                + " route=reporter"
                + ", scope=" + sanitize(sScope)
                + ", operation=" + sanitize(sOperation)
                + ", gate=" + sanitize(sGate)
                + ", reason=" + sanitize(sReason)
                + ", security-mode=compatibility"
                + ", result=would_reject"
                + ", resource-name=" + sanitize(sValue)
                + ", principal=unknown");
        }

    private static boolean isCompatibilityShadowReason(String sReason)
        {
        return "file-outside-root".equals(sReason)
                || "object-name-not-allowed".equals(sReason)
                || "object-name-pattern-too-broad".equals(sReason)
                || "remote-source-not-approved".equals(sReason)
                || "unsupported-column-class".equals(sReason)
                || "unsupported-uri-scheme".equals(sReason)
                || "untrusted-method-column".equals(sReason);
        }

    private static String sanitize(String sValue)
        {
        if (sValue == null)
            {
            return "redacted";
            }

        String        sLower = sValue.toLowerCase(Locale.ROOT);
        StringBuilder sb     = new StringBuilder(Math.min(32, sValue.length()));
        for (int i = 0, c = Math.min(32, sValue.length()); i < c; i++)
            {
            char ch = sLower.charAt(i);
            if (ch >= 'a' && ch <= 'z' || ch >= '0' && ch <= '9' || ch == '-' || ch == '_')
                {
                sb.append(ch);
                }
            }
        return sb.length() == 0 ? "redacted" : sb.toString();
        }

    // ----- constants ------------------------------------------------------

    private static final ThreadLocal<ReportSource> CURRENT_SOURCE =
            ThreadLocal.withInitial(() -> ReportSource.UNKNOWN);

    private static final Set<String> ALLOWED_DOMAINS = new HashSet<>(Arrays.asList(
            "Coherence", "javax.cache"));

    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            "Cache", "CacheConfiguration", "CacheStatistics", "Cluster", "CoherenceAdapter",
            "Connection", "ConnectionManager", "Delta", "Executor", "Federation",
            "GrpcConnection", "GrpcNamedCacheProxy", "GrpcProxy", "HttpSessionManager",
            "Journal", "Management", "Node", "PagedTopic", "PagedTopicSubscriber",
            "PagedTopicSubscriberGroup", "PartitionAssignment", "Platform", "Service",
            "StorageManager", "Test", "TestJoin", "TransactionManager", "View",
            "WebLogicHttpSessionManager"));

    /**
     * Comma-separated list of approved remote Reporter XML sources.
     */
    public static final String PROP_REMOTE_REPORT_ALLOWED = "coherence.management.report.remote.allowed";

    private ReporterSecurity()
        {
        }
    }
