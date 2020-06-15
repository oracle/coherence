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

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;

import com.oracle.coherence.io.json.genson.annotation.JsonCreator;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.coherence.io.json.genson.annotation.JsonProperty;
import com.oracle.coherence.io.json.genson.bean.ComplexObject;
import com.oracle.coherence.io.json.genson.bean.Feed;
import com.oracle.coherence.io.json.genson.bean.MediaContent;
import com.oracle.coherence.io.json.genson.bean.Primitives;
import com.oracle.coherence.io.json.genson.bean.Tweet;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import com.oracle.coherence.io.json.genson.stream.ObjectWriter;

public class JsonSerDeserSymetricTest {
  Genson genson = new GensonBuilder().useConstructorWithArguments(true).create();

  @Test
  public void testSerDeserByteArray() throws IOException {
    Primitives expected = createPrimitives();
    List<Integer> is = Arrays.asList(1, 2);
    genson.serialize(is, new GenericType<List<Number>>() {});
    byte[] json = genson.serializeBytes(expected);
    Primitives.assertComparePrimitives(expected, genson.deserialize(json, Primitives.class));
  }

  @Test
  public void testSerDeserStream() throws IOException {
    Primitives expected = createPrimitives();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    genson.serialize(expected, baos);
    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    Primitives.assertComparePrimitives(expected, genson.deserialize(bais, Primitives.class));
  }

  @Test
  public void testJsonComplexObjectSerDeser() throws IOException {
    ComplexObject coo = new ComplexObject(createPrimitives(), Arrays.asList(createPrimitives(),
      createPrimitives()), new Primitives[]{createPrimitives(), createPrimitives()});

    String json = genson.serialize(coo);
    ComplexObject co = genson.deserialize(json, ComplexObject.class);
    ComplexObject.assertCompareComplexObjects(co, coo);
  }

  @Test
  public void testPrimitiveSerDeser() throws IOException {
    int a = 54000048;
    int b = genson.deserialize(genson.serialize(a), int.class);
    assertEquals(a, b);

    double c = 64800486.000649463;
    double d = genson.deserialize(genson.serialize(c), double.class);
    assertTrue(c == d);

    String s = "hey you!";
    String s2 = genson.deserialize(genson.serialize(s), String.class);
    assertEquals(s, s2);
  }

  @Test
  public void testAbstractType() throws IOException {
    B b = new B();
    b.a = "aa";
    b.b = "bb";
    Genson genson = new GensonBuilder().useClassMetadata(true).create();
    String json = genson.serialize(b);
    B b2 = (B) genson.deserialize(json, Object.class);
    assertEquals(b.a, b2.a);
    assertEquals(b.b, b2.b);
  }

  @Test
  public void testSerializerAndDeserializeWithView() throws IOException {
    Person p = new Person("Mr");
    p.birthYear = 1986;
    p.name = "eugen";

    Genson genson = new GensonBuilder().useBeanViews(true)
      .useConstructorWithArguments(true).create();

    @SuppressWarnings("unchecked")
    String json = genson.serialize(p, ViewOfPerson.class);
    assertEquals("{\"age\":" + new ViewOfPerson().getAge(p) + ",\"gender\":\"M\",\"name\":\""
      + p.name + "\"}", json);
    @SuppressWarnings("unchecked")
    Person me = genson.deserialize(json, Person.class, ViewOfPerson.class);
    assertEquals(p.civility, me.civility);
    assertEquals(p.name, me.name);
    assertEquals(p.birthYear, me.birthYear);
  }

  @Test
  public void testWithUrlConverter() throws IOException {
    Genson genson = new GensonBuilder().withConverters(new Converter<URL>() {

      public void serialize(URL obj, ObjectWriter writer, Context ctx)
        throws IOException {
        writer.writeValue(obj.toExternalForm());
      }

      public URL deserialize(ObjectReader reader, Context ctx)
        throws IOException {
        return new URL(reader.valueAsString());
      }

    }).create();

    String serializedUrl = genson.serialize(new URL("http://www.google.com"));
    assertEquals("\"http://www.google.com\"", serializedUrl);
    assertEquals("http://www.google.com", genson.deserialize(serializedUrl, URL.class)
      .toExternalForm());
  }

  public ObjectMapper getMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(MapperFeature.AUTO_DETECT_FIELDS, true);
    mapper.setDateFormat(new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US));
    return mapper;
  }

  public Genson getGenson() {
    return new GensonBuilder().useDateFormat(
      new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy", Locale.US)).create();
  }

  @Test
  public void testDeserializeSerializeTweets() throws JsonParseException, JsonMappingException,
    IOException {
    ObjectMapper mapper = getMapper();
    Genson genson = getGenson();

    // we first deserialize the original data and ensure that genson deserialized it exactly as
    // jackson
    Tweet[] jacksonTweets = mapper.readValue(this.getClass()
      .getResourceAsStream("/TWEETS.json"), Tweet[].class);
    Tweet[] gensonTweets = genson.deserialize(new InputStreamReader(this.getClass()
      .getResourceAsStream("/TWEETS.json")), Tweet[].class);
    assertArrayEquals(jacksonTweets, gensonTweets);

    // and then we serialize it and try to deserialize again and match again what was
    // deserialized by jackson
    // this proves that genson serializes and deserializes correctly (except if there is a same
    // error in genson and jackson lol!)
    String tweetsString = genson.serialize(gensonTweets);
    gensonTweets = genson.deserialize(tweetsString, Tweet[].class);
    assertArrayEquals(jacksonTweets, gensonTweets);
  }

  @Test
  public void testDeserializeSerializeReaderShort() throws IOException {
    ObjectMapper mapper = getMapper();
    Genson genson = getGenson();

    // same test as before...
    Feed jacksonShortFeed = mapper.readValue(this.getClass()
      .getResourceAsStream("/READER_SHORT.json"), Feed.class);
    Feed gensonShortFeed = genson.deserialize(new InputStreamReader(this.getClass()
      .getResourceAsStream("/READER_SHORT.json")), Feed.class);
    assertEquals(jacksonShortFeed, gensonShortFeed);
    String shortFeedString = genson.serialize(gensonShortFeed);
    gensonShortFeed = genson.deserialize(shortFeedString, Feed.class);
    assertEquals(jacksonShortFeed, gensonShortFeed);

  }

  @Test
  public void testDeserializeSerializeReaderLong() throws IOException {
    ObjectMapper mapper = getMapper();
    Genson genson = getGenson();

    // and again for the long reader data...
    Feed jacksonLongFeed = mapper.readValue(this.getClass()
      .getResourceAsStream("/READER_LONG.json"), Feed.class);
    Feed gensonLongFeed = genson.deserialize(new InputStreamReader(this.getClass()
      .getResourceAsStream("/READER_LONG.json")), Feed.class);
    assertEquals(jacksonLongFeed, gensonLongFeed);
    String longFeedString = genson.serialize(gensonLongFeed);
    gensonLongFeed = genson.deserialize(longFeedString, Feed.class);
    assertEquals(jacksonLongFeed, gensonLongFeed);
  }

  @Test
  public void testWithCustomObjectConverter() throws IOException {
    Genson genson = new GensonBuilder().withConverters(new Converter<Point>() {

      public void serialize(Point obj, ObjectWriter writer, Context ctx)
        throws IOException {
        writer.beginObject().writeName("x").writeValue(obj.x).writeName("y").writeValue(
          obj.y).endObject();
      }

      public Point deserialize(ObjectReader reader, Context ctx)
        throws IOException {
        Point p = new Point();
        reader.beginObject();
        for (; reader.hasNext(); ) {
          reader.next();
          String name = reader.name();
          if ("x".equals(name))
            p.x = reader.valueAsInt();
          else if ("y".equals(name))
            p.y = reader.valueAsInt();
          else {
            // lets skip it
            reader.skipValue();
          }
        }
        reader.endObject();
        return p;
      }

    }).create();

    Point p = new Point(1, 2);
    String serializedPoint = genson.serialize(p);
    Point q = genson.deserialize(serializedPoint, Point.class);
    assertEquals(p.x, q.x);
    assertEquals(p.y, q.y);
  }

  @Test
  public void testSerializeDeserializeMediaContent() throws JsonParseException,
    JsonMappingException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    Genson genson = new Genson();
    MediaContent jacksonContent = mapper.readValue(this.getClass()
      .getResourceAsStream("/MEDIA_CONTENT.json"), MediaContent.class);
    MediaContent gensonContent = genson.deserialize(new InputStreamReader(this.getClass()
      .getResourceAsStream("/MEDIA_CONTENT.json")), MediaContent.class);
    assertEquals(jacksonContent, gensonContent);
    String json = genson.serialize(gensonContent);
    gensonContent = genson.deserialize(json, MediaContent.class);
    assertEquals(jacksonContent, gensonContent);
  }

  @Test public void testRoundTripOfPojoWithPrimitiveNumbers() {
    PojoWithPrimitives expected = new PojoWithPrimitives(2.1f, (short) 3, (byte) 11, 'a');
    assertEquals(expected, genson.deserialize(genson.serialize(expected), PojoWithPrimitives.class));
  }

  @Test public void testRoundTripOfPojoWithNumbers() {
    PojoWithBoxedPrimitives expected = new PojoWithBoxedPrimitives(2.1f, (short) 3, 'a', Byte.MAX_VALUE);
    assertEquals(expected, genson.deserialize(genson.serialize(expected), PojoWithBoxedPrimitives.class));
  }

  public class InnerClass {
    private final int value;

    public InnerClass(int value) {
      this.value = value;
    }
  }

  public static class Person {
    String civility;
    String name;
    int birthYear;

    Person(String civility) {
      this.civility = civility;
    }
  }

  public static class ViewOfPerson implements BeanView<Person> {
    public ViewOfPerson() {
    }

    static boolean usedCtr = false;

    @JsonCreator
    public static Person createNewPerson(String gender) {
      usedCtr = true;
      String civility = "M".equalsIgnoreCase(gender) ? "Mr"
        : "F".equalsIgnoreCase(gender) ? "UNKNOWN" : "";
      return new Person(civility);
    }

    public String getGender(Person p) {
      return "Mr".equalsIgnoreCase(p.civility) ? "M"
        : "Mrs".equalsIgnoreCase(p.civility) ? "F" : "UNKNOWN";
    }

    @JsonProperty(value = "name")
    public String getNameOf(Person p) {
      return p.name;
    }

    public int getAge(Person p) {
      return GregorianCalendar.getInstance().get(Calendar.YEAR) - p.birthYear;
    }

    public void setName(String name, Person p) {
      p.name = name;
    }

    @JsonProperty(value = "age")
    public void setBirthYear(int personBirthYear, Person p) {
      p.birthYear = GregorianCalendar.getInstance().get(Calendar.YEAR) - personBirthYear;
    }
  }

  public static class PojoWithBoxedPrimitives {
    public final Float aFloat;
    public final Short aShort;
    public final Character aChar;
    public final Byte aByte;

    public PojoWithBoxedPrimitives(Float aFloat, Short aShort, Character aChar, Byte aByte) {
      this.aFloat = aFloat;
      this.aShort = aShort;
      this.aChar = aChar;
      this.aByte = aByte;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      PojoWithBoxedPrimitives that = (PojoWithBoxedPrimitives) o;

      if (aFloat != null ? !aFloat.equals(that.aFloat) : that.aFloat != null) return false;
      if (aShort != null ? !aShort.equals(that.aShort) : that.aShort != null) return false;

      return true;
    }
  }

  public static class PojoWithPrimitives {
    public final float aFloat;
    public final short aShort;
    public final byte aByte;
    public final char aChar;

    public PojoWithPrimitives(float aFloat, short aShort, byte aByte, char aChar) {
      this.aFloat = aFloat;
      this.aShort = aShort;
      this.aByte = aByte;
      this.aChar = aChar;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      PojoWithPrimitives that = (PojoWithPrimitives) o;

      if (Float.compare(that.aFloat, aFloat) != 0) return false;
      if (aShort != that.aShort) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return 0;
    }
  }

  public static class A {
    String a;
  }

  public static class B extends A {
    String b;
  }

  private Primitives createPrimitives() {
    return new Primitives(1, new Integer(10), 1.00001, new Double(0.00001), "TEXT ...  HEY!",
      true, new Boolean(false));
  }
}
