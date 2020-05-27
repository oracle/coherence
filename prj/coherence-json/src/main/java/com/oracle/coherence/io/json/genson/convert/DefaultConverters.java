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


import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.oracle.coherence.io.json.genson.*;
import com.oracle.coherence.io.json.genson.annotation.*;
import com.oracle.coherence.io.json.genson.annotation.HandleBeanView;
import com.oracle.coherence.io.json.genson.reflect.BeanProperty;
import com.oracle.coherence.io.json.genson.reflect.TypeUtil;
import com.oracle.coherence.io.json.genson.stream.JsonType;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;
import com.oracle.coherence.io.json.genson.stream.ValueType;

import static com.oracle.coherence.io.json.genson.reflect.TypeUtil.*;

/**
 * This class contains all default converters and their factories. You can read the source code <a
 * href=
 * "http://code.google.com/p/genson/source/browse/src/main/java/com/owlike/genson/convert/DefaultConverters.java"
 * >here</a> as example on how to implement custom converters and factories.
 *
 * @author eugen
 */
public final class DefaultConverters {
  private DefaultConverters() {
  }

  @HandleClassMetadata
  public static class SetConverter<E> extends CollectionConverter<E> {

    public SetConverter(Class<E> eClass, Converter<E> elementConverter) {
      super(eClass, elementConverter);
    }

    @Override
    protected Collection<E> create() {
      return new HashSet<E>();
    }
  }

  @HandleClassMetadata
  public static class LinkedListConverter<E> extends CollectionConverter<E> {

    public LinkedListConverter(Class<E> eClass, Converter<E> elementConverter) {
      super(eClass, elementConverter);
    }

    @Override
    protected Collection<E> create() {
      return new LinkedList<E>();
    }
  }

  @HandleClassMetadata
  public static class TreeSetConverter<E> extends CollectionConverter<E> {

    public TreeSetConverter(Class<E> eClass, Converter<E> elementConverter) {
      super(eClass, elementConverter);
    }

    @Override
    public void serialize(Collection<E> array, ObjectWriter writer, Context ctx) throws Exception {
      TreeSet<E> treeSet = (TreeSet<E>) array;
      if (treeSet.comparator() != null) {
        throw new UnsupportedOperationException("Serialization and deserialization of TreeSet with Comparator is not supported. " +
          "You need to implement a custom Converter to handle it.");
      }
      super.serialize(array, writer, ctx);
    }

    @Override
    protected Collection<E> create() {
      return new TreeSet<E>();
    }
  }

  @HandleClassMetadata
  public static class LinkedHashSetConverter<E> extends CollectionConverter<E> {

    public LinkedHashSetConverter(Class<E> eClass, Converter<E> elementConverter) {
      super(eClass, elementConverter);
    }

    @Override
    protected Collection<E> create() {
      return new LinkedHashSet<E>();
    }
  }

  @HandleClassMetadata
  public static class ArrayDequeConverter<E> extends CollectionConverter<E> {

    public ArrayDequeConverter(Class<E> eClass, Converter<E> elementConverter) {
      super(eClass, elementConverter);
    }

    @Override
    protected Collection<E> create() {
      return new ArrayDeque<E>();
    }
  }

  @HandleClassMetadata
  public static class PriorityQueueConverter<E> extends CollectionConverter<E> {

    public PriorityQueueConverter(Class<E> eClass, Converter<E> elementConverter) {
      super(eClass, elementConverter);
    }

    @Override
    public void serialize(Collection<E> array, ObjectWriter writer, Context ctx) throws Exception {
      PriorityQueue<E> queue = (PriorityQueue<E>) array;
      if (queue.comparator() != null) {
        throw new UnsupportedOperationException("Serialization and deserialization of PriorityQueue with Comparator is not supported. " +
          "You need to implement a custom Converter to handle it.");
      }
      super.serialize(array, writer, ctx);
    }

    @Override
    protected Collection<E> create() {
      return new PriorityQueue<E>();
    }
  }

  @HandleClassMetadata
  public static class EnumSetConverter<E> extends CollectionConverter<E> {
    private final Class<E> eClass;

    public EnumSetConverter(Class<E> eClass, Converter<E> elementConverter) {
      super(eClass, elementConverter);
      this.eClass = eClass;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected Collection<E> create() {
      return EnumSet.noneOf((Class) eClass);
    }
  }

  @HandleClassMetadata
  public static class CollectionConverter<E> implements Converter<Collection<E>> {

    @SuppressWarnings("unused")
    private final Class<E> eClass;
    private final Converter<E> elementConverter;

    public CollectionConverter(Class<E> eClass, Converter<E> elementConverter) {
      this.eClass = eClass;
      this.elementConverter = elementConverter;
    }

    public Collection<E> deserialize(ObjectReader reader, Context ctx) throws Exception {
      reader.beginArray();
      Collection<E> col = create();
      for (; reader.hasNext(); ) {
        reader.next();
        E e = elementConverter.deserialize(reader, ctx);
        col.add(e);
      }
      reader.endArray();
      return col;
    }

    public void serialize(Collection<E> array, ObjectWriter writer, Context ctx) throws Exception {
      writer.beginArray();
      for (E e : array) {
        elementConverter.serialize(e, writer, ctx);
      }
      writer.endArray();
    }

    public Converter<E> getElementConverter() {
      return elementConverter;
    }

    protected Collection<E> create() {
      return new ArrayList<E>();
    }
  }

  public final static class SingleValueAsListFactory implements Factory<Converter<Collection<?>>> {
    public final static SingleValueAsListFactory instance = new SingleValueAsListFactory();

    Factory<Converter<Collection<?>>> defaultFactory = CollectionConverterFactory.instance;

    private SingleValueAsListFactory() {}

    @Override
    public Converter<Collection<?>> create(Type type, Genson genson) {
      final CollectionConverter defaultConverter = (CollectionConverter) defaultFactory.create(type, genson);

      return new Converter<Collection<?>>() {
        @Override
        public void serialize(Collection object, ObjectWriter writer, Context ctx) throws Exception {
          defaultConverter.serialize(object, writer, ctx);
        }

        @Override
        public Collection deserialize(ObjectReader reader, Context ctx) throws Exception {
          ValueType vt = reader.getValueType();
          if (vt != ValueType.ARRAY && vt != ValueType.NULL ) {
            Object object = defaultConverter.getElementConverter().deserialize(reader, ctx);
            Collection result = defaultConverter.create();
            result.add(object);
            return result;
          } else return defaultConverter.deserialize( reader, ctx );
        }
      };
    }
  }

  public final static class CollectionConverterFactory implements Factory<Converter<Collection<?>>> {
    public final static CollectionConverterFactory instance = new CollectionConverterFactory();

    private CollectionConverterFactory() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Converter<Collection<?>> create(Type forType, Genson genson) {
      Converter<?> elementConverter = genson.provideConverter(TypeUtil.getCollectionType(forType));

      Class<?> parameterRawClass = TypeUtil.getRawClass(TypeUtil.getCollectionType(forType));
      Class<?> rawClass = getRawClass(forType);

      if (EnumSet.class.isAssignableFrom(rawClass) && parameterRawClass.isEnum())
        return new EnumSetConverter(parameterRawClass, elementConverter);
      if (LinkedHashSet.class.isAssignableFrom(rawClass))
        return new LinkedHashSetConverter(parameterRawClass, elementConverter);
      if (TreeSet.class.isAssignableFrom(rawClass))
        return new TreeSetConverter(parameterRawClass, elementConverter);
      if (Set.class.isAssignableFrom(rawClass))
        return new SetConverter(parameterRawClass, elementConverter);
      if (LinkedList.class.isAssignableFrom(rawClass))
        return new LinkedListConverter(parameterRawClass, elementConverter);
      if (ArrayDeque.class.isAssignableFrom(rawClass))
        return new ArrayDequeConverter(parameterRawClass, elementConverter);
      if (PriorityQueue.class.isAssignableFrom(rawClass))
        return new PriorityQueueConverter(parameterRawClass, elementConverter);

      return new CollectionConverter(parameterRawClass, elementConverter);
    }
  }

  @HandleClassMetadata
  public static class ArrayConverter<E> implements Converter<Object> {
    private final Class<E> eClass;
    private final Converter<E> elementConverter;

    public ArrayConverter(Class<E> eClass, Converter<E> elementConverter) {
      this.eClass = eClass;
      this.elementConverter = elementConverter;
    }

    public void serialize(Object array, ObjectWriter writer, Context ctx) throws Exception {
      writer.beginArray();
      int len = Array.getLength(array);
      for (int i = 0; i < len; i++) {
        @SuppressWarnings("unchecked")
        E e = (E) Array.get(array, i);
        elementConverter.serialize(e, writer, ctx);
      }
      writer.endArray();
    }

    public Object deserialize(ObjectReader reader, Context ctx) throws Exception {
      reader.beginArray();
      int size = 10;
      Object array = Array.newInstance(eClass, size);
      int idx = 0;
      for (; reader.hasNext(); ) {
        reader.next();
        if (idx >= size) {
          size = size * 2 + 1;
          array = expandArray(array, idx, size);
        }
        Array.set(array, idx++, elementConverter.deserialize(reader, ctx));
      }
      reader.endArray();
      if (idx < size) {
        array = expandArray(array, idx, idx);
      }
      return array;
    }

    private Object expandArray(Object array, int len, int size) {
      Object tmpArray = Array.newInstance(eClass, size);
      System.arraycopy(array, 0, tmpArray, 0, len);
      return tmpArray;
    }
  }

  @HandleClassMetadata
  public final static class ByteArrayConverter implements Converter<byte[]> {
    public static final ByteArrayConverter instance = new ByteArrayConverter();

    private ByteArrayConverter() {
    }

    @Override
    public void serialize(byte[] object, ObjectWriter writer, Context ctx) {
      writer.writeValue(object);
    }

    @Override
    public byte[] deserialize(ObjectReader reader, Context ctx) {
      return reader.valueAsByteArray();
    }
  }

  @HandleClassMetadata
  public static class ByteArrayAsIntArrayConverter implements Converter<byte[]> {
    public static final ByteArrayAsIntArrayConverter instance = new ByteArrayAsIntArrayConverter();

    private ByteArrayAsIntArrayConverter() {
    }

    @Override
    public void serialize(byte[] object, ObjectWriter writer, Context ctx) throws Exception {
      writer.beginArray();
      for (int i = 0; i < object.length; i++) writer.writeValue(object[i]);
      writer.endArray();
    }

    @Override
    public byte[] deserialize(ObjectReader reader, Context ctx) throws Exception {
      byte[] array = new byte[256];
      reader.beginArray();
      int i;
      for (i = 0; reader.hasNext(); i++) {
        reader.next();
        Operations.expandArray(array, i, 2);
        array[i] = (byte) reader.valueAsInt();
      }
      reader.endArray();

      return Operations.truncateArray(array, i);
    }
  }

  public final static class ArrayConverterFactory implements Factory<Converter<Object>> {
    public final static ArrayConverterFactory instance = new ArrayConverterFactory();

    private ArrayConverterFactory() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Converter<Object> create(Type forType, Genson genson) {
      if (forType instanceof GenericArrayType
        || (forType instanceof Class<?> && ((Class<?>) forType).isArray())) {
        if (byte.class.equals(getCollectionType(forType))) {
          return (Converter) ByteArrayConverter.instance;
        } else {
          Converter<?> elementConverter = genson.provideConverter(TypeUtil
            .getCollectionType(forType));
          return new ArrayConverter(TypeUtil.getRawClass(TypeUtil
            .getCollectionType(forType)), elementConverter);
        }
      }
      return null;
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public final static class StringConverter implements Converter<String> {
    public final static StringConverter instance = new StringConverter();

    private StringConverter() {
    }

    public void serialize(String value, ObjectWriter writer, Context ctx) {
      writer.writeValue(value);
    }

    public String deserialize(ObjectReader reader, Context ctx) {
      return reader.valueAsString();
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public final static class BooleanConverter implements Converter<Boolean> {
    public final static BooleanConverter instance = new BooleanConverter();

    private BooleanConverter() {
    }

    public void serialize(Boolean obj, ObjectWriter writer, Context ctx) {
      writer.writeValue(obj.booleanValue());
    }

    public Boolean deserialize(ObjectReader reader, Context ctx) {
      return reader.valueAsBoolean();
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public final static class IntegerConverter implements Converter<Integer> {
    public final static IntegerConverter instance = new IntegerConverter();

    private IntegerConverter() {
    }

    public void serialize(Integer obj, ObjectWriter writer, Context ctx) {
      writer.writeValue(obj.intValue());
    }

    public Integer deserialize(ObjectReader reader, Context ctx) {
      return reader.valueAsInt();
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public final static class LongConverter implements Converter<Long> {
    public final static LongConverter instance = new LongConverter();

    private LongConverter() {
    }

    public Long deserialize(ObjectReader reader, Context ctx) {
      return reader.valueAsLong();
    }

    public void serialize(Long obj, ObjectWriter writer, Context ctx) {
      writer.writeValue(obj.longValue());
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public final static class ShortConverter implements Converter<Short> {
    public final static ShortConverter instance = new ShortConverter();

    private ShortConverter() {
    }

    public Short deserialize(ObjectReader reader, Context ctx) {
      return reader.valueAsShort();
    }

    public void serialize(Short obj, ObjectWriter writer, Context ctx) {
      writer.writeValue(obj.shortValue());
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public final static class DoubleConverter implements Converter<Double> {
    public final static DoubleConverter instance = new DoubleConverter();

    private DoubleConverter() {
    }

    public Double deserialize(ObjectReader reader, Context ctx) {
      return reader.valueAsDouble();
    }

    public void serialize(Double obj, ObjectWriter writer, Context ctx) {
      if (obj.isNaN() || obj.isInfinite()) {
        writer.writeUnsafeValue(obj.toString());
      } else {
        writer.writeValue(obj.doubleValue());
      }
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public final static class FloatConverter implements Converter<Float> {
    public final static FloatConverter instance = new FloatConverter();

    private FloatConverter() {
    }

    public Float deserialize(ObjectReader reader, Context ctx) {
      return reader.valueAsFloat();
    }

    public void serialize(Float obj, ObjectWriter writer, Context ctx) {
      if (obj.isNaN() || obj.isInfinite()) {
        writer.writeUnsafeValue(obj.toString());
      } else {
        writer.writeValue(obj.floatValue());
      }
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public final static class NumberConverter implements Converter<Number> {
    public final static NumberConverter instance = new NumberConverter();

    private NumberConverter() {
    }

    public Number deserialize(ObjectReader reader, Context ctx) {
      ValueType vt = reader.getValueType();
      if (ValueType.INTEGER.equals(vt))
        try {
          return reader.valueAsInt();
        }
        catch (NumberFormatException e) {
          // likely overflowing, let's try a long
          return reader.valueAsLong();
        }
      else if (ValueType.DOUBLE.equals(vt))
        return reader.valueAsDouble();
      else {
        String value = reader.valueAsString();
        return "".equals(value) ? null : parse(value, vt);
      }
    }

    public void serialize(Number obj, ObjectWriter writer, Context ctx) {
      if (isSpecialNumber(obj)) writer.writeUnsafeValue(obj.toString()); else writer.writeValue(obj);
    }

    private boolean isSpecialNumber(Number v) {
      if (v instanceof Double || v instanceof Float) {
        Double num = (Double) v;
        return num.isInfinite() || num.isNaN();
      } else {
        return false;
      }
    }

    private Number parse(String value, ValueType valueType) {
      try {
        if (value.indexOf('.') >= 0) {
          return Double.parseDouble(value);
        }
        long longValue = Long.parseLong(value);
        if (longValue <= Integer.MAX_VALUE && longValue >= Integer.MIN_VALUE) {
          return Integer.valueOf((int) longValue);
        }
        return Long.parseLong(value);
      } catch (NumberFormatException nfe) {
        throw new JsonBindingException("Could not convert input value " + value
          + " of type " + valueType + " to a Number type.", nfe);
      }
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public final static class CharConverter implements Converter<Character> {
    public final static CharConverter instance = new CharConverter();

    private CharConverter() {
    }

    public void serialize(Character obj, ObjectWriter writer, Context ctx) {
      writer.writeValue(String.format("\\u%04x", (int) obj)); // only supports basic lingual plane
    }

    public Character deserialize(ObjectReader reader, Context ctx) {
      String str = reader.valueAsString();
      if (str.length() > 1) throw new JsonBindingException(
        "Could not convert a string with length greater than 1 to a single char."
      );

      return str.charAt(0);
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public final static class ByteConverter implements Converter<Byte> {
    public final static ByteConverter instance = new ByteConverter();

    private ByteConverter() {
    }

    public void serialize(Byte obj, ObjectWriter writer, Context ctx) {
      writer.writeValue(obj.byteValue());
    }

    public Byte deserialize(ObjectReader reader, Context ctx) {
      return (byte) reader.valueAsInt();
    }
  }

  public final static class PrimitiveConverterFactory implements Factory<Converter<?>> {
    public final static PrimitiveConverterFactory instance = new PrimitiveConverterFactory();

    private PrimitiveConverterFactory() {
    }

    public Converter<?> create(Type type, Genson genson) {
      Class<?> rawClass = TypeUtil.getRawClass(type);

      if (rawClass == Boolean.class || rawClass == boolean.class) return BooleanConverter.instance;
      if (rawClass == Integer.class || rawClass == int.class) return IntegerConverter.instance;
      if (rawClass == Double.class || rawClass == double.class) return DoubleConverter.instance;
      if (rawClass == Long.class || rawClass == long.class) return LongConverter.instance;
      if (rawClass == Short.class || rawClass == short.class) return ShortConverter.instance;
      if (rawClass == Float.class || rawClass == float.class) return FloatConverter.instance;
      if (rawClass == Character.class || rawClass == char.class) return CharConverter.instance;
      if (rawClass == Byte.class || rawClass == byte.class) return ByteConverter.instance;

      return null;
    }
  }

  @HandleClassMetadata
  public static abstract class MapConverter<K, V> implements Converter<Map<K, V>> {
    private final Converter<V> valueConverter;
    private final KeyAdapter<K> keyAdapter;

    public MapConverter(KeyAdapter<K> keyAdapter, Converter<V> valueConverter) {
      this.keyAdapter = keyAdapter;
      this.valueConverter = valueConverter;
    }

    public Map<K, V> deserialize(ObjectReader reader, Context ctx) throws Exception {
      reader.beginObject();
      Map<K, V> map = create();
      for (; reader.hasNext(); ) {
        reader.next();
        map.put(keyAdapter.adapt(reader.name()), valueConverter.deserialize(reader, ctx));
      }
      reader.endObject();
      return map;
    }

    public void serialize(Map<K, V> obj, ObjectWriter writer, Context ctx) throws Exception {
      writer.beginObject();
      for (Map.Entry<K, V> entry : obj.entrySet()) {
        writer.writeName(keyAdapter.adapt(entry.getKey()));
        valueConverter.serialize(entry.getValue(), writer, ctx);
      }
      writer.endObject();
    }

    protected abstract Map<K, V> create();
  }

  public final static class HashMapConverter<K, V> extends MapConverter<K, V> {
    public HashMapConverter(KeyAdapter<K> keyAdapter, Converter<V> valueConverter) {
      super(keyAdapter, valueConverter);
    }

    @Override
    protected Map<K, V> create() {
      return new HashMap<K, V>();
    }
  }

  public final static class HashTableConverter<K, V> extends MapConverter<K, V> {
    public HashTableConverter(KeyAdapter<K> keyAdapter, Converter<V> valueConverter) {
      super(keyAdapter, valueConverter);
    }

    @Override
    protected Map<K, V> create() {
      return new Hashtable<K, V>();
    }
  }

  @SuppressWarnings("rawtypes")
  public final static class PropertiesConverter extends MapConverter {
    @SuppressWarnings("unchecked")
    public PropertiesConverter(KeyAdapter keyAdapter, Converter valueConverter) {
      super(keyAdapter, valueConverter);
    }

    @Override
    protected Map create() {
      return new Properties();
    }
  }

  public final static class TreeMapConverter<K, V> extends MapConverter<K, V> {
    public TreeMapConverter(KeyAdapter<K> keyAdapter, Converter<V> valueConverter) {
      super(keyAdapter, valueConverter);
    }

    @Override
    public void serialize(Map<K, V> obj, ObjectWriter writer, Context ctx) throws Exception {
      TreeMap<K, V> treeMap = (TreeMap<K, V>) obj;
      if (((TreeMap<K, V>) obj).comparator() != null)
        throw new UnsupportedOperationException("Serialization and deserialization of TreeMap with Comparator is not supported. " +
          "You need to implement a custom Converter to handle it.");

      super.serialize(obj, writer, ctx);
    }

    @Override
    protected Map<K, V> create() {
      return new TreeMap<K, V>();
    }
  }

  public final static class LinkedHashMapConverter<K, V> extends MapConverter<K, V> {
    public LinkedHashMapConverter(KeyAdapter<K> keyAdapter, Converter<V> valueConverter) {
      super(keyAdapter, valueConverter);
    }

    @Override
    protected Map<K, V> create() {
      return new LinkedHashMap<K, V>();
    }
  }

  public static abstract class KeyAdapter<K> {
    public abstract K adapt(String str);

    public abstract String adapt(K key);

    public final static KeyAdapter<Object> runtimeAdapter = new KeyAdapter<Object>() {
      @Override
      public Object adapt(String str) {
        return str;
      }

      @Override
      public String adapt(Object key) {
        return key.toString();
      }
    };

    public final static KeyAdapter<String> strAdapter = new KeyAdapter<String>() {
      @Override
      public String adapt(String key) {
        return key;
      }
    };

    public final static KeyAdapter<Short> shortAdapter = new KeyAdapter<Short>() {
      @Override
      public Short adapt(String str) {
        return Short.parseShort(str);
      }

      @Override
      public String adapt(Short key) {
        return key.toString();
      }
    };

    public final static KeyAdapter<Integer> intAdapter = new KeyAdapter<Integer>() {
      @Override
      public Integer adapt(String str) {
        return Integer.parseInt(str);
      }

      @Override
      public String adapt(Integer key) {
        return key.toString();
      }
    };

    public final static KeyAdapter<Long> longAdapter = new KeyAdapter<Long>() {
      @Override
      public Long adapt(String str) {
        return Long.parseLong(str);
      }

      @Override
      public String adapt(Long key) {
        return key.toString();
      }
    };

    public final static KeyAdapter<Float> floatAdapter = new KeyAdapter<Float>() {
      @Override
      public Float adapt(String str) {
        return Float.parseFloat(str);
      }

      @Override
      public String adapt(Float key) {
        return key.toString();
      }
    };

    public final static KeyAdapter<Double> doubleAdapter = new KeyAdapter<Double>() {
      @Override
      public Double adapt(String str) {
        return Double.parseDouble(str);
      }

      @Override
      public String adapt(Double key) {
        return key.toString();
      }
    };
  }

  @HandleClassMetadata
  public static class ComplexMapConverter<K, V> implements Converter<Map<K, V>> {
    private final Converter<K> keyConverter;
    private final Converter<V> valueConverter;

    private ComplexMapConverter(Converter<K> keyConverter, Converter<V> valueConverter) {
      super();
      this.keyConverter = keyConverter;
      this.valueConverter = valueConverter;
    }

    @Override
    public void serialize(Map<K, V> object, ObjectWriter writer, Context ctx) throws Exception {
      writer.beginArray();
      for (Map.Entry<K, V> entry : object.entrySet()) {
        writer.beginObject().writeName("key");
        keyConverter.serialize(entry.getKey(), writer, ctx);
        writer.writeName("value");
        valueConverter.serialize(entry.getValue(), writer, ctx);
        writer.endObject();
      }
      writer.endArray();
    }

    @Override
    public Map<K, V> deserialize(ObjectReader reader, Context ctx) throws Exception {
      Map<K, V> map = new HashMap<K, V>();
      reader.beginArray();
      while (reader.hasNext()) {
        reader.next();
        reader.beginObject();
        K key = null;
        V value = null;
        while (reader.hasNext()) {
          reader.next();
          if ("key".equals(reader.name())) {
            key = keyConverter.deserialize(reader, ctx);
          } else if ("value".equals(reader.name())) {
            value = valueConverter.deserialize(reader, ctx);
          }
        }
        map.put(key, value);
        reader.endObject();
      }
      reader.endArray();
      return map;
    }
  }

  public final static class MapConverterFactory implements Factory<Converter<? extends Map<?, ?>>> {
    public final static MapConverterFactory instance = new MapConverterFactory();

    private MapConverterFactory() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Converter<? extends Map<?, ?>> create(Type type, Genson genson) {
      // ok this is a fix but not the cleanest one... we make sure it is a parameterized type
      // otherwise we search for map impl in its hierarchy
      Type expandedType = type;
      if (getRawClass(type).getTypeParameters().length == 0) {
        expandedType = expandType(lookupGenericType(Map.class, getRawClass(type)), type);
      }

      Type keyType = typeOf(0, expandedType);
      Type valueType = typeOf(1, expandedType);
      Class<?> keyRawClass = getRawClass(keyType);
      KeyAdapter<?> keyAdapter = keyAdapter(keyRawClass);

      if (keyAdapter != null)
        return createConverter(getRawClass(type), keyAdapter, genson.provideConverter(valueType));
      else
        return new ComplexMapConverter(genson.provideConverter(keyType), genson.provideConverter(valueType));
    }

    public static KeyAdapter<?> keyAdapter(Class<?> keyRawClass) {
      if (Object.class.equals(keyRawClass)) return KeyAdapter.runtimeAdapter;
      else if (String.class.equals(keyRawClass)) return KeyAdapter.strAdapter;
      else if (int.class.equals(keyRawClass) || Integer.class.equals(keyRawClass))
        return KeyAdapter.intAdapter;
      else if (double.class.equals(keyRawClass) || Double.class.equals(keyRawClass))
        return KeyAdapter.doubleAdapter;
      else if (long.class.equals(keyRawClass) || Long.class.equals(keyRawClass))
        return KeyAdapter.longAdapter;
      else if (float.class.equals(keyRawClass) || Float.class.equals(keyRawClass))
        return KeyAdapter.floatAdapter;
      else if (short.class.equals(keyRawClass) || Short.class.equals(keyRawClass))
        return KeyAdapter.shortAdapter;
      else return null;
    }

    @SuppressWarnings("unchecked")
    private <K, V> MapConverter<K, V> createConverter(Class<?> typeOfMap,
                                                      KeyAdapter<K> keyAdapter, Converter<V> valueConverter) {
      if (Properties.class.equals(typeOfMap))
        return new PropertiesConverter(keyAdapter, valueConverter);

      if (Hashtable.class.equals(typeOfMap))
        return new HashTableConverter<K, V>(keyAdapter, valueConverter);

      if (TreeMap.class.equals(typeOfMap))
        return new TreeMapConverter<K, V>(keyAdapter, valueConverter);

      if (LinkedHashMap.class.equals(typeOfMap))
        return new LinkedHashMapConverter<K, V>(keyAdapter, valueConverter);

      return new HashMapConverter<K, V>(keyAdapter, valueConverter);
    }
  }

  @SuppressWarnings("rawtypes")
  public static class DateContextualFactory implements ContextualFactory {
    @Override
    public Converter create(BeanProperty property, Genson genson) {
      JsonDateFormat ann = property.getAnnotation(JsonDateFormat.class);
      if (ann != null) {
        Locale locale = ann.lang().isEmpty() ? Locale.getDefault() : new Locale(
          ann.lang());
        DateFormat dateFormat = ann.value() != null && !ann.value().isEmpty() ?
          new SimpleDateFormat(ann.value(), locale) : SimpleDateFormat.getInstance();

        if (Date.class.isAssignableFrom(property.getRawClass()))
          return new DateConverter(dateFormat, ann.asTimeInMillis());
        if (Calendar.class.isAssignableFrom(property.getRawClass()))
          return new CalendarConverter(
            new DateConverter(dateFormat, ann.asTimeInMillis()));
      }
      return null;
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public static class DateConverter implements Converter<Date> {
    private DateFormat dateFormat;
    private final boolean asTimeInMillis;

    public DateConverter() {
      this(SimpleDateFormat.getDateTimeInstance(), true);
    }

    public DateConverter(DateFormat dateFormat, boolean asTimeInMillis) {
      if (dateFormat == null) dateFormat = SimpleDateFormat.getDateTimeInstance();
      this.dateFormat = dateFormat;
      this.asTimeInMillis = asTimeInMillis;
    }

    public void serialize(Date obj, ObjectWriter writer, Context ctx) {
      if (asTimeInMillis)
        writer.writeValue(obj.getTime());
      else
        writer.writeUnsafeValue(format(obj));
    }

    protected synchronized String format(Date date) {
      return dateFormat.format(date);
    }

    public Date deserialize(ObjectReader reader, Context ctx) {
      try {
        ValueType valueType = reader.getValueType();
        if (valueType == ValueType.INTEGER)
          return new Date(reader.valueAsLong());
        else if (valueType == ValueType.STRING)
          return read(reader.valueAsString());
        else throw new JsonBindingException(String.format("Can not deserialize type %s to Date, " +
            "only numeric and string accepted.", valueType));
      } catch (ParseException e) {
        throw new JsonBindingException("Could not parse date " + reader.valueAsString(),
          e);
      }
    }

    protected synchronized Date read(String dateString) throws ParseException {
      return dateFormat.parse(dateString);
    }
  }

  public final static class UntypedConverterFactory implements Factory<Converter<Object>> {
    public final static UntypedConverterFactory instance = new UntypedConverterFactory();

    private UntypedConverterFactory() {
    }

    public final static class UntypedConverter implements Converter<Object> {
      final static UntypedConverter instance = new UntypedConverter();

      private UntypedConverter() {
      }

      public Object deserialize(ObjectReader reader, Context ctx) {
        Class<?> defaultClass = ctx.genson.defaultClass(reader.getValueType());
        return ctx.genson.deserialize(GenericType.of(defaultClass), reader, ctx);
      }

      public void serialize(Object obj, ObjectWriter writer, Context ctx) {
        if (Object.class.equals(obj.getClass()))
          throw new UnsupportedOperationException(
            "Serialization of type Object is not supported by default serializers.");
        ctx.genson.serialize(obj, obj.getClass(), writer, ctx);
      }
    }

    public Converter<Object> create(Type type, Genson genson) {
      if (TypeUtil.match(type, Object.class, true)) {
        return UntypedConverter.instance;
      }
      return null;
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public static class EnumConverter<T extends Enum<T>> implements Converter<T> {
    private final Class<T> eClass;
    private final Map<String, T> deserializationNames;
    private final boolean caseSensitive;

    public EnumConverter(Class<T> eClass, boolean caseSensitive) {
      this.eClass = eClass;
      this.caseSensitive = caseSensitive;
      deserializationNames = new HashMap<String, T>();
      for (Field f : eClass.getFields()) {
        try {
          if (!f.isEnumConstant()) {
            continue;
          }
          String name = caseSensitive ? f.getName(): f.getName().toUpperCase();
          deserializationNames.put(name, (T) f.get(null));
        } catch (IllegalAccessException e) {
          throw new JsonBindingException("Failed to get enum value " + f.getName(), e);
        }
      }
    }

    public void serialize(T obj, ObjectWriter writer, Context ctx) {
      writer.writeUnsafeValue(obj.name());
    }

    public T deserialize(ObjectReader reader, Context ctx) {
      String name = caseSensitive ? reader.valueAsString(): reader.valueAsString().toUpperCase();
      T value = deserializationNames.get(name);
      if (value == null) throw new JsonBindingException("No enum constant " + eClass.getCanonicalName() + "." + name);
      return value;
    }
  }

  public final static class EnumConverterFactory implements Factory<Converter<? extends Enum<?>>> {
    public final static EnumConverterFactory instance = new EnumConverterFactory(true);
    public final boolean caseSensitive;

    public EnumConverterFactory(boolean caseSensitive) {
      this.caseSensitive = caseSensitive;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public Converter<Enum<?>> create(Type type, Genson genson) {
      Class<?> rawClass = TypeUtil.getRawClass(type);
      return rawClass.isEnum() || Enum.class.isAssignableFrom(rawClass) ? new EnumConverter(
        rawClass, caseSensitive) : null;
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public static class URLConverter implements Converter<URL> {
    public final static URLConverter instance = new URLConverter();

    private URLConverter() {
    }

    public URL deserialize(ObjectReader reader, Context ctx) {
      try {
        return new URL(reader.valueAsString());
      } catch (MalformedURLException e) {
        throw new JsonBindingException("Can not deserializer <" + reader.valueAsString() + "> to URL.");
      }
    }

    public void serialize(URL object, ObjectWriter writer, Context ctx) {
      writer.writeValue(object.toExternalForm());
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public static class URIConverter implements Converter<URI> {
    public final static URIConverter instance = new URIConverter();

    private URIConverter() {
    }

    public void serialize(URI object, ObjectWriter writer, Context ctx) {
      writer.writeUnsafeValue(object.toString());
    }

    public URI deserialize(ObjectReader reader, Context ctx) {
      return URI.create(reader.valueAsString());
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public static class BigDecimalConverter implements Converter<BigDecimal> {
    public final static BigDecimalConverter instance = new BigDecimalConverter();

    private BigDecimalConverter() {
    }

    @Override
    public BigDecimal deserialize(ObjectReader reader, Context ctx) {
      return new BigDecimal(reader.valueAsString());
    }

    @Override
    public void serialize(BigDecimal object, ObjectWriter writer, Context ctx) {
      writer.writeValue(object.toString());
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public static class BigIntegerConverter implements Converter<BigInteger> {
    public final static BigIntegerConverter instance = new BigIntegerConverter();

    private BigIntegerConverter() {
    }

    @Override
    public BigInteger deserialize(ObjectReader reader, Context ctx) {
      return new BigInteger(reader.valueAsString());
    }

    @Override
    public void serialize(BigInteger object, ObjectWriter writer, Context ctx) {
      writer.writeValue(object.toString());
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public static class TimestampConverter implements Converter<Timestamp> {
    public final static TimestampConverter instance = new TimestampConverter();

    private TimestampConverter() {
    }

    @Override
    public Timestamp deserialize(ObjectReader reader, Context ctx) {
      return Timestamp.valueOf(reader.valueAsString());
    }

    @Override
    public void serialize(Timestamp object, ObjectWriter writer, Context ctx) {
      writer.writeValue(object.toString());
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public static class UUIDConverter implements Converter<UUID> {
    public final static UUIDConverter instance = new UUIDConverter();

    private UUIDConverter() {
    }

    @Override
    public void serialize(UUID object, ObjectWriter writer, Context ctx) {
      writer.writeValue(object.toString());
    }

    @Override
    public UUID deserialize(ObjectReader reader, Context ctx) {
      return UUID.fromString(reader.valueAsString());
    }

  }

  public final static class CalendarConverterFactory implements Factory<Converter<Calendar>> {
    private final CalendarConverter calendarConverter;

    public CalendarConverterFactory(DateConverter dateConverter) {
      this.calendarConverter = new CalendarConverter(dateConverter);
    }

    @Override
    public Converter<Calendar> create(Type type, Genson genson) {
      if (!Calendar.class.isAssignableFrom(TypeUtil.getRawClass(type)))
        throw new IllegalStateException(
          "CalendarConverterFactory create method can be called only for Calendar type and subtypes.");
      return calendarConverter;
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public static class CalendarConverter implements Converter<Calendar> {
    private final DateConverter dateConverter;

    CalendarConverter(final DateConverter dateConverter) {
      this.dateConverter = dateConverter;
    }

    @Override
    public void serialize(Calendar object, ObjectWriter writer, Context ctx) {
      dateConverter.serialize(object.getTime(), writer, ctx);
    }

    @Override
    public Calendar deserialize(ObjectReader reader, Context ctx) {
      Calendar cal = null;
      if (ValueType.NULL != reader.getValueType()) {
        cal = new GregorianCalendar();
        cal.setTime(dateConverter.deserialize(reader, ctx));
      }
      return cal;
    }
  }

  @HandleClassMetadata
  @HandleBeanView
  public final static class FileConverter implements Converter<File> {
    public final static FileConverter instance = new FileConverter();

    private FileConverter() {
    }

    @Override
    public void serialize(File object, ObjectWriter writer, Context ctx) {
      writer.writeValue(object.getPath());
    }

    @Override
    public File deserialize(ObjectReader reader, Context ctx) {
      return new File(reader.valueAsString());
    }

  }

  public final static class PropertyConverterFactory implements ContextualFactory<Object> {

    @SuppressWarnings("unchecked")
    @Override
    public Converter<Object> create(BeanProperty property, Genson genson) {
      JsonConverter ann = property.getAnnotation(JsonConverter.class);
      if (ann != null) {
        Type converterExpandedType = expandType(
          lookupGenericType(Converter.class, ann.value()), ann.value());
        Type converterPropertyType = typeOf(0, converterExpandedType);

        Class<?> propertyClass = property.getRawClass();
        if (propertyClass.isPrimitive()) propertyClass = wrap(propertyClass);

        // checking type consistency
        if (!match(propertyClass, converterPropertyType, false))
          throw new ClassCastException("The type defined in " + ann.value().getName()
            + " is not assignale from property " + property.getName()
            + " declared in " + property.getDeclaringClass());

        try {
          Constructor<?> ctr = ann.value().getConstructor();
          if (!ctr.isAccessible()) ctr.setAccessible(true);
          return (Converter<Object>) ctr.newInstance();

          // OMG...
        } catch (InstantiationException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } catch (SecurityException e) {
          throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
          throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
          throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
      return null;
    }
  }

  public static class WrappedRootValueConverter<T> implements Converter<T> {
    private final String inputName;
    private final String outputName;
    private final Converter<T> delegateConverter;

    public WrappedRootValueConverter(String inputName, String outputName, Converter<T> delegateConverter) {
      this.inputName = inputName;
      this.outputName = outputName;
      this.delegateConverter = delegateConverter;
    }

    @Override
    public void serialize(T object, ObjectWriter writer, Context ctx) throws Exception {
      if (writer.enclosingType() == JsonType.EMPTY) {
        writer.beginObject().writeName(outputName);
        delegateConverter.serialize(object, writer, ctx);
        writer.endObject();
      }
    }

    @Override
    public T deserialize(ObjectReader reader, Context ctx) throws Exception {
      T value = null;
      if (reader.enclosingType() == JsonType.EMPTY) {
        reader.beginObject();
        // Lets accept the case where the key is missing
        if (reader.hasNext()) {
          reader.next();

          if (!inputName.equalsIgnoreCase(reader.name())) {
            throw new JsonBindingException(
                String.format(
                    "Expected key %s for unwrapping the value, but encountered key %s",
                    inputName,
                    reader.name()
                )
            );
          }

          value = delegateConverter.deserialize(reader, ctx);
        }
        reader.endObject();
      }
      return value;
    }
  }

  public static class OptionalConverterFactory implements Factory<Converter<Optional<?>>> {
    public static final OptionalConverterFactory instance = new OptionalConverterFactory();
    private OptionalConverterFactory() {
    }

    @Override
    public Converter<Optional<?>> create(final Type type, final Genson genson) {
      Type optionalType = typeOf(0, type);
      //noinspection unchecked
      return new OptionalConverter(genson.provideConverter(optionalType));
    }
  }

  public static class OptionalConverter<T> implements Converter<Optional<T>> {

    private Converter<T> converter;

    public OptionalConverter(Converter<T> converter) {
      this.converter = converter;
    }

    @Override
    public void serialize(final Optional<T> optional, final ObjectWriter writer, final Context ctx) throws Exception {
      writer.beginObject();
      if (optional.isPresent()) {
        writer.writeName("value");
        converter.serialize(optional.get(), writer, ctx);
      }
      writer.endObject();
    }

    @Override
    public Optional<T> deserialize(final ObjectReader reader, final Context ctx) throws Exception {
      reader.beginObject();
      try {
        while (reader.hasNext()) {
          reader.next();
          if ("value".equals(reader.name())) {
            return Optional.of(converter.deserialize(reader, ctx));
          }
        }
        return Optional.empty();
      } finally {
        reader.endObject();
      }
    }
  }

  public static class OptionalIntConverter implements Converter<OptionalInt> {
    public final static OptionalIntConverter instance = new OptionalIntConverter();

    private OptionalIntConverter() {
    }

    @Override
    public void serialize(final OptionalInt optional, final ObjectWriter writer, final Context ctx) throws Exception {
      writer.beginObject();
      if (optional.isPresent()) {
        writer.writeName("value").writeValue(optional.getAsInt());
      }
      writer.endObject();
    }

    @Override
    public OptionalInt deserialize(final ObjectReader reader, final Context ctx) throws Exception {
      reader.beginObject();
      try {
        while (reader.hasNext()) {
          reader.next();
          if ("value".equals(reader.name())) {
            return OptionalInt.of(reader.valueAsInt());
          }
        }
        return OptionalInt.empty();
      } finally {
        reader.endObject();
      }
    }
  }

  public static class OptionalLongConverter implements Converter<OptionalLong> {
    public final static OptionalLongConverter instance = new OptionalLongConverter();

    private OptionalLongConverter() {
    }

    @Override
    public void serialize(final OptionalLong optional, final ObjectWriter writer, final Context ctx) throws Exception {
      writer.beginObject();
      if (optional.isPresent()) {
        writer.writeName("value").writeValue(optional.getAsLong());
      }
      writer.endObject();
    }

    public OptionalLong deserialize(final ObjectReader reader, final Context ctx) throws Exception {
      reader.beginObject();
      try {
        while (reader.hasNext()) {
          reader.next();
          if ("value".equals(reader.name())) {
            return OptionalLong.of(reader.valueAsLong());
          }
        }
        return OptionalLong.empty();
      } finally {
        reader.endObject();
      }
    }
  }

  public static class OptionalDoubleConverter implements Converter<OptionalDouble> {
    public final static OptionalDoubleConverter instance = new OptionalDoubleConverter();

    private OptionalDoubleConverter() {
    }

    @Override
    public void serialize(final OptionalDouble optional, final ObjectWriter writer, final Context ctx) throws Exception {
      writer.beginObject();
      if (optional.isPresent()) {
        writer.writeName("value").writeValue(optional.getAsDouble());
      }
      writer.endObject();
    }

    @Override
    public OptionalDouble deserialize(final ObjectReader reader, final Context ctx) throws Exception {
      reader.beginObject();
      try {
        while (reader.hasNext()) {
          reader.next();
          if ("value".equals(reader.name())) {
            return OptionalDouble.of(reader.valueAsDouble());
          }
        }
        return OptionalDouble.empty();
      } finally {
        reader.endObject();
      }
    }
  }
}
