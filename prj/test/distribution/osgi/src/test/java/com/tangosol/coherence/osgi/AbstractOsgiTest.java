/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi;

import com.tangosol.io.ClassLoaderAware;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import java.net.URISyntaxException;
import java.net.URL;

import java.security.CodeSource;
import java.security.ProtectionDomain;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static org.junit.Assume.assumeTrue;

/**
 * AbstractOsgiTest provides some common implementations to allow tests
 * to focus on ensuring functionality opposed to the mundane and repetitive
 * tasks in managing the {@link Container}.
 *
 * @author hr  2012.01.26
 * @since Coherence 12.1.2
 */
public abstract class AbstractOsgiTest
    {
    /**
     * Initialize and start the container.
     */
    @BeforeClass
    public static void init()
        {
        Container container = m_container = new FelixContainer();

        container.configure(new HashMap<String, String>()
            {{
            put(Constants.FRAMEWORK_BOOTDELEGATION, "com.sun.*,sun.*");
            }});

        if (container instanceof ClassLoaderAware)
            {
            ((ClassLoaderAware) container).setContextClassLoader(AbstractOsgiTest.class.getClassLoader());
            }

        container.start();
        }

    /**
     * Ensure we clean up the container.
     */
    @AfterClass
    public static void destroy()
        {
        if (m_container != null)
            {
            m_container.stop();
            }
        }

    /**
     * Log the passed message to standard out.
     *
     * @param sMsg  the message to be written
     */
    public static void log(String sMsg)
        {
        System.out.println(sMsg);
        }

    /**
     * Log the passed message to standard error.
     *
     * @param sMsg  the message to be written
     */
    public static void err(String sMsg)
        {
        System.err.println(sMsg);
        }

    /**
     * List all bundles deployed to the container.
     */
    public static void listBundles()
        {
        Container container = m_container;
        boolean   fIdeCtx   = System.getProperty("project.name") == null;
        if (!fIdeCtx || !(container instanceof AbstractContainer))
            {
            return;
            }

        AbstractContainer containerImpl = (AbstractContainer) container;
        containerImpl.listBundles();
        }

    /**
     * Using the provided class attempt to discover the root resource
     * containing the class. Iff this root resource is a directory continue
     * processing other possibilities until an acceptable match is discovered.
     * If a match can not be found attempt to deploy the original root
     * resource discovered.
     *
     * @param clzBundleEntry  a class that exists within the bundle used as a
     *                        locating mechanism
     *
     * @throws BundleException iff a problem occurs during deployment
     */
    protected void deployDependency(Class<?> clzBundleEntry) throws BundleException
        {
        ProtectionDomain domain    = clzBundleEntry.getProtectionDomain();
        CodeSource       cs        = domain == null ? null : domain.getCodeSource();
        URL              urlBundle = cs     == null ? null : cs.getLocation();
        Container        container = m_container;
        if (container == null)
            {
            throw new IllegalStateException("The container must be initialized prior to deploying a dependency");
            }
        if (urlBundle == null)
            {
            throw new IllegalStateException("Unable to derive the URL location of the class resource: " + clzBundleEntry);
            }

        File f = null;
        try
            {
            f = new File(urlBundle.toURI());
            }
        catch (URISyntaxException e)
            {
            }

        // best endeavours in finding a jar file that contains the class provided
        // first look at other locations on the class path that have the resource
        // followed by looking at jar files in parent directories
        String sResource = clzBundleEntry.getName().replace(".", "/") + ".class";
        if (f == null || f.isDirectory())
            {
            // perhaps the class is present in multiple locations in the same
            // class loader therefore see if other locations are jar files
            try
                {
                for (Enumeration<URL> enumURLs = clzBundleEntry.getClassLoader().getResources(sResource);
                     enumURLs.hasMoreElements(); )
                    {
                    String sUrl = enumURLs.nextElement().toExternalForm();
                    URL    url  = new URL(sUrl.substring(0, sUrl.indexOf(sResource)));
                    if (urlBundle.sameFile(url))
                        {
                        continue;
                        }

                    String sFile = url.getFile();
                    if (sFile.endsWith("!/"))
                        {
                        container.deploy(sFile.substring(0, sFile.length() - 2));
                        return;
                        }
                    }
                }
            catch (IOException e)
                {
                // fall back to the original url if problem has been encountered
                }

            }

        if (f.isDirectory())
            {
            // finally see if we can find a jar file with the resource in a parent
            // directory, common in the maven build layout
            File dirCurr = f.getParentFile();
            for (int i = 0; i < 2 && dirCurr != null; dirCurr = f.getParentFile(), ++i)
                {
                FilenameFilter filterJar = new FilenameFilter()
                    {
                    public boolean accept(File dir, String sName)
                        {
                        return sName.endsWith(".jar");
                        }
                    };
                for (File filePoss : dirCurr.listFiles(filterJar))
                    {
                    JarFile jarFile = null;
                    try
                        {
                        jarFile = new JarFile(filePoss);
                        ZipEntry entry = jarFile.getEntry(sResource);
                        if (entry != null)
                            {
                            container.deploy(filePoss.toURI().toURL().toExternalForm());
                            return;
                            }
                        }
                    catch (IOException e) {}
                    finally
                        {
                        if (jarFile != null)
                            {
                            try
                                {
                                jarFile.close();
                                }
                            catch (IOException e) {}
                            }
                        }
                    }
                }
            }

        container.deploy(urlBundle.toExternalForm());
        }

    /**
     * Using the provided dependency name discover the resource from the
     * class path and deploy it to the container. If the resource can not be
     * found in the class path the method will assume the resource exists in
     * the same location as the <tt>coherence.jar</tt>.
     *
     * @param sName  the name of the resource
     *
     * @throws BundleException iff a problem occurs during deployment
     */
    protected void deployDependency(String sName) throws BundleException
        {
        Container container = m_container;
        if (container == null)
            {
            throw new IllegalStateException("The container must be initialized prior to deploying a dependency");
            }

        String sClassPath = System.getProperty("java.class.path");
        int    iJar       = sClassPath.indexOf(sName);

        if (iJar == -1)
            {
            // if we can not find the jar in the class-path we fall back on
            // the hope it is within the same location as coherence.jar
            iJar = sClassPath.indexOf("coherence.jar");
            }

        String sJarLocation = sClassPath.substring(sClassPath.lastIndexOf(File.pathSeparator, iJar) + 1, iJar) + sName;
        File   dependency   = new File(sJarLocation);

        assumeTrue(dependency.exists());
        container.deploy(dependency.toURI().toString());
        }

    /**
     * Deploy a library to the container using the provided bnd file. This is
     * typically used when the classes are available to the current class
     * loader that are referred to in the provided bnd file. The most common
     * use case is wrapping a third-party library which does not provide
     * OSGi metadata.
     *
     * @param sBndFileName  the name of the BND file
     *
     * @throws BundleException iff a problem occurs during deployment
     */
    protected void libDeploy(String sBndFileName) throws BundleException
        {
        Container container    = m_container;
        if (container == null)
            {
            throw new IllegalStateException("The container must be initialized prior to deploying a dependency");
            }

        container.packageAndDeploy(BND_FILE_LOCATION + sBndFileName);
        }

    /**
     * Return a {@link Matcher} implementation that given a {@link Bundle}
     * can determine whether the state is as expected.
     *
     * @param nState  the expected state
     *
     * @return the {@link Matcher} to verify state
     */
    public static Matcher<Bundle> hasState(int nState)
        {
        return new BundleStateMatcher(nState);
        }

    /**
     * A {@link Matcher} implementation that give a {@link Bundle} can
     * determine whether the state is as expected.
     */
    public static class BundleStateMatcher
            extends BaseMatcher<Bundle>
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct a BundleStateMatcher with the provided state.
         *
         * @param nState  the state that the bundle should be in.
         */
        protected BundleStateMatcher(int nState)
            {
            m_nState = nState;
            }

        // ----- Matcher methods --------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean matches(Object item)
            {
            int nState       = m_nState;
            int nActualState = 0;

            if (item instanceof Bundle)
                {
                nActualState = ((Bundle) item).getState();
                }
            else if (item instanceof Integer)
                {
                nActualState = (Integer) item;
                }

            return (nActualState & nState) == nState;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void describeTo(Description description)
            {
            description.appendValue(AbstractContainer.getState(m_nState));
            }

        // ----- data members -----------------------------------------------

        /**
         * The state to verify the bundle is in.
         */
        private int m_nState;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The location of BND files used when typically providing OSGi metadata
     * to libraries.
     */
    protected static final String BND_FILE_LOCATION = "META-INF/services/bnd/";

    // ----- data members ---------------------------------------------------

    /**
     * The OSGi {@link Container} implementation.
     */
    protected static Container m_container;
    }
