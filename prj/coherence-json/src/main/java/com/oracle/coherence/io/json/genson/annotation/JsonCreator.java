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


import java.lang.annotation.*;

/**
 * Static methods annotated with @JsonCreator annotation will act as method factories. These methods can
 * take arguments that match properties from the json stream. If you use default configuration you
 * must annotate each argument with {@link com.oracle.coherence.io.json.genson.annotation.JsonProperty JsonProperty} and
 * define a name.
 * <p/>
 * By default if a object contains constructors and methods annotated with @JsonCreator the factory
 * methods will be privileged.
 *
 * @author eugen
 * @see com.oracle.coherence.io.json.genson.annotation.JsonProperty JsonProperty
 * @see com.oracle.coherence.io.json.genson.reflect.BeanMutatorAccessorResolver.StandardMutaAccessorResolver
 * StandardMutaAccessorResolver
 */
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface JsonCreator {
}
