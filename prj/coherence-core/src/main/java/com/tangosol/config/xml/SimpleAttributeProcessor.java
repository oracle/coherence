/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;

import com.tangosol.run.xml.XmlAttribute;
import com.tangosol.run.xml.XmlValue;

import java.lang.reflect.Constructor;

/**
 * A {@link SimpleAttributeProcessor} is a simple {@link AttributeProcessor} implementation that will construct,
 * initialize (via constructor injection) and return a specific type of object based on information in
 * an {@link XmlAttribute}.
 *
 * @author dr  2011.05.14
 * @author bo  2011.06.22
 * @since Coherence 12.1.2
 */
public class SimpleAttributeProcessor<T>
        implements AttributeProcessor<T>
    {
    // ----- constructor ----------------------------------------------------

    /**
     * Construct a {@link SimpleAttributeProcessor}.
     *
     * @param clzAttribute  the {@link Class} of the attribute value to be returned
     */
    public SimpleAttributeProcessor(Class<T> clzAttribute)
        {
        m_clzAttribute = clzAttribute;
        }

    // ----- AttributeProcessor interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public T process(ProcessingContext context, XmlAttribute attribute)
            throws ConfigurationException
        {
        try
            {
            // is the type an Enum?
            if (m_clzAttribute.isEnum())
                {
                // determine the value as a string
                String sValue = attribute.getXmlValue().getString();

                try
                    {
                    return (T) Enum.valueOf((Class<Enum>) m_clzAttribute, sValue);
                    }
                catch (Exception exception)
                    {
                    // the enum is unknown/unsupported
                    throw new ClassCastException(String.format("The specified Enum value '%s' is unknown.", sValue));
                    }
                }

            // attempt to construct the instance with an XmlAttribute
            try
                {
                Constructor<T> constructor = m_clzAttribute.getConstructor(XmlAttribute.class);

                return constructor.newInstance(attribute);
                }
            catch (NoSuchMethodException e)
                {
                // SKIP: nothing to do here as we'll try a different constructor
                }

            // attempt to construct the instance with an XmlAttribute XmlValue
            try
                {
                Constructor<T> constructor = m_clzAttribute.getConstructor(XmlValue.class);

                return constructor.newInstance(attribute.getXmlValue());
                }
            catch (NoSuchMethodException e)
                {
                // SKIP: nothing to do here as we'll try a different constructor
                }

            // attempt to construct the instance with the String representation of the XmlValue
            try
                {
                Constructor<T> constructor = m_clzAttribute.getConstructor(String.class);

                return constructor.newInstance(attribute.getXmlValue().getString());
                }
            catch (NoSuchMethodException e)
                {
                throw new ConfigurationException(String.format("Can't instantiate the required class [%s] using the attribute [%s]",
                    m_clzAttribute,
                    attribute), "The required class does not provide a public single argument constructor for either String, XmlAttribute or XmlValue type values.");
                }
            }
        catch (ConfigurationException e)
            {
            throw e;
            }
        catch (Exception exception)
            {
            throw new ConfigurationException(String.format("Can't instantiate the required class [%s] using the attribute [%s]",
                m_clzAttribute,
                attribute), "The required class must define a public single argument constructor for String, XmlAttribute or XmlValue type values", exception);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Class} of the value returned by the {@link AttributeProcessor}.
     */
    private Class<T> m_clzAttribute;
    }
