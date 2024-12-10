/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.schema;

import java.util.HashMap;

/**
 * Schema representation of POF map.
 *
 * @author as  2013.11.18
 */
public class PofMap
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code PofMap} instance.
     */
    public PofMap()
        {
        }

    /**
     * Construct {@code PofMap} instance.
     *
     * @param sMapClass    the type of map
     * @param sKeyClass    the type of map keys
     * @param sValueClass  the type of map values
     */
    public PofMap(String sMapClass, String sKeyClass, String sValueClass)
        {
        m_sMapClass   = sMapClass;
        m_sKeyClass   = sKeyClass;
        m_sValueClass = sValueClass;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Return the name of the map class to use.
     *
     * @return the name of the map class to use
     */
    public String getMapClass()
        {
        return m_sMapClass;
        }

    /**
     * Set the name of the map class to use.
     *
     * @param sMapClass  the name of the map class to use
     */
    public void setMapClass(String sMapClass)
        {
        m_sMapClass = sMapClass;
        }

    /**
     * Return the name of the key class to use.
     *
     * @return the name of the key class to use
     */
    public String getKeyClass()
        {
        return m_sKeyClass;
        }

    /**
     * Set the name of the key class to use.
     *
     * @param sKeyClass  the name of the key class to use
     */
    public void setKeyClass(String sKeyClass)
        {
        m_sKeyClass = sKeyClass;
        }

    /**
     * Return the name of the value class to use.
     *
     * @return the name of the value class to use
     */
    public String getValueClass()
        {
        return m_sValueClass;
        }

    /**
     * Set the name of the value class to use.
     *
     * @param sValueClass  the name of the value class to use
     */
    public void setValueClass(String sValueClass)
        {
        m_sValueClass = sValueClass;
        }

    // ---- constructors ----------------------------------------------------

    /**
     * The name of the map class to use.
     */
    private String m_sMapClass = HashMap.class.getName();

    /**
     * The name of the key class to use.
     */
    private String m_sKeyClass = Object.class.getName();

    /**
     * The name of the value class to use.
     */
    private String m_sValueClass = Object.class.getName();
    }
