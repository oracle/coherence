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


import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

import com.oracle.coherence.io.json.genson.*;
import com.oracle.coherence.io.json.genson.reflect.BeanCreator.BeanCreatorProperty;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;
import com.oracle.coherence.io.json.genson.stream.ValueType;

/**
 * BeanDescriptors are used to serialize/deserialize objects based on their fields, methods and
 * constructors. By default it is supposed to work on JavaBeans, however it can be configured and
 * extended to support different kind of objects.
 * <p/>
 * In most cases BeanDescriptors should not be used directly as it is used internally to support
 * objects not handled by the default Converters. The most frequent case when you will use directly
 * a BeanDescriptor is when you want to deserialize into an existing instance. Here is an example :
 * <p/>
 * <pre>
 * Genson genson = new Genson();
 * BeanDescriptorProvider provider = genson.getBeanDescriptorProvider();
 * BeanDescriptor&lt;MyClass&gt; descriptor = provider.provide(MyClass.class, genson);
 *
 * MyClass existingInstance = descriptor.deserialize(existingInstance, new JsonReader(&quot;{}&quot;),
 * 		new Context(genson));
 * </pre>
 *
 * @param <T> type that this BeanDescriptor can serialize and deserialize.
 * @author Eugen Cepoi
* @see BeanDescriptorProvider
 */
public class BeanDescriptor<T> implements Converter<T> {
  final Class<?> fromDeclaringClass;
  final Class<T> ofClass;
  final Map<String, PropertyMutator> mutableProperties;
  final List<PropertyAccessor> accessibleProperties;
  final boolean failOnMissingProperty;

  final BeanCreator creator;
  private final boolean _noArgCtr;

  private static final Object MISSING = new Object();

  // Used as a cache so we just copy it instead of recreating and assigning the default values
  private Object[] globalCreatorArgs;

  private final static Comparator<BeanProperty> _readablePropsComparator = new Comparator<BeanProperty>() {
    public int compare(BeanProperty o1, BeanProperty o2) {
      return o1.name.compareToIgnoreCase(o2.name);
    }
  };

  public BeanDescriptor(Class<T> forClass, Class<?> fromDeclaringClass,
                        List<PropertyAccessor> readableBps,
                        Map<String, PropertyMutator> writableBps, BeanCreator creator,
                        boolean failOnMissingProperty) {
    this.ofClass = forClass;
    this.fromDeclaringClass = fromDeclaringClass;
    this.creator = creator;
    this.failOnMissingProperty = failOnMissingProperty;
    mutableProperties = writableBps;

    Collections.sort(readableBps, _readablePropsComparator);

    accessibleProperties = Collections.unmodifiableList(readableBps);
    if (this.creator != null) {
      _noArgCtr = this.creator.parameters.size() == 0;
      globalCreatorArgs = new Object[creator.parameters.size()];
      Arrays.fill(globalCreatorArgs, MISSING);
    } else {
      _noArgCtr = false;
    }
  }

  public boolean isReadable() {
    return !accessibleProperties.isEmpty();
  }

  public boolean isWritable() {
    return creator != null;
  }

  public void serialize(T obj, ObjectWriter writer, Context ctx) {
    RuntimePropertyFilter  runtimePropertyFilter  = ctx.genson.runtimePropertyFilter();
    UnknownPropertyHandler unknownPropertyHandler = ctx.genson.unknownPropertyHandler();

    writer.beginObject();
    for (PropertyAccessor accessor : accessibleProperties) {
      if (runtimePropertyFilter.shouldInclude(accessor, ctx)) accessor.serialize(obj, writer, ctx);
    }
    if (unknownPropertyHandler != null) {
      unknownPropertyHandler.writeUnknownProperties(obj, writer, ctx);
    }
    writer.endObject();
  }

  public T deserialize(ObjectReader reader, Context ctx) {
    T bean = null;
    // optimization for default ctr
    if (_noArgCtr) {
      bean = ofClass.cast(creator.create());
      deserialize(bean, reader, ctx);
    } else {
      if (creator == null) {

        // It could be we're executing this block of code as
        // the metadata for the type is using an interface type
        // to resolve against which can't be instantiated and
        // there is no class metadata avaialble.  In this case,
        // if the configured default object is assignable to
        // the interface type, then deserialize into the that
        // type.
        Class<?> defaultObjectClass = ctx.genson.defaultClass(ValueType.OBJECT);
        Exception thrown = null;
        if (ofClass.isAssignableFrom(defaultObjectClass)) {
          final int modifiers = defaultObjectClass.getModifiers();
          if ((!java.lang.reflect.Modifier.isAbstract(modifiers)
              && !java.lang.reflect.Modifier.isInterface(modifiers))
              && Modifier.isPublic(modifiers)) {
            try {
              Converter c = ctx.genson.provideConverter(ctx.genson.defaultClass(ValueType.OBJECT));
              if (c != null) {
                //noinspection unchecked
                return (T) c.deserialize(reader, ctx);
              }
            } catch (Exception e) {
              thrown = e;
            }
          }
        }
        String message = "No constructor has been found for type " + ofClass;
        if (null != thrown) {
            throw new JsonBindingException(message, thrown);
        } else {
            throw new JsonBindingException(message);
        }
      }
      bean = _deserWithCtrArgs(reader, ctx);
    }
    return bean;
  }

  public void deserialize(T into, ObjectReader reader, Context ctx) {
    RuntimePropertyFilter  runtimePropertyFilter  = ctx.genson.runtimePropertyFilter();
    UnknownPropertyHandler unknownPropertyHandler = ctx.genson.unknownPropertyHandler();

    reader.beginObject();
    for (; reader.hasNext(); ) {
      reader.next();
      String propName = reader.name();
      PropertyMutator mutator = mutableProperties.get(propName);
      if (mutator != null) {
        if (runtimePropertyFilter.shouldInclude(mutator, ctx)) {
          mutator.deserialize(into, reader, ctx);
        } else {
          reader.skipValue();
        }
      } else if (unknownPropertyHandler != null) {
        unknownPropertyHandler.readUnknownProperty(propName, reader, ctx).accept(into);
      } else if (failOnMissingProperty) throw missingPropertyException(propName);
      else reader.skipValue();
    }
    reader.endObject();
  }


  protected T _deserWithCtrArgs(ObjectReader reader, Context ctx) {
    List<String> names = new ArrayList<>();
    List<Object> values = new ArrayList<>();
    List<Consumer<T>> unknownProperties = new ArrayList<>();

    RuntimePropertyFilter runtimePropertyFilter = ctx.genson.runtimePropertyFilter();
    UnknownPropertyHandler unknownPropertyHandler = ctx.genson.unknownPropertyHandler();

    reader.beginObject();
    for (; reader.hasNext(); ) {
      reader.next();
      String propName = reader.name();
      PropertyMutator muta = mutableProperties.get(propName);

      if (muta != null) {
        if (runtimePropertyFilter.shouldInclude(muta, ctx)) {
          Object param = muta.deserialize(reader, ctx);
          names.add(propName);
          values.add(param);
        } else {
          reader.skipValue();
        }
      } else if (unknownPropertyHandler != null) {
        Consumer<T> callback = unknownPropertyHandler.readUnknownProperty(propName, reader, ctx);
        unknownProperties.add(callback);
      } else if (failOnMissingProperty) throw missingPropertyException(propName);
      else reader.skipValue();
    }

    int size = names.size();
    int foundCtrParameters = 0;
    Object[] creatorArgs = globalCreatorArgs.clone();
    String[] newNames = new String[size];
    Object[] newValues = new Object[size];

    for (int i = 0, j = 0; i < size; i++) {
      BeanCreatorProperty mp = creator.paramsAndAliases.get(names.get(i));
      if (mp != null) {
        creatorArgs[mp.index] = values.get(i);
        foundCtrParameters++;
      } else {
        newNames[j] = names.get(i);
        newValues[j] = values.get(i);
        j++;
      }
    }

    if (foundCtrParameters < creator.parameters.size()) updateWithDefaultValues(creatorArgs, ctx.genson);

    T bean = ofClass.cast(creator.create(creatorArgs));
    for (int i = 0; i < size; i++) {
      PropertyMutator property = mutableProperties.get(newNames[i]);
      if (property != null) {
        property.mutate(bean, newValues[i]);
      }
    }
    unknownProperties.forEach(callback -> callback.accept(bean));

    reader.endObject();
    return bean;
  }

  private void updateWithDefaultValues(Object[] creatorArgs, Genson genson) {
    for (int i = 0; i < creatorArgs.length; i++) {
      if (creatorArgs[i] == MISSING) {
        for (BeanCreatorProperty property : creator.parameters.values()) {
          if (property.index == i) {
            creatorArgs[i] = genson.defaultValue(property.getRawClass());
            break;
          }
        }
      }
    }
  }

  public Class<T> getOfClass() {
    return ofClass;
  }

  private JsonBindingException missingPropertyException(String name) {
   return new JsonBindingException("No matching property in " + getOfClass() + " for key " + name);
  }
}