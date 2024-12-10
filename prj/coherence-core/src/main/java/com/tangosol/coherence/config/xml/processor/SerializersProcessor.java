/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.io.Serializer;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;

import java.util.Map;

/**
 * An {@link com.tangosol.config.xml.ElementProcessor} for the &lt;serializers%gt; element of
 * Coherence Operational Configuration files.
 *
 * @author jf  2015.03.03
 * @since Coherence 12.2.1
 */
@XmlSimpleName("serializers")
public class SerializersProcessor
        implements ElementProcessor<Void>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public Void process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // process the children of the <serializers>
        Map<String, ?> mapProcessedChildren = context.processElementsOf(xmlElement);

        // add all of the ParameterizedBuilders to the ParameterizedBuilderRegistry
        ParameterizedBuilderRegistry registry = context.getCookie(ParameterizedBuilderRegistry.class);

        Base.azzert(registry != null);

        for (Map.Entry<String, ?> entry : mapProcessedChildren.entrySet())
            {
            String sName    = entry.getKey();
            Object oBuilder = entry.getValue();

            if (oBuilder instanceof ParameterizedBuilder)
                {
                ParameterizedBuilder<?> builder = (ParameterizedBuilder<?>) oBuilder;

                registry.registerBuilder(Serializer.class, sName,
                                         (ParameterizedBuilder<Serializer>) builder);
                }
            else
                {
                throw new ConfigurationException("The specified <serializer> [" + sName
                    + "] is not a ParameterizedBuilder<Serializer>", "Use <instance> element to specify a ParameterizedBuilder<Serializer> implementation");

                }
            }

        return null;
        }
    }
