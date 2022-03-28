/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.NamedResourceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.StaticFactoryInstanceBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;


import com.tangosol.run.xml.QualifiedName;
import com.tangosol.run.xml.XmlElement;

import java.util.List;

/**
 * An {@link ElementProcessorHelper} provides a number of helper methods for {@link ElementProcessor}s.
 *
 * @author bo  2012.02.02
 * @since Coherence 12.1.2
 */
public class ElementProcessorHelper
    {
    /**
     * Attempts to process the specified {@link XmlElement} to produce a {@link ParameterizedBuilder} given a
     * {@link ProcessingContext}.
     *
     * @param context  the {@link ProcessingContext} to use
     * @param element  the {@link XmlElement} that contains the definition of a {@link ParameterizedBuilder}
     *
     * @return  a {@link ParameterizedBuilder} or <code>null</code> if one is not available
     *
     * @throws ConfigurationException if an error occurs while processing the {@link ParameterizedBuilder}
     */
    public static ParameterizedBuilder<?> processParameterizedBuilder(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        ParameterizedBuilder<?> bldr;

        // determine the prefix of the namespace in which we're operating as this should be used
        // for locating child elements that define custom builders in this element
        String sPrefix = element.getQualifiedName().getPrefix();

        // does this element define a "class-scheme"?
        QualifiedName qName = new QualifiedName(sPrefix, "class-scheme");
        XmlElement    xml   = element.getElement(qName.getName());

        if (xml != null)
            {
            // have the context process the "class-scheme" element
            bldr = (ParameterizedBuilder<?>) context.processElement(xml);
            }
        else
            {
            // does this element define an "instance" element?
            qName = new QualifiedName(sPrefix, "instance");
            xml   = element.getElement(qName.getName());

            if (xml != null)
                {
                // have the context process the "instance" element
                bldr = (ParameterizedBuilder<?>) context.processElement(xml);
                }
            else
                {
                // does this element define a "class-name" element?
                qName = new QualifiedName(sPrefix, "class-name");
                xml   = element.getElement(qName.getName());

                if (xml != null)
                    {
                    // use an InstanceBuilder to capture the "class-name" definition
                    bldr = context.inject(new InstanceBuilder(), element);
                    }
                else
                    {
                    // does this element define a "class-factory-name" element?
                    qName = new QualifiedName(sPrefix, "class-factory-name");
                    xml   = element.getElement(qName.getName());

                    if (xml != null)
                        {
                        // use a StaticFactoryInstanceBuilder to capture the "class-factory-name"
                        bldr = context.inject(new StaticFactoryInstanceBuilder(), element);
                        }
                    else
                        {
                        // does this element define a "resource" element?
                        qName = new QualifiedName(sPrefix, "resource");
                        xml   = element.getElement(qName.getName());

                        if (xml != null)
                            {
                            bldr = (ParameterizedBuilder<?>) context.processElement(xml);
                            if (bldr instanceof NamedResourceBuilder)
                                {
                                bldr = ((NamedResourceBuilder) bldr).getDelegate();
                                }
                            }
                        else
                            {
                            // when there's only a single child element and it's from a foreign namespace,
                            // assume that it produces a builder and return it if it is
                            List<XmlElement> listChildren = element.getElementList();

                            if (listChildren.size() == 1
                                    && !listChildren.get(0).getQualifiedName().getPrefix().equals(sPrefix))
                                {
                                bldr = ensureBuilderOrNull(context.processOnlyElementOf(element));
                                }
                            else if (listChildren.size() == 2)
                                {
                                // or there may be a scheme-name element and a foreign namespace
                                qName = new QualifiedName(sPrefix, "scheme-name");

                                XmlElement elementOne = listChildren.get(0);
                                XmlElement elementTwo = listChildren.get(1);

                                if (elementOne.getQualifiedName().equals(qName)
                                        && !elementTwo.getQualifiedName().getPrefix().equals(sPrefix))
                                    {
                                    // first element is scheme-name and second element is a foreign namespace
                                    bldr = ensureBuilderOrNull(context.processElement(elementTwo));
                                    }
                                else if (!elementOne.getQualifiedName().getPrefix().equals(sPrefix)
                                        && elementTwo.getQualifiedName().equals(qName))
                                    {
                                    // second element is scheme-name and first element is a foreign namespace
                                    bldr = ensureBuilderOrNull(context.processElement(elementOne));
                                    }
                                else
                                    {
                                    bldr = null;
                                    }
                                }
                            else
                                {
                                // no custom builder is available
                                bldr = null;
                                }
                            }
                        }
                    }
                }
            }

        return bldr;
        }

    private static ParameterizedBuilder<?> ensureBuilderOrNull(Object oBuilder)
        {
        if (oBuilder instanceof ParameterizedBuilder)
            {
            return (ParameterizedBuilder<?>) oBuilder;
            }
        else
            {
            // not a builder, so ignore it
            return null;
            }
        }
    }
