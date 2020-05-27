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

package com.oracle.coherence.io.json.genson.convert;


import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.reflect.BeanProperty;

/**
 * <b>Beta feature</b>
 * <br/>
 * Create method signature and BeanProperty might change in the future.
 * Allows to create a converter for some type T based on bean property available at compile time
 * (ex: you can not use it with map keys because they exist only at runtime). This feature works
 * only for POJO databinding, in could be improved implying some refactoring.
 *
 * @param <T> the type of objects handled by Converters built by this factory
 * @author eugen
 */
public interface ContextualFactory<T> {
  /**
   * Return an instance of a converter working with objects of type T based on property argument
   * or null.
   */
  public Converter<T> create(BeanProperty property, Genson genson);
}
