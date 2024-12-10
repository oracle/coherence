/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


/**
* The ClassLoaderAware interface provides the ability to configure a ClassLoader
* to be used by the implementing object when loading classes or resources.
*
* @author jh  2006.07.21
*
* @since Coherence 3.2
*/
public interface ClassLoaderAware
    {
    /**
    * Retrieve the context ClassLoader for this object. The context ClassLoader
    * is provided by the creator of the object for use by the object when
    * loading classes and resources.
    *
    * @return the context ClassLoader for this object
    *
    * @see Thread#getContextClassLoader()
    */
    public ClassLoader getContextClassLoader();

    /**
    * Specify the context ClassLoader for this object. The context ClassLoader
    * can be set when the object is created, and allows the creator to provide
    * the appropriate class loader to be used by the object when when loading
    * classes and resources.
    *
    * @param loader  the context ClassLoader for this object
    */
    public void setContextClassLoader(ClassLoader loader);
    }
