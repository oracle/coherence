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

package com.oracle.coherence.io.json.genson.functional;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.oracle.coherence.io.json.genson.*;

import com.oracle.coherence.io.json.genson.JsonBindingException;
import com.oracle.coherence.io.json.genson.convert.DefaultConverters;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultConvertersTest {
  private Genson genson = new Genson();

  @Test public void testCaseInsensitiveEnum() {
    try {
      genson.deserialize("\"ReD\"", Color.class);
      fail();
    } catch(JsonBindingException e) {}

    Color actual = new GensonBuilder().withConverterFactory(new DefaultConverters.EnumConverterFactory(false)).create()
                     .deserialize("\"ReD\"", Color.class);

    assertEquals(Color.red, actual);
  }

  @Test
  public void testReadWriteByteAsInt() {
    Genson genson = new GensonBuilder().useConstructorWithArguments(true).useByteAsInt(true).create();
    PojoWithByteArray expected = new PojoWithByteArray(5, 777.777, "ABCD".getBytes());
    String json = genson.serialize(expected);
    PojoWithByteArray actual = genson.deserialize(json, PojoWithByteArray.class);

    assertEquals("{\"b\":[65,66,67,68],\"f\":777.777,\"i\":5}", json);
    assertArrayEquals(expected.b, actual.b);
    assertEquals(expected.f, actual.f, 1e-21);
    assertEquals(expected.i, actual.i);
  }

  @Test
  public void testRoundTripOfLinkedList() {
    testCollectionImplementationsRoundTrip(LinkedList.class);
  }

  @Test
  public void testRoundTripOfTreeSet() {
    testCollectionImplementationsRoundTrip(TreeSet.class);
  }

  @Test(expected = JsonBindingException.class)
  public void testSerializationOfSortedSetWithComparatorShouldFail() {
    genson.serialize(new TreeSet<Object>(new Comparator<Object>() {
      @Override
      public int compare(Object o1, Object o2) {
        return 0;
      }
    }));
  }

  @Test
  public void testRoundTripOfLinkedHashSet() {
    testCollectionImplementationsRoundTrip(LinkedHashSet.class);
  }

  @Test
  public void testRoundTripOfArrayDeque() {
    testCollectionImplementationsRoundTrip(ArrayDeque.class);
  }

  @Test
  public void testRoundTripOfPriorityQueue() {
    testCollectionImplementationsRoundTrip(PriorityQueue.class);
  }

  @Test
  public void testRoundTripOfTreeMap() {
    testMapImplementationsRoundTrip(TreeMap.class);
  }

  @Test
  public void testRoundTripOfLinkedHashMap() {
    testMapImplementationsRoundTrip(LinkedHashMap.class);
  }

  @Test
  public void testPojoWithBytes() {
    Genson genson = new GensonBuilder().useConstructorWithArguments(true).create();
    PojoWithByteArray expected = new PojoWithByteArray(5, 777.777, "ABCD".getBytes());
    String json = genson.serialize(expected);
    PojoWithByteArray actual = genson.deserialize(json, PojoWithByteArray.class);

    assertEquals("{\"b\":\"QUJDRA==\",\"f\":777.777,\"i\":5}", json);
    assertArrayEquals(expected.b, actual.b);
    assertEquals(expected.f, actual.f, 1e-21);
    assertEquals(expected.i, actual.i);
  }

  @Test
  public void testByteArray() throws UnsupportedEncodingException {
    byte[] byteArray = "hey convert me to bytes".getBytes("UTF-8");
    String json = genson.serialize(byteArray);
    assertArrayEquals(byteArray, genson.deserialize(json, byte[].class));
  }

  @Test
  public void testEnumSet() {
    EnumSet<Color> foo = EnumSet.of(Color.blue, Color.red);
    String json = genson.serialize(foo);
    EnumSet<Color> bar = genson.deserialize(json, new GenericType<EnumSet<Color>>() {
    });
    assertTrue(bar.contains(Color.blue));
    assertTrue(bar.contains(Color.red));
  }

  @Test
  public void testClassMetadataOnceWhenUsedWithRuntimeType() {
    Genson genson = new GensonBuilder().useRuntimeType(true)
      .addAlias("subBean", SubBean.class).useClassMetadata(true).create();
    RootBean rootBean = new SubBean();
    rootBean.bean = new SubBean();
    assertEquals("{\"@class\":\"subBean\",\"bean\":{\"@class\":\"subBean\",\"bean\":null}}", genson.serialize(rootBean));
  }

  @Test
  public void testMapWithPrimitiveKeys() {
    Map<Long, String> expected = new HashMap<Long, String>();
    expected.put(5L, "hey");
    String json = genson.serialize(expected);
    // due to type erasure we consider keys as strings
    @SuppressWarnings("rawtypes")
    Map map = genson.deserialize(json, Map.class);
    assertNull(map.get(5L));
    assertNotNull(map.get("5"));

    // when map type is defined we deserialize to expected primitive types
    map = genson.deserialize(json, new GenericType<Map<Long, String>>() {
    });
    assertEquals(expected.get(5L), map.get(5L));
  }

  @Test
  public void testPropertiesConverter() {
    Properties props = new Properties();
    props.put("key", "value");
    String json = genson.serialize(props);
    assertEquals("value", genson.deserialize(json, Properties.class).get("key"));
  }

  @Test
  public void testComplexMapConverter() {
    Map<UUID, List<UUID>> expected = new HashMap<UUID, List<UUID>>();
    expected.put(UUID.randomUUID(), Arrays.asList(UUID.randomUUID(), UUID.randomUUID()));
    expected.put(UUID.randomUUID(), Arrays.asList(UUID.randomUUID()));
    expected.put(null, null);
    String json = genson.serialize(expected, new GenericType<Map<UUID, List<UUID>>>() {
    });
    assertEquals(expected, genson.deserialize(json, new GenericType<Map<UUID, List<UUID>>>() {
    }));
  }

  @Test
  public void testUUIDConverter() {
    UUID uuid = UUID.randomUUID();
    String json = genson.serialize(uuid);
    assertEquals(uuid, genson.deserialize(json, UUID.class));
  }

  @Test
  public void testDateConverter() {
    Genson genson = new GensonBuilder().useDateFormat(
      new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.FRENCH)).create();
    Date date = new Date();
    String json = genson.serialize(date);
    Date dateDeserialized = genson.deserialize(json, Date.class);
    assertEquals(date.toString(), dateDeserialized.toString());
  }

  @Test public void dateConverterShouldAcceptStringAndNumeric() {
    long now = System.currentTimeMillis();

    Date date1 = genson.deserialize(""+now, Date.class);
    assertEquals(now, date1.getTime());

    DateFormat df = SimpleDateFormat.getDateTimeInstance();
    String strDate = df.format(new Date(now));
    Date date2 = genson.deserialize(String.format("\"%s\"", strDate), Date.class);
    assertEquals(strDate, df.format(date2));
  }

  @Test
  public void testCalendarConverter() {
    Genson genson = new GensonBuilder().useDateAsTimestamp(true).create();
    Calendar cal = Calendar.getInstance();
    String json = genson.serialize(cal);
    Calendar cal2 = genson.deserialize(json, Calendar.class);
    assertEquals(cal.getTime(), cal2.getTime());
  }

  private <T extends Map> void testMapImplementationsRoundTrip(Class<T> clazz) {

    try {
      T expected = clazz.newInstance();
      expected.put("a", 1);
      expected.put("b", 2);
      expected.put("C", 3);

      T actual = genson.deserialize(genson.serialize(expected), clazz);

      assertEquals(expected.getClass(), actual.getClass());

      Iterator<Map.Entry<String, Integer>> actualIt = actual.entrySet().iterator();
      Iterator<Map.Entry<String, Integer>> expectedIt = expected.entrySet().iterator();
      while (actualIt.hasNext() && expectedIt.hasNext())
        assertEquals(expectedIt.next().getKey(), actualIt.next().getKey());

      assertEquals(actualIt.hasNext(), expectedIt.hasNext());
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private <T extends Collection> void testCollectionImplementationsRoundTrip(Class<T> clazz) {
    try {
      T expected = clazz.newInstance();
      expected.add(1l);
      expected.add(2l);
      expected.add(3l);

      GenericType<T> type = GenericType.of(clazz);

      T actual = genson.deserialize(genson.serialize(expected, type), type);

      assertEquals(expected.getClass(), actual.getClass());

      Iterator<Integer> actualIt = actual.iterator();
      Iterator<Integer> expectedIt = expected.iterator();
      while (actualIt.hasNext() && expectedIt.hasNext())
        assertEquals(expectedIt.next(), actualIt.next());

      assertEquals(actualIt.hasNext(), expectedIt.hasNext());

    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static enum Color {
    blue, red;
  }

  public static class RootBean {
    public RootBean bean;
  }

  public static class SubBean extends RootBean {
  }

  public static class PojoWithByteArray {
    int i;
    double f;
    byte[] b;

    PojoWithByteArray(int i, double f, byte[] b) {
      this.i = i;
      this.f = f;
      this.b = b;
    }
  }
}