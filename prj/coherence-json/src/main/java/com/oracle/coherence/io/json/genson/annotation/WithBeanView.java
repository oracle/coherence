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

import com.oracle.coherence.io.json.genson.BeanView;

/**
 * Annotation used actually only in spring web integration
 * {@link com.oracle.coherence.io.json.genson.ext.spring.GensonMessageConverter GensonMessageConverter} to indicate
 * at runtime what BeanView must be used. Its intended to be used in conjunction
 * with springs @ResponseBody/@RequestBody and @RequestMapping annotations.
 *
 * @author Eugen Cepoi
*/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface WithBeanView {
  Class<? extends BeanView<?>>[] views() default {};
}
