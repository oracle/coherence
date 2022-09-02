/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

import com.oracle.coherence.common.base.Classes;

import java.io.File;
import java.io.IOException;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import java.text.MessageFormat;

import java.util.Collections;
import java.util.ListResourceBundle;

import static com.tangosol.util.Base.ensureClassLoader;


/**
* Implement simple basis for package (and other) resources.
*
* @version 1.00, 11/17/97
* @author  Cameron Purdy
*/
public abstract class Resources
        extends ListResourceBundle
    {
    // the following two items should be implemented by derived classes
    /*
    public Object[][] getContents()
        {
        return resources;
        }

    static final Object[][] resources =
        {
        {key, "text"},
        ...
        }
    */

    /**
    * Get the specified resource text.
    *
    * @param sKey     the resource key
    * @param sDefault returns this string if the resource cannot be found
    *
    * @return the requested string, formatted if specified
    */
    public String getString(String sKey, String sDefault)
        {
        return getString(sKey, null, sDefault);
        }

    /**
    * Get the specified resource string.
    *
    * @param sKey     the resource key
    * @param asParam  an array of arguments to fill in replaceable parameters
    * @param sDefault returns this string if the resource cannot be found
    *
    * @return the requested string, formatted if specified
    */
    public String getString(String sKey, String[] asParam, String sDefault)
        {
        String sBase;
        try
            {
            sBase = getString(sKey);
            }
        catch (Exception e)
            {
            sBase = sDefault;
            }

        if (asParam != null && asParam.length > 0)
            {
            try
                {
                return MessageFormat.format(sBase, (Object[]) asParam);
                }
            catch (Exception e)
                {
                int c = asParam.length;
                for (int i = 0; i < c; ++i)
                    {
                    sBase += "\n[" + i + "]=\""
                           + (asParam[i] == null ? "<null>" : asParam[i])
                           + '\"';
                    }
                }
            }

        return sBase;
        }

    /**
    * Find the URL of the resource with the given name using the specified
    * ClassLoader or the following ClassLoaders:
    * <ul>
    *   <li>The Thread Context {@link ClassLoader}</li>
    *   <li>The {@link ClassLoader} used to load {@link Base}, which represents the Coherence Class Loader</li>
    *   <li>The System {@link ClassLoader}</li>
    * </ul>
    * <p>
    * If a resource with the given name is not found, this method attempts
    * to find the resource using a fully-qualified or relative version of the
    * specified name. As a last attempt, the name will be treated as a URL.
    *
    * @param sName   the name of the resource
    * @param loader  the {@link ClassLoader} used to locate the resource; if null,
    *                or resource is not found, the list of {@link ClassLoader}s
    *                described above will be tried
    *
    * @return the URL of the resource or null if the resource could not be found
    *         and the resource name is not a valid URL specification
    */
    public static URL findResource(String sName, ClassLoader loader)
        {
        return findResource(sName, loader, null);
        }

    /**
    * Find the URL of the resource with the given name using the specified
    * ClassLoader or the following ClassLoaders:
    * <ul>
    *   <li>The Thread Context {@link ClassLoader}</li>
    *   <li>The {@link ClassLoader} used to load {@link Base}, which represents the Coherence Class Loader</li>
    *   <li>The System {@link ClassLoader}</li>
    * </ul>
    * <p>
    * If a resource with the given name is not found, this method attempts
    * to find the resource using a fully-qualified or relative version of the
    * specified name. As a last attempt, the name will be treated as a URL.
    * <p>
    * If the resource cannot be found, try to locate the resource in the
    * {@link #DEFAULT_RESOURCE_PACKAGE default package}.
    *
    *
    * @param sName   the name of the resource
    * @param loader  the {@link ClassLoader} used to locate the resource; if null,
    *                or resource is not found, the list of {@link ClassLoader}s
    *                described above will be tried
    *
    * @return the URL of the resource or null if the resource could not be found
    *         and the resource name is not a valid URL specification
    */
    public static URL findResourceOrDefault(String sName, ClassLoader loader)
        {
        return findResource(sName, loader, Resources.getDefaultName(sName));
        }

    /**
    * Find the URL of the resource with the given name using the specified
    * ClassLoader or the following ClassLoaders:
    * <ul>
    *   <li>The Thread Context {@link ClassLoader}</li>
    *   <li>The {@link ClassLoader} used to load {@link Base}, which represents the Coherence Class Loader</li>
    *   <li>The System {@link ClassLoader}</li>
    * </ul>
    * <p>
    * If a resource with the given name is not found, this method attempts
    * to find the resource using a fully-qualified or relative version of the
    * specified name. As a last attempt, the name will be treated as a URL.
    *
    * @param sName     the name of the resource
    * @param loader    the {@link ClassLoader} used to locate the resource; if null,
    *                  or resource is not found, the list of {@link ClassLoader}s
    *                  described above will be tried
    * @param sDefault  the default resource to attempt to locate if the specified
    *                  resource cannot be located
    *
    * @return the URL of the resource or null if the resource could not be found
    *         and the resource name is not a valid URL specification
    */
    public static URL findResource(String sName, ClassLoader loader, String sDefault)
        {
        URL url = findRelativeOrAbsoluteResource(sName, loader);

        if (url == null)
            {
            url = findRelativeOrAbsoluteResource(sName, Thread.currentThread().getContextClassLoader());
            }

        if (url == null)
            {
            url = findRelativeOrAbsoluteResource(sName, Base.class.getClassLoader());
            }

        if (url == null)
            {
            url = findRelativeOrAbsoluteResource(sName, ClassLoader.getSystemClassLoader());
            }

        if (url == null)
            {
            try
                {
                url = new URL(sName);
                }
            catch (Exception ignored) {}
            }

        if (url == null && sDefault != null && !sDefault.isBlank())
            {
            // try the default name
            url = findResource(sDefault, loader, null);
            }

        return url;
        }

    /**
     * Find the URLs of all resources with the given name using the specified
     * {@link ClassLoader}. If the name is an absolute path then return that file
     * otherwise search for all the resources with the name in the classpath.
     *
     * @param sName   the name of the resource
     * @param loader  the ClassLoader used to locate the resource; this method
     *                returns null if a null ClassLoader is provided
     *
     * @return the {@link Iterable} of URLs
     */
    public static Iterable<URL> findResources(String sName, ClassLoader loader) throws IOException {
        URL url = getFileURL(sName);
        if (url != null)
            {
            return Collections.singleton(url);
            }

        return Collections.list(ensureClassLoader(loader).getResources(sName));
        }

    /**
    * Find the URL of the resource with the given name using the specified
    * {@link ClassLoader}.  An attempt is made with both a relative URL
    * and an absolute (fully-qualified) URL if required.  This method will
    * only search the provided ClassLoader; it is recommended to use
    * {@link #findResource(String, ClassLoader)} for a more exhaustive search.
    *
    * @param sName   the name of the resource
    * @param loader  the ClassLoader used to locate the resource; this method
    *                returns null if a null ClassLoader is provided
    *
    * @return the URL of the resource or null if the resource could not be found
    *         or if a null ClassLoader is provided
    *
    * @see #findResource(String, ClassLoader)
    */
    public static URL findRelativeOrAbsoluteResource(String sName, ClassLoader loader)
        {
        if (loader == null)
            {
            return null;
            }

        URL url = loader.getResource(sName);
        if (url == null)
            {
            url = (sName.startsWith("/")
                 ? loader.getResource(sName.substring(1))
                 : loader.getResource('/' + sName));
            }

        return url;
        }

    /**
    * Obtain a URL for an existing file with the specified name.
    *
    * @param sName  the name of the file
    *
    * @return the file URL, or null if file does not exist
    */
    public static URL getFileURL(String sName)
        {
        URL url = null;
        try
            {
            File file = new File(sName);
            if (file.exists())
                {
                try
                    {
                    file = file.getCanonicalFile();
                    }
                catch (IOException ioex)
                    {
                    file = file.getAbsoluteFile();
                    }
                url = file.toURI().toURL();
                }
            }
        catch (Exception e) // MalformedURLException or SecurityException
            {
            }

        return url;
        }

    /**
    * Return a URL to the specified file or resource, using the specified class
    * loader or a {@link Base#getContextClassLoader() context ClassLoader}.
    * <p>
    * This method attempts to locate a file with the specified name or path.  If
    * the file does not exist or cannot be read, this method attempts to locate
    * a resource with the given name, using the specified class loader or
    * context class loader.
    * </p>
    * If a resource with the given name is not found, this method attempts
    * to find the resource using a fully-qualified or relative version of the
    * specified name. As a last attempt, the name will be treated as a URL.
    *
    * @param sName   the name of the file or resource
    * @param loader  the ClassLoader used to locate the resource; if null,
    *                {@link Base#getContextClassLoader()} is used
    *
    * @return the URL of the file or resource or null if the resource could not
    *         be found and the resource name is not a valid URL specification
    */
    public static URL findFileOrResource(String sName, ClassLoader loader)
        {
        return findFileOrResource(sName, loader, null);
        }

    /**
    * Return a URL to the specified file or resource, using the specified class
    * loader or a {@link Base#getContextClassLoader() context ClassLoader}.
    * <p>
    * This method attempts to locate a file with the specified name or path.  If
    * the file does not exist or cannot be read, this method attempts to locate
    * a resource with the given name, using the specified class loader or
    * context class loader.
    * </p>
    * If a resource with the given name is not found, this method attempts
    * to find the resource using a fully-qualified or relative version of the
    * specified name. As a last attempt, the name will be treated as a URL.
    * </p>
    * If the resource cannot be located then the {@link #DEFAULT_RESOURCE_PACKAGE}
    * will be searched for the resource.
    *
    * @param sName     the name of the file or resource
    * @param loader    the ClassLoader used to locate the resource; if null,
    *                  {@link Base#getContextClassLoader()} is used
    *
    * @return the URL of the file or resource or, {@code null} if the resource could
    *         not be found
    */
    public static URL findFileOrResourceOrDefault(String sName, ClassLoader loader)
        {
        return findFileOrResource(sName, loader, Resources.getDefaultName(sName));
        }

    /**
    * Return a URL to the specified file or resource, using the specified class
    * loader or a {@link Base#getContextClassLoader() context ClassLoader}.
    * <p>
    * This method attempts to locate a file with the specified name or path.  If
    * the file does not exist or cannot be read, this method attempts to locate
    * a resource with the given name, using the specified class loader or
    * context class loader.
    * </p>
    * If a resource with the given name is not found, this method attempts
    * to find the resource using a fully-qualified or relative version of the
    * specified name. As a last attempt, the name will be treated as a URL.
    * </p>
    * If the resource cannot be located then the {@code sDefault} resource will
    * be located by recursively calling this method (with a {@code null} default)
    *
    * @param sName     the name of the file or resource
    * @param loader    the ClassLoader used to locate the resource; if null,
    *                  {@link Base#getContextClassLoader()} is used
    * @param sDefault  the name of the default file or resource to locate if the
    *                  initial resource cannot be located.
    *
    * @return the URL of the file or resource or, if the resource could not
    *         be found, and the resource name is not a valid URL specification,
    *         the default resource URL will be returned, or {@code null}, if
    *         neither the primary nor default resources can be located
    */
    public static URL findFileOrResource(String sName, ClassLoader loader, String sDefault)
        {
        URL url = getFileURL(sName);
        return url == null ? findResource(sName, loader, sDefault) :  url;
        }

    /**
     * Locate the specified resource and return its {@link InputStream}.
     *
     * @param s  the resource to find
     *
     * @return the resources {@link InputStream} or {@code null} if the resource does not exist
     *
     * @throws IOException  if an {@link InputStream} cannot be opened
     */

    public static InputStream findInputStream(String s) throws IOException
        {
        String sScheme;

        URI uri = null;
        try
            {
            uri     = URI.create(s);
            sScheme = uri.getScheme();
            }
        catch (Exception e)
            {
            // not a valid URI, so scheme is null
            sScheme = null;
            }

        ClassLoader loader  = Classes.getContextClassLoader();
        URL         url;
        InputStream in;

        if (sScheme == null || sScheme.isEmpty())
            {
            // no scheme so try looking up as a file or classpath resource
            url = Resources.findFileOrResource(s, null);
            }
        else
            {
            try
                {
                url = uri.toURL();
                }
            catch (MalformedURLException e)
                {
                // not a valid URL, assume it is just a file
                url = Resources.findFileOrResource(s, null);
                }
            }

        in = loader.getResourceAsStream(url.getFile());

        if (in == null)
            {
            in = url.openStream();
            }

        return in;
        }

    private static String getDefaultName(String sName)
        {
        if (sName == null || sName.isBlank())
            {
            return null;
            }

        if (sName.charAt(0) == '/')
            {
            return DEFAULT_RESOURCE_PACKAGE + sName;
            }

        return DEFAULT_RESOURCE_PACKAGE + "/" + sName;
        }
    // ----- constants ------------------------------------------------------

    /**
     * The name of the package to look for a default resource if the original name could
     * not be located.
     */
    public static final String DEFAULT_RESOURCE_PACKAGE = "/com/oracle/coherence/defaults";

    }
