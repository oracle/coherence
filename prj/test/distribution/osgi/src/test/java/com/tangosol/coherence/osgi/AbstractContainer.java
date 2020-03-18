/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi;

import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.util.Base;
import com.tangosol.util.Resources;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import java.net.URISyntaxException;
import java.net.URL;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tangosol.util.Base.azzert;
import static com.tangosol.util.Base.ensureRuntimeException;

/**
 * AbstractContainer provides container non-specific implementations of the
 * functionality exposed via {@link Container}. Typically this can be
 * achieved by staying within the confinements of the OSGi API.
 * Implementation specific details are delegated to subclasses via methods;
 * {@link #initialize()}, {@link #getFrameworkFactory()}.
 *
 * @author hr  2012.01.27
 * @since Coherence 12.1.2
 */
public abstract class AbstractContainer
        implements Container, ClassLoaderAware
    {

    // ----- Container methods ----------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(Map<String, String> mapConfigProperties)
        {
        Map<String,String> mapProps = m_mapProperties;
        if (mapProps == null)
            {
            mapProps = m_mapProperties = instantiateDefaultProps();
            }
        mapProps.putAll(mapConfigProperties);
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void start()
        {
        if (!initialize() && m_fwk != null && m_ctxBundle != null)
            {
            // already started the container
            return;
            }

        try
            {
            FrameworkFactory factory = getFrameworkFactory();

            Framework fwk = factory.newFramework(m_mapConfig);
            fwk.init();

            m_fwk       = fwk;
            m_ctxBundle = fwk.getBundleContext();

            fwk.start();
            }
        catch(Exception e)
            {
            err("Error in starting OSGi framework: " + e.getMessage());
            throw ensureRuntimeException(e);
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop()
        {
        Framework fwk = m_fwk;
        if (fwk != null)
            {
            try
                {
                fwk.stop();
                fwk.waitForStop(0);
                }
            catch (Exception e)
                {
                err("Error in shutting down OSGi Framework: " + e.getMessage());
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle deploy(String sBundleLocation) throws BundleException
        {
        Bundle bundle = m_ctxBundle.installBundle(sBundleLocation);

        if (!isFragment(bundle))
            {
            bundle.start();
            }

        return bundle;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle packageAndDeploy(String sBndFileLocation) throws BundleException
        {
        return packageAndDeploy(sBndFileLocation, ensureProperties());
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle packageAndDeploy(String sBndFileLocation, Properties props) throws BundleException
        {
        URL urlProps = Resources.findFileOrResource(sBndFileLocation, getContextClassLoader());

        if (urlProps == null)
            {
            throw new IllegalArgumentException(String.format("The file location provided [%s] does not contain"
                    + " a properties file to be used as input to BND", sBndFileLocation));
            }

        Builder    bundleBuilder = new Builder();
        Properties propBnd       = new Properties();
        try
            {
            propBnd.load(urlProps.openStream());
            }
        catch (IOException e)
            {
            throw new IllegalArgumentException("The properties file provided with BND "
                    + "content is invalid: " + sBndFileLocation, e);
            }

        replaceProperties(propBnd, props);

        String sFileNamePrefix = (String) propBnd.get("Bundle-Name");
               sFileNamePrefix = sFileNamePrefix == null || sFileNamePrefix.isEmpty()
                       ? (String) propBnd.get("Bundle-SymbolicName") : sFileNamePrefix;

        bundleBuilder.setProperties(propBnd);

        String   sClassPath = System.getProperty("java.class.path");

        if (sClassPath.contains(";") && sClassPath.contains("\\"))
            {

            // workaround failure for windows file separator being used in regex in bundleBuilder.setClasspath.
            sClassPath = sClassPath.replace('\\', '/');
            }

        System.out.println("native file system separator: [" + File.pathSeparator + "] java.class.path=[" + sClassPath + "]");

        try
            {
            bundleBuilder.setClasspath(sClassPath.split(File.pathSeparator));
            }
        catch (Throwable t)
            {
            System.out.println("handled unexpected exception: " + t);
            t.printStackTrace();
            throw t;
            }

        Jar  jar      = null;
        File tempFile = null;
        try
            {
            jar      = bundleBuilder.build();
            tempFile = File.createTempFile(sFileNamePrefix + "-", ".jar");

            jar.write(tempFile);
            }
        catch (Exception e)
            {
            throw new RuntimeException("Error encountered in generating bundle", e);
            }
        finally
            {
            if (jar != null)
                {
                jar.close();
                }
            }

        if (tempFile == null)
            {
            throw new IllegalStateException("Error encountered in generating bundle");
            }

        Bundle bundleDeployed = null;
        try
            {
            bundleDeployed = deploy(tempFile.toURI().toString());
            }
        finally
            {
            tempFile.delete();
            }

        return bundleDeployed;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Bundle getBundle(String sBundleName)
        {
        Bundle match = null;
        for (Bundle proposedBundle : m_ctxBundle.getBundles())
            {
            Dictionary<String,String> dictHeaders = proposedBundle.getHeaders();
            String                    sName       = dictHeaders.get("Bundle-Name");

            if (sName.equalsIgnoreCase(sBundleName))
                {
                match = proposedBundle;
                break;
                }
            }
        return match;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public BundleContext getSystemBundleContext()
        {
        return m_ctxBundle;
        }

    // ----- ClassLoaderAware methods ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getContextClassLoader()
        {
        ClassLoader classLoader = m_classLoader;
        if (classLoader == null)
            {
            classLoader = m_classLoader = Base.getContextClassLoader();
            }
        return classLoader;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setContextClassLoader(ClassLoader loader)
        {
        azzert(loader != null, "The class loader for AbstractContainer can not be set to null");

        m_classLoader = loader;
        }

    /**
     * Initialize the properties that will be used by the container. These
     * properties must be set on the <tt>m_propsConfig</tt>. This method is
     * called immediately before {@link #start()}.
     *
     * @return whether this method did perform any intialization
     */
    protected abstract boolean initialize();

    /**
     * Based on the provided bundle determine whether it is a fragment. This
     * is specifically used to determine whether {@link #deploy(String)}
     * should start the deployed bundle.
     *
     * @param bundle  the bundle to interrogate
     *
     * @return whether the bundle is a fragment
     */
    protected abstract boolean isFragment(Bundle bundle);

    /**
     * Return an implementation of the OSGi {@link FrameworkFactory}. The
     * implementation specific factory will return a {@link Framework}
     * implementation.
     *
     * @return implementation specific {@link FrameworkFactory}
     *
     * @throws Exception iff the {@link FrameworkFactory} could not be
     *                   determined
     */
    protected abstract FrameworkFactory getFrameworkFactory() throws Exception;

    // ----- helpers --------------------------------------------------------

    /**
     * List all bundles deployed to the container to the {@link PrintStream}
     * this instance is configured with
     * ({@link #setStandardOutput(PrintStream)}).
     */
    public void listBundles()
        {
        log("+---------------------------------------------------------------------------------------------+");
        log("|     Bundle ID      |         Bundle Name          |    Bundle Version  |    Bundle State    |");
        log("+---------------------------------------------------------------------------------------------+");
        for (Bundle bundle : m_ctxBundle.getBundles())
            {
            Dictionary<String,String> dictHeaders = bundle.getHeaders();

            String sName    = dictHeaders.get("Bundle-Name");
            String sVersion = String.valueOf(bundle.getVersion());
            long   lId      = bundle.getBundleId();
            String sState   = getState(bundle.getState());

            log(String.format("|%1$20d|%2$30s|%3$20s|%4$20s|", lId, sName, sVersion, sState));
            }
        log("+---------------------------------------------------------------------------------------------+");
        log("");
        }

    /**
     * Return a string representation of the bit-wise state integer.
     *
     * @param nState bit-wise state integer
     *
     * @return string representation of the state
     */
    public static String getState(int nState)
        {
        StringBuilder sState = new StringBuilder();

        for (int i = -1; ++i >= 0 && nState > 0; nState >>>= 1)
            {
            sState.append((nState & 0x00000001) == 0x00000001 ? m_saState[i]
                    + (nState > 1 ? " | " : "") : "");
            }

        return sState.toString();
        }

    /**
     * Instantiate a Map to be used for configuration parameters that act as
     * defaults across all containers. Typically this configuration aids
     * the container's execution within a unit test context.
     * <p>
     * These parameters can be overridden via the {@link #configure(Map)}
     * method.
     *
     * @return a map of the default parameters to be set
     */
    protected static Map<String,String> instantiateDefaultProps()
        {
        return new HashMap<String, String>()
            {{
            String sStoragePath = determineStorageDir();
            if (sStoragePath != null)
                {
                put(Constants.FRAMEWORK_STORAGE, sStoragePath);
                }
            put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
            }};
        }

    /**
     * Based on the executing context derive the most appropriate location
     * for the container storage.
     * <p>
     * Iff the executing context sets a system
     * property of {@literal tests.build.dir}, such as from ant, we shall use
     * the parent directory of this location.
     * <p>
     * The absence of the above property will result in the use of the parent
     * directory to the root directory that contains this compiled class.
     * <p>
     * The directory determined from the above two scenarios has a
     * {@literal container-cache} string appended.
     *
     * @return the location to store bundle artifacts
     */
    protected static String determineStorageDir()
        {
        String sStoragePath = getTestBuildPath();
        return sStoragePath == null ? null : sStoragePath
            + (sStoragePath.charAt(sStoragePath.length() - 1) == File.separatorChar ? "" : File.separator)
            + "container-cache";
        }

    /**
     * Get the test build directory based on Coherence build standards, or fall
     * back to the location of this compiled class.
     *
     * @return the location of the directory for test build artifacts
     */
    protected static String getTestBuildPath()
        {
        String sStoragePath = System.getProperty("tests.build.dir");
        if (sStoragePath == null || sStoragePath.trim().length() <= 0)
            {
            ProtectionDomain domain     = AbstractContainer.class.getProtectionDomain();
            CodeSource       codeSource = domain     == null ? null : domain.getCodeSource();
            URL              urlPath    = codeSource == null ? null : codeSource.getLocation();

            if (urlPath != null)
                {
                try
                    {
                    sStoragePath = new File(urlPath.toURI()).getParent();
                    }
                catch (URISyntaxException e)
                    {
                    }
                }
            }
        return sStoragePath;
        }

    /**
     * Based on the provided {@link Properties} ({@literal propLookup})
     * discover any tokens that should be replaced in each value within
     * {@link Properties} ({@literal propDest}).
     *
     * @param propDest    the properties to discover tokens eligible for
     *                    replacement
     * @param propLookup  the properties to inject into the values
     */
    protected void replaceProperties(Properties propDest, Properties propLookup)
        {
        for (Map.Entry entry : propDest.entrySet())
            {
            String  sValue = (String) entry.getValue();
            Matcher matcher = m_saExpressionPattern.matcher(sValue);

            StringBuffer sbuf = new StringBuffer();

            while (matcher.find())
                {
                String sMatch = matcher.group();
                String sKey   = sMatch.substring(2, sMatch.length() - 1).trim();

                matcher.appendReplacement(sbuf, propLookup.containsKey(sKey)
                    ? propLookup.getProperty(sKey) : sMatch);
                }
            matcher.appendTail(sbuf);
            entry.setValue(sbuf.toString());
            }
        }

    /**
     * Ensure we have {@link Properties} available with some common values.
     * If these common values are available via System properties these are
     * used, which allows JVM argument override and execution in ant
     * environments, however if not we shall locate the build.properties
     * based on Coherence build structure and combine with System properties.
     *
     * @return the derived {@link Properties}
     */
    protected Properties ensureProperties()
        {
        Properties props = m_props;
        if (props == null)
            {
            props = m_props = System.getProperties();
            if (!props.containsKey("project.version") || !props.containsKey("project.version.next"))
                {
                throw new IllegalStateException("OSGi Tests expect to have project.version and "
                    + "project.version.next available via system properties");
                }
            }
        return props;
        }

    /**
     * Derives the location of the {@literal build.properties} based on
     * Coherence build standards.
     *
     * @return the location of the {@literal build.properties}
     */
    protected File getBuildPropertiesFile()
        {
        File file = new File(getTestBuildPath());
        if (!file.exists())
            {
            throw new IllegalStateException("Could not establish location of build.properties "
                + "as " + file + " does not exist");
            }

        file = new File(file.getParentFile().getParent() + File.separator + m_saBuildPropertiesLocation);
        if (!file.exists())
            {
            throw new IllegalStateException("Could not establish location of build.properties "
                + "as " + file + " does not exist");
            }
        return file;
        }

    /**
     * Log the message to the configured {@link PrintStream}.
     *
     * @param sMsg  the message to send to the PrintStream
     */
    protected void log(String sMsg)
        {
        m_cout.println(sMsg);
        }

    /**
     * Log the message to the error stream.
     *
     * @param sMsg  the message to log the error stream
     */
    protected void err(String sMsg)
        {
        m_cerr.println(sMsg);
        }

    /**
     * Set the output stream to the specified {@link PrintStream}.
     *
     * @param cout  the {@link PrintStream} to be used for std out
     */
    protected void setStandardOutput(PrintStream cout)
        {
        m_cout = cout == null ? m_cout : cout;
        }

    /**
     * Set the error stream to the specified {@link PrintStream}.
     *
     * @param cerr  the {@link PrintStream} to be used for std out
     */
    protected void setErrorOut(PrintStream cerr)
        {
        m_cerr = cerr == null ? m_cerr : cerr;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The string representation for each state.
     */
    private static final String[] m_saState = new String[]{"UNINSTALLED", "INSTALLED", "RESOLVED", "STARTING", "STOPPING", "ACTIVE"};

    /**
     * Location of the build.properties relative to the Coherence Dev Home.
     */
    private static final String   m_saBuildPropertiesLocation = "prj" + File.separator + "build.properties";

    /**
     * Pattern used to identify expression that can be replaced in a string.
     */
    private static final Pattern  m_saExpressionPattern       = Pattern.compile("\\$\\{\\w+(\\.\\w+)*\\}");

    // ----- data members ---------------------------------------------------

    /**
     * The map used to store all configuration properties to be propagated to
     * the container.
     */
    protected Map<String,String> m_mapProperties = instantiateDefaultProps();

    /**
     * The System Bundle's {@link BundleContext}.
     */
    protected BundleContext      m_ctxBundle;

    /**
     * The underlying OSGi framework implementation.
     */
    protected Framework          m_fwk;

    /**
     * The {@link PrintStream} to send output messages to.
     */
    protected PrintStream        m_cout = System.out;

    /**
     * The {@link PrintStream} to send error messages to.
     */
    protected PrintStream        m_cerr = System.err;

    /**
     * Amalgamation of properties to send to the container.
     */
    protected Map<String,String> m_mapConfig;

    /**
     * The configured class loader.
     */
    private   ClassLoader        m_classLoader;

    /**
     * Properties used to inject into bnd file(s).
     */
    private   Properties         m_props;
    }
