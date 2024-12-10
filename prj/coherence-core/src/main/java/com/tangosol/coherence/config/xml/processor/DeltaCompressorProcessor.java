/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.io.BinaryDeltaCompressor;
import com.tangosol.io.DecoratedBinaryDeltaCompressor;
import com.tangosol.io.DecorationOnlyDeltaCompressor;
import com.tangosol.io.DeltaCompressor;

import com.tangosol.run.xml.XmlElement;

/**
 * An ElementProcessor for processing &lt;compressor&gt; configurations.
 *
 * @author bo  2013.04.01
 * @since Coherence 12.1.3
 */
@XmlSimpleName("compressor")
public class DeltaCompressorProcessor
        implements ElementProcessor<DeltaCompressor>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public DeltaCompressor process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        // attempt to locate a ParameterizedBuilder
        ParameterizedBuilder<?> bldr = ElementProcessorHelper.processParameterizedBuilder(context, xmlElement);

        if (bldr instanceof ParameterizedBuilder)
            {
            try
                {
                ParameterizedBuilder<DeltaCompressor> bldrDeltaCompressor =
                    (ParameterizedBuilder<DeltaCompressor>) bldr;

                return bldrDeltaCompressor.realize(context.getDefaultParameterResolver(),
                                                   context.getContextClassLoader(), null);
                }
            catch (Exception e)
                {
                throw new ConfigurationException("Invalid <" + xmlElement.getName()
                    + "> declaration.  The specified builder doesn't produce a DeltaCompressor in [" + xmlElement
                    + "]", "Please specify a <" + xmlElement.getName() + ">", e);
                }
            }
        else if (xmlElement.getString().equals("standard"))
            {
            // COH-5528 workaround: use the plain binary delta compressor even
            // for POF until the performance regression is solved.
            return new DecoratedBinaryDeltaCompressor(new BinaryDeltaCompressor());

            // COH-5250: create a "dummy" serializer to check for POF
            // ClassLoader         loader     = Base.getContextClassLoader();
            // SerializerFactory   factory    = deps.getSerializerFactory();
            // Serializer          serializer = factory == null
            // ? ExternalizableHelper.ensureSerializer(loader) : factory.createSerializer(loader);
            //
            // return new DecoratedBinaryDeltaCompressor(serializer instanceof PofContext
            // ? ExternalizableHelper.getDeltaCompressor(serializer, new PofDeltaCompressor())
            // : new BinaryDeltaCompressor());
            }
        else
            {
            return new DecorationOnlyDeltaCompressor();
            }
        }
    }
