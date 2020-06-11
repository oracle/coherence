/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.mp.config;


import com.tangosol.util.MapEvent;


/**
 * An event that will be raised whenever any config property is added to,
 * updated in, or removed from the {@link CoherenceConfigSource}.
 *
 * @author Aleks Seovic  2020.06.11
 */
public class ConfigPropertyChanged
    {
    ConfigPropertyChanged(MapEvent<String, String> event)
        {
        m_sKey      = event.getKey();
        m_sValue    = event.getNewValue();
        m_sOldValue = event.getOldValue();
        }

    // ---- accessors -------------------------------------------------------

    /**
     * The key of the configuration property that was modified.
     *
     * @return the key of the configuration property that was modified
     */
    public String getKey()
        {
        return m_sKey;
        }

    /**
     * The new value of the configuration property that was modified.
     *
     * @return the new value of the configuration property that was modified;
     *         can be {@code null} if the property was removed, or the value
     *         explicitly set to {@code null}
     */
    public String getValue()
        {
        return m_sValue;
        }

    /**
     * The old value of the configuration property that was modified.
     *
     * @return the old value of the configuration property that was modified;
     *         can be {@code null} if the property was inserted, or the previous
     *         value was explicitly set to {@code null}
     */
    public String getOldValue()
        {
        return m_sOldValue;
        }

    // ---- Object methods --------------------------------------------------

    public String toString()
        {
        return "ConfigPropertyChanged{" +
               "key='" + m_sKey + '\'' +
               ", value='" + m_sValue + '\'' +
               ", oldValue='" + m_sOldValue + '\'' +
               '}';
        }

    // ---- data members ----------------------------------------------------

    private final String m_sKey;
    private final String m_sValue;
    private final String m_sOldValue;
    }
