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

package com.oracle.coherence.io.json.genson.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JsonProperty annotation can be used to define the name of a property. You can apply it on fields
 * and methods. In that case this name will be used instead of the conventional one computed from
 * the signature. You can also use this annotation on parameters of creator methods and on
 * constructor parameters. In that case Genson during deserialization will try to use those names to
 * match the properties from the json stream. By default it is used in
 * {@link com.oracle.coherence.io.json.genson.reflect.PropertyNameResolver.AnnotationPropertyNameResolver
 * AnnotationPropertyNameResolver}.
 *
 * @author eugen
 * @see com.oracle.coherence.io.json.genson.reflect.PropertyNameResolver.AnnotationPropertyNameResolver
 * AnnotationPropertyNameResolver
 * @see com.oracle.coherence.io.json.genson.annotation.JsonCreator JsonCreator
 * @see com.oracle.coherence.io.json.genson.annotation.JsonIgnore JsonIgnore
 */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface JsonProperty {
  /**
   * The name of that property.
   */
  String value() default "";

  /**
   * A list of aliases to use during deserialization for this property. Note that during serialization this is not used.
   */
  String[] aliases() default {};

  /**
   * Whether this property must be serialized. Default is true, the property will be serialized.
   */
  boolean serialize() default true;

  /**
   * Whether this property must be deserialized. Default is true, the property will be
   * deserialized.
   */
  boolean deserialize() default true;
}
