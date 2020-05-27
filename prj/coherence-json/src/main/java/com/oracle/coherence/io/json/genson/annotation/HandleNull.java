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
 * Similar to {@link HandleClassMetadata}, put this annotation on your Converters, Serializers and
 * Deserializers to disable Genson default null handling (
 * {@link com.oracle.coherence.io.json.genson.convert.NullConverter NullConverter}). In that case you will have to
 * write the code that handles nulls during serialization and deserialization of your type (and not
 * of its content). This feature is mainly for internal use.
 *
 * @author eugen
 * @see HandleClassMetadata
 * @see com.oracle.coherence.io.json.genson.convert.NullConverter NullConverter
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface HandleNull {

}
