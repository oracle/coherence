/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

import java.lang.reflect.Constructor;

/**
 * A {@link SimpleElementProcessor} is an {@link ElementProcessor} implementation that will construct,
 * initialize (via constructor injection) and return a specific type of object based on information in
 * an {@link XmlElement}.
 *
 * @author dr  2011.05.14
 * @author bo  2011.06.23
 * @since Coherence 12.1.2
 */
public class SimpleElementProcessor<T>
        implements ElementProcessor<T>
    {
    // ----- constructor ----------------------------------------------------

    /**
     * Construct a {@link SimpleElementProcessor}.
     *
     * @param clzElement  The {@link Class} of objects returned by the {@link ElementProcessor}
     */
    public SimpleElementProcessor(Class<T> clzElement)
        {
        m_clzElement = clzElement;
        }

    // ----- ElementProcessor interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public T process(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        try
            {
            // is the type an Enum?
            if (m_clzElement.isEnum())
                {
                // determine the value as a string
                String sValue = element.getString();

                try
                    {
                    return (T) Enum.valueOf((Class<Enum>) m_clzElement, sValue);
                    }
                catch (Exception exception)
                    {
                    // the enum is unknown/unsupported
                    throw new ClassCastException(String.format("The specified Enum value '%s' is unknown.", sValue));
                    }
                }

            // attempt to construct the instance with an XmlValue
            try
                {
                Constructor<T> constructor = m_clzElement.getConstructor(XmlValue.class);

                return constructor.newInstance(element);
                }
            catch (NoSuchMethodException e)
                {
                // SKIP: nothing to do here as we'll try a different constructor
                }

            // attempt to construct the instance with an XmlElement
            try
                {
                Constructor<T> constructor = m_clzElement.getConstructor(XmlElement.class);

                return constructor.newInstance(element);
                }
            catch (NoSuchMethodException e)
                {
                // SKIP: nothing to do here as we'll try a different constructor
                }

            // attempt to construct the instance with the String representation of the XmlElement
            try
                {
                Constructor<T> constructor = m_clzElement.getConstructor(String.class);

                return constructor.newInstance(element.toString());
                }
            catch (NoSuchMethodException e)
                {
                // SKIP: nothing to do here as we'll try a different constructor
                }

            // when everything else has failed attempt to construct the instance using setter injection
            // starting with a no-args constructor.
            return context.inject(m_clzElement.newInstance(), element);
            }
        catch (ConfigurationException e)
            {
            throw e;
            }
        catch (Exception exception)
            {
            throw new ConfigurationException(String.format("Can't instantiate the required class [%s] using the element [%s]",
                m_clzElement,
                element), "The required class does not provide a public no args constructor or a single argument constructor for either String, XmlValue or XmlElement type values.", exception);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Class} of the value returned by the {@link ElementProcessor}.
     */
    private Class<T> m_clzElement;
    }
