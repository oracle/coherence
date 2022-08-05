/*
 * Copyright (c) 2019, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package common.data;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.json.bind.annotation.JsonbProperty;


public class PutAll<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * required for default deserialization
     */
    @SuppressWarnings("unused")
    public PutAll()
        {
        }

    public PutAll(Map<K, V> map)
        {
        this.m_map = new LinkedHashMap<>(map);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (o instanceof PutAll)
            {
            PutAll that = (PutAll) o;
            return this.m_map.equals(that.m_map);
            }
        return false;
        }

    @Override
    public int hashCode()
        {
        return m_map == null ? 5 : m_map.hashCode();
        }

    // ----- data members ---------------------------------------------------

    @JsonbProperty("map")
    public Map<K, V> m_map;
    }
