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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.oracle.coherence.io.json.genson.Trilean.FALSE;
import static com.oracle.coherence.io.json.genson.Trilean.TRUE;
import static com.oracle.coherence.io.json.genson.reflect.TypeUtil.*;

import com.oracle.coherence.io.json.genson.*;
import com.oracle.coherence.io.json.genson.annotation.JsonCreator;
import com.oracle.coherence.io.json.genson.reflect.PropertyAccessor.MethodAccessor;
import com.oracle.coherence.io.json.genson.reflect.PropertyMutator.MethodMutator;

/**
 * This class constructs BeanDescriptors for the {@link com.oracle.coherence.io.json.genson.BeanView BeanView}
 * mechanism. This class is mainly intended for internal use. It can be directly used if needed to
 * get a BeanDescriptor instance for a BeanView (for example if you want to deserialize into an
 * existing object and apply a BeanView). Extending BeanViewDescriptorProvider should be avoided.
 *
 * @author eugen
 */
public class BeanViewDescriptorProvider extends BaseBeanDescriptorProvider {

  private Map<Class<?>, BeanView<?>> views;
  private Map<Class<?>, BeanDescriptor<?>> descriptors = new ConcurrentHashMap<Class<?>, BeanDescriptor<?>>();

  public BeanViewDescriptorProvider(ContextualConverterFactory ctxConverterFactory,
                                    Map<Class<?>, BeanView<?>> views, BeanPropertyFactory propertyFactory,
                                    BeanMutatorAccessorResolver mutatorAccessorResolver,
                                    PropertyNameResolver nameResolver) {
    super(ctxConverterFactory, propertyFactory, mutatorAccessorResolver, nameResolver, true, false, true);
    this.views = views;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> BeanDescriptor<T> provide(Class<T> ofClass,
                                       Type ofType, Genson genson) {
    Class<?> rawClass = getRawClass(ofType);
    if (!BeanView.class.isAssignableFrom(rawClass))
      throw new IllegalArgumentException("Expected argument of type "
        + BeanView.class.getName() + " but provided " + rawClass);

    BeanDescriptor<T> descriptor = (BeanDescriptor<T>) descriptors.get(rawClass);
    if (descriptor == null) {
      Class<?> parameterizedTypeForBeanView =
        getRawClass(expandType(BeanView.class.getTypeParameters()[0], ofType));
      if (!ofClass.isAssignableFrom(parameterizedTypeForBeanView)) {
        throw new IllegalArgumentException(
          "Expected type for ofClass parameter is " + parameterizedTypeForBeanView
            + " but provided is " + ofClass);
      }

      try {
        if (!views.containsKey(rawClass)) {
          Constructor<BeanView<T>> ctr =
            (Constructor<BeanView<T>>) rawClass.getDeclaredConstructor();
          if (!ctr.isAccessible()) ctr.setAccessible(true);
          views.put(rawClass, ctr.newInstance());
        }
        descriptor = super.provide(ofClass, ofType, genson);
        descriptors.put(rawClass, descriptor);
      } catch (SecurityException e) {
        throw couldNotInstantiateBeanView(ofClass, e);
      } catch (NoSuchMethodException e) {
        throw couldNotInstantiateBeanView(ofClass, e);
      } catch (IllegalArgumentException e) {
        throw couldNotInstantiateBeanView(ofClass, e);
      } catch (InstantiationException e) {
        throw couldNotInstantiateBeanView(ofClass, e);
      } catch (IllegalAccessException e) {
        throw couldNotInstantiateBeanView(ofClass, e);
      } catch (InvocationTargetException e) {
        throw couldNotInstantiateBeanView(ofClass, e);
      }
    }
    return descriptor;
  }

  private JsonBindingException couldNotInstantiateBeanView(Class<?> beanViewClass,
                                                           Exception e) {
    return new JsonBindingException("Could not instantiate BeanView "
      + beanViewClass.getName()
      + ", BeanView implementations must have a public no arg constructor.", e);
  }

  @Override
  public List<BeanCreator> provideBeanCreators(Type ofType, Genson genson) {
    List<BeanCreator> creators = new ArrayList<BeanCreator>();
    for (Class<?> clazz = getRawClass(ofType); clazz != null && !Object.class.equals(clazz); clazz =
      clazz.getSuperclass()) {
      provideMethodCreators(clazz, creators, ofType, genson);
    }
    Type viewForType = TypeUtil.expandType(BeanView.class.getTypeParameters()[0], ofType);
    List<BeanCreator> oCtrs = super.provideBeanCreators(viewForType, genson);
    creators.addAll(oCtrs);
    return creators;
  }

  public static class BeanViewPropertyFactory implements BeanPropertyFactory {
    private final Map<Class<?>, BeanView<?>> views;

    public BeanViewPropertyFactory(Map<Class<?>, BeanView<?>> views) {
      this.views = views;
    }

    public PropertyAccessor createAccessor(String name, Method method, Type ofType,
                                           Genson genson) {
      // the target bean must be first (and single) parameter for beanview accessors
      BeanView<?> beanview = views.get(getRawClass(ofType));
      if (beanview != null) {
        Type superTypeWithParameter =
          TypeUtil.lookupGenericType(BeanView.class, beanview.getClass());
        Class<?> tClass =
          getRawClass(typeOf(0,
            expandType(superTypeWithParameter, beanview.getClass())));
        Type type = expandType(method.getGenericReturnType(), ofType);
        return new BeanViewPropertyAccessor(name, method, type, beanview, tClass);
      } else return null;
    }

    public PropertyMutator createMutator(String name, Method method, Type ofType, Genson genson) {
      // the target bean must be second parameter for beanview mutators
      BeanView<?> beanview = views.get(getRawClass(ofType));
      if (beanview != null) {
        Type superTypeWithParameter =
          TypeUtil.lookupGenericType(BeanView.class, beanview.getClass());
        Class<?> tClass =
          getRawClass(typeOf(0,
            expandType(superTypeWithParameter, beanview.getClass())));
        Type type = expandType(method.getGenericParameterTypes()[0], ofType);
        return new BeanViewPropertyMutator(name, method, type, beanview, tClass);
      } else return null;
    }

    @Override
    public PropertyAccessor createAccessor(String name, Field field, Type ofType,
                                           Genson genson) {
      return null;
    }

    @Override
    public BeanCreator createCreator(Type ofType, Constructor<?> ctr,
                                     String[] resolvedNames, boolean annotated, Genson genson) {
      return null;
    }

    @Override
    public BeanCreator createCreator(Type ofType, Method method,
                                     String[] resolvedNames, boolean annotated, Genson genson) {
      return null;
    }

    @Override
    public PropertyMutator createMutator(String name, Field field, Type ofType,
                                         Genson genson) {
      return null;
    }
  }

  public static class BeanViewMutatorAccessorResolver implements BeanMutatorAccessorResolver {

    public Trilean isAccessor(Field field, Class<?> baseClass) {
      return FALSE;
    }

    public Trilean isAccessor(Method method, Class<?> baseClass) {
      Type expectedType = TypeUtil.lookupGenericType(BeanView.class, baseClass);
      expectedType = TypeUtil.expandType(expectedType, baseClass);
      expectedType = TypeUtil.typeOf(0, expectedType);
      int modifiers = method.getModifiers();
      return Trilean.valueOf((method.getName().startsWith("get") || (method.getName()
        .startsWith("is") && (TypeUtil.match(method.getGenericReturnType(),
        Boolean.class, false) || boolean.class.equals(method.getReturnType()))))
        && TypeUtil.match(expectedType, method.getGenericParameterTypes()[0], false)
        && Modifier.isPublic(modifiers)
        && !Modifier.isAbstract(modifiers)
        && !Modifier.isNative(modifiers));
    }

    public Trilean isCreator(Constructor<?> constructor, Class<?> baseClass) {
      int modifier = constructor.getModifiers();
      return Trilean.valueOf(Modifier.isPublic(modifier)
        || !(Modifier.isPrivate(modifier) || Modifier.isProtected(modifier)));
    }

    public Trilean isCreator(Method method, Class<?> baseClass) {
      if (method.getAnnotation(JsonCreator.class) != null) {
        if (Modifier.isStatic(method.getModifiers())) return TRUE;
        throw new JsonBindingException("Method " + method.toGenericString()
          + " annotated with @Creator must be static!");
      }
      return FALSE;
    }

    @Override
    public boolean isCreatorAnnotated(final Constructor<?> constructor) {
      return constructor.getAnnotation(JsonCreator.class) != null;
    }

    @Override
    public boolean isCreatorAnnotated(final Method method) {
      return method.getAnnotation(JsonCreator.class) != null;
    }

    public Trilean isMutator(Field field, Class<?> baseClass) {
      return FALSE;
    }

    public Trilean isMutator(Method method, Class<?> baseClass) {
      Type expectedType = TypeUtil.lookupGenericType(BeanView.class, baseClass);
      expectedType = TypeUtil.expandType(expectedType, baseClass);
      expectedType = TypeUtil.typeOf(0, expectedType);
      int modifiers = method.getModifiers();
      return Trilean.valueOf(method.getName().startsWith("set")
        && void.class.equals(method.getReturnType())
        && method.getGenericParameterTypes().length == 2
        && TypeUtil.match(expectedType, method.getGenericParameterTypes()[1], false)
        && Modifier.isPublic(modifiers) && !Modifier.isAbstract(modifiers)
        && !Modifier.isNative(modifiers));
    }

  }

  private static class BeanViewPropertyAccessor extends MethodAccessor {
    private final BeanView<?> _view;

    public BeanViewPropertyAccessor(String name, Method getter, Type type, BeanView<?> target,
                                    Class<?> tClass) {
      super(name, getter, type, tClass);
      this._view = target;
    }

    @Override
    public Object access(Object target) {
      try {
        return _getter.invoke(_view, target);
      } catch (IllegalArgumentException e) {
        throw couldNotAccess(e);
      } catch (IllegalAccessException e) {
        throw couldNotAccess(e);
      } catch (InvocationTargetException e) {
        throw couldNotAccess(e);
      }
    }
  }

  private static class BeanViewPropertyMutator extends MethodMutator {
    private final BeanView<?> _view;

    public BeanViewPropertyMutator(String name, Method setter, Type type, BeanView<?> target,
                                   Class<?> tClass) {
      super(name, setter, type, tClass);
      this._view = target;
    }

    @Override
    public void mutate(Object target, Object value) {
      try {
        _setter.invoke(_view, value, target);
      } catch (IllegalArgumentException e) {
        throw couldNotMutate(e);
      } catch (IllegalAccessException e) {
        throw couldNotMutate(e);
      } catch (InvocationTargetException e) {
        throw couldNotMutate(e);
      }
    }
  }
}
