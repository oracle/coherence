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

package com.oracle.coherence.io.json.genson;


import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.oracle.coherence.io.json.genson.reflect.BeanDescriptor;
import com.oracle.coherence.io.json.genson.reflect.BeanDescriptorProvider;
import com.oracle.coherence.io.json.genson.reflect.DefaultTypes;
import com.oracle.coherence.io.json.genson.reflect.RuntimePropertyFilter;
import com.oracle.coherence.io.json.genson.reflect.UnknownPropertyHandler;
import com.oracle.coherence.io.json.genson.stream.*;

/**
 * <p/>
 * Main class of the library. Instances of Genson are thread safe and should be reused.
 * You can instantiate it multiple times but it is better to have an instance per configuration
 * so you can benefit of better performances.
 * <p/>
 * For more examples have a look at the <a
 * href="http://owlike.github.io/genson/">online documentation</a>.
 * <p/>
 * <p/>
 * To create a new instance of Genson you can use the default no arg constructor or the
 * {@link GensonBuilder} class to have control over Gensons configuration. <br>
 * A basic usage of Genson would be:
 * <p/>
 * <pre>
 * Genson genson = new Genson();
 * String json = genson.serialize(new int[] { 1, 2, 3 });
 * int[] arrayOfInt = genson.deserialize(json, int[].class);
 * // (multi-dimensional arrays are also supported)
 *
 * // genson can also deserialize primitive types without class information
 * Number[] arrayOfNumber = genson.deserialize(&quot;[1, 2.03, 8877463]&quot;, Number[].class);
 * // or even
 * Object[] arrayOfUnknownTypes = genson
 * 		.deserialize(&quot;[1, false, null, 4.05, \&quot;hey this is a string!\&quot;]&quot;, Object[].class);
 * // it can also deserialize json objects of unknown types to standard java types such as Map, Array, Long etc.
 * Map&lt;String, Object&gt; map = (Map&lt;String, Object&gt;) genson.deserialize(&quot;{\&quot;foo\&quot;:1232}&quot;, Object.class);
 * </pre>
 * <p/>
 *
 * Every object serialized with Genson, can be deserialized back into its concrete type! Just enable
 * it via the builder {@link com.oracle.coherence.io.json.genson.GensonBuilder#useClassMetadata(boolean)}.
 *
 * You can also deserialize to objects that don't provide a default constructor with no arguments
 * {@link com.oracle.coherence.io.json.genson.GensonBuilder#useConstructorWithArguments(boolean)}.
 *
 * @author eugen
 */
public final class Genson {
  /**
   * Default genson configuration, the default configuration (sers, desers, etc) will be shared
   * accros all default Genson instances.
   */
  private final static Genson _default = new GensonBuilder().create();
  private final static Charset UTF8_CHARSET = Charset.forName("UTF-8");

  private final ConcurrentHashMap<Type, Converter<?>> converterCache = new ConcurrentHashMap<Type, Converter<?>>();
  private final Factory<Converter<?>> converterFactory;
  private final BeanDescriptorProvider beanDescriptorFactory;
  private final Map<Class<?>, String> classAliasMap;
  private final Map<String, Class<?>> aliasClassMap;
  private final Map<String, String> aliasPackageMap;
  private final Map<String, String> packageAliasMap;
  private final boolean skipNull;
  private final boolean htmlSafe;
  private final boolean withClassMetadata;
  private final boolean withMetadata;
  private final boolean strictDoubleParse;
  private final boolean indent;
  private final boolean failOnMissingProperty;
  private final EncodingAwareReaderFactory readerFactory = new EncodingAwareReaderFactory();
  private final Map<Class<?>, Object> defaultValues;
  private final DefaultTypes defaultTypes;
  private final RuntimePropertyFilter runtimePropertyFilter;
  private final UnknownPropertyHandler unknownPropertyHandler;
  private volatile ClassLoader loader;

  /**
   * The default constructor will use the default configuration provided by the {@link GensonBuilder}.
   * In most cases using this default constructor will suffice.
   */
  public Genson() {
    this(_default.converterFactory, _default.beanDescriptorFactory,
      _default.skipNull, _default.htmlSafe, _default.aliasClassMap, _default.aliasPackageMap,
      _default.withClassMetadata, _default.strictDoubleParse, _default.indent,
      _default.withMetadata, _default.failOnMissingProperty, _default.defaultValues,
      _default.defaultTypes, _default.runtimePropertyFilter, _default.unknownPropertyHandler,
        Genson.class.getClassLoader());
  }

  /**
   * Instead of using this constructor you should use {@link GensonBuilder}.
   * @param converterFactory  providing instance of converters.
   * @param beanDescProvider  providing instance of {@link BeanDescriptor
   *                          BeanDescriptor} used during bean serialization and deserialization.
   * @param skipNull          indicates whether null values should be serialized. False by default, null values
   *                          will be serialized.
   * @param htmlSafe          indicates whether \,<,>,&,= characters should be replaced by their Unicode
   *                          representation.
   * @param classAliases      association map between classes and their aliases, used if withClassMetadata is
   *                          true.
   * @param packageAliases    association map between a group of classes in a package and their alias,
   *                          used if withClassMetadata is true.
   * @param withClassMetadata indicates whether class name should be serialized and used during deserialization
   *                          to determine the type. False by default.
   * @param strictDoubleParse indicates whether to use or not double approximation. If false (by default) it
   *                          enables Genson custom double parsing algorithm, that is an approximation of
   *                          Double.parse but is a lot faster. If true, Double.parse method will be usead
   *                          instead. In most cases you should be fine with Genson algorithm, but if for some
   *                          reason you need to have 100% match with Double.parse, then enable strict parsing.
   * @param indent            true if outputed json must be indented (pretty printed).
   * @param withMetadata      true if ObjectReader instances must be configured with metadata feature enabled.
   *                          if withClassMetadata is true withMetadata will be automatically true.
   * @param failOnMissingProperty throw a JsonBindingException when a key in the json stream does not match a property in the Java Class.
   * @param defaultValues contains a mapping from the raw class to the default value that should be used when the property is missing.
   * @param defaultValues contains a mapping from the raw class to the default value that should be used when the property is missing.
   * @param runtimePropertyFilter is used to define what bean properties should be excluded from ser/de at runtime.
   * @param unknownPropertyHandler is used to handle unknown properties during ser/de.
   */
  public Genson(Factory<Converter<?>> converterFactory, BeanDescriptorProvider beanDescProvider,
                boolean skipNull, boolean htmlSafe, Map<String, Class<?>> classAliases,
                Map<String,String> packageAliases, boolean withClassMetadata,
                boolean strictDoubleParse, boolean indent, boolean withMetadata, boolean failOnMissingProperty,
                Map<Class<?>, Object> defaultValues, DefaultTypes defaultTypes,
                RuntimePropertyFilter runtimePropertyFilter, UnknownPropertyHandler unknownPropertyHandler,
                ClassLoader classLoader) {
    this.converterFactory = converterFactory;
    this.beanDescriptorFactory = beanDescProvider;
    this.skipNull = skipNull;
    this.htmlSafe = htmlSafe;
    this.aliasClassMap = classAliases;
    this.aliasPackageMap = packageAliases;
    this.withClassMetadata = withClassMetadata;
    this.defaultValues = defaultValues;
    this.defaultTypes = defaultTypes;
    this.runtimePropertyFilter = runtimePropertyFilter;
    this.classAliasMap = new HashMap<Class<?>, String>(classAliases.size());
    for (Map.Entry<String, Class<?>> entry : classAliases.entrySet()) {
      classAliasMap.put(entry.getValue(), entry.getKey());
    }
    this.packageAliasMap = new HashMap<>(aliasPackageMap.size());
    for (Map.Entry<String, String> entry : aliasPackageMap.entrySet()) {
      packageAliasMap.put(entry.getValue(), entry.getKey());
    }
    this.strictDoubleParse = strictDoubleParse;
    this.indent = indent;
    this.withMetadata = withClassMetadata || withMetadata;
    this.failOnMissingProperty = failOnMissingProperty;
    this.unknownPropertyHandler = unknownPropertyHandler;
    this.loader = classLoader;
  }

  /**
   * Provides an instance of Converter capable of handling objects of type forType.
   *
   * @param forType the type for which a converter is needed.
   * @return the converter instance.
   * @throws com.oracle.coherence.io.json.genson.JsonBindingException if a problem occurs during converters lookup/construction.
   */
  @SuppressWarnings("unchecked")
  public <T> Converter<T> provideConverter(Type forType) {
    if (Boolean.TRUE.equals(ThreadLocalHolder.get("__GENSON$DO_NOT_CACHE_CONVERTER", Boolean.class))) {
      return (Converter<T>) converterFactory.create(forType, this);
    } else {
      Converter<T> converter = (Converter<T>) converterCache.get(forType);
      if (converter == null) {
        converter = (Converter<T>) converterFactory.create(forType, this);
        if (converter == null)
          throw new JsonBindingException("No converter found for type " + forType);
        converterCache.putIfAbsent(forType, converter);
      }
      return converter;
    }
  }

  /**
   * Serializes the object into a json string.
   *
   * @param object object to be serialized.
   * @return the serialized object as a string.
   * @throws com.oracle.coherence.io.json.genson.JsonBindingException if there was any kind of error during serialization.
   * @throws JsonStreamException                    if there was a problem during writing of the object to the output.
   */
  public String serialize(Object object) {
    StringWriter sw = new StringWriter();
    ObjectWriter writer = createWriter(sw);

    if (object == null) serializeNull(writer);
    else serialize(object, object.getClass(), writer, new Context(this));

    return sw.toString();
  }

  /**
   * Serializes the object using the type of GenericType instead of using its runtime type.
   *
   * @param object object to be serialized.
   * @param type   the type of the object to be serialized.
   * @return json string representation.
   * @throws com.oracle.coherence.io.json.genson.JsonBindingException
   * @throws JsonStreamException
   */
  public String serialize(Object object, GenericType<?> type) {
    StringWriter sw = new StringWriter();
    ObjectWriter writer = createWriter(sw);

    if (object == null) serializeNull(writer);
    else serialize(object, type.getType(), writer, new Context(this));

    return sw.toString();
  }

  /**
   * Serializes the object using the specified BeanViews.
   *
   * @param object
   * @param withViews the BeanViews to apply during this serialization.
   * @return the json string representing this object
   * @throws com.oracle.coherence.io.json.genson.JsonBindingException
   * @throws com.oracle.coherence.io.json.genson.stream.JsonStreamException
   * @see BeanView
   */
  public String serialize(Object object, Class<? extends BeanView<?>> firstView, Class<? extends BeanView<?>>... withViews) {
    StringWriter sw = new StringWriter();
    ObjectWriter writer = createWriter(sw);

    List<Class<? extends BeanView<?>>> views = new ArrayList(withViews.length);
    for (Class<? extends BeanView<?>> view : withViews) views.add(view);
    views.add(firstView);

    if (object == null) serializeNull(writer);
    else serialize(object, object.getClass(), writer, new Context(this, views));

    return sw.toString();
  }

  /**
   * Serializes this object to the passed Writer, as Genson did not instantiate it, you are
   * responsible of calling close on it.
   */
  public void serialize(Object object, GenericType<?> type, Writer writer) {
    ObjectWriter objectWriter = createWriter(writer);

    if (object == null) serializeNull(objectWriter);
    else serialize(object, type.getType(), objectWriter, new Context(this));
  }

  /**
   * Serializes this object to the passed Writer, as Genson did not instantiate it, you are
   * responsible of calling close on it.
   */
  public void serialize(Object object, Writer writer) {
    ObjectWriter objectWriter = createWriter(writer);

    if (object == null) serializeNull(objectWriter);
    else serialize(object, object.getClass(), objectWriter, new Context(this));
  }

  /**
   * Serializes this object to the passed OutputStream, as Genson did not instantiate it, you are
   * responsible of calling close on it.
   */
  public void serialize(Object object, GenericType<?> type, OutputStream output) {
    ObjectWriter objectWriter = createWriter(output);

    if (object == null) serializeNull(objectWriter);
    else serialize(object, type.getType(), objectWriter, new Context(this));
  }

  /**
   * Serializes this object to the passed OutputStream, as Genson did not instantiate it, you are
   * responsible of calling close on it.
   */
  public void serialize(Object object, OutputStream output) {
    ObjectWriter objectWriter = createWriter(output);

    if (object == null) serializeNull(objectWriter);
    else serialize(object, object.getClass(), objectWriter, new Context(this));
  }

  /**
   * Serializes this object to its json form in a byte array.
   */
  public byte[] serializeBytes(Object object) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectWriter objectWriter = createWriter(baos);

    if (object == null) serializeNull(objectWriter);
    else serialize(object, object.getClass(), objectWriter, new Context(this));

    return baos.toByteArray();
  }

  /**
   * Serializes this object and writes its representation to writer. As you are providing the
   * writer instance you also must ensure to call flush and close on it when you are done.
   *
   * @param object
   * @param writer into which to write the serialized object.
   * @throws com.oracle.coherence.io.json.genson.JsonBindingException
   * @throws JsonStreamException
   */
  public void serialize(Object object, ObjectWriter writer, Context ctx) {
    if (object == null) serializeNull(writer);
    else serialize(object, object.getClass(), writer, ctx);
  }

  /**
   * Serializes this object and writes its representation to writer. As you are providing the
   * writer instance you also must ensure to call close on it when you are done.
   */
  public void serialize(Object object, Type type, ObjectWriter writer, Context ctx) {
    Serializer<Object> ser = provideConverter(type);
    try {
      ser.serialize(object, writer, ctx);
      writer.flush();
    } catch (Exception e) {
      throw new JsonBindingException("Failed to serialize object of type " + type, e);
    }
  }

  private void serializeNull(ObjectWriter writer) {
    try {
      writer.writeNull();
      writer.flush();
    } catch (Exception e) {
      throw new JsonBindingException("Could not serialize null value.", e);
    }
  }

  /**
   * Deserializes fromSource String into an instance of toClass.
   *
   * @param fromSource source from which to deserialize.
   * @param toClass    type into which to deserialize.
   * @throws com.oracle.coherence.io.json.genson.JsonBindingException
   * @throws JsonStreamException
   */
  public <T> T deserialize(String fromSource, Class<T> toClass) {
    return deserialize(GenericType.of(toClass), createReader(new StringReader(fromSource)),
      new Context(this));
  }

  /**
   * Deserializes to an instance of T. GenericType is useful when you want to deserialize to a
   * list or map (or any other type with generics).
   *
   * @param fromSource
   * @param toType
   * @throws com.oracle.coherence.io.json.genson.JsonBindingException
   * @throws JsonStreamException
   * @see GenericType
   */
  public <T> T deserialize(String fromSource, GenericType<T> toType) {
    return deserialize(toType, createReader(new StringReader(fromSource)), new Context(this));
  }

  /**
   * Deserializes the incoming json stream into an instance of T.
   * Genson did not create the instance of Reader so it will not be closed
   */
  public <T> T deserialize(Reader reader, GenericType<T> toType) {
    return deserialize(toType, createReader(reader), new Context(this));
  }

  /**
   * Deserializes the incoming json stream into an instance of T.
   * Genson did not create the instance of Reader so it will not be closed
   */
  public <T> T deserialize(Reader reader, Class<T> toType) {
    return deserialize(GenericType.of(toType), createReader(reader), new Context(this));
  }

  /**
   * Deserializes the incoming json stream into an instance of T.
   * Genson did not create the instance of InputStream so it will not be closed
   */
  public <T> T deserialize(InputStream input, Class<T> toType) {
    return deserialize(GenericType.of(toType), createReader(input), new Context(this));
  }

  /**
   * Deserializes the incoming json stream into an instance of T.
   * Genson did not create the instance of InputStream so it will not be closed.
   */
  public <T> T deserialize(InputStream input, GenericType<T> toType) {
    return deserialize(toType, createReader(input), new Context(this));
  }

  /**
   * Deserializes the incoming json byte array into an instance of T.
   */
  public <T> T deserialize(byte[] input, Class<T> toType) {
    return deserialize(GenericType.of(toType), createReader(input), new Context(this));
  }

  /**
   * Deserializes the incoming json byte array into an instance of T.
   */
  public <T> T deserialize(byte[] input, GenericType<T> toType) {
    return deserialize(toType, createReader(input), new Context(this));
  }

  public <T> T deserialize(String fromSource, GenericType<T> toType, Class<? extends BeanView<?>>... withViews) {
    StringReader reader = new StringReader(fromSource);
    return deserialize(toType, createReader(reader),
      new Context(this, Arrays.asList(withViews)));
  }

  public <T> T deserialize(String fromSource, Class<T> toType, Class<? extends BeanView<?>>... withViews) {
    StringReader reader = new StringReader(fromSource);
    return deserialize(GenericType.of(toType), createReader(reader),
      new Context(this, Arrays.asList(withViews)));
  }

  public <T> T deserialize(GenericType<T> type, Reader reader, Class<? extends BeanView<?>>... withViews) {
    return deserialize(type, createReader(reader), new Context(this, Arrays.asList(withViews)));
  }

  public <T> T deserialize(GenericType<T> type, ObjectReader reader, Context ctx) {
    Deserializer<T> deser = provideConverter(type.getType());
    try {
      return deser.deserialize(reader, ctx);
    } catch (Exception e) {
      throw new JsonBindingException("Could not deserialize to type " + type.getRawClass(), e);
    }
  }

  /**
   * @see #deserializeInto(com.oracle.coherence.io.json.genson.stream.ObjectReader, Object, Context)
   */
  public <T> T deserializeInto(String json, T object) {
    return deserializeInto(createReader(new StringReader(json)), object, new Context(this));
  }

  /**
   * @see #deserializeInto(com.oracle.coherence.io.json.genson.stream.ObjectReader, Object, Context)
   */
  public <T> T deserializeInto(byte[] jsonBytes, T object) {
    return deserializeInto(createReader(jsonBytes), object, new Context(this));
  }

  /**
   * @see #deserializeInto(com.oracle.coherence.io.json.genson.stream.ObjectReader, Object, Context)
   */
  public <T> T deserializeInto(InputStream is, T object) {
    return deserializeInto(createReader(is), object, new Context(this));
  }

  /**
   * @see #deserializeInto(com.oracle.coherence.io.json.genson.stream.ObjectReader, Object, Context)
   */
  public <T> T deserializeInto(Reader reader, T object) {
    return deserializeInto(createReader(reader), object, new Context(this));
  }

  /**
   * Deserializes the stream in the existing object. Note however that this works only for Pojos
   * and doesn't handle nested objects (will be overridden by the values from the stream).
   *
   * @return the object enriched with the properties from the stream.
   */
  public <T> T deserializeInto(ObjectReader reader, T object, Context ctx) {
    BeanDescriptor<T> bd = (BeanDescriptor<T>) getBeanDescriptorProvider().provide(object.getClass(), this);
    bd.deserialize(object, reader, ctx);
    return object;
  }

  /**
   * @see #deserializeValues(com.oracle.coherence.io.json.genson.stream.ObjectReader, GenericType)
   */
  public <T> Iterator<T> deserializeValues(final InputStream is, final Class<T> type) {
    return deserializeValues(createReader(is), GenericType.of(type));
  }

  /**
   * This can be used to deserialize in an efficient streaming fashion a sequence of objects.
   * Note that you can use this method when your values are wrapped in an array (valid json) but also
   * when they all are root values (not enclosed in an array). For example:
   *
   * <pre>
   *   Genson genson = new Genson();
   *   ObjectReader reader = genson.createReader(json);
   *
   *   for (Iterator&lt;LogEntry> it = genson.deserializeValues(reader, GenericType.of(LogEntry.class));
   *    it.hasNext(); ) {
   *      // do something
   *      LogEntry p = it.next();
   *   }
   * </pre>
   * @param reader an instance of the ObjectReader to use (obtained with genson.createReader(...) for ex.),
   *               note that you are responsible of closing it.
   * @param type to deserialize to
   * @param <T>
   * @return an iterator of T
   */
  public <T> Iterator<T> deserializeValues(final ObjectReader reader, final GenericType<T> type) {
    final boolean isArray = reader.getValueType() == ValueType.ARRAY;
    if (isArray == true) {
      reader.beginArray();
    }

    return new Iterator<T>() {
      final Converter<T> converter = provideConverter(type.getType());
      final Context ctx = new Context(Genson.this);

      @Override
      public boolean hasNext() {
        boolean hasMore = reader.hasNext();
        if (isArray && !hasMore) reader.endArray();
        return hasMore;
      }

      @Override
      public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        reader.next();
        try {
          return converter.deserialize(reader, ctx);
        } catch (Exception e) {
          throw new JsonBindingException("Could not deserialize to type " + type.getRawClass(), e);
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

    /**
     * Searches if an alias has been registered for clazz or package. If not, it will take the class full name and
     * use it as alias. This method never returns <code>null</code>.
     */
  public <T> String aliasFor(Class<T> clazz) {
    String alias = classAliasMap.get(clazz);
    if (alias == null) {
      alias = clazz.getName();

      // next, check if the alias matches any defined package aliases
      int classNameBoundary = alias.lastIndexOf('.');
      String packageName = alias.substring(0, classNameBoundary);

      String packageAlias = packageAliasMap.get(packageName);
      if (packageAlias != null) {
        String className = alias.substring(classNameBoundary);
        alias = packageAlias + className;
        aliasClassMap.put(alias, clazz);
      }

      classAliasMap.put(clazz, alias);
    }
    return alias;
  }

  /**
   * Searches for the class matching this alias, if none will try to use the alias as the class
   * name.
   *
   * @param alias
   * @return The class matching this alias.
   * @throws ClassNotFoundException thrown if no class has been registered for this alias and the alias it self does
   *                                not correspond to the full name of a class.
   */
  public Class<?> classFor(String alias) throws ClassNotFoundException {
    Class<?> clazz = aliasClassMap.get(alias);
    if (clazz == null) {
      // check to see if this was an aliased package
      int separator = alias.indexOf('.');
      if (alias.indexOf('.', separator + 1) < 0) {
        String packageAlias = alias.substring(0, separator);
        String packageToUse = aliasPackageMap.get(packageAlias);
        if (packageToUse != null) {
          String className = alias.substring(separator);
          alias = packageToUse + className;
        }
      }

      if (loader != null) {
        clazz = Class.forName(alias, true, loader);
      } else {
        clazz = Class.forName(alias);
      }

      aliasClassMap.put(alias, clazz);
    }
    return clazz;
  }

  /**
   * Creates a new ObjectWriter with this Genson instance configuration and default encoding to
   * UTF8.
   */
  public ObjectWriter createWriter(OutputStream os) {
    return new JsonWriter(new OutputStreamWriter(os, UTF8_CHARSET), skipNull, htmlSafe, indent);
  }

  /**
   * Creates a new ObjectWriter with this Genson instance configuration.
   */
  public ObjectWriter createWriter(OutputStream os, Charset charset) {
    return createWriter(new OutputStreamWriter(os, charset));
  }

  /**
   * Creates a new ObjectWriter with this Genson instance configuration.
   */
  public ObjectWriter createWriter(Writer writer) {
    return new JsonWriter(writer, skipNull, htmlSafe, indent);
  }

  /**
   * @see #createReader(java.io.InputStream)
   */
  public ObjectReader createReader(byte[] in) {
    try {
      return createReader(readerFactory.createReader(new ByteArrayInputStream(in)));
    } catch (IOException e) {
      throw new JsonStreamException("Failed to detect encoding.", e);
    }
  }

  /**
   * Creates a new ObjectReader with this Genson instance configuration and tries to detect the encoding
   * from the stream content.
   */
  public ObjectReader createReader(InputStream is) {
    try {
      return createReader(readerFactory.createReader(is));
    } catch (IOException e) {
      throw new JsonStreamException("Failed to detect encoding.", e);
    }
  }

  /**
   * Creates a new ObjectReader with this Genson instance configuration.
   */
  public ObjectReader createReader(InputStream is, Charset charset) {
    return createReader(new InputStreamReader(is, charset));
  }

  /**
   * Creates a new ObjectReader with this Genson instance configuration.
   */
  public ObjectReader createReader(Reader reader) {
    return new JsonReader(reader, strictDoubleParse, withMetadata);
  }

  public boolean isSkipNull() {
    return skipNull;
  }

  public boolean isHtmlSafe() {
    return htmlSafe;
  }

  public boolean isWithClassMetadata() {
    return withClassMetadata;
  }

  public BeanDescriptorProvider getBeanDescriptorProvider() {
    return beanDescriptorFactory;
  }

  public boolean failOnMissingProperty() {
    return this.failOnMissingProperty;
  }

  /**
   * @return the defined default value for type clazz or null if none is defined. Intended for internal use.
   */
  public <T> T defaultValue(Class<T> clazz) {
    return (T) defaultValues.get(clazz);
  }

  /**
   * Get the default class to use for deserialization of the
   * specified {@link ValueType}.
   *
   * @param type  the {@code ValueType} to get the default class for
   *
   * @return the default class for the specified {@code ValueType}
   */
  public Class<?> defaultClass(ValueType type) {
    return defaultTypes.getClass(type);
  }

  public RuntimePropertyFilter runtimePropertyFilter() {
    return runtimePropertyFilter;
  }

  public UnknownPropertyHandler unknownPropertyHandler() {
    return unknownPropertyHandler;
  }

  public ClassLoader getClassLoader() {
    return loader;
  }

  public void setLoader(final ClassLoader loader) {
    this.loader = loader;
  }

  /**
   * @deprecated use GensonBuilder
   */
  @Deprecated
  public static class Builder extends GensonBuilder {

  }
}
