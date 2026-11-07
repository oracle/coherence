/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.unit.LowUnits;
import com.tangosol.coherence.config.unit.Units;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

import java.text.ParseException;

/**
 * Processes low-watermark expressions without extending high-units syntax.
 *
 * @author Aleks Seovic  2026.11.06
 * @since 27.01
 */
public class LowUnitsProcessor
        extends AbstractEmptyElementProcessor<Expression<Units>>
    {
    /**
     * Construct a processor that preserves the default for empty elements.
     */
    public LowUnitsProcessor()
        {
        super(EmptyElementBehavior.IGNORE);
        }

    @Override
    public Expression<Units> onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        try
            {
            Expression<Object> expression = context.getExpressionParser()
                    .parse(xmlElement.getString(), Object.class);
            return resolver ->
                {
                Object value = expression.evaluate(resolver);
                return value instanceof Units units ? units : new LowUnits(String.valueOf(value));
                };
            }
        catch (ParseException e)
            {
            throw new ConfigurationException("Failed to parse low-units",
                    "Specify a unit count, memory size, or percentage strictly between zero and 100", e);
            }
        }
    }
