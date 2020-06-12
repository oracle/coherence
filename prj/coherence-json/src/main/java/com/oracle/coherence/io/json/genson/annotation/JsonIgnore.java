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
 * You can annotate with @JsonIgnore the methods, fields and creators that must be ignored during
 * serialization AND deserialization. To exclude property from only deserialization and keep it
 * during serialization use @JsonIgnore(serialize=true), for example if you annotate a field with
 * &#64;JsonIgnore(serialize=true,deserialize=true) it will have no effect!
 *
 * @author Eugen Cepoi
* @see com.oracle.coherence.io.json.genson.annotation.JsonProperty JsonProperty
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface JsonIgnore {
  /**
   * Whether to include this property in serialization. False by default, the property won't be
   * serialized.
   */
  boolean serialize() default false;

  /**
   * Whether to include this property in deserialization. False by default, the property won't be
   * deserialized.
   */
  boolean deserialize() default false;
}
