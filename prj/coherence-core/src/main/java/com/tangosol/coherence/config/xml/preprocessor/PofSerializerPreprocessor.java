/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;

import com.tangosol.run.xml.XmlElement;

/**
 * A {@link PofSerializerPreprocessor} is an {@link ElementPreprocessor} that
 * replaces any occurrence of {@literal <serializer>pof</serializer>} with a
 * {@link com.tangosol.io.pof.ConfigurablePofContext} configuration, passing in
 * the provided POF configuration URI in the initialization parameters.
 *
 * @author pfm  2012.01.20
 * @since Coherence 12.1.2
 */
public class PofSerializerPreprocessor
        implements ElementPreprocessor
    {
    // ----- ElementPreprocessor methods ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preprocess(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        if (element.getQualifiedName().getLocalName().equals("serializer"))
            {
            Object oVal = element.getValue();

            if (oVal != null && oVal instanceof String && ((String) oVal).equals("pof"))
                {
                ParameterResolver parameterResolver = context.getDefaultParameterResolver();

                Parameter         paramPofConfigUri = parameterResolver.resolve("pof-config-uri");

                if (paramPofConfigUri != null)
                    {
                    String sPofConfigUri = paramPofConfigUri.evaluate(parameterResolver).as(String.class);

                    if (sPofConfigUri != null && !sPofConfigUri.isEmpty())
                        {
                        element.setString(null);

                        XmlElement xmlInstance = element.ensureElement("instance");

                        xmlInstance.ensureElement("class-name").setString("com.tangosol.io.pof.ConfigurablePofContext");
                        xmlInstance.ensureElement("init-params/init-param/param-type").setString("String");
                        xmlInstance.ensureElement("init-params/init-param/param-value").setString(sPofConfigUri);

                        return true;
                        }
                    }
                }
            }

        return false;
        }
    }
