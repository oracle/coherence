/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.coherence.config.CacheConfig;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;


/**
 * A {@link DefaultsCreationPreprocessor} is an {@link ElementPreprocessor} that
 * creates necessary defaults, like for {@literal <serializer>}s, if they are missing
 * from the {@literal <defaults>} element.
 *
 * @author bo  2014.03.21
 * @since Coherence 12.2.1
 */
public class DefaultsCreationPreprocessor
        implements ElementPreprocessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link DefaultsCreationPreprocessor}.
     */
    public DefaultsCreationPreprocessor()
        {
        }

    // ----- ElementPreprocessor methods ------------------------------------

    @Override
    public boolean preprocess(ProcessingContext context, XmlElement xmlConfig)
            throws ConfigurationException
        {
        // assume we didn't update the defaults
        boolean fUpdatedDefaults = false;

        // we only process the root element
        String sName = xmlConfig.getName();
        if (sName.equals(CacheConfig.TOP_LEVEL_ELEMENT_NAME))
            {
            XmlElement xmlDefaults = xmlConfig.getElement("defaults");

            // ----- create the <defaults><serializer>pof</serializer></defaults> when required -----
            ParameterResolver parameterResolver = context.getDefaultParameterResolver();
            Parameter         paramPofConfigUri = parameterResolver.resolve("pof-config-uri");

            String sPofConfigUri = paramPofConfigUri == null ? null :
                   paramPofConfigUri.evaluate(parameterResolver).as(String.class);

            if (sPofConfigUri != null && !sPofConfigUri.isEmpty())
                {
                // When a POF Config URI is specified we must ensure that the
                // <defaults><serializer>pof</serializer><defaults> is defined for pof
                // (if not defined for something else)
                if (xmlDefaults == null)
                    {
                    xmlDefaults      = addDefaults(xmlConfig);
                    fUpdatedDefaults = true;
                    }

                XmlElement xmlSerializer = xmlDefaults.getElement("serializer");
                if (xmlSerializer == null)
                    {
                    xmlSerializer = xmlDefaults.addElement("serializer");
                    xmlSerializer.setString("pof");

                    fUpdatedDefaults = true;
                    }
                }

            // ---- backward compatibility: check for the root "scope-name" element
            // (deprecated in 12.1.2)
            XmlElement xmlScope = xmlConfig.getElement("scope-name");
            if (xmlScope != null && !xmlScope.isEmpty())
                {
                // move the "scope-name" element from the root into the "defaults"
                if (xmlDefaults == null)
                    {
                    xmlDefaults      = addDefaults(xmlConfig);
                    fUpdatedDefaults = true;
                    }

                xmlConfig.getElementList().remove(xmlScope);

                XmlElement xmlDefaultScope = xmlDefaults.getElement("scope-name");
                if (xmlDefaultScope == null)
                    {
                    // according to the scheme, the "scope-name" should be first
                    xmlDefaults.getElementList().add(0, xmlScope);
                    fUpdatedDefaults = true;
                    }
                else if (xmlDefaults.isEmpty())
                    {
                    xmlDefaults.setString(xmlScope.getString());
                    fUpdatedDefaults = true;
                    }
                }
            }

        return fUpdatedDefaults;
        }

    /**
     * Add the top level "defaults" element.
     *
     * @param xmlConfig the top level "configuration" element
     *
     * @return newly created "defaults" element
     */
    private XmlElement addDefaults(XmlElement xmlConfig)
        {
        assert xmlConfig.getElement("defaults") == null;

        // make sure defaults is created as the first child of configuration;
        // otherwise the XML validation may fail
        XmlElement xmlDefaults = new SimpleElement("defaults");
        xmlConfig.getElementList().add(0, xmlDefaults);

        return xmlDefaults;
        }
    }
