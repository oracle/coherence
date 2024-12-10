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

package com.oracle.coherence.io.json.genson.ext.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.oracle.coherence.io.json.genson.ext.GensonBundle;

import com.oracle.coherence.io.json.genson.reflect.BeanMutatorAccessorResolver.AnnotationPropertyResolver;
import com.oracle.coherence.io.json.genson.reflect.PropertyNameResolver.AnnotationPropertyNameResolver;

/**
 * Genson bundle which allows usage of Jackson annotations for serialization.
 *
 * @author Aleks Seovic  2018.05.21
*/
public class JacksonBundle extends GensonBundle {
    @Override
    public void configure(GensonBuilder builder) {
        builder.with(new JacksonAnnotationPropertyResolver()).with(new JacksonAnnotationPropertyNameResolver());
    }

    private static class JacksonAnnotationPropertyResolver extends AnnotationPropertyResolver {
      public JacksonAnnotationPropertyResolver() {
        super(JsonProperty.class, JsonIgnore.class, JsonCreator.class);
      }
    }

    private static class JacksonAnnotationPropertyNameResolver extends AnnotationPropertyNameResolver<JsonProperty> {
      public JacksonAnnotationPropertyNameResolver() {
        super(JsonProperty.class);
      }

      @Override
      protected String getNameFromAnnotation(final JsonProperty annotation) {
        return annotation.value();
      }
    }
}
