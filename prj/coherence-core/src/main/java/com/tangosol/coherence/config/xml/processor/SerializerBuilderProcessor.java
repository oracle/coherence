/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializationLimitPolicy;
import com.tangosol.run.xml.XmlElement;

/**
 * An {@link com.tangosol.config.xml.ElementProcessor} for &lt;serializer&gt; elements defined by
 * a Coherence Operational Configuration file.
 *
 * @author jf  2015.03.03
 * @since Coherence 12.2.1
 */
@XmlSimpleName("serializer")
public class SerializerBuilderProcessor
        extends AbstractEmptyElementProcessor<ParameterizedBuilder<Serializer>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link com.tangosol.coherence.config.xml.processor.SerializerBuilderProcessor}.
     */
    public SerializerBuilderProcessor()
        {
        super(EmptyElementBehavior.IGNORE);
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    @Override
    public ParameterizedBuilder<Serializer> onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        SerializationLimitPolicy policy = SerializationLimitPolicy.fromXml(
                xmlElement.getElement(SerializationLimitPolicy.XML_LIMITS));

        // assume the <serializer> contains a builder definition
        ParameterizedBuilder bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr == null)
            {
            throw new ConfigurationException("<serializer> fails to correctly define a ParameterizedBuilder<Serializer> implementation: "
                                             + xmlElement, "Please define a <serializer>");
            }

        return policy == null
               ? (ParameterizedBuilder<Serializer>) bldr
               : new LimitAwareSerializerBuilder((ParameterizedBuilder<Serializer>) bldr, policy);
        }

    // ----- inner class: LimitAwareSerializerBuilder -----------------------

    /**
     * Builder wrapper that applies serializer limits to realized serializers.
     */
    protected static class LimitAwareSerializerBuilder
            implements ParameterizedBuilder<Serializer>
        {
        protected LimitAwareSerializerBuilder(ParameterizedBuilder<Serializer> builder,
                                             SerializationLimitPolicy policy)
            {
            f_builder = builder;
            f_policy  = policy;
            }

        @Override
        public Serializer realize(com.tangosol.config.expression.ParameterResolver resolver,
                                  ClassLoader loader,
                                  com.tangosol.coherence.config.ParameterList listParameters)
            {
            return SerializationLimitPolicy.apply(f_builder.realize(resolver, loader, listParameters), f_policy);
            }

        private final ParameterizedBuilder<Serializer> f_builder;

        private final SerializationLimitPolicy f_policy;
        }
    }
