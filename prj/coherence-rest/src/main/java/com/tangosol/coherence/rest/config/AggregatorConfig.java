/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.config;

import com.tangosol.coherence.rest.util.aggregator.AggregatorFactory;

import com.tangosol.util.InvocableMap;

/**
 * The AggregatorConfig class encapsulates information related to a
 * Coherence REST EntryAggregator configuration.
 *
 * @author vp 2011.07.08
 */
public class AggregatorConfig
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a AggregatorConfig.
     *
     * @param sName  the aggregator name
     * @param clz    the aggregator or aggregator factory class
     */
    public AggregatorConfig(String sName, Class clz)
        {
        if (InvocableMap.EntryAggregator.class.isAssignableFrom(clz) ||
            AggregatorFactory.class.isAssignableFrom(clz))
            {
            m_sName = sName;
            m_clz   = clz;
            }
        else
            {
            throw new IllegalArgumentException("class \"" + clz.getName()
                    + "\" does not implement the EntryAggregator or AggregatorFactory interface");
            }
        }

    //----- accessors -------------------------------------------------------

    /**
     * Determine the name of the aggregator.
     *
     * @return the aggregator name
     */
    public String getAggregatorName()
        {
        return m_sName;
        }

    /**
     * Determine the class of the aggregator or aggregator factory.
     *
     * @return the aggregator or aggregator factory class
     */
    public Class getAggregatorClass()
        {
        return m_clz;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Aggregator name.
     */
    private final String m_sName;

    /**
     * Aggregator or aggregator factory class.
     */
    private final Class m_clz;
    }