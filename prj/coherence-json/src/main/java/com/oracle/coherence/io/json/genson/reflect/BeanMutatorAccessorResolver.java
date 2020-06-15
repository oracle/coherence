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


import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.oracle.coherence.io.json.genson.Trilean.FALSE;
import static com.oracle.coherence.io.json.genson.Trilean.TRUE;
import static com.oracle.coherence.io.json.genson.Trilean.UNKNOWN;

import com.oracle.coherence.io.json.genson.JsonBindingException;
import com.oracle.coherence.io.json.genson.Trilean;
import com.oracle.coherence.io.json.genson.annotation.JsonCreator;
import com.oracle.coherence.io.json.genson.annotation.JsonIgnore;
import com.oracle.coherence.io.json.genson.annotation.JsonProperty;

/**
 * BeanMutatorAccessorResolver interface must be implemented by class who want to resolve mutators
 * (fields or methods that allow you to modify a property), accessors (fields or methods that allow
 * you to retrieve the value of a property) and creators (constructors or static methods that allow
 * you to create objects).
 * <p/>
 * All methods return a {@link com.oracle.coherence.io.json.genson.Trilean Trilean}, so it may be TRUE, FALSE or UNKNOWN.
 * This will allow us to separate the kind of information each implementation works on (one for
 * annotations, another for visibility, etc) and to chain them. It also allows an easier addition of
 * new features without modifying the existing code. Have a look at <a href=
 * "http://code.google.com/p/genson/source/browse/src/main/java/com/owlike/genson/reflect/BeanMutatorAccessorResolver.java"
 * >StandardMutaAccessorResolver</a> for an example of BeanMutatorAccessorResolver implementation.
 * <p/>
 * To register your own implementation instead of the one by default use the genson builder.
 * <p/>
 * <pre>
 * new Genson.Builder().set(yourImplementation).create();
 * </pre>
 *
 * @author Eugen Cepoi
* @see com.oracle.coherence.io.json.genson.Trilean Trilean
 * @see StandardMutaAccessorResolver
 * @see AbstractBeanDescriptorProvider
 * @see BaseBeanDescriptorProvider
 * @see BeanViewDescriptorProvider
 */
public interface BeanMutatorAccessorResolver {
  Trilean isCreator(Constructor<?> constructor, Class<?> fromClass);

  Trilean isCreator(Method method, Class<?> fromClass);

  boolean isCreatorAnnotated(Constructor<?> constructor);

  boolean isCreatorAnnotated(Method method);

  Trilean isAccessor(Field field, Class<?> fromClass);

  Trilean isAccessor(Method method, Class<?> fromClass);

  Trilean isMutator(Field field, Class<?> fromClass);

  Trilean isMutator(Method method, Class<?> fromClass);

  class PropertyBaseResolver implements BeanMutatorAccessorResolver {
    @Override
    public Trilean isAccessor(Field field, Class<?> fromClass) {
      return UNKNOWN;
    }

    @Override
    public Trilean isAccessor(Method method, Class<?> fromClass) {
      return UNKNOWN;
    }

    @Override
    public Trilean isCreator(Constructor<?> constructor, Class<?> fromClass) {
      return UNKNOWN;
    }

    @Override
    public Trilean isCreator(Method method, Class<?> fromClass) {
      return UNKNOWN;
    }

    @Override
    public boolean isCreatorAnnotated(final Constructor<?> constructor) {
      return false;
    }

    @Override
    public boolean isCreatorAnnotated(final Method method) {
      return false;
    }

    @Override
    public Trilean isMutator(Field field, Class<?> fromClass) {
      return UNKNOWN;
    }

    @Override
    public Trilean isMutator(Method method, Class<?> fromClass) {
      return UNKNOWN;
    }
  }

  /**
   * A basic {@link Annotation} scanning {@link BeanMutatorAccessorResolver} assuming the use of three
   * primary {@link Annotation}s:
   * <ul>
   *   <li>
   *     <code>propertyAnnotation</code>: an annotation, such as {@link JsonProperty}, denoting a property
   *     to serialize/deserialize.
   *   </li>
   *   <li>
   *     <code>exclusionAnnotation</code>: an annotation, such as {@link JsonIgnore} denoting an exclusion of a
   *     property from serialization/deserialization.
   *  </li>
   *  <li>
   *    <code>creatorAnnotation</code>: an annotation, such as {@link JsonCreator} denoting a constructor or
   *    factory method to use when deserializing an entity.
   *  </li>
   * </ul>
   *
   * @since 1.5.0
   */
  class AnnotationPropertyResolver implements BeanMutatorAccessorResolver {
    /**
     * An {@link Annotation} that functions similarly to {@link JsonProperty}.
     */
    protected Class<? extends Annotation> propertyAnnotation;

    /**
     * An {@link Annotation} that functions similarly to {@link JsonIgnore}.
     */
    protected Class<? extends Annotation> exclusionAnnotation;

    /**
     * An {@link Annotation} that functions similarly to {@link JsonCreator}.
     * This value may be <code>null</code>.
     */
    protected Class<? extends Annotation> creatorAnnotation;

    /**
     * Create a new resolver for the specified annotations.  The notion of property, exclusion, and creator
     * will be carried forward in the documentation of the class.
     *
     * @param propertyAnnotation  @see {@link #propertyAnnotation}
     * @param exclusionAnnotation @see {@link #exclusionAnnotation}
     * @param creatorAnnotation   @see {@link #creatorAnnotation}
     */
    public AnnotationPropertyResolver(Class<? extends Annotation> propertyAnnotation,
                                      Class<? extends Annotation> exclusionAnnotation,
                                      Class<? extends Annotation> creatorAnnotation) {
      this.propertyAnnotation = propertyAnnotation;
      this.exclusionAnnotation = exclusionAnnotation;
      this.creatorAnnotation = creatorAnnotation;
    }

    /**
     * Determines if the configured {@link #creatorAnnotation} is present on the provided {@link Constructor}.
     *
     * @param constructor the {@link Constructor} to scan
     * @param fromClass   the {@link Class} the {@link Constructor} is associated with
     *
     * @return {@link Trilean#TRUE} if the {@link #creatorAnnotation} is found, otherwise {@link Trilean#UNKNOWN}
     */
    @Override
    public Trilean isCreator(Constructor<?> constructor, Class<?> fromClass) {
      if (creatorAnnotation != null) {
        /*
         * hum... it depends on different things, such as parameters name resolution, types, etc
         * but we are not supposed to handle it here... lets only check visibility and handle it
         * in the provider implementations
         */
        if (find(creatorAnnotation, constructor, fromClass) != null) {
            return TRUE;
        }
      }
      return UNKNOWN;
    }

    /**
     * Determines if the configured {@link #creatorAnnotation} is present on the provided {@link Method}.
     *
     * @param method    the {@link Method} to scan
     * @param fromClass the {@link Class} the {@link Method} is associated with
     *
     * @throws JsonBindingException if the annotated {@link Method} is not <code>public</code> and <code>static</code>
     *
     * @return {@link Trilean#TRUE} if the {@link #creatorAnnotation} is found, otherwise {@link Trilean#UNKNOWN}
     */
    @Override
    public Trilean isCreator(Method method, Class<?> fromClass) {
      if (creatorAnnotation != null) {
        if (find(creatorAnnotation, method, fromClass) != null) {
          if (Modifier.isPublic(method.getModifiers()) && Modifier.isStatic(method.getModifiers())) {
              return TRUE;
          } else {
            throw new JsonBindingException(String.format("Method [%s] annotated with [%s] must be static!",
                method.toGenericString(), creatorAnnotation));
          }
        }
      }
      return UNKNOWN;
    }

    @Override
    public boolean isCreatorAnnotated(final Constructor<?> constructor) {
      if (constructor == null || creatorAnnotation == null) {
        return false;
      }
      return constructor.getAnnotation(creatorAnnotation) != null;
    }

    @Override
    public boolean isCreatorAnnotated(final Method method) {
      if (method == null || creatorAnnotation == null) {
        return false;
      }
      return method.getAnnotation(creatorAnnotation) != null;
    }

    /**
     * Determines if the configured {@link #propertyAnnotation} is present on the provided {@link Field}.
     * Invoked during serialization.
     *
     * @param field     the {@link Field} to scan
     * @param fromClass the {@link Class} the {@link Field} is associated with
     *
     * @return {@link Trilean#FALSE} if the {@link Field} is annotated with the {@link #exclusionAnnotation},
     *   {@link Trilean#TRUE} if the {@link #propertyAnnotation} is found, otherwise returns {@link Trilean#UNKNOWN}
     */
    @Override
    public Trilean isAccessor(Field field, Class<?> fromClass) {
      if (field.isSynthetic() || ignore(field, field.getType(), true)) {
        return FALSE;
      }
      if (include(field, field.getType(), true)) {
        return TRUE;
      }
      return UNKNOWN;
    }

    /**
     * Determines if the configured {@link #propertyAnnotation} is present on the provided {@link Field}.
     * Invoked during serialization.
     *
     * @param method    the {@link Method} to scan
     * @param fromClass the {@link Class} the {@link Field} is associated with
     *
     * @return {@link Trilean#FALSE} if the {@link Method} is annotated with the {@link #exclusionAnnotation},
     *   {@link Trilean#TRUE} if the {@link #propertyAnnotation} is found, otherwise returns {@link Trilean#UNKNOWN}
     */
    @Override
    public Trilean isAccessor(Method method, Class<?> fromClass) {
      if (ignore(method, method.getReturnType(), true)) return FALSE;

      String name = getGetterName(method);

      if (name != null) {
        if (include(method, method.getReturnType(), true)) return TRUE;
        if (find(exclusionAnnotation, fromClass, "set" + name, method.getReturnType()) != null)
          return FALSE;
      }
      return UNKNOWN;
    }

    /**
     * Determines if the configured {@link #propertyAnnotation} is present on the provided {@link Field}.
     * Invoked during deserialization.
     *
     * @param field     the {@link Field} to scan
     * @param fromClass the {@link Class} the {@link Field} is associated with
     *
     * @return {@link Trilean#FALSE} if the {@link Field} is annotated with the {@link #exclusionAnnotation},
     *   {@link Trilean#TRUE} if the {@link #propertyAnnotation} is found, otherwise returns {@link Trilean#UNKNOWN}
     */
    @Override
    public Trilean isMutator(Field field, Class<?> fromClass) {
      if (field.isSynthetic() || ignore(field, field.getType(), false)) {
        return FALSE;
      }
      if (include(field, field.getType(), false)) {
        return TRUE;
      }
      return UNKNOWN;
    }

    /**
     * Determines if the configured {@link #propertyAnnotation} is present on the provided {@link Field}.
     * Invoked during serialization.
     *
     * @param method    the {@link Method} to scan
     * @param fromClass the {@link Class} the {@link Field} is associated with
     *
     * @return {@link Trilean#FALSE} if the {@link Method} is annotated with the {@link #exclusionAnnotation},
     *   {@link Trilean#TRUE} if the {@link #propertyAnnotation} is found, otherwise returns {@link Trilean#UNKNOWN}
     */
    @Override
    public Trilean isMutator(Method method, Class<?> fromClass) {
      Class<?> paramClass = method.getParameterTypes().length == 1 ? method
          .getParameterTypes()[0] : Object.class;
      if (ignore(method, paramClass, false)) return FALSE;

      String name = getSetterName(method);
      if (name != null) {
        if (include(method, method.getReturnType(), false)) return TRUE;

        // Exclude it if there is a corresponding accessor annotated with JsonIgnore
        if (find(exclusionAnnotation, fromClass, "get" + name) != null) return FALSE;
        if (paramClass.equals(boolean.class) || paramClass.equals(Boolean.class)) {
          if (find(exclusionAnnotation, fromClass, "is" + name) != null)
            return FALSE;
        }
      }
      return UNKNOWN;
    }

    /**
     * Return the property name based on argument {@link Method}.  This assumes a <code>getter</code> using standard
     * Java Beans naming conventions.
     *
     * @param method the {@link Method} to extract the name from
     *
     * @return the property name or <code>null</code> if the method isn't a <code>getter</code>.
     */
    protected String getGetterName(Method method) {
      String name = method.getName();
      Class<?> returnType = method.getReturnType();
      if (name.startsWith("get") && name.length() > 3) {
        return name.substring(3);
      } else if (name.startsWith("is") && name.length() > 2
          && (returnType == boolean.class || returnType == Boolean.class)) {
        return name.substring(2);
      }
      return null;
    }

    /**
     * Return the property name based on argument {@link Method}.  This assumes a <code>setter</code> using standard
     * Java Beans naming conventions.
     *
     * @param method the {@link Method} to extract the name from
     *
     * @return the property name or <code>null</code> if the method isn't a <code>setter</code>.
     */
    protected String getSetterName(Method method) {
      String name = method.getName();
      if (name.startsWith("set") && name.length() > 3) {
        return name.substring(3);
      }
      return null;
    }

    /**
     * Scans for the presence of the {@link #exclusionAnnotation}/
     *
     * @param property the property to scan
     * @param ofType the associated {@link Class}
     * @param forSerialization flag indicating if this exclusion check is for a serialization
     *                         or deserialization operation
     *
     * @return <code>true</code> if the {@link Annotation} is found, otherwise <code>false</code>
     */
    @SuppressWarnings("unused")
    protected boolean ignore(AccessibleObject property, Class<?> ofType, boolean forSerialization) {
      return find(exclusionAnnotation, property, ofType) != null;
    }

    /**
     * Scans for the presence of the {@link #propertyAnnotation}.
     *
     * @param property the property to scan
     * @param ofType the associated {@link Class}
     * @param forSerialization flag indicating if this exclusion check is for a serialization
     *                         or deserialization operation
     *
     * @return <code>true</code> if the {@link Annotation} is found, otherwise <code>false</code>
     */
    @SuppressWarnings("unused")
    protected boolean include(AccessibleObject property, Class<?> ofType, boolean forSerialization) {
      return find(propertyAnnotation, property, ofType) != null;
    }

    /**
     * Scan for the argument {@link Annotation} on the argument {@link AccessibleObject}.
     *
     * @param annotation the {@link Annotation} to scan for
     * @param onObject the entity that may be annotated with the specified {@link Annotation}
     * @param onClass the {@link Class} associated with the {@link AccessibleObject}
     *
     * @return the {@link Annotation} if found, otherwise returns <code>null</code>
     */
    protected <A extends Annotation> A find(Class<A> annotation, AccessibleObject onObject, Class<?> onClass) {
      A ann = onObject.getAnnotation(annotation);
      if (ann != null) return ann;
      return find(annotation, onClass);
    }

    /**
     * Scan for the argument {@link Annotation} on the argument {@link Class}.
     *
     * @param annotation the {@link Annotation} to scan for
     * @param onClass the {@link Class} to scan
     *
     * @return the {@link Annotation} if found, otherwise returns <code>null</code>
     */
    protected <A extends Annotation> A find(Class<A> annotation, Class<?> onClass) {
      A ann = onClass.getAnnotation(annotation);
      if (ann == null && onClass.getPackage() != null)
        ann = onClass.getPackage().getAnnotation(annotation);
      return ann;
    }

    /**
     * Scan for the argument {@link Annotation} on a method matching the argument method names and parameters.
     *
     * @param annotation the {@link} Annotation to scan for
     * @param inClass the {@link Class} to search for a matching method
     * @param methodName the method's name
     * @param parameterTypes the method's argument types
     *
     * @return the {@link Annotation} if found, otherwise returns <code>null</code>
     */
    protected <A extends Annotation> A find(Class<A> annotation, Class<?> inClass, String methodName,
                                            Class<?>... parameterTypes) {
      for (Class<?> clazz = inClass; clazz != null; clazz = clazz.getSuperclass()) {
        try {
          for (Method m : clazz.getDeclaredMethods())
            if (m.getName().equals(methodName)
                && Arrays.equals(m.getParameterTypes(), parameterTypes))
              if (m.isAnnotationPresent(annotation))
                return m.getAnnotation(annotation);
              else
                break;

        } catch (SecurityException e) {
          throw new RuntimeException(e);
        }
      }
      return null;
    }
  }

  class CompositeResolver implements BeanMutatorAccessorResolver {
    private List<BeanMutatorAccessorResolver> components;

    public CompositeResolver(List<BeanMutatorAccessorResolver> components) {
      if (components == null || components.isEmpty()) {
        throw new IllegalArgumentException(
          "The composite resolver must have at least one resolver as component!");
      }
      this.components = new LinkedList<>(components);
    }

    public CompositeResolver add(BeanMutatorAccessorResolver... resolvers) {
      components.addAll(0, Arrays.asList(resolvers));
      return this;
    }

    @Override
    public Trilean isAccessor(Field field, Class<?> fromClass) {
      Trilean resolved = UNKNOWN;
      for (Iterator<BeanMutatorAccessorResolver> it = components.iterator(); resolved == null || resolved.equals(UNKNOWN)
        && it.hasNext(); ) {
        resolved = it.next().isAccessor(field, fromClass);
      }
      return resolved;
    }

    @Override
    public Trilean isAccessor(Method method, Class<?> fromClass) {
      Trilean resolved = UNKNOWN;
      for (Iterator<BeanMutatorAccessorResolver> it = components.iterator(); resolved == null || resolved.equals(UNKNOWN)
        && it.hasNext(); ) {
        resolved = it.next().isAccessor(method, fromClass);
      }
      return resolved;
    }

    @Override
    public Trilean isCreator(Constructor<?> constructor, Class<?> fromClass) {
      Trilean resolved = UNKNOWN;
      for (Iterator<BeanMutatorAccessorResolver> it = components.iterator(); resolved == null || resolved.equals(UNKNOWN)
        && it.hasNext(); ) {
        resolved = it.next().isCreator(constructor, fromClass);
      }
      return resolved;
    }

    @Override
    public Trilean isCreator(Method method, Class<?> fromClass) {
      Trilean resolved = UNKNOWN;
      for (Iterator<BeanMutatorAccessorResolver> it = components.iterator(); resolved == null || resolved.equals(UNKNOWN)
        && it.hasNext(); ) {
        resolved = it.next().isCreator(method, fromClass);
      }
      return resolved;
    }

    @Override
    public boolean isCreatorAnnotated(final Constructor<?> constructor) {
      boolean resolved = false;
      for (Iterator<BeanMutatorAccessorResolver> it = components.iterator(); !resolved && it.hasNext(); ) {
        resolved = it.next().isCreatorAnnotated(constructor);
      }
      return resolved;
    }

    @Override
    public boolean isCreatorAnnotated(final Method method) {
      boolean resolved = false;
      for (Iterator<BeanMutatorAccessorResolver> it = components.iterator(); !resolved && it.hasNext(); ) {
        resolved = it.next().isCreatorAnnotated(method);
      }
      return resolved;
    }

    @Override
    public Trilean isMutator(Field field, Class<?> fromClass) {
      Trilean resolved = UNKNOWN;
      for (Iterator<BeanMutatorAccessorResolver> it = components.iterator(); resolved == null || resolved.equals(UNKNOWN)
        && it.hasNext(); ) {
        resolved = it.next().isMutator(field, fromClass);
      }
      return resolved;
    }

    @Override
    public Trilean isMutator(Method method, Class<?> fromClass) {
      Trilean resolved = UNKNOWN;
      for (Iterator<BeanMutatorAccessorResolver> it = components.iterator(); resolved == null || resolved.equals(UNKNOWN)
        && it.hasNext(); ) {
        resolved = it.next().isMutator(method, fromClass);
      }
      return resolved;
    }
  }

  class GensonAnnotationPropertyResolver extends AnnotationPropertyResolver {

    public GensonAnnotationPropertyResolver() {
      super(JsonProperty.class, JsonIgnore.class, JsonCreator.class);
    }

    @Override
    protected boolean ignore(final AccessibleObject property, final Class<?> ofType, final boolean forSerialization) {
      if (super.ignore(property, ofType, forSerialization)) {
        JsonIgnore ignore = find(JsonIgnore.class, property, property.getClass());
        return ((forSerialization) ? !ignore.serialize() : !ignore.deserialize());
      }
      return false;
    }

    @Override
    protected boolean include(final AccessibleObject property, final Class<?> ofType, final boolean forSerialization) {
      if (super.include(property, ofType, forSerialization)) {
        JsonProperty prop = find(JsonProperty.class, property, property.getClass());
        return ((forSerialization) ? prop.serialize() : prop.deserialize());
      }
      return false;
    }
  }

  /**
   * Standard implementation of BeanMutatorAccessorResolver.
   * Actually this implementation handles filtering by signature conventions (Java Bean) and visibility.
   *
   * @author Eugen Cepoi
*/
  class StandardMutaAccessorResolver implements BeanMutatorAccessorResolver {
    private final VisibilityFilter fieldVisibilityFilter;
    private final VisibilityFilter methodVisibilityFilter;
    private final VisibilityFilter creatorVisibilityFilter;

    /**
     * Creates a new instance of StandardMutaAccessorResolver with
     * {@link VisibilityFilter#PACKAGE_PUBLIC} visibility for fields,
     * {@link VisibilityFilter#PACKAGE_PUBLIC} visibility for methods and creators.
     */
    public StandardMutaAccessorResolver() {
      this(VisibilityFilter.PACKAGE_PUBLIC, VisibilityFilter.PACKAGE_PUBLIC,
        VisibilityFilter.PACKAGE_PUBLIC);
    }

    /**
     * Use this constructor if you want to customize the visibility filtering.
     *
     * @param fieldVisibilityFilter {@link VisibilityFilter} for object fields
     * @param methodVisibilityFilter {@link VisibilityFilter} for object getters and setters
     * @param creatorVisibilityFilter {@link VisibilityFilter} for constructors
     */
    public StandardMutaAccessorResolver(VisibilityFilter fieldVisibilityFilter,
                                        VisibilityFilter methodVisibilityFilter, VisibilityFilter creatorVisibilityFilter) {
      super();
      this.fieldVisibilityFilter = fieldVisibilityFilter;
      this.methodVisibilityFilter = methodVisibilityFilter;
      this.creatorVisibilityFilter = creatorVisibilityFilter;
    }

    /**
     * Will resolve all public/package and non transient/static fields as accesssors.
     */
    public Trilean isAccessor(Field field, Class<?> fromClass) {
      return Trilean.valueOf(fieldVisibilityFilter.isVisible(field));
    }

    /**
     * Resolves all public methods starting with get/is (boolean) and parameter less as
     * accessors.
     */
    public Trilean isAccessor(Method method, Class<?> fromClass) {
      if (!method.isBridge()) {
        String name = method.getName();
        int len = name.length();
        if (methodVisibilityFilter.isVisible(method)
          && ((len > 3 && name.startsWith("get")) || (len > 2 && name.startsWith("is") && (TypeUtil
          .match(TypeUtil.expandType(method.getGenericReturnType(), fromClass),
            Boolean.class, false) || TypeUtil.match(
          method.getGenericReturnType(), boolean.class, false))))
          && method.getParameterTypes().length == 0)
          return TRUE;
      }

      return FALSE;
    }

    public Trilean isCreator(Constructor<?> constructor, Class<?> fromClass) {
      return Trilean.valueOf(creatorVisibilityFilter.isVisible(constructor));
    }

    public Trilean isCreator(Method method, Class<?> fromClass) {
      return FALSE;
    }

    @Override
    public boolean isCreatorAnnotated(final Constructor<?> constructor) {
      return false;
    }

    @Override
    public boolean isCreatorAnnotated(final Method method) {
      return false;
    }

    public Trilean isMutator(Field field, Class<?> fromClass) {
      return Trilean.valueOf(fieldVisibilityFilter.isVisible(field));
    }

    public Trilean isMutator(Method method, Class<?> fromClass) {
      if (!method.isBridge()) {
        if (methodVisibilityFilter.isVisible(method) && method.getName().length() > 3
          && method.getName().startsWith("set") && method.getParameterTypes().length == 1
          && method.getReturnType() == void.class)
          return TRUE;
      }

      return FALSE;
    }
  }

}
