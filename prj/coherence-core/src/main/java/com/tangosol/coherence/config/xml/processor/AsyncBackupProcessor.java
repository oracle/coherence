/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.PartitionAssignmentStrategyBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Expression;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;

import java.text.ParseException;

/**
 * An {@link ElementProcessor} for an &lt;async-backup&gt; Configuration.
 */
@XmlSimpleName("async-backup")
public class AsyncBackupProcessor
        implements ElementProcessor<Duration>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public Duration process(ProcessingContext context, XmlElement xmlElement)
        {
        String sValue;
        try
            {
            sValue = context.getExpressionParser().parse(xmlElement.getString(), String.class)
                        .evaluate(context.getDefaultParameterResolver());
            }
        catch (ParseException pe)
            {
            throw new ConfigurationException("Failed to parse the specified async-backup",
                        "Please ensure a correct async-backup is specified", pe);
            }

        Boolean FAsync = (Boolean) XmlHelper.convert(sValue, XmlValue.TYPE_BOOLEAN);
        if (FAsync != null)
            {
            return FAsync ? new Duration(0L) : null;
            }

        return new Duration(sValue);
        }
    }
