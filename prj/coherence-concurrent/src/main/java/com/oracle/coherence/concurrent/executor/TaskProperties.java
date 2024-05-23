/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A basic implementation of a {@link Task.Properties}.
 *
 * @param <V>  the value type of the property
 *
 * @author bo, lh
 * @since 21.06
 */
public class TaskProperties<V>
        implements Task.Properties
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link TaskProperties}.
     */
    public TaskProperties()
        {
        }

    // ----- Task.Properties: interface -------------------------------------

    @Override
    public <V extends Serializable> V get(String sKey)
        {
        if (m_properties != null)
            {
            return (V) m_properties.get(sKey);
            }
        return null;
        }

    @Override
    public <V extends Serializable> V put(String sKey, V value)
        {
        if (m_properties == null)
            {
            m_properties = new HashMap();
            }

        return (V) m_properties.put(sKey, value);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the current properties.
     *
     * @return the current properties
     */
    public Map getProperties()
        {
        return m_properties;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map that stores the properties.
     */
    protected Map<String, Object> m_properties;
    }
