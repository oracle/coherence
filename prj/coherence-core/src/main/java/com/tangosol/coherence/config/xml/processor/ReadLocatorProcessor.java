/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ReadLocatorBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import java.text.ParseException;

/**
 * A {@link ReadLocatorProcessor} is responsible for processing the
 * {@code read-locator} {@link XmlElement xml} to produce a {@link ReadLocatorBuilder}.
 *
 * @author hr
 * @since 21.12
 */
@XmlSimpleName("read-locator")
public class ReadLocatorProcessor
        implements ElementProcessor<ReadLocatorBuilder>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public ReadLocatorBuilder process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        ReadLocatorBuilder bldr = new ReadLocatorBuilder();

        if (element.getElementList().isEmpty())
            {
            try
                {
                String sValue = element.getString().trim();

                bldr.setMemberLocatorType(context.getExpressionParser().parse(sValue, String.class));
                }
            catch (ParseException e)
                {
                throw new ConfigurationException("Failed to parse the specified read-locator",
                                                 "Please ensure a correct read-locator is specified", e);
                }
            }
        else
            {
            bldr.setCustomBuilder(context.processOnlyElementOf(element));
            }

        return bldr;
        }
    }
