/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ElementCalculatorBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import java.text.ParseException;

/**
 * A {@link ElementCalculatorProcessor} is responsible for processing an element-calculator
 * {@link XmlElement} to produce an {@link ElementCalculatorBuilder}.
 *
 * @author Jonathan Knight  2021.05.17
 * @since 21.06
 */
@XmlSimpleName("element-calculator")
public class ElementCalculatorProcessor
        implements ElementProcessor<ElementCalculatorBuilder>
    {
    @Override
    public ElementCalculatorBuilder process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        ElementCalculatorBuilder bldr = new ElementCalculatorBuilder();

        if (element.getElementList().isEmpty())
            {
            try
                {
                String sValue = element.getString().trim();

                bldr.setElementCalculatorType(context.getExpressionParser().parse(sValue, String.class));
                }
            catch (ParseException e)
                {
                throw new ConfigurationException("Failed to parse the specified element-calculator",
                                                 "Please ensure a correct element-calculator is specified", e);
                }
            }
        else
            {
            bldr.setCustomBuilder(context.processOnlyElementOf(element));
            }

        return bldr;
        }
    }
