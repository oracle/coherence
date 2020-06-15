/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.util;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;


/**
 * A simple resource-loading facility.
 * <p/>
 * This class is similar to the JDK-provided ServiceLoader, but instead of
 * discovering and instantiating service classes it will find the resources
 * with a specified name.
 *
 * @author as  2013.11.21
 */
public class ResourceLoader
        implements Iterable<InputStream>
    {
    /**
     * Construct ResourceLoader instance.
     *
     * @param  resourceName  the resource name
     * @param  loader        the class loader to be used to load resources
     */
    private ResourceLoader(String resourceName, ClassLoader loader)
        {
        try
            {
            m_resourceName = resourceName;
            m_resources = loader.getResources(resourceName);
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    /**
     * Iterates over the discovered resources and returns an InputStream
     * for each resource.
     *
     * @return resource iterator
     */
    @Override
    public Iterator<InputStream> iterator()
        {
        return new Iterator<InputStream>()
            {
            @Override
            public boolean hasNext()
                {
                return m_resources.hasMoreElements();
                }

            @Override
            public InputStream next()
                {
                try
                    {
                    return m_resources.nextElement().openStream();
                    }
                catch (IOException e)
                    {
                    throw new RuntimeException(e);
                    }
                }

            @Override
            public void remove()
                {
                }
            };
        }

    /**
     * Creates a new resource loader for the given resource name and class
     * loader.
     *
     * @param  resourceName  the resource name
     * @param  loader        the class loader to be used to load resources
     *
     * @return A new resource loader
     */
    public static ResourceLoader load(String resourceName, ClassLoader loader)
    {
        return new ResourceLoader(resourceName, loader);
    }

    /**
     * Creates a new resource loader for the given resource name, using the
     * current thread's {@linkplain Thread#getContextClassLoader
     * context class loader}.
     *
     * <p> An invocation of this convenience method of the form
     *
     * <blockquote><pre>
     * ResourceLoader.load(<i>resourceName</i>)</pre></blockquote>
     *
     * is equivalent to
     *
     * <blockquote><pre>
     * ResourceLoader.load(<i>resourceName</i>,
     *                    Thread.currentThread().getContextClassLoader())</pre></blockquote>
     *
     * @param  resourceName  the resource name
     *
     * @return A new resource loader
     */
    public static ResourceLoader load(String resourceName) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return ResourceLoader.load(resourceName, cl);
    }

    /**
     * Creates a new resource loader for the given resource name, using the
     * extension class loader.
     *
     * <p> This convenience method simply locates the extension class loader,
     * call it <tt><i>extClassLoader</i></tt>, and then returns
     *
     * <blockquote>
     *     <pre>
     * ServiceLoader.load(<i>resourceName</i>, <i>extClassLoader</i>)
     *     </pre>
     * </blockquote>
     *
     * <p> If the extension class loader cannot be found then the system class
     * loader is used; if there is no system class loader then the bootstrap
     * class loader is used.
     *
     * <p> This method is intended for use when only installed providers are
     * desired.  The resulting loader will only find and load resources that
     * have been installed into the current Java virtual machine; resources on
     * the application's class path will be ignored.
     *
     * @param  resourceName  the resource name
     *
     * @return A new resource loader
     */
    public static ResourceLoader loadInstalled(String resourceName) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        ClassLoader prev = null;
        while (cl != null) {
            prev = cl;
            cl = cl.getParent();
        }
        return ResourceLoader.load(resourceName, prev);
    }

    /**
     * Returns a string describing this resource loader.
     *
     * @return  A descriptive string
     */
    public String toString()
        {
        return "ResourceLoader{" +
               "resourceName='" + m_resourceName + '\'' +
               '}';
        }

    // ---- Data members ----------------------------------------------------

    /**
     * Resource name to search for.
     */
    private final String m_resourceName;

    /**
     * Discovered resources.
     */
    private final Enumeration<URL> m_resources;
    }
