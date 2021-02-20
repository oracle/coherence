/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.WrapperException;

/**
 * An ElementProcessor that will parse a &lt;serializer&gt; and produce a
 * suitable SerializerFactory
 *
 * @author bo  2013.03.07
 * @since Coherence 12.1.3
 */
@XmlSimpleName("serializer")
public class SerializerFactoryProcessor
        extends AbstractEmptyElementProcessor<SerializerFactory>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link SerializerFactoryProcessor}.
     */
    public SerializerFactoryProcessor()
        {
        super(EmptyElementBehavior.IGNORE);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public SerializerFactory onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        SerializerFactory factory;

        // attempt to locate a ParameterizedBuilder
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr == null)
            {
            // The <serializer> element is _not_ empty; ie: it contains
            // a string value (e.g. <serializer>pof</serializer>).
            // it must be a named/registered serializer factory, so let's look it up
            String sName = xmlElement.getString();

            // grab the operational context from which we can lookup the serializer
            OperationalContext ctxOperational = context.getCookie(OperationalContext.class);

            if (ctxOperational == null)
                {
                throw new ConfigurationException("Attempted to resolve the OperationalContext in [" + xmlElement
                    + "] but it was not defined", "The registered ElementHandler for the <serializer> element is not operating in an OperationalContext");
                }
            else
                {
                // attempt to resolve the serializer
                factory = ctxOperational.getSerializerMap().get(sName);

                if (factory == null)
                    {
                    throw new IllegalArgumentException("Serializer name \"" + sName + "\" is undefined:\n"
                                                       + xmlElement);
                    }
                }
            }
        else
            {
            final ParameterResolver                resolver       = context.getDefaultParameterResolver();
            final ParameterizedBuilder<Serializer> bldrSerializer = (ParameterizedBuilder<Serializer>) bldr;
            final XmlElement                       f_xmlElement   = xmlElement;

            // adapt the ParameterizedBuilder<Serializer> into a SerializerFactory
            factory = new SerializerFactory()
                {
                @Override
                public Serializer createSerializer(ClassLoader loader)
                    {
                    try
                        {
                        Serializer serializer = bldrSerializer.realize(resolver, loader, null);

                        if (serializer instanceof ClassLoaderAware)
                            {
                            ((ClassLoaderAware) serializer).setContextClassLoader(loader);
                            }

                        return serializer;
                        }
                    catch (Exception e)
                        {
                        throw new ConfigurationException("Expected a ParameterizedBuilder<SerializerFactory>, but found ["
                                                         + bldrSerializer + "] after parsing [" + f_xmlElement
                                                         + "]", " Please specify the name of a registered <serializer> "
                                                             + "or a ParameterizedBuilder<SerializerFactory>", e);
                        }
                    }
                };
            }

        return factory;
        }
    }
