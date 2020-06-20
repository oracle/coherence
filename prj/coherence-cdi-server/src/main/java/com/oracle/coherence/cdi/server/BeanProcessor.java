/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

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
    @Override
    public BeanBuilder process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        return context.inject(new BeanBuilder(element.getString()), element);
        }
    }
