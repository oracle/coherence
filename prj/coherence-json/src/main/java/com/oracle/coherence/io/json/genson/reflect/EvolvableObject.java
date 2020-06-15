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


import com.oracle.coherence.io.json.genson.annotation.JsonIgnore;

import javax.json.JsonValue;
import java.util.HashMap;
import java.util.Map;

/**
 * Convenience base class for {@link Evolvable} data classes.
 *
 * @author Aleks Seovic  2018.05.20
*/
public abstract class EvolvableObject implements Evolvable {
    @JsonIgnore
    private Map<String, JsonValue> unknownProperties;

    @Override
    public void addUnknownProperty(String propName, JsonValue propValue) {
        if (unknownProperties == null) {
            unknownProperties = new HashMap<>();
        }
        unknownProperties.put(propName, propValue);
    }

    @Override
    public Map<String, JsonValue> unknownProperties() {
        return unknownProperties;
    }
}
