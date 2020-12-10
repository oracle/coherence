/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ListResourceBundle;


/**
 * Implement simple basis for package (and other) resources.
 *
 * @author Cameron Purdy
 * @version 1.00, 11/17/97
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
     * ClassLoader or the following Classes:
     * <ul>
     * <li>The Thread Context {@link ClassLoader}</li>
     * <li>The {@link ClassLoader} used to load {@link Classes}, which represents the Coherence Class Loader</li>
     * <li>The System {@link ClassLoader}</li>
     * </ul>
     * <p/>
     * If a resource with the given name is not found, this method attempts
     * to find the resource using a fully-qualified or relative version of the
     * specified name. As a last attempt, the name will be treated as a URL.
     *
     * @param sName  the name of the resource
     * @param loader the {@link ClassLoader} used to locate the resource; if null,
     *               or resource is not found, the list of {@link ClassLoader}s
     *               described above will be tried
     * @return the URL of the resource or null if the resource could not be found
     * and the resource name is not a valid URL specification
     */
    public static URL findResource(String sName, ClassLoader loader)
        {
        URL url = findRelativeOrAbsoluteResource(sName, loader);

        if (url == null)
            {
            url = findRelativeOrAbsoluteResource(sName, Thread.currentThread().getContextClassLoader());
            }

        if (url == null)
            {
            url = findRelativeOrAbsoluteResource(sName, Classes.class.getClassLoader());
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
            catch (Exception e)
                {
                }
            }

        return url;
        }

    /**
     * Find the URL of the resource with the given name using the specified
     * {@link ClassLoader}.  An attempt is made with both a relative URL
     * and an absolute (fully-qualified) URL if required.  This method will
     * only search the provided ClassLoader; it is recommended to use
     * {@link #findResource(String, ClassLoader)} for a more exhaustive search.
     *
     * @param sName  the name of the resource
     * @param loader the ClassLoader used to locate the resource; this method
     *               returns null if a null ClassLoader is provided
     * @return the URL of the resource or null if the resource could not be found
     * or if a null ClassLoader is provided
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
     * @param sName the name of the file
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
     * loader or a {@link Classes#getContextClassLoader() context ClassLoader}.
     * <p/>
     * This method attempts to locate a file with the specified name or path.  If
     * the file does not exist or cannot be read, this method attempts to locate
     * a resource with the given name, using the specified class loader or
     * context class loader.
     * </p>
     * If a resource with the given name is not found, this method attempts
     * to find the resource using a fully-qualified or relative version of the
     * specified name. As a last attempt, the name will be treated as a URL.
     *
     * @param sName  the name of the file or resource
     * @param loader the ClassLoader used to locate the resource; if null,
     *               {@link Classes#getContextClassLoader()} is used
     * @return the URL of the file or resource or null if the resource could not
     * be found and the resource name is not a valid URL specification
     */
    public static URL findFileOrResource(String sName, ClassLoader loader)
        {
        URL url = getFileURL(sName);
        return url == null ? findResource(sName, loader) : url;
        }
    }
