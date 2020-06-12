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


import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.oracle.coherence.io.json.genson.annotation.JsonProperty;

/**
 * This interface is intended to be implemented by classes who want to change the way genson does
 * name resolution. The resolved name will be used in the generated stream during serialization and
 * injected into constructors/or setters during deserialization. If you can not resolve the name
 * just return null. You can have a look at the <a href=
 * "http://code.google.com/p/genson/source/browse/src/main/java/com/owlike/genson/reflect/PropertyNameResolver.java"
 * >source code</a> for an example.
 *
 * @author eugen
 * @see com.oracle.coherence.io.json.genson.annotation.JsonProperty JsonProperty
 */
public interface PropertyNameResolver {
  /**
   * Resolve the parameter name on position parameterIdx in the constructor fromConstructor.
   *
   * @param parameterIdx parameter index
   * @param fromConstructor the constructor being processed
   *
   * @return the resolved name of the parameter or <code>null</code>
   */
  String resolve(int parameterIdx, Constructor<?> fromConstructor);

  /**
   * Resolve the name of the parameter with parameterIdx as index in fromMethod method.
   *
   * @param parameterIdx parameter index
   * @param fromMethod the method being processed
   *
   * @return the resolved name of the parameter or <code>null</code>
   */
  String resolve(int parameterIdx, Method fromMethod);

  /**
   * Resolve the property name from this field.
   *
   * @param fromField - the field to use for name resolution.
   * @return the resolved name or null.
   */
  String resolve(Field fromField);

  /**
   * Resolve the property name from this method.
   *
   * @param fromMethod - the method to be used for name resolution.
   * @return the resolved name or null.
   */
  String resolve(Method fromMethod);

  class CompositePropertyNameResolver implements PropertyNameResolver {
    private List<PropertyNameResolver> components;

    public CompositePropertyNameResolver(List<PropertyNameResolver> components) {
      if (components == null || components.isEmpty()) {
        throw new IllegalArgumentException(
          "The composite resolver must have at least one resolver as component!");
      }
      this.components = new LinkedList<>(components);
    }

    public CompositePropertyNameResolver add(PropertyNameResolver... resolvers) {
      // should at the head position so custom resolvers a privileged
      components.addAll(0, Arrays.asList(resolvers));
      return this;
    }

    public String resolve(int parameterIdx, Constructor<?> fromConstructor) {
      String resolvedName = null;
      for (Iterator<PropertyNameResolver> it = components.iterator(); resolvedName == null
        && it.hasNext(); ) {
        resolvedName = it.next().resolve(parameterIdx, fromConstructor);
      }
      return resolvedName;
    }

    public String resolve(int parameterIdx, Method fromMethod) {
      String resolvedName = null;
      for (Iterator<PropertyNameResolver> it = components.iterator(); resolvedName == null
        && it.hasNext(); ) {
        resolvedName = it.next().resolve(parameterIdx, fromMethod);
      }
      return resolvedName;
    }

    public String resolve(Field fromField) {
      String resolvedName = null;
      for (Iterator<PropertyNameResolver> it = components.iterator(); resolvedName == null
        && it.hasNext(); ) {
        resolvedName = it.next().resolve(fromField);
      }
      return resolvedName;
    }

    public String resolve(Method fromMethod) {
      String resolvedName = null;
      for (Iterator<PropertyNameResolver> it = components.iterator(); resolvedName == null
        && it.hasNext(); ) {
        resolvedName = it.next().resolve(fromMethod);
      }
      return resolvedName;
    }

  }

  class ConventionalBeanPropertyNameResolver implements PropertyNameResolver {

    public String resolve(int parameterIdx, Constructor<?> fromConstructor) {
      return null;
    }

    public String resolve(Field fromField) {
      return fromField.getName();
    }

    public String resolve(Method fromMethod) {
      String name = fromMethod.getName();
      int length = -1;

      if (name.startsWith("get"))
        length = 3;
      else if (name.startsWith("is"))
        length = 2;
      else if (name.startsWith("set"))
        length = 3;

      if (length > -1 && length < name.length()) {
        return Character.toLowerCase(name.charAt(length)) + name.substring(length + 1);
      } else
        return null;
    }

    public String resolve(int parameterIdx, Method fromMethod) {
      return null;
    }

  }

  /**
   * JsonProperty resolver based on @JsonProperty annotation. Can be used on fields, methods and
   * constructor parameters.
   */
  abstract class AnnotationPropertyNameResolver<A extends Annotation> implements PropertyNameResolver {
    /**
     * An {@link Annotation} that functions similarly to {@link JsonProperty}.
     */
    protected Class<A> propertyAnnotation;

    public AnnotationPropertyNameResolver(Class<A> propertyAnnotation) {
      this.propertyAnnotation = propertyAnnotation;
    }

    @SuppressWarnings("unchecked")
    public String resolve(int parameterIdx, Constructor<?> fromConstructor) {
      return getNameFromParameterAnnotations(fromConstructor.getParameterAnnotations()[parameterIdx]);
    }

    @SuppressWarnings("unchecked")
    public String resolve(int parameterIdx, Method fromMethod) {
      return getNameFromParameterAnnotations(fromMethod.getParameterAnnotations()[parameterIdx]);
    }

    public String resolve(Field fromField) {
      return getName(fromField);
    }

    public String resolve(Method fromMethod) {
      return getName(fromMethod);
    }

    protected String getName(AnnotatedElement annElement) {
      A annotation = annElement.getAnnotation(propertyAnnotation);
      if (annotation != null) {
        String name = getNameFromAnnotation(annotation);
        return "".equals(name) ? null : name;
      }
      return null;
    }

    @SuppressWarnings("unchecked")
    protected String getNameFromParameterAnnotations(Annotation[] annotations) {
      String name = null;
      for (int i = 0, len = annotations.length; i < len; i++) {
        Annotation annotation = annotations[i];
        if (propertyAnnotation.isInstance(annotation)) {
          name = getNameFromAnnotation((A) annotation);
          break;
        }
      }
      return "".equals(name) ? null : name;
    }

    protected abstract String getNameFromAnnotation(A annotation);
  }

  class GensonAnnotationPropertyNameResolver extends AnnotationPropertyNameResolver<JsonProperty> {
    public GensonAnnotationPropertyNameResolver() {
      super(JsonProperty.class);
    }

    @Override
    protected String getNameFromAnnotation(final JsonProperty annotation) {
      return annotation.value();
    }
  }
}
