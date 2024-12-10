/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import java.util.Map;
import java.util.Properties;

/**
 * A {@link PropertiesParameterResolver} is a {@link ParameterResolver} that is
 * based on a {@link Properties} instance.
 *
 * @author bo  2011.06.22
 * @since Coherence 12.1.2
 */
public class PropertiesParameterResolver
        implements ParameterResolver
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link PropertiesParameterResolver} using a {@link Map}
     * of values.
     * 
     * @param map  a {@link Map} of Strings to Strings defining the properties
     *                (the map values are copied)
     */
    public PropertiesParameterResolver(Map<String, String> map) 
        {
        m_properties = new Properties();
        if (map != null) 
            {
            for (String key : map.keySet()) 
                {
                m_properties.put(key,  map.get(key));
                }
            }
        }
    
    // ----- ParameterResolver interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameter resolve(String sName)
        {
        String sValue;

        try
            {
            sValue = m_properties.getProperty(sName);
            }
        catch (SecurityException e)
            {
            sValue = null;
            }

        if (sValue == null)
            {
            return null;
            }
        else
            {
            return new Parameter(sName, sValue);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Properties} from which to resolve {@link Parameter}s.
     */
    private Properties m_properties;
    }
