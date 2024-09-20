/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.SimpleEnumerator;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import java.util.regex.Pattern;

/**
 * ClassPathResourceDiscoverer is {@link ResourceDiscoverer} implementation
 * that uses its {@link ClassLoader} ({@link #getContextClassLoader()}) to
 * determine appropriate artifacts based on the discriminator passed to
 * {@link #discoverResource(String, String)}. The resource discovery
 * mechanism employed by this class resides in
 * {@link #findUrls(String, String)} and by default uses
 * {@link ClassLoader#getResources(String)}. This has some limitations
 * including the inability refer to a resource prefix in the case of a jar.
 * Thus the discriminator must be a resource that can be referenced in all
 * artifacts that may have resources complying to the regular expression.
 * <p>
 * Each of these derived artifacts
 * ({@link URL}s) are evaluated against a resource discoverer chain
 * {@link #getDiscovererChain()}.
 * <p>
 * Each resource discoverer is given the opportunity discover all
 * leaf-resources in the current artifact. ResourceDiscoverer's that
 * implement {@link Filter} are provided the URL of the artifact to either
 * disregard the artifact or confirm acceptance to interrogate said artifact.
 * The chain of ResourceDiscoverer's are provided both the URL
 * and a string expression. The expression is the same passed to
 * {@link #discoverResource(String, String)} and could represent a package.
 *
 * @author hr  2011.10.18
 * @author Gunnar Hillert 2024.04.20
 *
 * @since Coherence 12.1.2
 *
 * @see ResourceDiscoverer
 * @see InformedResourceDiscoverer
 * @see FileBasedResourceDiscoverer
 * @see JarFileBasedResourceDiscoverer
 */
public class ClassPathResourceDiscoverer
        extends AbstractFilteredResourceDiscoverer<String>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ClassPathResourceDiscoverer with default configuration.
     */
    public ClassPathResourceDiscoverer()
        {
        this(null, null);
        }

    /**
     * Construct a ClassPathResourceDiscoverer with the provided list of
     * {@link ResourceDiscoverer}s and a set of acceptable file types.
     *
     * @param setFileTypes     the set of acceptable file types
     * @param listDiscoverers  the {@link ResourceDiscoverer} chain to call
     *                         for each discovered resource
     */
    public ClassPathResourceDiscoverer(Set<String> setFileTypes, List<ResourceDiscoverer<URL>> listDiscoverers)
        {
        m_listDiscoverers = listDiscoverers == null ? m_listDiscoverers : listDiscoverers;
        m_setFileTypes    = setFileTypes    == null ? m_setFileTypes    : setFileTypes;

        initialize();
        }

    // ----- AbstractFilteredResourceDiscoverer methods ---------------------

    /**
     * Returns an {@link Enumeration} of classpath relative resources that
     * comply to the provided {@code sExpression}. This implementation uses
     * a chain of {@link ResourceDiscoverer}s to determine leaf-resources.
     *
     * <b>Note:</b>A null or empty discriminator will result in using the
     * {@literal sExpression} as the discriminator. This default logic can
     * be overridden by providing a custom implementation of
     * {@link #findUrls(String, String)}.
     *
     * @param sDiscriminator  a string representing the resource that allows
     *                        the discovery of all artifacts (jar files or
     *                        directories in the class path) that should be
     *                        interrogated for resources complying to the
     *                        {@literal sExpression}
     * @param sExpression     the regular expression applied to all child
     *                        resources of the discovered resource via
     *                        {@literal sDiscriminator}
     *
     * @return an Enumeration of resources that comply to provided
     *         {@literal sExpression}
     */
    @Override
    public Collection<URI> discoverResource(String sDiscriminator, String sExpression)
        {
                 sExpression  = sExpression == null ? "" : sExpression;
        Set<URI> setResources = new HashSet<>();
        String   sAppendExpr  = buildFileTypeExpression();

        try
            {
            for (Enumeration<URL> enumResources = findUrls(sDiscriminator, sExpression); enumResources.hasMoreElements(); )
                {
                URL url = enumResources.nextElement();
                for (ResourceDiscoverer<URL> discoverer : getDiscovererChain())
                    {
                    if (discoverer instanceof Filter && !((Filter) discoverer).evaluate(url))
                        {
                        continue;
                        }

                    Enumeration<URL> enumStringPaths = discoverer.discover(".*" + sExpression + ".*" + sAppendExpr, url);
                    while (enumStringPaths != null && enumStringPaths.hasMoreElements())
                        {
                        URI uri = toURI(enumStringPaths.nextElement());
                        if (uri != null)
                            {
                            setResources.add(uri);
                            }
                        }
                    }
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        return setResources;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Set<String> getPermittedProtocols()
        {
        return new HashSet<String>();
        }

    /**
     * Based on the set of file types this implementation was instantiated
     * with build a regular expression that results in an OR of the file
     * types.
     *
     * @return regular expression permitting certain file types
     */
    protected String buildFileTypeExpression()
        {
        Set<String>   setFileTypes = m_setFileTypes;
        StringBuilder sbldrExpr    = new StringBuilder("(");

        for (Iterator<String> iterFileTypes = setFileTypes.iterator(); iterFileTypes.hasNext(); )
            {
            String sFileType = iterFileTypes.next();
            sbldrExpr.append("\\.")
                     .append(sFileType)
                     .append(iterFileTypes.hasNext() ? "|" : ")$");
            }
        return sbldrExpr.length() == 1 ? "" : sbldrExpr.toString();
        }

    /**
     * Based on the provided {@literal sDiscriminator} and
     * {@literal sExpression} derive an {@link Enumeration} of {@link URL}s
     * that are likely candidates of satisfying {@literal sExpression}. These
     * {@link URL}s will be passed down a chain of {@link ResourceDiscoverer}s
     * for interrogation.
     *
     * @param sDiscriminator  a string representing the resource that allows
     *                        the discovery of all artifacts (jar files or
     *                        directories in the class path) that should be
     *                        interrogated for resources complying to the
     *                        {@literal sExpression}
     * @param sExpression     the regular expression applied to all child
     *                        resources of the discovered resource via
     *                        {@literal sDiscriminator}
     *
     * @return an Enumeration or URLs that may comply to the {@literal sExpression}
     *
     * @throws IOException
     */
    protected Enumeration<URL> findUrls(String sDiscriminator, String sExpression) throws IOException
        {
        return getContextClassLoader().getResources(sDiscriminator == null
                || sDiscriminator.isEmpty() ? sExpression : sDiscriminator);
        }

    // ----- ClassLoaderHolder methods --------------------------------------

    /**
     * Ensure all {@link ClassLoaderAware} resources this class is aware of
     * are updated with the new {@link ClassLoader}.
     *
     * @param loader  the context ClassLoader for this object
     */
    @Override
    public void setContextClassLoader(ClassLoader loader)
        {
        super.setContextClassLoader(loader);

        List<ResourceDiscoverer<URL>> listDiscoverers = getDiscovererChain();
        if (listDiscoverers == null)
            {
            return;
            }

        for (ResourceDiscoverer discoverer : listDiscoverers)
            {
            discoverer.setContextClassLoader(getContextClassLoader());
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns a list of {@link ResourceDiscoverer}s this class is configured
     * to use.
     *
     * @return a list of ResourceDiscoverer's this class is configured
     *         to use
     */
    public List<ResourceDiscoverer<URL>> getDiscovererChain()
        {
        return m_listDiscoverers;
        }

    /**
     * Sets a list of {@link ResourceDiscoverer}s this class is configured
     * to use.
     *
     * @param listDiscoverers  the list of ResourceDiscoverer's this class
     *                         should use to determine leaf-resources
     */
    public void setDiscoverers(List<ResourceDiscoverer<URL>> listDiscoverers)
        {
        Base.azzert(listDiscoverers != null, "ClassPathResourceDiscoverer requires a non-null set of child discoverers");
        m_listDiscoverers = listDiscoverers;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Based on the state of the class initialize it with sensible defaults.
     */
    protected void initialize()
        {
        List<ResourceDiscoverer<URL>> listDiscoverers = m_listDiscoverers;
        if (listDiscoverers.size() == 0)
            {
            listDiscoverers.add(new FileBasedResourceDiscoverer());
            listDiscoverers.add(new JarFileBasedResourceDiscoverer());
            }
        }

    // ----- inner class: InformedResourceDiscoverer ------------------------

    /**
     * An InformedResourceDiscoverer is provided a number of {@link URL}s to
     * be interrogated by a chain of {@link ResourceDiscoverer}
     * implementations.
     * <p>
     * This ResourceDiscoverer implementation is useful when the
     * resources are known upfront precluding the need of a discovery
     * mechanism such as a {@link ClassLoader#getResources(String)} request.
     */
    public static class InformedResourceDiscoverer
            extends ClassPathResourceDiscoverer
        {

        // ----- constructors -----------------------------------------------

        /**
         * Construct as InformedResourceDiscoverer with the provided array of
         * {@link URL}s.
         *
         * @param aUrl  array of URLs passed down the chain of
         *              {@link ResourceDiscoverer}s
         */
        public InformedResourceDiscoverer(URL[] aUrl)
            {
            this(null, null, aUrl);
            }

        /**
         * Construct an InformedResourceDiscoverer with the passed chain of
         * {@link ResourceDiscoverer}s and an array of {@link URL}s.
         *
         * @param setFileTypes     the set of acceptable file types
         * @param listDiscoverers  chain of ResourceDiscoverer's
         * @param aUrl             array of URLs passed down the chain
         *                         of ResourceDiscoverer's
         */
        public InformedResourceDiscoverer(Set<String> setFileTypes, List<ResourceDiscoverer<URL>> listDiscoverers, URL[] aUrl)
            {
            super(setFileTypes, listDiscoverers);
            m_aUrl = aUrl;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Enumeration<URL> findUrls(String sDiscriminator, String sExpression) throws IOException
            {
            return new SimpleEnumerator(m_aUrl);
            }

        // ----- data members -----------------------------------------------

        /**
         * Array of {@link URL}s passed down the chain of
         * {@link ResourceDiscoverer}s.
         */
        protected URL[] m_aUrl;
        }


    // ----- inner class: FileBasedResourceDiscoverer -----------------------

    /**
     * A {@link ResourceDiscoverer} implementation that works against "file"
     * based resources, such that the {@link URL} provided has a "file"
     * protocol. This implementation will traverse the directory tree
     * discovering all complying leaf-resources.
     *
     * @author hr  2011.10.18
     */
    protected static class FileBasedResourceDiscoverer
            extends AbstractFilteredResourceDiscoverer<URL>
        {

        // ----- AbstractFilteredResourceDiscoverer methods -----------------

        /**
         * {@inheritDoc}
         */
        protected Set<String> getPermittedProtocols()
            {
            return s_setProtocols;
            }

        // ----- ResourceDiscoverer interface -------------------------------

        /**
         * Starting from the {@code urlRoot} traverse the directory tree
         * evaluating all leaf-resources against {@code sFileExpression}
         * returning complying resources. This method will return the full
         * path of the resource.
         *
         * @param sFileExpression  the expression that each leaf-resource
         *                         must match
         * @param urlRoot          the root element to start traversing from
         *
         * @return a number of resources that complied with the
         *         {@code sFileExpression} and belong to {@code urlRoot}
         */
        @Override
        public Collection<URI> discoverResource(String sFileExpression, URL urlRoot)
            {
            Set<URI>    setResources = new HashSet<>();
            URI         uriRoot      = toURI(urlRoot);
            if (uriRoot == null)
                {
                Logger.warn("FileBasedResourceDiscoverer requires a non-null and non-empty root directory");
                return setResources;
                }

            File root = new File(uriRoot);

            if (root.isFile() && root.canRead())
                {
                setResources.add(uriRoot);
                return setResources;
                }

            if (ensureDirectory(root) == null)
                {
                return setResources;
                }

            Pattern regEx = Pattern.compile(sFileExpression);
            findFiles(root, regEx, setResources);

            return setResources;
            }

        // ----- helpers ----------------------------------------------------

        /**
         * From the current directory ({@code root}), find all files that
         * match the {@code regEx} parameter adding them to the
         * {@code setResources}. This method will be called recursively if
         * any of the files discovered are directories.
         *
         * @param root          the directory to start from
         * @param regEx         the pattern to match file names against
         * @param setResources  the set of resources to add complying files
         *                      to
         */
        protected void findFiles(File root, final Pattern regEx, Set<URI> setResources)
            {
            File[] complyingFiles = root.listFiles(new FileFilter()
                {
                public boolean accept(File pathname)
                    {
                    // ensure path separators use the java & unix format
                    String sPath = pathname.getAbsolutePath();
                    return pathname.isDirectory() || regEx.matcher(sPath.contains("\\")
                            ? sPath.replaceAll("\\\\", "/") : sPath).matches();
                    }
                });

            for (int i = 0; i< complyingFiles.length; ++i)
                {
                File complyingFile = complyingFiles[i];
                if (complyingFile.isFile())
                    {
                    setResources.add(complyingFile.toURI());
                    }
                else if (complyingFile.isDirectory())
                    {
                    if (ensureDirectory(complyingFile) == null)
                        {
                        continue;
                        }
                    findFiles(complyingFile, regEx, setResources);
                    }
                }
            }

        /**
         * Based on {@code root} ensure it is a directory and can be
         * read. Display a warning message if not the case and return null.
         *
         * @param root  the file resource
         *
         * @return the file reference
         */
        protected File ensureDirectory(File root)
            {
            if (!root.isDirectory())
                {
                Logger.warn(String.format(
                        "FileBasedResourceDiscoverer root directory should be a valid directory [rootDir: %s]",
                        root.getAbsolutePath()));
                return null;
                }

            if (!root.canRead())
                {
                Logger.warn(String.format(
                        "FileBasedResourceDiscoverer root directory should be a readable directory [rootDir: %s]",
                        root.getAbsolutePath()));
                return null;
                }

            return root;
            }

        // ----- constants --------------------------------------------------

        /**
         * All protocols this {@link ResourceDiscoverer} can operate against.
         */
        protected static final Set<String> s_setProtocols = new HashSet<String>(Arrays.asList("file"));
        }

    // ----- inner class: JarFileBasedResourceDiscoverer --------------------
    
    /**
     * A {@link ResourceDiscoverer} implementation that works against "jar",
     * "zip", "wsjar", "code-source", based resources, such that the
     * {@link URL} provided has one of the mentioned protocols. This
     * implementation will traverse the directory tree discovering all
     * complying leaf-resources.
     *
     * @author hr  2011.10.18
     */
    protected static class JarFileBasedResourceDiscoverer
            extends AbstractFilteredResourceDiscoverer<URL>
        {

        // ----- AbstractFilteredResourceDiscoverer methods -----------------

        /**
         * {@inheritDoc}
         */
        @Override
        protected Set<String> getPermittedProtocols()
            {
            return s_setProtocols;
            }

        /**
         * Starting from the {@code urlRoot} traverse the directory tree
         * evaluating all leaf-resources against {@code sFileExpression}
         * returning complying resources. This method will return the full
         * path of the resource.
         *
         * @param urlRoot          the root element to start traversing from
         * @param sFileExpression  the expression that each leaf-resource
         *                         must match
         *
         * @return a number of resources that complied with the
         *         {@code sFileExpression} and belong to {@code urlRoot}
         */
        @Override
        public Collection<URI> discoverResource(String sFileExpression, URL urlRoot)
            {
            Set<URI>   setResources = new HashSet<>();
            Pattern    regEx        = Pattern.compile(sFileExpression);
            try
                {
                URLConnection urlCon = urlRoot.openConnection();
                JarFile       jarFile;

                if (urlCon instanceof JarURLConnection)
                    {
                    JarURLConnection jarUrl = (JarURLConnection) urlCon;
                    jarUrl.setUseCaches(false);
                    jarFile      = jarUrl.getJarFile();
                    }
                else
                    {
                    // attempt to create a File reference removing any notation
                    // specific to the jar protocol
                    String sFullLocation = urlRoot.getFile();
                    int    iEmbeddedRef  = sFullLocation.indexOf("!/");
                    String sLocation     = iEmbeddedRef == -1 ? sFullLocation : sFullLocation.substring(0, iEmbeddedRef);

                    // try to create a URI out using the location
                    // if not possible assume the format of sLocation to be
                    // "file:/foo" or "someother:/"
                    // in case of "/dir1/dir:2/" pass this as the fs location
                    try
                        {
                        jarFile = new JarFile(new URI(sLocation.replace(" ", "%20")).getSchemeSpecificPart());
                        }
                    catch (URISyntaxException e)
                        {
                        int iColon = sLocation.indexOf(":");
                        jarFile = new JarFile(iColon == -1  || sLocation.substring(0, iColon).contains(File.separator)
                                ? sLocation : sLocation.substring(iColon + 1));
                        }
                    }

                // scan the entries adding all matched values to the results
                for (Enumeration<JarEntry> enumEntries = jarFile.entries(); enumEntries.hasMoreElements(); )
                    {
                    JarEntry entry     = enumEntries.nextElement();
                    String   sResource = entry.getName();

                    if (regEx.matcher(sResource).matches())
                        {
                        URI uri = toURI(new URL(urlRoot, "/" + sResource));
                        if (uri != null)
                            {
                            setResources.add(uri);
                            }
                        }
                    }
                }
            catch (IOException e)
                {
                Logger.warn(String.format(
                        "JarFileBasedResourceDiscoverer could not open a connection to the resource [url: %s]"
                        + ", hence this resource will not be used",
                        urlRoot.toExternalForm()));
                }
            return setResources;
            }

        // ----- constants --------------------------------------------------

        /**
         * All protocols this {@link ResourceDiscoverer} can operate against.
         */
        protected static final Set<String> s_setProtocols = new HashSet<String>(Arrays.asList("jar", "zip", "wsjar", "code-source"));
        }

    // ----- data member ----------------------------------------------------

    /**
     * The set of accepted file types to return as a result of
     * {@link #discoverResource(String, String)}.
     */
    protected Set<String>                   m_setFileTypes = new HashSet<String>(Arrays.asList("class"));

    /**
     * A {@link List} of {@link ResourceDiscoverer}s the
     * ClassPathResourceDiscoverer will use to discover leaf-resources from
     * different types of resources.
     */
    protected List<ResourceDiscoverer<URL>> m_listDiscoverers = new ArrayList<ResourceDiscoverer<URL>>(2);
    }
