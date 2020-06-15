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

import static org.junit.Assert.*;

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.StringReader;
import java.util.*;

import com.oracle.coherence.io.json.genson.*;
import com.oracle.coherence.io.json.genson.stream.ObjectReader;
import org.junit.Test;

import com.oracle.coherence.io.json.genson.annotation.JsonProperty;
import com.oracle.coherence.io.json.genson.bean.ComplexObject;
import com.oracle.coherence.io.json.genson.bean.Primitives;
import com.oracle.coherence.io.json.genson.bean.Media.Player;
import com.oracle.coherence.io.json.genson.reflect.BeanDescriptor;
import com.oracle.coherence.io.json.genson.stream.JsonReader;

public class JsonDeserializationTest {
  final Genson genson = new GensonBuilder().useConstructorWithArguments(true).create();

  @Test public void testShouldDeserializeUsingSetterAlias() {
    assertEquals(2, genson.deserialize("{\"a2\":2}", PojoWithAliasInSetter.class).a);
  }

  @Test public void testShouldDeserializeUsingConstructorAlias() {
    assertEquals(2, genson.deserialize("{\"a2\":2}", PojoWithAliasInConstructor.class).a);
  }

  @Test public void testDeserializeMissingValueToDefaultValueInConstructor() {
    Genson genson = new GensonBuilder().useConstructorWithArguments(true).useDefaultValue(5, int.class).create();

    assertEquals(5, genson.deserialize("{}", ConstructorWithPrimitive.class).value);
  }

  @Test public void testDeserializeSpecialFloatingPointNumbers() {
    assertTrue(genson.deserialize("\"NaN\"", Double.class).isNaN());

    Double negInfinity = genson.deserialize("\"-Infinity\"", Double.class);
    assertTrue(negInfinity.isInfinite());
    assertTrue(negInfinity < 0);

    Double posInfinity = genson.deserialize("\"Infinity\"", Double.class);
    assertTrue(posInfinity.isInfinite());
    assertTrue(posInfinity > 0);
  }

  @Test public void testDeserializeSingleObjectAsList() {
    PojoWithList actual = new GensonBuilder().acceptSingleValueAsList(true).create()
        .deserialize("{\"listOfInt\": 1}", PojoWithList.class);

    assertEquals(1, actual.listOfInt.get(0).intValue());
  }

  @Test public void testReadMultipleRootObjectsNotEnclosedInArrayAndMapManually() {
    GenericType<Pojo> type = GenericType.of(Pojo.class);
    Context ctx = new Context(genson);

    int i = 1;
    for (ObjectReader reader = genson.createReader(new StringReader("{\"a\":1}{\"a\":2}"));
      reader.hasNext(); reader.next(), i++) {
      Pojo p = genson.deserialize(type, reader, ctx);
      assertEquals(p.a, i);
    }
    assertEquals(i, 3);
  }

  @Test public void testReadMultipleRootObjectsNotEnclosedInArrayAndBind() {
    ObjectReader reader = genson.createReader(new StringReader("{\"a\":1}{\"a\":2}"));
    int i = 1;
    for (Iterator<Pojo> it = genson.deserializeValues(reader, GenericType.of(Pojo.class));
      it.hasNext(); i++) {
      Pojo p = it.next();
      assertEquals(p.a, i);
    }
    assertEquals(i, 3);
  }

  @Test
  public void testASMResolverShouldNotFailWhenUsingBootstrapClassloader() {
    assertNotNull(genson.deserialize("{}", Exception.class));
  }

  @Test
  public void testJsonEmptyObject() {
    String src = "{}";
    Primitives p = genson.deserialize(src, Primitives.class);
    assertNull(p.getText());
  }

  @Test
  public void testJsonEmptyArray() {
    String src = "[]";
    Integer[] p = genson.deserialize(src, Integer[].class);
    assertTrue(p.length == 0);
  }

  @Test
  public void testJsonComplexObjectEmpty() {
    String src = "{\"primitives\":null,\"listOfPrimitives\":[], \"arrayOfPrimitives\": null}";
    ComplexObject co = genson.deserialize(src, ComplexObject.class);

    assertNull(co.getPrimitives());
    assertTrue(co.getListOfPrimitives().size() == 0);
    assertNull(co.getArrayOfPrimitives());
  }

  @Test
  public void testDeserializeEmptyJson() {
    // FIXME disabled in favor of the new changes as anyway it is very unlikely that people would need to deser an
    // empty string to a root primitive value.
//    Integer i = genson.deserialize("\"\"", Integer.class);
//    assertNull(i);
    Integer i = genson.deserialize("", Integer.class);
    assertNull(i);
    i = genson.deserialize("null", Integer.class);
    assertNull(i);

    int[] arr = genson.deserialize("", int[].class);
    assertNull(arr);
    arr = genson.deserialize("null", int[].class);
    assertNull(arr);
    arr = genson.deserialize("[]", int[].class);
    assertNotNull(arr);

    Primitives p = genson.deserialize("", Primitives.class);
    assertNull(p);
    p = genson.deserialize("null", Primitives.class);
    assertNull(p);
    p = genson.deserialize("{}", Primitives.class);
    assertNotNull(p);
  }

  @Test
  public void testJsonNumbersLimit() {
    String src = "[" + String.valueOf(Long.MAX_VALUE) + "," + String.valueOf(Long.MIN_VALUE)
      + "]";
    long[] arr = genson.deserialize(src, long[].class);
    assertTrue(Long.MAX_VALUE == arr[0]);
    assertTrue(Long.MIN_VALUE == arr[1]);

    src = "[" + String.valueOf(Double.MAX_VALUE) + "," + String.valueOf(Double.MIN_VALUE) + "]";
    double[] arrD = genson.deserialize(src, double[].class);
    assertTrue(Double.MAX_VALUE == arrD[0]);
    assertTrue(Double.MIN_VALUE == arrD[1]);
  }

  @Test
  public void testPrimitiveNumbers() {
    assertEquals(0, genson.deserialize("0", Number.class));
    assertEquals(Integer.MAX_VALUE, genson.deserialize(String.valueOf(Integer.MAX_VALUE), Number.class));
    assertEquals(Long.MAX_VALUE, genson.deserialize(String.valueOf(Long.MAX_VALUE), Number.class));
    assertEquals(Double.MAX_VALUE, genson.deserialize(String.valueOf(Double.MAX_VALUE), Number.class));
  }

  @Test
  public void testJsonPrimitivesObject() {
    String src = "{\"intPrimitive\":1, \"integerObject\":7, \"doublePrimitive\":1.01,"
      + "\"doubleObject\":2.003,\"text\": \"HEY...YA!\", "
      + "\"booleanPrimitive\":true,\"booleanObject\":false}";
    Primitives p = genson.deserialize(src, Primitives.class);
    assertEquals(p.getIntPrimitive(), 1);
    assertEquals(p.getIntegerObject(), new Integer(7));
    assertEquals(p.getDoublePrimitive(), 1.01, 0);
    assertEquals(p.getDoubleObject(), new Double(2.003));
    assertEquals(p.getText(), "HEY...YA!");
    assertEquals(p.isBooleanPrimitive(), true);
    assertEquals(p.isBooleanObject(), Boolean.FALSE);
  }

  @Test
  public void testJsonDoubleArray() {
    String src = "[5,      0.006, 9.0E-11 ]";
    double[] array = genson.deserialize(src, double[].class);
    assertEquals(array[0], 5, 0);
    assertEquals(array[1], 0.006, 0);
    assertEquals(array[2], 0.00000000009, 0);
  }

  @Test
  public void testJsonComplexObject() {
    ComplexObject coo = new ComplexObject(createPrimitives(), Arrays.asList(createPrimitives(),
      createPrimitives()), new Primitives[]{createPrimitives(), createPrimitives()});
    ComplexObject co = genson.deserialize(coo.jsonString(), ComplexObject.class);
    ComplexObject.assertCompareComplexObjects(co, coo);
  }

  @Test
  public void testJsonComplexObjectSkipValue() {
    ComplexObject coo = new ComplexObject(createPrimitives(), Arrays.asList(createPrimitives(),
      createPrimitives(), createPrimitives(), createPrimitives(), createPrimitives(),
      createPrimitives()), new Primitives[]{createPrimitives(), createPrimitives()});

    DummyWithFieldToSkip dummy = new DummyWithFieldToSkip(coo, coo, createPrimitives(),
      Arrays.asList(coo));
    DummyWithFieldToSkip dummy2 = genson.deserialize(dummy.jsonString(),
      DummyWithFieldToSkip.class);

    ComplexObject.assertCompareComplexObjects(dummy.getO1(), dummy2.getO1());
    ComplexObject.assertCompareComplexObjects(dummy.getO2(), dummy2.getO2());
    Primitives.assertComparePrimitives(dummy.getP(), dummy2.getP());
  }

  @Test
  public void testJsonToBeanWithConstructor() {
    String json = "{\"other\":{\"name\":\"TITI\", \"age\": 13}, \"name\":\"TOTO\", \"age\":26}";
    BeanWithConstructor bean = genson.deserialize(json, BeanWithConstructor.class);
    assertEquals(bean.age, 26);
    assertEquals(bean.name, "TOTO");
    assertEquals(bean.other.age, 13);
    assertEquals(bean.other.name, "TITI");
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testJsonToUntypedList() {
    String src = "[1, 1.1, \"aa\", true, false]";
    List<Object> l = genson.deserialize(src, List.class);
    assertArrayEquals(new Object[]{1L, 1.1, "aa", true, false},
      l.toArray(new Object[l.size()]));
  }

  @Test
  public void testMultidimensionalArray() {
    String json = "[[[42,24]],[[43,34]]]";
    long[][][] array = genson.deserialize(json, long[][][].class);
    assertArrayEquals(new long[]{42L, 24L}, array[0][0]);
    assertArrayEquals(new long[]{43L, 34L}, array[1][0]);

    String json3 = "[[[\"abc\"],[42,24],[\"def\"],[43,34]]]";
    genson.deserialize(json3, Object[][][].class);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testContentDrivenDeserialization() {
    String src = "{\"list\":[1, 2.3, 5, null]}";
    TypeVariableList<Number> tvl = genson.deserialize(src, TypeVariableList.class);
    assertArrayEquals(tvl.list.toArray(new Number[tvl.list.size()]), new Number[]{1, 2.3, 5,
      null});

    String json = "[\"hello\",5,{\"name\":\"GREETINGS\",\"source\":\"guest\"}]";
    Map<String, String> map = new HashMap<String, String>();
    map.put("name", "GREETINGS");
    map.put("source", "guest");
    assertEquals(Arrays.asList("hello", 5L, map), genson.deserialize(json, Collection.class));

    // doit echouer du a la chaine et que list est de type <E extends Number>
    src = "{\"list\":[1, 2.3, 5, \"a\"]}";
    try {
      tvl = genson.deserialize(src, TypeVariableList.class);
      fail();
    } catch (Exception e) {
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testUntypedDeserializationToMap() {
    String src = "{\"key1\": 1, \"key2\": 1.2, \"key3\": true, \"key4\": \"string\", \"key5\": null, \"list\":[1,2.0005,3]}";
    Map<String, Object> map = genson.deserialize(src, Map.class);
    assertEquals(map.get("key1"), 1L);
    assertEquals(map.get("key2"), 1.2);
    assertEquals(map.get("key3"), true);
    assertEquals(map.get("key4"), "string");
    assertNull(map.get("key5"));
    List<Number> list = (List<Number>) map.get("list");
    assertEquals(Arrays.asList(1L, 2.0005, 3L), list);
  }

  @Test
  public void testDeserializeWithConstructorAndFieldsAndSetters() {
    String json = "{\"p0\":0,\"p1\":1,\"p2\":2,\"shouldSkipIt\":55, \"nameInJson\": 3}";
    ClassWithConstructorFieldsAndGetters c = genson.deserialize(json,
      ClassWithConstructorFieldsAndGetters.class);
    assertEquals(c.p0, new Integer(0));
    assertEquals(c.p1, new Integer(1));
    assertEquals(c.p2, new Integer(2));
    assertTrue(c.constructorCalled);
  }

  @Test
  public void testDeserializeWithConstructorAndMissingFields() {
    String json = "{\"p0\":0,\"p1\":1, \"nameInJson\": 3}";
    ClassWithConstructorFieldsAndGetters c = genson.deserialize(json,
      ClassWithConstructorFieldsAndGetters.class);
    assertTrue(c.constructorCalled);
    assertEquals(new Integer(0), c.p0);
    assertEquals(new Integer(1), c.p1);
    assertEquals(null, c.p2);
    assertEquals(new Integer(3), c.hidden);
  }

  @Test
  public void testDeserializeWithConstructorMixedAnnotation() {
    String json = "{\"p0\":0,\"p1\":1,\"p2\":2,\"shouldSkipIt\":55,   \"nameInJson\":\"125\"}";
    ClassWithConstructorFieldsAndGetters c = genson.deserialize(json,
      ClassWithConstructorFieldsAndGetters.class);
    assertTrue(c.constructorCalled);
    assertEquals(c.p0, new Integer(0));
    assertEquals(c.p1, new Integer(1));
    assertEquals(c.p2, new Integer(2));
    assertEquals(c.hidden, new Integer(125));
  }

  @Test
  public void testDeserializeJsonWithClassAlias() {
    Genson genson = new GensonBuilder().addAlias("rect", Rectangle.class).create();
    Shape p = genson.deserialize("{\"@class\":\"rect\"}", Shape.class);
    assertTrue(p instanceof Rectangle);
    p = genson.deserialize("{\"@class\":\"java.awt.Rectangle\"}", Shape.class);
    assertTrue(p instanceof Rectangle);
  }

  @Test
  public void testDeserializeJsonWithPackageAlias() {
    Genson genson = new GensonBuilder().addPackageAlias("awt", "java.awt").create();
    Shape p = genson.deserialize("{\"@class\":\"awt.Rectangle\"}", Shape.class);
    assertTrue(p instanceof Rectangle);
    p = genson.deserialize("{\"@class\":\"java.awt.Rectangle\"}", Shape.class);
    assertTrue(p instanceof Rectangle);
  }

  @Test
  public void testDeserealizeIntoExistingBean() {
    BeanDescriptor<ClassWithConstructorFieldsAndGetters> desc = (BeanDescriptor<ClassWithConstructorFieldsAndGetters>) genson
      .getBeanDescriptorProvider().provide(ClassWithConstructorFieldsAndGetters.class,
        ClassWithConstructorFieldsAndGetters.class, genson);
    ClassWithConstructorFieldsAndGetters c = new ClassWithConstructorFieldsAndGetters(1, 2, "3") {
      @Override
      public void setP2(Integer p2) {
        this.p2 = p2;
      }
    };
    c.constructorCalled = false;
    String json = "{\"p0\":0,\"p1\":1,\"p2\":2,\"shouldSkipIt\":55,   \"nameInJson\":\"125\"}";
    desc.deserialize(c, new JsonReader(json), new Context(genson));
    assertFalse(c.constructorCalled);
    assertEquals(c.p0, new Integer(0));
    assertEquals(c.p1, new Integer(1));
    assertEquals(c.p2, new Integer(2));
  }

  @Test
  public void testDeserializeEnum() {
    assertEquals(Player.JAVA, genson.deserialize("\"JAVA\"", Player.class));
  }

  @Test(expected = JsonBindingException.class) public void testDeserWithMissingPropertyShouldFail() {
    new GensonBuilder()
      .failOnMissingProperty(true)
      .create()
      .deserialize("{\"missingKey\": 1}", Empty.class);
  }

  @Test public void testDeserInExistingInstance() {
    Pojo pojo = new Pojo();
    pojo.a = 1;
    pojo.b = 2;
    genson.deserializeInto("{\"b\":3,\"str\":\"foo\"}", pojo);

    assertEquals(1, pojo.a);
    assertEquals(3, pojo.b);
    assertEquals("foo", pojo.str);
  }

  @Test
  public void testDeserializationOptional() {
    assertFalse(genson.deserialize("{}", Optional.class).isPresent());
    assertEquals(Optional.of("string"), genson.deserialize("{\"value\":\"string\"}", Optional.class));
    assertEquals(Optional.of(42L), genson.deserialize("{\"value\":42}", Optional.class));
    assertEquals(Optional.of(42.35D), genson.deserialize("{\"value\":42.35}", Optional.class));
    assertEquals(Optional.of(true), genson.deserialize("{\"value\":true}", Optional.class));
    assertEquals(Optional.of(false), genson.deserialize("{\"value\":false}", Optional.class));

    Genson genson = new GensonBuilder().useClassMetadata(true).create();
    assertEquals(Optional.of(new BeanWithConstructor("Bilbo Baggins", 111, null)),
                 genson.deserialize("{\"@class\":\"java.util.Optional\",\"value\":{\"@class\":\"com.oracle.coherence.io.json.genson.convert.JsonDeserializationTest$BeanWithConstructor\",\"age\":111,\"name\":\"Bilbo Baggins\",\"other\":null}}", Optional.class));
  }
  
  @Test
  public void testDeserializationOptionalInt() {
    assertFalse(genson.deserialize("{}", OptionalInt.class).isPresent());
    assertEquals(OptionalInt.of(42),
        genson.deserialize("{\"value\":42}", OptionalInt.class));
  }

  @Test
  public void testDeserializationOptionalLong() {
    assertFalse(genson.deserialize("{}", OptionalLong.class).isPresent());
    assertEquals(OptionalLong.of(42L),
        genson.deserialize("{\"value\":42}", OptionalLong.class));
  }

  @Test
  public void testDeserializationOptionalDouble() {
    assertFalse(genson.deserialize("{}", OptionalDouble.class).isPresent());
    assertEquals(OptionalDouble.of(42D),
        genson.deserialize("{\"value\":42.0}", OptionalDouble.class));
  }

  @Test
  public void testDeserializationChar() {
    assertEquals('C', genson.deserialize("\"\\u0043\"", Object.class));
    // ensure multiple unicode escapes are not considered a char type and is returned as String
    assertEquals("CCC", genson.deserialize("\"\\u0043\\u0043\\u0043\"", Object.class));
  }

  @Test
  public void testDeserializationOfBeanWithOptionals() {
    BeanWithOptionals expected = new BeanWithOptionals(Optional.of("Hello World"),
        OptionalInt.of(42),
        OptionalLong.of(42L),
        OptionalDouble.of(42D));

    BeanWithOptionals result = genson.deserialize(
        "{\"optionalDouble\":{\"value\":42.0},\"optionalInt\":{\"value\":42},\"optionalLong\":{\"value\":42},\"optionalType\":{\"value\":\"Hello World\"}}",
        BeanWithOptionals.class);
    assertEquals(expected, result);
  }

  @Test
  public void testDeserializationCollectionNullElementsWithSkipNullTrue() {
    Genson genson = new GensonBuilder().setSkipNull(true).create();
    List result = genson.deserialize("[\"a\",null,\"b\"]", List.class);
    List<String> strings = new ArrayList<>();
    strings.add("a");
    strings.add(null);
    strings.add("b");
    assertEquals(strings, result);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static class BeanWithOptionals {
    @JsonProperty
    private Optional<String> optionalType;
    @JsonProperty
    private OptionalInt optionalInt;
    @JsonProperty
    private OptionalLong optionalLong;
    @JsonProperty
    private OptionalDouble optionalDouble;

    @SuppressWarnings("unused")
    public BeanWithOptionals() {}

    public BeanWithOptionals(final Optional<String> optionalType,
                             final OptionalInt optionalInt,
                             final OptionalLong optionalLong,
                             final OptionalDouble optionalDouble) {
      this.optionalType = optionalType;
      this.optionalInt = optionalInt;
      this.optionalLong = optionalLong;
      this.optionalDouble = optionalDouble;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final BeanWithOptionals that = (BeanWithOptionals) o;
      return Objects.equals(optionalType, that.optionalType) &&
          Objects.equals(optionalInt, that.optionalInt) &&
          Objects.equals(optionalLong, that.optionalLong) &&
          Objects.equals(optionalDouble, that.optionalDouble);
    }

    @Override
    public int hashCode() {
      return Objects.hash(optionalType, optionalInt, optionalLong, optionalDouble);
    }
  }

  public static class BeanWithConstructor {
    final String name;
    final int age;
    final BeanWithConstructor other;

    public BeanWithConstructor(@JsonProperty(value = "name") String name,
                               @JsonProperty(value = "age") int age,
                               @JsonProperty(value = "other") BeanWithConstructor other) {
      this.name = name;
      this.age = age;
      this.other = other;
    }

    public void setName(String name) {
          fail();
      }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final BeanWithConstructor that = (BeanWithConstructor) o;
      return age == that.age &&
          Objects.equals(name, that.name) &&
          Objects.equals(other, that.other);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, age, other);
    }
  }

  public static class Empty {}

  public static class Pojo {
    public String str;
    public int a;
    public int b;
  }

  @SuppressWarnings("unused")
  private static class ClassWithConstructorFieldsAndGetters {
    final Integer p0;
    private Integer p1;
    protected Integer p2;
    transient final Integer hidden;
    // should not be serialized
    public transient boolean constructorCalled = false;

    public ClassWithConstructorFieldsAndGetters(Integer p0, Integer p2,
                                                @JsonProperty(value = "nameInJson") String dontCareOfTheName) {
      constructorCalled = true;
      this.p0 = p0;
      this.p2 = p2;
      this.hidden = Integer.parseInt(dontCareOfTheName);
    }

    public void setP1(Integer p1) {
      this.p1 = p1;
    }

    // use the constructor
    public void setP2(Integer p2) {
      fail();
      this.p2 = p2;
    }
  }

  @SuppressWarnings("unused")
  private static class TypeVariableList<E extends Number> {
    List<E> list;

    public TypeVariableList() {
    }

    public List<E> getList() {
      return list;
    }

    public void setList(List<E> list) {
      this.list = list;
    }

  }

  public static class PojoWithList {
    public List<Integer> listOfInt;
  }

  @SuppressWarnings("unused")
  private static class DummyWithFieldToSkip {
    ComplexObject o1;
    ComplexObject o2;
    Primitives p;
    List<ComplexObject> list;

    public DummyWithFieldToSkip() {
    }

    public DummyWithFieldToSkip(ComplexObject o1, ComplexObject o2, Primitives p,
                                List<ComplexObject> list) {
      super();
      this.o1 = o1;
      this.o2 = o2;
      this.p = p;
      this.list = list;
    }

    public String jsonString() {
      StringBuilder sb = new StringBuilder();

      sb.append("{\"list\":[");
      if (list != null) {
        for (int i = 0; i < list.size(); i++)
          sb.append(list.get(i).jsonString()).append(',');
        sb.append(list.get(list.size() - 1).jsonString());
      }
      sb.append("],\"o1\":").append(o1.jsonString()).append(",\"o2\":")
        .append(o2.jsonString()).append(",\"ooooooSkipMe\":").append(o2.jsonString())
        .append(",\"p\":").append(p.jsonString()).append('}');

      return sb.toString();
    }

    public ComplexObject getO1() {
      return o1;
    }

    public void setO1(ComplexObject o1) {
      this.o1 = o1;
    }

    public ComplexObject getO2() {
      return o2;
    }

    public void setO2(ComplexObject o2) {
      this.o2 = o2;
    }

    public Primitives getP() {
      return p;
    }

    public void setP(Primitives p) {
      this.p = p;
    }
  }

  public static class ConstructorWithPrimitive {
    final int value;

    public ConstructorWithPrimitive(int value) {
      this.value = value;
    }
  }

  private Primitives createPrimitives() {
    return new Primitives(1, new Integer(10), 1.00001, new Double(0.00001), "TEXT ...  HEY!",
      true, new Boolean(false));
  }

  static class PojoWithAliasInSetter {
    private int a;

    public int getA() {
      return a;
    }

    @JsonProperty(aliases = {"a2"})
    public void setA(int a) {
      this.a = a;
    }
  }

  static class PojoWithAliasInConstructor {
    final int a;

    public PojoWithAliasInConstructor(@JsonProperty(aliases = {"a2"}) int a) {
      this.a = a;
    }

    public int getA() {
      return a;
    }
  }
}
