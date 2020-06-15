/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Copyright 2011-2014 Genson - Cepoi Eugen
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.oracle.coherence.io.json.genson.reflect;


import com.oracle.coherence.io.json.genson.stream.ValueType;

import java.util.HashMap;
import java.util.Map;

/**
 * Default type mappings.
 * <p/>
 * This class allows users to change how various {@link ValueType}
 * enum values should be mapped to Java classes during deserialization.
 *
 * @author Aleks Seovic  2018.06.05
*/
public class DefaultTypes {
    private final Map<ValueType, Class<?>> mappings = new HashMap<>();

    /**
     * Default constructor.
     */
    public DefaultTypes() {
        for (ValueType type : ValueType.values()) {
            mappings.put(type, type.toClass());
        }
    }

    /**
     * Set the class to use for deserialization of the
     * specified {@link ValueType}.
     *
     * @param type  the {@code ValueType} to set the default class for
     * @param clazz the default class for the specified {@code ValueType}
     *
     * @return this instance
     */
    public DefaultTypes setClass(ValueType type, Class<?> clazz) {
        mappings.put(type, clazz);
        return this;
    }

    /**
     * Get the class to use for deserialization of the
     * specified {@link ValueType}.
     *
     * @param type  the {@code ValueType} to get the default class for
     *
     * @return the default class for the specified {@code ValueType}
     */
    public Class<?> getClass(ValueType type) {
        return mappings.get(type);
    }
}
