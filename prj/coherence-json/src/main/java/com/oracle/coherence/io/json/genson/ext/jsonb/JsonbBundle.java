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

package com.oracle.coherence.io.json.genson.ext.jsonb;

import com.oracle.coherence.io.json.genson.GensonBuilder;

import com.oracle.coherence.io.json.genson.ext.GensonBundle;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;

import static com.oracle.coherence.io.json.genson.reflect.PropertyNameResolver.AnnotationPropertyNameResolver;
import static com.oracle.coherence.io.json.genson.reflect.BeanMutatorAccessorResolver.AnnotationPropertyResolver;

/**
 * Genson bundle which allows usage of JSON-B annotations for serialization.
 *
 * @author Aleksandar Seovic  2018.05.21
 */
public class JsonbBundle extends GensonBundle {
  @Override
  public void configure(GensonBuilder builder) {
    builder.with(new JsonbAnnotationPropertyResolver()).with(new JsonbAnnotationPropertyNameResolver());
  }

  private static class JsonbAnnotationPropertyResolver extends AnnotationPropertyResolver {
    public JsonbAnnotationPropertyResolver() {
      super(JsonbProperty.class, JsonbTransient.class, JsonbCreator.class);
    }
  }

  private static class JsonbAnnotationPropertyNameResolver extends AnnotationPropertyNameResolver<JsonbProperty> {
    public JsonbAnnotationPropertyNameResolver() {
      super(JsonbProperty.class);
    }

    @Override
    protected String getNameFromAnnotation(final JsonbProperty annotation) {
      return annotation.value();
    }
  }
}
