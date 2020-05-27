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
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.oracle.coherence.io.json.genson.annotation.JsonProperty;

/**
 * Represents a bean property, in practice it can be an object field, method (getter/setter) or
 * constructor parameter.
 *
 * @author eugen
 */
public abstract class BeanProperty {
  protected final String name;
  protected final Type type;
  protected final Class<?> declaringClass;
  protected final Class<?> concreteClass;
  protected Annotation[] annotations;
  protected final int modifiers;

  protected BeanProperty(String name, Type type, Class<?> declaringClass,
                         Class<?> concreteClass, Annotation[] annotations, int modifiers) {
    this.name = name;
    this.type = type;
    this.declaringClass = declaringClass;
    this.concreteClass = concreteClass;
    this.annotations = annotations;
    this.modifiers = modifiers;
  }

  /**
   * @return The class in which this property is declared
   */
  public Class<?> getDeclaringClass() {
    return declaringClass;
  }

  /**
   * @return The final concrete class from which this property has been resolved.
   * For example if this property is defined in class Root but was resolved for class Child extends Root,
   * then getConcreteClass would return Child class and getDeclaringClass would return Root class.
   */
  public Class<?> getConcreteClass() { return concreteClass; }

  /**
   * The name of this property (not necessarily the original one).
   */
  public String getName() {
    return name;
  }

  /**
   * @return the type of the property
   */
  public Type getType() {
    return type;
  }

  public Class<?> getRawClass() {
    return TypeUtil.getRawClass(type);
  }

  public int getModifiers() {
    return modifiers;
  }

  public String[] aliases() {
    JsonProperty ann = getAnnotation(JsonProperty.class);
    return ann != null ? ann.aliases() : new String[]{};
  }

  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    for (Annotation ann : annotations)
      if (annotationClass.isInstance(ann)) return annotationClass.cast(ann);
    return null;
  }

  void updateBoth(BeanProperty otherBeanProperty) {

    // FIXME: we don't care for duplicate annotations as it should not change the behaviour - actually we do as it can change the behaviour...
    // an easy solution would be to forbid duplicate annotations, which can make sense.
    if (annotations.length > 0 || otherBeanProperty.annotations.length > 0) {
      Annotation[] mergedAnnotations = new Annotation[annotations.length + otherBeanProperty.annotations.length];

      System.arraycopy(annotations, 0, mergedAnnotations, 0, annotations.length);
      System.arraycopy(otherBeanProperty.annotations, 0, mergedAnnotations, annotations.length, otherBeanProperty.annotations.length);

      if (otherBeanProperty.annotations.length > 0) this.annotations = mergedAnnotations;
      // update also the other bean property with the merged result.
      // This is easier rather than do it in one direction and then in the other one.
      if (annotations.length > 0) otherBeanProperty.annotations = mergedAnnotations;
    }
  }

  /**
   * Used to give priority to implementations, for example by default a method would have a higher
   * priority than a field because it can do some logic. The greater the priority value is the
   * more important is this BeanProperty.
   *
   * @return the priority of this BeanProperty
   */
  abstract int priority();

  abstract String signature();
}
