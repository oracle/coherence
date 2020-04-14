/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.config;

import com.tangosol.coherence.rest.util.processor.ProcessorFactory;

import com.tangosol.util.InvocableMap;

/**
 * The ProcessorConfig class encapsulates information related to a
 * Coherence REST EntryProcessor configuration.
 *
 * @author vp 2011.07.08
 */
public class ProcessorConfig
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ProcessorConfig.
     *
     * @param sName  the processor name
     * @param clz    the processor or processor factory class
     */
    public ProcessorConfig(String sName, Class clz)
        {
        if (InvocableMap.EntryProcessor.class.isAssignableFrom(clz) ||
            ProcessorFactory.class.isAssignableFrom(clz))
            {
            m_sName = sName;
            m_clz   = clz;
            }
        else
            {
            throw new IllegalArgumentException("class \"" + clz.getName()
                   + "\" does not implement the EntryProcessor or ProcessorFactory interface");
            }
        }

    //----- accessors -------------------------------------------------------

    /**
     * Determine the name of the processor.
     *
     * @return the processor name
     */
    public String getProcessorName()
        {
        return m_sName;
        }

    /**
     * Determine the class of the processor or its factory.
     *
     * @return the processor or processor factory class
     */
    public Class getProcessorClass()
        {
        return m_clz;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Processor name.
     */
    private final String m_sName;

    /**
     * Processor or processor factory class.
     */
    private final Class m_clz;
    }