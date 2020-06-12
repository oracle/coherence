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
 * Annotated Serializer/Deserializer/Converter with @HandleClassMetadata indicate that they will
 * handle @class metadata during serialization and deserialization. By default it is handled by the
 * library in {@link com.oracle.coherence.io.json.genson.convert.ClassMetadataConverter ClassMetadataConverter}. Default
 * converters from {@link com.oracle.coherence.io.json.genson.convert.DefaultConverters DefaultConverters} annotated with @HandleClassMetadata
 * do not serialize type information nor do they use it during deserialization. For security reasons
 * class metadata is disabled by default. To enable it
 * {@link com.oracle.coherence.io.json.genson.GensonBuilder#useClassMetadata(boolean)}
 * GensonBuilder.useClassMetadata(true)}
 *
 * @author Eugen Cepoi
* @see com.oracle.coherence.io.json.genson.convert.ClassMetadataConverter ClassMetadataConverter
 * @see com.oracle.coherence.io.json.genson.GensonBuilder#useClassMetadata(boolean)
 * Genson.Builder.setWithClassMetadata(true)
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface HandleClassMetadata {

}
