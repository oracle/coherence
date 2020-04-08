/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.StaticFactoryInstanceBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;




import com.tangosol.run.xml.QualifiedName;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;

import java.util.List;


/**
 * An {@link InstanceProcessor} is responsible for processing {@literal <instance>} {@link XmlElement}s
 * to produce {@link ParameterizedBuilder}s.
 *
 * @author bo  2011.09.28
 * @since Coherence 12.1.2
 */
@XmlSimpleName("instance")
public class InstanceProcessor
        implements ElementProcessor<ParameterizedBuilder<Object>>
    {
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public ParameterizedBuilder<Object> process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        ParameterizedBuilder<Object> builder;

        // when there's a single element in an <instance> and it is different from the
        // Coherence namespace, then assume we're using a custom-namespace that will return
        // a ParameterizedBuilder; this permits use of foreign namespaces in <instance> elements
        // that produce custom ParameterizedBuilders.
        if (isForeignNamespace(element))
            {
            // process the custom-namespace element (it should return a ParameterizedBuilder)
            Object oBuilder = context.processOnlyElementOf(element);

            // ensure we have a ParameterizedBuilder
            if (oBuilder instanceof ParameterizedBuilder)
                {
                builder = (ParameterizedBuilder<Object>) oBuilder;
                }
            else
                {
                throw new ConfigurationException(String
                    .format(
                        "The element [%s] can't be used within a <%s> as the associated namespace implementation "
                        + "doesn't return a ParameterizedBuilder.", element.getElementList().get(0), element
                            .getName()), "Please ensure that the namespace handler for the element constructs "
                            + "a ParameterizedBuilder for the element.");
                }
            }
        else
            {
            // determine if we need to use a static factory or regular class-scheme
            if (element.getElement(new QualifiedName(element.getQualifiedName(), "class-factory-name").getName())
                == null)
                {
                // configure a regular InstanceBuilder based on the <instance> declaration
                builder = context.inject(new InstanceBuilder(), element);
                }
            else
                {
                // configure a StaticFactoryInstanceBuilder based on the <instance> declaration
                builder = context.inject(new StaticFactoryInstanceBuilder(), element);
                }
            }

        return builder;
        }

    /**
     * Return true if the given element meets the following criteria:
     * <ol>
     *     <li>there is a single sub element</li>
     *     <li>the namespace of the sub element is not in the Coherence namespace</li>
     * </ol>
     * @param element the {@literal <instance>} element to inspect
     *
     * @return true if the given element is in a foreign namespace
     */
    protected boolean isForeignNamespace(XmlElement element)
        {
        List listElement = element.getElementList();

        return (listElement.size() == 1)
            && !(Base.equals(((XmlElement) listElement.get(0)).getQualifiedName().getPrefix(),
                element.getQualifiedName().getPrefix()));
        }
    }
