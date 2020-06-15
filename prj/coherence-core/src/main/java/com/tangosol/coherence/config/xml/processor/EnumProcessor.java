/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

/**
 * A {@link EnumProcessor} is responsible for processing Coherence Enum values
 * and return the corresponding Enum type.
 *
 * @author lh  2016.06.14
 * @since Coherence 12.2.1
 */
public class EnumProcessor<T extends Enum<?>>
        extends AbstractEmptyElementProcessor<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link EnumProcessor}.
     *
     * @param clzEnum  the class type of enum
     */
    public EnumProcessor(Class<T> clzEnum)
        {
        super(EmptyElementBehavior.IGNORE);
        m_clzEnum = clzEnum;
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public T onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        for (T type : m_clzEnum.getEnumConstants())
            {
            if (type.name().equalsIgnoreCase(xmlElement.getValue().toString()))
                {
                return type;
                }
            }

        return null;
        }

    Class<T> m_clzEnum;
    }
