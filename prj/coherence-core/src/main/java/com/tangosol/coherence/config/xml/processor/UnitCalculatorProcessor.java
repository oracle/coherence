/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.UnitCalculatorBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;


import com.tangosol.net.cache.ConfigurableCacheMap.UnitCalculator;

import com.tangosol.run.xml.XmlElement;

import java.text.ParseException;

/**
 * A {@link UnitCalculatorProcessor} is responsible for processing a
 * unit-calculator {@link XmlElement} to produce a {@link UnitCalculatorBuilder}.
 *
 * @author pfm  2011.12.02
 * @since Coherence 12.1.2
 */
@XmlSimpleName("unit-calculator")
public class UnitCalculatorProcessor
        implements ElementProcessor<UnitCalculatorBuilder>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public UnitCalculatorBuilder process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        UnitCalculatorBuilder bldr = new UnitCalculatorBuilder();

        if (element.getElementList().isEmpty())
            {
            try
                {
                String sValue = element.getString().trim();

                bldr.setUnitCalculatorType(context.getExpressionParser().parse(sValue, String.class));
                }
            catch (ParseException e)
                {
                throw new ConfigurationException("Failed to parse the specifie unit-calculator",
                                                 "Please ensure a correct unit-calculator is specified", e);
                }
            }
        else
            {
            bldr.setCustomBuilder((ParameterizedBuilder<UnitCalculator>) context.processOnlyElementOf(element));
            }

        return bldr;
        }
    }
