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


import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.annotation.JsonCreator;
import com.oracle.coherence.io.json.genson.reflect.BeanCreator.BeanCreatorProperty;

import static com.oracle.coherence.io.json.genson.reflect.TypeUtil.*;
import static com.oracle.coherence.io.json.genson.Trilean.*;

/**
 * Standard implementation of AbstractBeanDescriptorProvider that uses
 * {@link BeanMutatorAccessorResolver} and {@link PropertyNameResolver}. If you want to change the
 * way BeanDescriptors are created you can subclass this class and override the needed methods. If
 * you only want to create instances of your own PropertyMutators/PropertyAccessors or BeanCreators
 * just override the corresponding createXXX methods.
 *
 * @author eugen
 */
public class BaseBeanDescriptorProvider extends AbstractBeanDescriptorProvider {

  private final static Comparator<BeanCreator> _beanCreatorsComparator = new Comparator<BeanCreator>() {
    public int compare(BeanCreator o1, BeanCreator o2) {
      return o1.parameters.size() - o2.parameters.size();
    }
  };

  private final BeanPropertyFactory propertyFactory;
  protected final BeanMutatorAccessorResolver mutatorAccessorResolver;
  protected final PropertyNameResolver nameResolver;
  protected final boolean useGettersAndSetters;
  protected final boolean useFields;
  protected final boolean favorEmptyCreators;

  public BaseBeanDescriptorProvider(ContextualConverterFactory ctxConverterFactory, BeanPropertyFactory propertyFactory,
                                    BeanMutatorAccessorResolver mutatorAccessorResolver, PropertyNameResolver nameResolver,
                                    boolean useGettersAndSetters, boolean useFields, boolean favorEmptyCreators) {
    super(ctxConverterFactory);

    if (mutatorAccessorResolver == null)
      throw new IllegalArgumentException("mutatorAccessorResolver must be not null!");
    if (nameResolver == null)
      throw new IllegalArgumentException("nameResolver must be not null!");
    if (propertyFactory == null)
      throw new IllegalArgumentException("propertyFactory must be not null!");

    this.propertyFactory = propertyFactory;
    this.mutatorAccessorResolver = mutatorAccessorResolver;
    this.nameResolver = nameResolver;
    this.useFields = useFields;
    this.useGettersAndSetters = useGettersAndSetters;
    if (!useFields && !useGettersAndSetters)
      throw new IllegalArgumentException("You must allow at least one mode: with fields or methods.");
    this.favorEmptyCreators = favorEmptyCreators;
  }

  @Override
  public List<BeanCreator> provideBeanCreators(Type ofType, Genson genson) {
    List<BeanCreator> creators = new ArrayList<BeanCreator>();
    Class<?> ofClass = getRawClass(ofType);
    if (ofClass.isMemberClass() && (ofClass.getModifiers() & Modifier.STATIC) == 0)
      return creators;

    provideConstructorCreators(ofType, creators, genson);
    for (Class<?> clazz = ofClass; clazz != null && !Object.class.equals(clazz); clazz = clazz
      .getSuperclass()) {
      provideMethodCreators(clazz, creators, ofType, genson);
    }
    return creators;
  }

  @Override
  public void provideBeanPropertyAccessors(Type ofType,
                                           Map<String, LinkedList<PropertyAccessor>> accessorsMap, Genson genson) {
    ArrayDeque<Class<?>> classesToInspect = new ArrayDeque<Class<?>>();
    classesToInspect.push(getRawClass(ofType));
    while (!classesToInspect.isEmpty()) {
      Class<?> clazz = classesToInspect.pop();
      if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
        classesToInspect.push(clazz.getSuperclass());
      }
      for (Class<?> anInterface : clazz.getInterfaces()) classesToInspect.push(anInterface);

      // first lookup for fields
      if (useFields) provideFieldAccessors(clazz, accessorsMap, ofType, genson);
      // and now search methods (getters)
      if (useGettersAndSetters) provideMethodAccessors(clazz, accessorsMap, ofType, genson);
    }
  }

  @Override
  public void provideBeanPropertyMutators(Type ofType,
                                          Map<String, LinkedList<PropertyMutator>> mutatorsMap, Genson genson) {
    ArrayDeque<Class<?>> classesToInspect = new ArrayDeque<Class<?>>();
    classesToInspect.push(getRawClass(ofType));
    while (!classesToInspect.isEmpty()) {
      Class<?> clazz = classesToInspect.pop();
      if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class) {
        classesToInspect.push(clazz.getSuperclass());
      }
      for (Class<?> anInterface : clazz.getInterfaces()) classesToInspect.push(anInterface);

      // first lookup for fields
      if (useFields) provideFieldMutators(clazz, mutatorsMap, ofType, genson);
      // and now search methods (getters)
      if (useGettersAndSetters) provideMethodMutators(clazz, mutatorsMap, ofType, genson);
    }
  }

  protected void provideConstructorCreators(Type ofType, List<BeanCreator> creators, Genson genson) {
    Class<?> ofClass = getRawClass(ofType);
    Constructor<?>[] ctrs = ofClass.getDeclaredConstructors();
    for (Constructor<?> ctr : ctrs) {
      if (TRUE == mutatorAccessorResolver.isCreator(ctr, ofClass)) {
        boolean annotated = mutatorAccessorResolver.isCreatorAnnotated(ctr);
        Type[] parameterTypes = ctr.getGenericParameterTypes();
        int paramCnt = parameterTypes.length;
        String[] parameterNames = new String[paramCnt];
        int idx = 0;
        for (; idx < paramCnt; idx++) {
          String name = nameResolver.resolve(idx, ctr);
          if (name == null) break;
          parameterNames[idx] = name;
        }

        if (idx == paramCnt) {
          BeanCreator creator = propertyFactory.createCreator(ofType, ctr, parameterNames, annotated, genson);
          creators.add(creator);
        }
      }
    }
  }

  protected void provideMethodCreators(Class<?> ofClass, List<BeanCreator> creators, Type ofType,
                                       Genson genson) {
    Method[] ctrs = ofClass.getDeclaredMethods();
    for (Method ctr : ctrs) {
      if (TRUE == mutatorAccessorResolver.isCreator(ctr, getRawClass(ofType))) {
        boolean annotated = mutatorAccessorResolver.isCreatorAnnotated(ctr);
        Type[] parameterTypes = ctr.getGenericParameterTypes();
        int paramCnt = parameterTypes.length;
        String[] parameterNames = new String[paramCnt];
        int idx = 0;
        for (; idx < paramCnt; idx++) {
          String name = nameResolver.resolve(idx, ctr);
          if (name == null) break;
          parameterNames[idx] = name;
        }

        if (idx == paramCnt) {
          BeanCreator creator = propertyFactory.createCreator(ofType, ctr, parameterNames, annotated, genson);
          creators.add(creator);
        }
      }
    }
  }

  protected void provideFieldAccessors(Class<?> ofClass,
                                       Map<String, LinkedList<PropertyAccessor>> accessorsMap, Type ofType, Genson genson) {
    Field[] fields = ofClass.getDeclaredFields();
    for (Field field : fields) {
      if (TRUE == mutatorAccessorResolver.isAccessor(field, getRawClass(ofType))) {
        String name = nameResolver.resolve(field);
        if (name == null) {
          throw new IllegalStateException("Field '" + field.getName() + "' from class "
            + ofClass.getName()
            + " has been discovered as accessor but its name couldn't be resolved!");
        }
        PropertyAccessor accessor = propertyFactory.createAccessor(name, field, ofType, genson);
        update(accessor, accessorsMap);
      }
    }
  }

  protected void provideMethodAccessors(Class<?> ofClass,
                                        Map<String, LinkedList<PropertyAccessor>> accessorsMap, Type ofType, Genson genson) {
    Method[] methods = ofClass.getDeclaredMethods();
    for (Method method : methods) {
      if (TRUE == mutatorAccessorResolver.isAccessor(method, getRawClass(ofType))) {
        String name = nameResolver.resolve(method);
        if (name == null) {
          throw new IllegalStateException("Method '" + method.getName() + "' from class "
            + ofClass.getName()
            + " has been discovered as accessor but its name couldn't be resolved!");
        }
        PropertyAccessor accessor = propertyFactory.createAccessor(name, method, ofType, genson);
        update(accessor, accessorsMap);
      }
    }
  }

  protected void provideFieldMutators(Class<?> ofClass,
                                      Map<String, LinkedList<PropertyMutator>> mutatorsMap, Type ofType, Genson genson) {
    Field[] fields = ofClass.getDeclaredFields();
    for (Field field : fields) {
      if (TRUE == mutatorAccessorResolver.isMutator(field, getRawClass(ofType))) {
        String name = nameResolver.resolve(field);
        if (name == null) {
          throw new IllegalStateException("Field '" + field.getName() + "' from class "
            + ofClass.getName()
            + " has been discovered as mutator but its name couldn't be resolved!");
        }

        PropertyMutator mutator = propertyFactory.createMutator(name, field, ofType, genson);
        update(mutator, mutatorsMap);
      }
    }
  }

  protected void provideMethodMutators(Class<?> ofClass,
                                       Map<String, LinkedList<PropertyMutator>> mutatorsMap, Type ofType, Genson genson) {
    Method[] methods = ofClass.getDeclaredMethods();
    for (Method method : methods) {
      if (TRUE == mutatorAccessorResolver.isMutator(method, getRawClass(ofType))) {
        String name = nameResolver.resolve(method);
        if (name == null) {
          throw new IllegalStateException("Method '" + method.getName() + "' from class "
            + ofClass.getName()
            + " has been discovered as mutator but its name couldn't be resolved!");
        }
        PropertyMutator mutator = propertyFactory.createMutator(name, method, ofType, genson);
        update(mutator, mutatorsMap);
      }
    }
  }

  protected <T extends BeanProperty> void update(T property, Map<String, LinkedList<T>> map) {
    LinkedList<T> accessors = map.get(property.name);
    if (accessors == null) {
      accessors = new LinkedList<T>();
      map.put(property.name, accessors);
    }
    accessors.add(property);
  }

  @Override
  protected BeanCreator checkAndMerge(Type ofType, List<BeanCreator> creators) {
    Class<?> ofClass = getRawClass(ofType);
    // hum maybe do not check this case as we may have class that will only be serialized so
    // they do not need a ctr?
    // if (creators == null || creators.isEmpty())
    // throw new IllegalStateException("Could not create BeanDescriptor for type "
    // + ofClass.getName() + ", no creator has been found.");
    if (creators == null || creators.isEmpty()) return null;

    // now lets do the merge
    if (favorEmptyCreators) {
      Collections.sort(creators, _beanCreatorsComparator);
    }

    boolean hasCreatorAnnotation = false;
    BeanCreator creator = null;

    // first lets do some checks
    for (int i = 0; i < creators.size(); i++) {
      BeanCreator ctr = creators.get(i);
      if (ctr.isAnnotated()) {
        if (!hasCreatorAnnotation)
          hasCreatorAnnotation = true;
        else
          _throwCouldCreateBeanDescriptor(ofClass,
            " only one @JsonCreator annotation per class is allowed.");
      }
    }

    if (hasCreatorAnnotation) {
      for (BeanCreator ctr : creators)
        if (ctr.isAnnotated()) return ctr;
    } else {
      creator = creators.get(0);
    }

    return creator;
  }

  protected void _throwCouldCreateBeanDescriptor(Class<?> ofClass, String reason) {
    throw new IllegalStateException("Could not create BeanDescriptor for type "
      + ofClass.getName() + "," + reason);
  }

  @Override
  protected PropertyAccessor checkAndMergeAccessors(String name,
                                                    LinkedList<PropertyAccessor> accessors) {
    PropertyAccessor accessor = _mostSpecificPropertyDeclaringClass(name, accessors);
    return VisibilityFilter.ABSTRACT.isVisible(accessor.getModifiers()) ? accessor : null;
  }

  @Override
  protected PropertyMutator checkAndMergeMutators(String name,
                                                  LinkedList<PropertyMutator> mutators) {
    PropertyMutator mutator = _mostSpecificPropertyDeclaringClass(name, mutators);
    return VisibilityFilter.ABSTRACT.isVisible(mutator.getModifiers()) ? mutator : null;
  }

  protected <T extends BeanProperty> T _mostSpecificPropertyDeclaringClass(String name,
                                                                           LinkedList<T> properties) {
    Iterator<T> it = properties.iterator();
    T property = it.next();
    for (; it.hasNext(); ) {
      T next = it.next();
      // Doesn't matter which one will be used, we want to merge the metadata
      next.updateBoth(property);
      // 1 we search the most specialized class containing this property
      // with highest priority
      if ((property.declaringClass.equals(next.declaringClass) && property.priority() < next.priority())
      || property.declaringClass.isAssignableFrom(next.declaringClass)) {
        property = next;
      } else continue;
    }

    return property;
  }

  @Override
  protected void mergeMutatorsWithCreatorProperties(Type ofType, Map<String, PropertyMutator> mutators, BeanCreator creator) {

    for (Map.Entry<String, ? extends BeanCreatorProperty> entry : creator.parameters.entrySet()) {
      PropertyMutator muta = mutators.get(entry.getKey());
      if (muta == null) {
        // add to mutators only creator properties that don't exist as standard
        // mutator (dont exist as field or method, but only as ctr arg)
        BeanCreatorProperty ctrProperty = entry.getValue();
        mutators.put(entry.getKey(), ctrProperty);
      } else {
        // update the creator property annotations with mutator annotations and vice versa
        entry.getValue().updateBoth(muta);
      }
    }
  }

  @Override
  protected void mergeAccessorsWithCreatorProperties(Type ofType, List<PropertyAccessor> accessors, BeanCreator creator) {
    // do nothing
  }
}
