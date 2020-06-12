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


import com.oracle.coherence.io.json.genson.Context;
import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.JsonBindingException;
import com.oracle.coherence.io.json.genson.ext.jsr353.JSR353Bundle;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

import javax.json.JsonValue;

import java.util.Map;
import java.util.function.Consumer;

/**
 * An implementation of an {@link UnknownPropertyHandler} that supports
 * evolution of data classes via {@link Evolvable} interface.
 * <p>
 * If the target object we are deserializing into is {@link Evolvable},
 * this handler will add any unknown properties encountered during
 * deserialization into {@link Evolvable#unknownProperties()} map,
 * and will write them out along with all known properties during
 * subsequent serialization.
 * <p>
 * This prevents data loss when serializing and deserializing the same
 * JSON payload using different versions of Java data classes.
 *
 * @author Aleks Seovic  2018.05.20
*/
public class EvolvableHandler implements UnknownPropertyHandler {
    private static final Converter<JsonValue> CONVERTER = new JSR353Bundle.JsonValueConverter();

    @Override
    public <T> Consumer<T> readUnknownProperty(String propName, ObjectReader reader, Context ctx) {
        try {
            JsonValue propValue = CONVERTER.deserialize(reader, ctx);

            return objTarget -> {
                if (objTarget instanceof Evolvable) {
                    ((Evolvable) objTarget).addUnknownProperty(propName, propValue);
                }
            };
        } catch (Exception e) {
            throw new JsonBindingException(e);
        }
    }

    @Override
    public <T> void writeUnknownProperties(T bean, ObjectWriter writer, Context ctx) {
        try {
            if (bean instanceof Evolvable) {
                Map<String, JsonValue> props = ((Evolvable) bean).unknownProperties();
                if (props != null) {
                    for (String propName : props.keySet()) {
                        writer.writeName(propName);
                        CONVERTER.serialize(props.get(propName), writer, ctx);
                    }
                }
            }
        } catch (Exception e) {
            throw new JsonBindingException(e);
        }
    }
}