/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import javax.enterprise.inject.spi.CDI;

/**
 * Element processor for {@code <cdi:bean/>} XML element.
 *
 * @author Aleks Seovic  2019.10.02
 * @since 20.06
 */
@XmlSimpleName("bean")
public class BeanProcessor
        implements ElementProcessor<BeanBuilder>
    {
    public BeanProcessor()
        {
        try
            {
            m_cdi = CDI.current();
            }
        catch (Throwable thrown)
            {
            Logger.err("Error obtaining CDI", thrown);
            }
        }

    @Override
    public BeanBuilder process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        return context.inject(new BeanBuilder(m_cdi, element.getString()), element);
        }

    void setCDI(CDI<Object> cdi)
        {
        m_cdi = cdi == null ? CDI.current() : cdi;
        }

    // ----- data members ---------------------------------------------------

    private CDI<Object> m_cdi;
    }
