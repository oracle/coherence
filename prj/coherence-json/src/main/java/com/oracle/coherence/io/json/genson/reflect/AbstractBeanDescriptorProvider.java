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


import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.oracle.coherence.io.json.genson.Converter;
import com.oracle.coherence.io.json.genson.Factory;
import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.ThreadLocalHolder;
import com.oracle.coherence.io.json.genson.convert.ContextualFactory;

import static com.oracle.coherence.io.json.genson.reflect.TypeUtil.*;

/**
 * Abstract implementation of {@link BeanDescriptorProvider} applying the template pattern.
 * <p/>
 * If you wonder how to implement the different abstract methods defined in this class have a look
 * at the <a href=
 * "http://code.google.com/p/genson/source/browse/src/main/java/com/owlike/genson/reflect/BaseBeanDescriptorProvider.java"
 * >BaseBeanDescriptorProvider</a>.
 *
 * @author eugen
 */
public abstract class AbstractBeanDescriptorProvider implements BeanDescriptorProvider {
  final static String CONTEXT_KEY = "__GENSON$CREATION_CONTEXT";
  final static String DO_NOT_CACHE_CONVERTER_KEY = "__GENSON$DO_NOT_CACHE_CONVERTER";

  public final static class ContextualConverterFactory {
    private final List<? extends ContextualFactory<?>> contextualFactories;

    public ContextualConverterFactory(List<? extends ContextualFactory<?>> contextualFactories) {
      this.contextualFactories = contextualFactories != null ? new ArrayList<ContextualFactory<?>>(
        contextualFactories) : new ArrayList<ContextualFactory<?>>();
    }

    Converter<?> provide(BeanProperty property, Genson genson) {
      Type type = property.getType();
      for (Iterator<? extends ContextualFactory<?>> it = contextualFactories.iterator(); it
        .hasNext(); ) {
        ContextualFactory<?> factory = it.next();
        Converter<?> object = null;
        Type factoryType = lookupGenericType(ContextualFactory.class, factory.getClass());
        factoryType = expandType(factoryType, factory.getClass());
        Type factoryParameter = typeOf(0, factoryType);

        if (type instanceof Class<?> && ((Class<?>) type).isPrimitive())
          type = wrap((Class<?>) type);

        if (match(type, factoryParameter, false)
          && (object = factory.create(property, genson)) != null) {
          return object;
        }
      }
      return null;
    }
  }

  public final static class ContextualFactoryDecorator implements Factory<Converter<?>> {
    private final Factory<Converter<?>> delegatedFactory;

    public ContextualFactoryDecorator(Factory<Converter<?>> delegatedFactory) {
      this.delegatedFactory = delegatedFactory;
    }

    @Override
    public Converter<?> create(Type type, Genson genson) {
      Converter<?> converter = ThreadLocalHolder.get(CONTEXT_KEY, Converter.class);
      if (converter != null) return converter;
      return delegatedFactory.create(type, genson);
    }
  }

  private final ContextualConverterFactory contextualConverterFactory;

  protected AbstractBeanDescriptorProvider(ContextualConverterFactory contextualConverterFactory) {
    this.contextualConverterFactory = contextualConverterFactory;
  }

  @Override
  public <T> BeanDescriptor<T> provide(Class<T> type, Genson genson) {
    return provide(type, type, genson);
  }

  @Override
  public <T> BeanDescriptor<T> provide(Class<T> ofClass, Type ofType, Genson genson) {
    Map<String, LinkedList<PropertyMutator>> mutatorsMap = new LinkedHashMap<String, LinkedList<PropertyMutator>>();
    Map<String, LinkedList<PropertyAccessor>> accessorsMap = new LinkedHashMap<String, LinkedList<PropertyAccessor>>();

    List<BeanCreator> creators = provideBeanCreators(ofType, genson);

    provideBeanPropertyAccessors(ofType, accessorsMap, genson);
    provideBeanPropertyMutators(ofType, mutatorsMap, genson);

    List<PropertyAccessor> accessors = new ArrayList<PropertyAccessor>(accessorsMap.size());
    for (Map.Entry<String, LinkedList<PropertyAccessor>> entry : accessorsMap.entrySet()) {
      PropertyAccessor accessor = checkAndMergeAccessors(entry.getKey(), entry.getValue());
      // in case of...
      if (accessor != null) accessors.add(accessor);
    }

    Map<String, PropertyMutator> mutators = new HashMap<String, PropertyMutator>(mutatorsMap.size());
    for (Map.Entry<String, LinkedList<PropertyMutator>> entry : mutatorsMap.entrySet()) {
      PropertyMutator mutator = checkAndMergeMutators(entry.getKey(), entry.getValue());
      if (mutator != null) mutators.put(mutator.name, mutator);
    }

    BeanCreator ctr = checkAndMerge(ofType, creators);
    if (ctr != null) mergeAccessorsWithCreatorProperties(ofType, accessors, ctr);
    if (ctr != null) mergeMutatorsWithCreatorProperties(ofType, mutators, ctr);

    // 1 - prepare the converters for the accessors
    for (PropertyAccessor accessor : accessors) {
      accessor.propertySerializer = provide(accessor, genson);
    }

    // 2 - prepare the mutators
    for (PropertyMutator mutator : mutators.values()) {
      mutator.propertyDeserializer = provide(mutator, genson);
    }

    // 3 - prepare the converters for creator parameters
    if (ctr != null) {
      for (PropertyMutator mutator : ctr.parameters.values()) {
        mutator.propertyDeserializer = provide(mutator, genson);
      }
    }

    for (PropertyMutator p : mutators.values()) {
      for (String alias : p.aliases()) mutators.put(alias, p);
    }

    // lets fail fast if the BeanDescriptor has been built for the wrong type.
    // another option could be to pass in all the methods an additional parameter Class<T> that
    // would not necessarily correspond to the rawClass of ofType. In fact we authorize that
    // ofType rawClass is different from Class<T>, but the BeanDescriptor must match!
    BeanDescriptor<T> descriptor = create(ofClass, ofType, ctr, accessors, mutators, genson);
    if (!ofClass.isAssignableFrom(descriptor.getOfClass()))
      throw new ClassCastException("Actual implementation of BeanDescriptorProvider "
        + getClass()
        + " seems to do something wrong. Expected BeanDescriptor for type " + ofClass
        + " but provided BeanDescriptor for type " + descriptor.getOfClass());
    return descriptor;
  }

  private Converter<Object> provide(BeanProperty property, Genson genson) {
    // contextual converters must not be retrieved from cache nor stored in cache, by first
    // trying to create it and reusing it during the
    // call to genson.provideConverter we avoid retrieving it from cache, and by setting
    // DO_NOT_CACHE_CONVERTER to true we tell genson not to store
    // this converter in cache

    @SuppressWarnings("unchecked")
    Converter<Object> converter = (Converter<Object>) contextualConverterFactory.provide(
      property, genson);
    if (converter != null) {
      ThreadLocalHolder.store(DO_NOT_CACHE_CONVERTER_KEY, true);
      ThreadLocalHolder.store(CONTEXT_KEY, converter);
    }
    try {
      return genson.provideConverter(property.type);
    } finally {
      if (converter != null) {
        ThreadLocalHolder.remove(DO_NOT_CACHE_CONVERTER_KEY, Boolean.class);
        ThreadLocalHolder.remove(CONTEXT_KEY, Converter.class);
      }
    }
  }

  /**
   * Creates an instance of BeanDescriptor based on the passed arguments. Subclasses can override
   * this method to create their own BeanDescriptors.
   *
   * @param forClass
   * @param ofType
   * @param creator
   * @param accessors
   * @param mutators
   * @return a instance
   */
  protected <T> BeanDescriptor<T> create(Class<T> forClass, Type ofType, BeanCreator creator,
                                         List<PropertyAccessor> accessors, Map<String, PropertyMutator> mutators,
                                         Genson genson) {
    return new BeanDescriptor<T>(forClass, getRawClass(ofType), accessors, mutators, creator, genson.failOnMissingProperty());
  }

  /**
   * Provides a list of {@link BeanCreator} for type ofType.
   *
   * @param ofType
   * @param genson
   * @return a list of resolved bean creators
   */
  protected abstract List<BeanCreator> provideBeanCreators(Type ofType, Genson genson);

  /**
   * Adds resolved {@link PropertyMutator} to mutatorsMap.
   *
   * @param ofType
   * @param mutatorsMap
   * @param genson
   */
  protected abstract void provideBeanPropertyMutators(Type ofType,
                                                      Map<String, LinkedList<PropertyMutator>> mutatorsMap, Genson genson);

  /**
   * Adds resolved {@link PropertyAccessor} to accessorsMap.
   *
   * @param ofType
   * @param accessorsMap
   * @param genson
   */
  protected abstract void provideBeanPropertyAccessors(Type ofType,
                                                       Map<String, LinkedList<PropertyAccessor>> accessorsMap, Genson genson);

  /**
   * Implementations of this method can do some additional checks on the creators validity or do
   * any other operations related to creators. This method must merge all creators into a single
   * one.
   *
   * @param creators
   * @return the creator that will be used by the BeanDescriptor
   */
  protected abstract BeanCreator checkAndMerge(Type ofType, List<BeanCreator> creators);

  /**
   * Implementations are supposed to merge the {@link PropertyMutator}s from mutators list into a
   * single PropertyMutator.
   *
   * @param name
   * @param mutators
   * @return a single PropertyMutator or null.
   */
  protected abstract PropertyMutator checkAndMergeMutators(String name,
                                                           LinkedList<PropertyMutator> mutators);

  /**
   * Implementations may do additional merge operations based on the resolved creator
   * parameters and the resolved mutators.
   *
   * @param ofType
   * @param mutators
   * @param creator
   */
  protected abstract void mergeMutatorsWithCreatorProperties(Type ofType, Map<String, PropertyMutator> mutators, BeanCreator creator);

  /**
   * Implementations may do additional merge operations based on the resolved creator
   * parameters and the resolved accessors.
   *
   * @param ofType
   * @param accessors
   * @param creator
   */
  protected abstract void mergeAccessorsWithCreatorProperties(Type ofType, List<PropertyAccessor> accessors, BeanCreator creator);

  /**
   * Implementations are supposed to merge the {@link PropertyAccessor}s from accessors list into
   * a single PropertyAccessor.
   *
   * @param name
   * @param accessors
   * @return a single property accessor for this name
   */
  protected abstract PropertyAccessor checkAndMergeAccessors(String name,
                                                             LinkedList<PropertyAccessor> accessors);
}
