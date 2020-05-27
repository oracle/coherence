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

package com.oracle.coherence.io.json.genson.ext;

import com.oracle.coherence.io.json.genson.GensonBuilder;
import com.oracle.coherence.io.json.genson.reflect.AbstractBeanDescriptorProvider.ContextualConverterFactory;
import com.oracle.coherence.io.json.genson.reflect.BeanDescriptorProvider;
import com.oracle.coherence.io.json.genson.reflect.BeanMutatorAccessorResolver;
import com.oracle.coherence.io.json.genson.reflect.BeanPropertyFactory;
import com.oracle.coherence.io.json.genson.reflect.PropertyNameResolver;

/**
 * Bundles allow to package all kind of Genson customizations into a single module and register
 * them all together. Extensions are registered using Genson.Builder.
 * <p/>
 * <pre>
 * Genson genson = new GensonBuilder().with(new SuperCoolExtension()).create();
 * </pre>
 * <p/>
 * Extension configuration is mixed with user custom configuration (no way to distinguish them),
 * however user custom config. has preference over bundle configuration. This means that you can
 * override bundle configuration with custom one.
 * <p/>
 *
 * <b>Important note, bundles must be registered after any other configuration.</b>
 *
 * This part of the API is still in beta, it could change in the future in order to make it more
 * powerful.
 *
 * @author eugen
 */
public abstract class GensonBundle {
  /**
   * This method does not provide any guarantee to when it is called: before user config, during,
   * or after. Thus it should not rely on accessor methods from GensonBuilder they might not reflect
   * the final configuration. Use the builder to register your components.
   */
  public abstract void configure(GensonBuilder builder);

  public BeanDescriptorProvider createBeanDescriptorProvider(ContextualConverterFactory contextualConverterFactory,
                                                             BeanPropertyFactory propertyFactory,
                                                             BeanMutatorAccessorResolver propertyResolver,
                                                             PropertyNameResolver nameResolver,
                                                             GensonBuilder builder) {
    return null;
  }
}
