/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.IOException;


/**
* Provides an ObjectInputStream that uses a caller provided
* ClassLoader to resolve classes during objects deserialization.
*
* @author gg  2001.12.26
*/
public class ResolvingObjectInputStream
        extends ObjectInputStream
        implements Resolving
    {
    /**
    * Create an ObjectInputStream that reads from the specified InputStream
    * using the specified ClassLoader to resolve classes.
    *
    * @param stream  the underlying <code>InputStream</code> from which to read
    * @param loader  the <code>ClassLoader</code> to use for class resolution
    *
    * @exception IOException if an exception occurred in the underlying stream.
    *
    * @see java.io.ObjectInputStream
    */
    public ResolvingObjectInputStream(InputStream  stream, ClassLoader loader)
            throws IOException
        {
        super(stream);
        m_loader = loader;
        }

    /**
    * Load the local class equivalent of the specified stream class description.
    *
    * @param descriptor  serialization descriptor for a class
    *
    * @return a Class object corresponding to <code>descriptor</code>
    *
    * @exception IOException if an exception occurred in the underlying stream.
    * @exception ClassNotFoundException if the corresponding class cannot be found.
    */
    protected Class resolveClass(ObjectStreamClass descriptor)
            throws IOException, ClassNotFoundException
        {
        ClassLoader loader = m_loader;

        if (loader != null)
            {
            try
                {
                return Class.forName(descriptor.getName(), false, loader);
                }
            catch (ClassNotFoundException e)
                {
                // resort to the default behavior
                }
            }

        return super.resolveClass(descriptor);
        }

    /*
     * WORK IN PROGRESS:
     *  This could be used in conjunction with Externalizable
     *  implemented by the NamedCache instance
     *
     * This method allows trusted subclasses of ObjectInputStream
     * to substitute one object for another during deserialization.
     * It is called after an object has been read but before it is
     * returned from readObject.
     *
     * @param obj object to be substituted
     *
     * @return the substituted object
     *
     * @exception IOException Any of the usual Input/Output exceptions.
     *
     * @see ObjectInputStream#resolveObject(Object)
     *
    The following should be added to the constructor:

        m_service = service;
        enableResolve(true);

    protected Object resolveObject(Object obj)
    	    throws IOException
        {
        if (obj instanceof NamedCache)
            {
            NamedCache   map     = (NamedCache) obj;
            String       sName   = map.getCacheName();
            CacheService service = m_service;

            obj = service.ensureCache(sName, m_loader);
            }

        return obj;
        }
     */

    // ---- data fields -----------------------------------------------------

    /**
    * The class loader used to resolve deserialized classes.
    */
    private ClassLoader m_loader;
    }