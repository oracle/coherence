/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.coherence.config.Config;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.SimpleValue;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

/**
 * Marks the shipped hostname-verifier system-property fallback before the
 * generic system-property preprocessor removes the provenance.
 *
 * @author Aleks Seovic  2026.05.19
 * @since 26.07
 */
public class SSLHostnameVerifierPreprocessor
        implements ElementPreprocessor
    {
    @Override
    public boolean preprocess(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        if (!"action".equals(element.getName()) || element.getParent() == null
                || !"hostname-verifier".equals(element.getParent().getName()))
            {
            return false;
            }

        XmlValue attr = element.getAttribute(ATTR_SYSTEM_PROPERTY);
        if (attr == null || !PROP_HOSTNAME_VERIFICATION.equals(attr.getString())
                || !"allow".equals(element.getString().trim()))
            {
            return false;
            }

        if (!hasPropertyValue(context, PROP_HOSTNAME_VERIFICATION))
            {
            element.setAttribute(ATTR_SYSTEM_PROPERTY_DEFAULT, new SimpleValue(PROP_HOSTNAME_VERIFICATION, true));
            }
        return false;
        }

    /**
     * Return {@code true} if the specified property is set in either the
     * processing context resolver or the Coherence configuration source.
     *
     * @param context  the processing context
     * @param sName    the property name
     *
     * @return {@code true} iff the property has a value
     */
    private boolean hasPropertyValue(ProcessingContext context, String sName)
        {
        try
            {
            ParameterResolver resolver  = context.getDefaultParameterResolver();
            Parameter         parameter = resolver == null ? null : resolver.resolve(sName);

            if (parameter != null)
                {
                Value  value  = parameter.evaluate(resolver);
                Object oValue = value == null ? null : value.get();
                if (oValue != null)
                    {
                    return true;
                    }
                }
            }
        catch (Exception ignored)
            {
            // match SystemPropertyPreprocessor behavior.
            }

        return Config.getProperty(sName) != null;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Hostname-verification system property.
     */
    public static final String PROP_HOSTNAME_VERIFICATION = "coherence.security.hostname.verification";

    /**
     * Internal attribute that marks an unset system-property fallback.
     */
    public static final String ATTR_SYSTEM_PROPERTY_DEFAULT = "system-property-default";

    private static final String ATTR_SYSTEM_PROPERTY = "system-property";

    /**
     * Singleton instance.
     */
    public static final SSLHostnameVerifierPreprocessor INSTANCE = new SSLHostnameVerifierPreprocessor();
    }
