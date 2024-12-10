/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.persistence.PersistenceEnvironment;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.PersistenceEnvironmentParamBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;

import java.util.Map;

/**
 * An {@link ElementProcessor} for the &lt;persistence-environments%gt; element of
 * Coherence Operational Configuration files.
 *
 * @author jf  2015.03.03
 * @since Coherence 12.2.1
 */
@XmlSimpleName("persistence-environments")
public class PersistenceEnvironmentsProcessor
        implements ElementProcessor<Void>
    {
    // ----- ElementProcessor methods ---------------------------------------

    @Override
    public Void process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // process the children of the <persistence-environments>
        Map<String, ?> mapProcessedChildren = context.processElementsOf(xmlElement);

        // add all of the ParameterizedBuilders to the ParameterizedBuilderRegistry
        ParameterizedBuilderRegistry registry = context.getCookie(ParameterizedBuilderRegistry.class);

        assert registry != null;

        for (Map.Entry<String, ?> entry : mapProcessedChildren.entrySet())
            {
            String sName    = entry.getKey();
            Object oBuilder = entry.getValue();

            if (oBuilder instanceof ParameterizedBuilder)
                {
                @SuppressWarnings("unchecked")
                ParameterizedBuilder<PersistenceEnvironment> builder =
                        (ParameterizedBuilder<PersistenceEnvironment>) oBuilder;

                registry.registerBuilder(PersistenceEnvironment.class, sName, builder);
                }
            else
                {
                throw new ConfigurationException("The specified <persistence-environment> [" + sName
                            + "] is not a ParameterizedBuilder<PersistenceEnvironment>",
                        "Use <instance> element to specify a ParameterizedBuilder<PersistenceEnvironment> implementation");

                }
            }

        return null;
        }

    // ----- inner class: PersistenceEnvironmentProcessor -------------------

    /**
     * An {@link com.tangosol.config.xml.ElementProcessor} for children elements
     * of  &lt;persistence-environments%gt; element of Coherence Operational
     * Configuration files. Produces a {@link PersistenceEnvironmentParamBuilder}.
     */
    @XmlSimpleName("persistence-environment")
    public static class PersistenceEnvironmentProcessor
            extends AbstractEmptyElementProcessor<PersistenceEnvironmentParamBuilder>
        {
        @Override
        protected PersistenceEnvironmentParamBuilder onProcess(ProcessingContext context, XmlElement xmlElement)
                throws ConfigurationException
            {
            return context.inject(new PersistenceEnvironmentParamBuilder(), xmlElement);
            }
        }
    }
