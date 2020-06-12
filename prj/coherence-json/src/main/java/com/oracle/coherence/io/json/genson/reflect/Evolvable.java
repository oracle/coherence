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


import javax.json.JsonValue;
import java.util.Map;

/**
 * An interface that can be implemented by data classes
 * in order to support schema evolution.
 * <p>
 * This interface is used in combination with {@link EvolvableHandler}
 * in order to prevent data loss during serialization across different
 * versions of data classes.
 *
 * @author Aleks Seovic  2018.05.20
*/
public interface Evolvable {
    /**
     * Add unknown property to this instance.
     *
     * @param propName  property name
     * @param propValue property value
     */
    void addUnknownProperty(String propName, JsonValue propValue);

    /**
     * Return a map of unknown properties.
     *
     * @return a map of unknown properties
     */
    Map<String, JsonValue> unknownProperties();
}
