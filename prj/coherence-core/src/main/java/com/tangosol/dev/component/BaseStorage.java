/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.component;


import com.tangosol.util.Base;


/**
* BaseStorage is a super class for non-synthetic Storage implementations
* such as ArchivedStorage, OSStorage, JarStorage, NullStorage
*
* @version 1.00, 05/18/2001
* @author  gg
*/
public abstract class BaseStorage
        extends Base
        implements Storage
    {
    /**
    * Return a locator assosiated with this Storage (for example <Project>:<Library>)
    */
    public Object getLocator()
        {
        return m_locator;
        }

    /**
    * Set a locator assosiated with this Storage
    *
    * All the methods in the Storage interface that return StringTable object with
    * Component or JCS names will carry the locator object as a corresponding value
    */
    public void setLocator(Object oLocator)
        {
        m_locator = oLocator;
        }
    
    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "AbstractStorage";

    /**
    * Locator object
    */
    private Object m_locator;
    }
