/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.coherence.config.Config;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.SystemPropertyParameterResolver;
import com.tangosol.config.expression.Value;
import com.tangosol.config.expression.ValueMacroExpression;

import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;


import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

/**
 * A {@link SystemPropertyPreprocessor} is an {@link ElementPreprocessor} that will
 * replace {@link XmlElement} content annotated with "system-property" attributes with appropriate
 * {@link System#getProperties()}.
 * <p>
 * The element's value is processed for macro expansion. The macro syntax is <tt><i>${system-property default-value}</i></tt>.
 * Thus, a value of <tt>near-<i>${coherence.client direct}</i></tt> can be macro expanded by default to <tt>near-direct</tt>.
 * If system property <i>coherence.client</i> is set to <tt>remote</tt>, then the value would be expanded to <tt>near-remote</tt>.
 *
 * @author bo  2011.08.03
 * @since Coherence 12.1.2
 */
public class SystemPropertyPreprocessor
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
        boolean           fUpdated  = false;
        XmlValue          attribute = element.getAttribute(SYSTEM_PROPERTY);
        ParameterResolver resolver  = context.getDefaultParameterResolver();
        if (attribute != null)
            {
            // remove the attribute
            element.setAttribute(SYSTEM_PROPERTY, null);

            // set the element's value from the specified system property
            try
                {
                String sName = attribute.getString();

                // try the context's resolver first
                Parameter parameter = resolver.resolve(sName);
                if (parameter != null)
                    {
                    Value  value  = parameter.evaluate(resolver);
                    Object oValue = value.get();
                    if (oValue != null)
                        {
                        element.setString(String.valueOf(oValue));
                        fUpdated = true;
                        }
                    }

                if (!fUpdated)
                    {
                    String sValue = Config.getProperty(sName);

                    if (sValue != null)
                        {
                        element.setString(sValue);
                        fUpdated = true;
                        }
                    }
                }
            catch (Exception e)
                {
                // ignore security exception accessing config property
                }
            }

        fUpdated |= processValueMacro(element, resolver);

        return fUpdated;
        }

    /**
     * Process macros embedded in element's value
     *
     * @param element the {@link XmlElement} to preprocess
     *
     * @return true iff the String value of element was macro expanded
     */
    static public boolean processValueMacro(XmlElement element)
        {
        return processValueMacro(element, SystemPropertyParameterResolver.INSTANCE);
        }

    /**
     * Process macros embedded in element's value
     *
     * @param element  the {@link XmlElement} to preprocess
     * @param resolver the {@link ParameterResolver} to use to resolve macro values
     *
     * @return true iff the String value of element was macro expanded
     */
    static public boolean processValueMacro(XmlElement element, ParameterResolver resolver)
        {
        if (ValueMacroExpression.containsMacro(element.getString()))
            {
            ValueMacroExpression macroExpr = new ValueMacroExpression(element.getString().trim());
            String sValue = macroExpr.evaluate(resolver);
            element.setString(sValue);
            return true;
            }
        else
            {
            return false;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * This singleton instance of the {@link SystemPropertyPreprocessor}.
     */
    public static final SystemPropertyPreprocessor INSTANCE = new SystemPropertyPreprocessor();

    /**
     * The constant for the "system-property" attribute.
     */
    private static final String SYSTEM_PROPERTY = "system-property";
    }
