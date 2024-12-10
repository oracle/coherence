/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;

import com.tangosol.dev.assembler.ClassFile;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

import java.util.Hashtable;

/**
* A class-loader that uses the Loader to load customized classes
* and resources. Since the loadClassData and loadResourceData are
* used repetitevely by the Packager, and the CacheStorage doesn't
* cache the classes and resources, we cache the results here.
*
* @version 1.0, 2000.01.12
* @version 1.1, 2001.06.20
* @author  cp
* @author  gg
*/
public class ComponentClassLoader
        extends ClassLoader
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Instantiate the ClassLoader using a default parent
    * and the specified Loader
    */
    public ComponentClassLoader(Loader loader)
        {
        this(null, loader);
        }

    /**
    * Instantiate the ClassLoader using a specified parent ClassLoader
    * and Loader
    */
    public ComponentClassLoader(ClassLoader parent, Loader loader)
        {
        super(ensureParent(parent));

        if (loader == null)
            {
            throw new IllegalArgumentException("The loader must be specified");
            }
        m_loader = loader;
        }


    // ----- Accessors ------------------------------------------------------

    /**
    * Return the Loader used by this ComponentClassLoader
    */
    public Loader getLoader()
        {
        return m_loader;
        }


    // ----- ClassLoader ----------------------------------------------------

    /**
    * Load a class by name.
    *
    * @param sName     the name of the class
    * @param fResolve  if <code>true</code> then resolve the class
    *
    * @return the Class class
    *
    * @exception ClassNotFoundException
    */
    protected Class loadClass(String sName, boolean fResolve)
            throws ClassNotFoundException
        {
        // a) the Loader can load any class
        // b) relocatable class can only be loaded by the Loader

        byte[] ab = null;

        if (!isSystemClass(sName))
            {
            try
                {
                if (DEBUG && sName.startsWith(CLASS_PREFIX))
                    {
                    Base.out("Loading relocatable class: " + sName);
                    }
                ab = loadClassData(sName);
                }
            catch (ComponentException e)
                {
                if (DEBUG)
                    {
                    Base.err("An error (" + e + ") occurred loading the class " + sName + ":");
                    Base.err(e);
                    }

                throw new ClassNotFoundException(sName, e);
                }
            }

        if (ab == null)
            {
            if (sName.startsWith(CLASS_PREFIX))
                {
                throw new ClassNotFoundException(sName);
                }

            return super.loadClass(sName, fResolve);
            }

        try
            {
            Class clz = defineClass(sName, ab, 0, ab.length);

            if (fResolve)
                {
                resolveClass(clz);
                }

            return clz;
            }
        catch (Throwable e)
            {
            if (DEBUG)
                {
                Base.out("Failed to define class " + sName + ": " + e);
                }
            return super.loadClass(sName, fResolve);
            }
        }

    /**
     * Returns an input stream for reading the specified resource.
     *
     * @param  sName the resource name
     * @return an input stream for reading the resource, or <code>null</code>
     *         if the resource could not be found
     */
    public InputStream getResourceAsStream(String sName)
        {
        if (DEBUG && sName.startsWith(RESOURCE_PREFIX))
            {
            Base.out("Loading relocatable resource: " + sName);
            }

        byte[] ab;

        try
            {
            ab = loadResourceData(sName);
            }
        catch (IOException e)
            {
            if (DEBUG)
                {
                Base.err("An error (" + e + ") occurred loading the resource " + sName + ":");
                Base.err(e);
                }
            return null;
            }

        if (ab == null)
            {
            if (sName.startsWith(RESOURCE_PREFIX))
                {
                return null;
                }
            else
                {
                return super.getResourceAsStream(sName);
                }
            }
        else
            {
            return new ByteArrayInputStream(ab);
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Load the byte codes for the specified class name
    */
    public byte[] loadClassData(String sName)
            throws ComponentException
        {
        byte[] ab = (byte[]) m_tblClasses.get(sName);

        if (ab == null)
            {
            ClassFile clsf = m_loader.loadClass(sName);
            if (clsf != null)
                {
                ab = clsf.getBytes();
                m_tblClasses.put(sName, ab);
                }
            }
        return ab;
        }

    /**
    * Load the binary data for the specified resource
    */
    public byte[] loadResourceData(String sName)
            throws IOException
        {
        byte[] ab = (byte[]) m_tblResources.get(sName);

        if (ab == null)
            {
            ab = m_loader.loadResource(sName);
            if (ab != null)
                {
                m_tblResources.put(sName, ab);
                }
            }
        return ab;
        }

    /**
    * If no parent class loader is specified, use this class's loader.
    *
    * @param parent  the parent class loader to use or null
    *
    * @return a parent class loader
    */
    static ClassLoader ensureParent(ClassLoader parent)
        {
        if (parent == null)
            {
            parent = ComponentClassLoader.class.getClassLoader();
            if (parent == null)
                {
                parent = getSystemClassLoader();
                }
            }
        return parent;
        }

    // ----- TAPS support ---------------------------------------------------

    /**
    * @return true if the class should not be attemted to be loaded via TAPS
    */
    protected boolean isSystemClass(String sName)
        {
        return sName.startsWith("java")
            || sName.startsWith("sun")
            || sName.startsWith("com.sun")
            || sName.startsWith("com.tangosol")
            || sName.startsWith("com.xtangosol");
        }

    /**
    * The entry point for the any application that wants to load
    * classes from a TAPS storage usong TAPSLoader as a class loader.
    *
    * Currently the storage intialization requires the following environment
    * variables be set:
    *   tangosol.taps.repos - the repository URI (i.e. "j:\tangosol\prj")
    *   tangosol.taps.prj   = the project name   (i.e. "examples:1.0")
    */
    public static void main(String[] asArg)
            throws Exception
        {
        int cArgs = asArg.length;
        Base.azzert(cArgs > 0, "Class name must be specified");

        Class  clzTapsLoader = Class.forName(TAPS_LOADER);
        Loader loader        = (Loader) clzTapsLoader.newInstance();

        ClassHelper.invoke(loader, "configureStorage", ClassHelper.VOID);

        ClassLoader cl      = new ComponentClassLoader(loader);
        Class       clzMain = cl.loadClass(asArg[0]);

        if (--cArgs > 0)
            {
            String[] as = new String[cArgs];
            System.arraycopy(asArg, 1, as, 0, cArgs);
            asArg = as;
            }
        else
            {
            asArg = new String[0];
            }
        Thread.currentThread().setContextClassLoader(cl);
        ClassHelper.invokeStatic(clzMain, "main", new Object[] {asArg});
        }

    // ----- TAPS loader ----------------------------------------------------

    /**
    * The TAPSLoader component
    */
    private final static String TAPS_LOADER = "com.tangosol.tde.component.dev.service.TAPSLoader";


    // ----- data members ---------------------------------------------------

    /**
    * Used to determine whether additional debug output is permitted.
    */
    private static final boolean DEBUG = false;

    /**
    * Package handled by this ClassLoader.
    */
    private static final String CLASS_PREFIX = ClassFile.Relocator.PACKAGE.replace('/', '.');

    /**
    * Resources handled by this ClassLoader.
    */
    private static final String RESOURCE_PREFIX = ClassFile.Relocator.PACKAGE;

    /**
    * The Loader to use.
    */
    private Loader m_loader;

    /**
    * The cache for classes and resources
   */
    private Hashtable m_tblClasses   = new Hashtable();
    private Hashtable m_tblResources = new Hashtable();
    }
