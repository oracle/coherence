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

package com.oracle.coherence.io.json.genson;


import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

import com.oracle.coherence.io.json.genson.reflect.TypeUtil;


/**
 * Wrapper class must be extended by decorated converters that wrap other converters. This allows to
 * access merged class information of wrapped converter and the converter itself. So instead of
 * doing myObject.getClass().isAnnotationPresent(..) you will do myObject.isAnnotationPresent(..),
 * where myObject is an instance of Wrapper. For example to check if a converter (or any another
 * encapsulated converter and so on) has annotation @HandleNull you will do it that way:
 * <p/>
 * <pre>
 * Wrapper.toAnnotatedElement(converter).isAnnotationPresent(HandleNull.class);
 * </pre>
 * <p/>
 * In the future there may be other methods to access other kind of class information.
 *
 * @author Eugen Cepoi
*/
public abstract class Wrapper<T> implements AnnotatedElement {
  private AnnotatedElement wrappedElement;
  protected volatile T wrapped;

  protected Wrapper() {
  }

  protected Wrapper(T wrappedObject) {
    if (wrappedObject == null)
      throw new IllegalArgumentException("Null not allowed!");
    decorate(wrappedObject);
  }

  public Annotation[] getAnnotations() {
    return Operations.union(Annotation[].class, wrappedElement.getAnnotations(), getClass()
      .getAnnotations());
  }

  public <A extends Annotation> A getAnnotation(Class<A> aClass) {
    A ann = wrappedElement.getAnnotation(aClass);
    return ann == null ? getClass().getAnnotation(aClass) : ann;
  }

  public Annotation[] getDeclaredAnnotations() {
    return Operations.union(Annotation[].class, wrappedElement.getDeclaredAnnotations(),
      getClass().getDeclaredAnnotations());
  }

  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    return wrappedElement.isAnnotationPresent(annotationClass)
      || getClass().isAnnotationPresent(annotationClass);
  }

  // package visibility as a convenience for CircularClassReferenceConverter
  protected void decorate(T object) {
    if (wrappedElement != null)
      throw new IllegalStateException("An object is already wrapped!");
    if (object instanceof AnnotatedElement)
      this.wrappedElement = (AnnotatedElement) object;
    else
      this.wrappedElement = object.getClass();
    this.wrapped = object;
  }

  public T unwrap() {
    return wrapped;
  }

  /**
   * This method acts as an adapter to AnnotatedElement, use it when you need to work on a
   * converter annotations. In fact "object" argument will usually be of type converter. If this
   * class is a wrapper than it will cast it to annotatedElement (as Wrapper implements
   * AnnotatedElement). Otherwise we will return the class of this object.
   *
   * @param object may be an instance of converter for example
   * @return an annotatedElement that allows us to get annotations from this object and it's
   * wrapped classes if it is a Wrapper.
   */
  public static AnnotatedElement toAnnotatedElement(Object object) {
    if (object == null)
      return null;
    if (isWrapped(object))
      return (AnnotatedElement) object;
    else
      return object.getClass();
  }

  public static boolean isWrapped(Object object) {
    return object instanceof Wrapper;
  }

  /**
   * @return true if this object or its wrapped object (if the object extends Wrapper) is of type clazz.
   */
  public static boolean isOfType(Object object, Class<?> clazz) {
    return TypeUtil.match(object.getClass(), clazz, false) ||
             (isWrapped(object) && Wrapper.isOfType(((Wrapper<?>) object).unwrap(), clazz));
  }
}
