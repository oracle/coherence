/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;


import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;
import com.tangosol.io.WrapperStreamFactory;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * An {@link ElementProcessor} that will parse a &lt;use-filters&gt; and produce
 * list of {@link WrapperStreamFactory}s.
 *
 * @author bo  2013.06.02
 * @since Coherence 12.1.3
 */
@XmlSimpleName("use-filters")
@Deprecated
public class WrapperStreamFactoryListProcessor
        implements ElementProcessor<List<WrapperStreamFactory>>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public List<WrapperStreamFactory> process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // grab the operational context from which we can lookup the serializer
        OperationalContext ctxOperational = context.getCookie(OperationalContext.class);

        if (ctxOperational == null)
            {
            throw new ConfigurationException("Attempted to resolve the OperationalContext in [" + xmlElement
                + "] but it was not defined", "The registered ElementHandler for the <use-filters> element is not operating in an OperationalContext");
            }

        Map<String, WrapperStreamFactory> mapFactoriesByName = ctxOperational.getFilterMap();

        ArrayList<WrapperStreamFactory>   listFactories      = new ArrayList<WrapperStreamFactory>();

        for (Iterator<XmlElement> iterFilterNames = xmlElement.getElements("filter-name"); iterFilterNames.hasNext(); )
            {
            XmlElement xmlFilterName = iterFilterNames.next();
            String     sFilterName   = xmlFilterName.getString().trim();

            if (!sFilterName.isEmpty())
                {
                WrapperStreamFactory filterFactory = mapFactoriesByName.get(sFilterName);

                if (filterFactory == null)
                    {
                    throw new ConfigurationException("Could not locate the specified filter [" + sFilterName + "] in ["
                        + xmlElement
                        + "]", "Please ensure that the specified filter is correctly defined in the operational configuration");
                    }
                else
                    {
                    listFactories.add(filterFactory);
                    }
                }
            }

        return listFactories;
        }
    }
