/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema;

import java.util.ArrayList;

/**
 * Schema representation of POF collection.
 * 
 * @author as  2013.11.18
 */
public class PofCollection
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code PofCollection} instance.
     */
    public PofCollection()
        {
        }

    /**
     * Construct {@code PofCollection} instance.
     *
     * @param sCollectionClass  the type of collection
     * @param sElementClass     the type of collection elements
     */
    public PofCollection(String sCollectionClass, String sElementClass)
        {
        m_sCollectionClass = sCollectionClass;
        m_sElementClass = sElementClass;
        }

    protected PofCollection(Class sCollectionClass)
        {
        m_sCollectionClass = sCollectionClass.getName();
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the name of the collection class to use.
     *
     * @return the name of the collection class to use
     */
    public String getCollectionClass()
        {
        return m_sCollectionClass;
        }

    /**
     * Set the name of the collection class to use.
     *
     * @param sCollectionClass  the name of the collection class to use
     */
    public void setCollectionClass(String sCollectionClass)
        {
        m_sCollectionClass = sCollectionClass;
        }

    /**
     * Return the name of the collection element class to use.
     *
     * @return the name of the collection element class to use
     */
    public String getElementClass()
        {
        return m_sElementClass;
        }

    /**
     * Set the name of the collection element class to use.
     *
     * @param sElementClass  the name of the collection element class to use
     */
    public void setElementClass(String sElementClass)
        {
        m_sElementClass = sElementClass;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The name of the collection class to use.
     */
    private String m_sCollectionClass = ArrayList.class.getName();

    /**
     * The name of the collection element class to use.
     */
    private String m_sElementClass    = Object.class.getName();
    }
