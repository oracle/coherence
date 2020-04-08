/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.json;

import java.util.LinkedHashMap;
import java.util.Map;

public class PutAll<K, V>
    {
    /**
     * required for default deserialization
     */
    @SuppressWarnings("unused")
    public PutAll()
        {
        }

    public PutAll(Map<K, V> map)
        {
        this.map = new LinkedHashMap<>(map);
        }

    @Override
    public boolean equals(Object o)
        {
        if (o instanceof PutAll)
            {
            PutAll that = (PutAll) o;
            return this.map.equals(that.map);
            }
        return false;
        }

    @Override
    public int hashCode()
        {
        return map == null ? 5 : map.hashCode();
        }

    public Map<K, V> map;
    }
