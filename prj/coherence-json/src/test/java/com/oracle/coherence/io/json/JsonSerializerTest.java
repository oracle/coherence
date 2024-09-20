/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.oracle.coherence.common.base.SimpleHolder;

import com.oracle.coherence.io.json.genson.Genson;
import com.oracle.coherence.io.json.genson.JsonBindingException;

import com.oracle.coherence.io.json.genson.stream.JsonReader;
import com.oracle.coherence.io.json.genson.stream.JsonWriter;

import com.tangosol.io.ByteArrayWriteBuffer;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;

import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.aggregator.BigDecimalAverage;
import com.tangosol.util.aggregator.BigDecimalMax;
import com.tangosol.util.aggregator.BigDecimalMin;
import com.tangosol.util.aggregator.BigDecimalSum;
import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.function.Remote;

import common.data.JavaNumberType;
import common.data.Person;
import common.data.PutAll;

import java.io.IOException;
import java.io.StringWriter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import java.nio.charset.StandardCharsets;

import java.time.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.naming.Name;

import org.junit.BeforeClass;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * JSON serialization tests.
 *
 * @author Aleks Seovic  2017.10.02
 * @author jf  2018.03.09
 * @since 20.06
 */
@SuppressWarnings({"unchecked", "rawtypes"})
class JsonSerializerTest
        extends AbstractSerializerTest
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void _beforeClass()
        {
        System.setProperty("coherence.log.level", "9");
        }

    // ----- test cases -----------------------------------------------------
    @Test
    void shouldFallBackToJsonObjectWhenClassIsMissing()
        {
        String json = "{\"@class\":\"com.missing.Point\",\"x\":22,\"y\":42}";

        JsonSerializer serializer = new JsonSerializer(Base.getContextClassLoader(),
                                                       builder -> builder.setEnforceTypeAliases(false),
                                                       false);

        JsonObject     obj        = (JsonObject) serializer.underlying().deserialize(json, Object.class);
        assertEquals("com.missing.Point", obj.getClassName());
        assertEquals(22, obj.getInt("x"));
        assertEquals(42, obj.getInt("y"));

        String json2 = serializer.underlying().serialize(obj);
        assertEquals(json, json2);
        }

    @Test
    void shouldBeClassLoaderAware()
        {
        ClassLoader    loader     = Thread.currentThread().getContextClassLoader();
        JsonSerializer serializer = new JsonSerializer();

        assertThat(serializer.getContextClassLoader(), is(nullValue()));

        serializer.setContextClassLoader(loader);

        assertThat(serializer.getContextClassLoader(), is(sameInstance(loader)));
        }

    @Test
    void testBasicSerialization()
            throws IOException
        {
        assertRoundTrip(null);
        assertRoundTrip(0);
        assertRoundTrip(-1);
        assertRoundTrip(1);
        assertRoundTrip(0L);
        assertRoundTrip(-1L);
        assertRoundTrip(1L);
        assertRoundTrip(0f);
        assertRoundTrip(-1f);
        assertRoundTrip(1f);
        assertRoundTrip(0d);
        assertRoundTrip(-1d);
        assertRoundTrip(1d);
        assertRoundTrip(true);
        assertRoundTrip(false);
        assertRoundTrip("test");
        assertRoundTrip(new Person("Aleks", 43, false));

        JsonObject o = new JsonObject();
        o.put("name", "Aleks");
        o.put("age", 43);
        o.put("minor", false);
        assertRoundTrip(o);
        }

    @Test
    void testMapWithCharKey()
            throws Exception
        {
        Map<Character, String> map = new HashMap<>();
        map.put('$', "$");
        assertRoundTrip(map);

        Map<String, Character> map2 = new HashMap<>();
        map2.put("$", '$');
        assertRoundTrip(map2);
        }

    @Test
    void testBigDecimal()
            throws IOException
        {
        BigDecimal decimal = new BigDecimal("9.0000045678901");
        System.out.printf("BigDecimal: %s, %s", decimal, decimal.precision());
        assertRoundTrip(decimal);
        }


    @Test
    void testMapWithConverter()
            throws IOException
        {
        Map<Person, String> map = new LinkedHashMap<>();
        map.put(new Person("Smith", 12, true), "smith");
        map.put(new Person("Jones", 24, false), "jones");

        PutAll<Person, String> expected = new PutAll<>(map);
        assertRoundTrip(expected);
        }

    @Test
    @Disabled("Excluded due to bug in PutAll equality")
    void testMapWithAnArrayValue()
            throws IOException
        {
        Map<Person, Object> map = new LinkedHashMap<>();
        map.put(new Person("Smith", 12, true), new Integer[] {1, 2, 3});
        map.put(new Person("Robinson", 24, false), new String[] {"foo", "bar"});
        map.put(new Person("Jones_StringArrayList", 27, false), new ArrayList<>(Arrays.asList("arraylist_foo", "arraylist_bar")));
        map.put(new Person("Jones", 26, false), new Person[] {new Person("foo", 1, true), new Person("bar", 2, false)});
        map.put(new Person("JonesAL", 26, false),
                new ArrayList<>(Arrays.asList(new Person("al_foo", 1, true), new Person("al_bar", 2, false))));
        map.put(new Person("BigMike", 28, false),
                new BigInteger[] {BigInteger.ONE, BigInteger.TEN, new BigInteger("987654321987654321987654321")});

        PutAll<Person, Object> expected = new PutAll<>(map);
        assertRoundTrip(expected);
        }

    @Test
    @Disabled("Excluded due to bug in PutAll equality")
    void testMapWithBigIntegerArray()
            throws IOException, ClassNotFoundException
        {
        Map<Person, Object> map = new LinkedHashMap<>();
        map.put(new Person("BigDealWithArray", 18, true),
                new BigInteger[] {BigInteger.ONE, BigInteger.TEN, new BigInteger("987654321987654321987654321")});
        map.put(new Person("BigMikeWithArrayList", 28, false),
                new ArrayList(Arrays.asList(BigInteger.ONE, BigInteger.TEN, new BigInteger("987654321987654321987654321"))));

        PutAll<Person, Object> expected = new PutAll<>(map);
        assertRoundTrip(expected);

        assertJsonProperties(expected, "PutAllValuesBigIntegerArray");
        }

    @Test
    @Disabled("Excluded due to bug in PutAll equality")
    void testLongHandling()
            throws IOException, ClassNotFoundException
        {
        Map<Long, String> map = new LinkedHashMap<>();
        map.put(Integer.MAX_VALUE + 3L, "smith");
        map.put(Integer.MAX_VALUE + 4L, "jones");
        map.put(1L, "robinson");

        PutAll<Long, String> expected = new PutAll<>(map);
        assertRoundTrip(expected);
        assertJsonProperties(expected, "PutAllLongKeys");
        }

    @Test
    void testBigInteger() throws IOException
        {
        BiFunction<Object, Object, Boolean> compare = (expected, actual) ->
        {
        // account for json deserialization to deserialize to smallest
        // java data type.  Compare as BigInteger.
        List<Number> expectedNums = (List<Number>) expected;
        List<BigInteger> actualNums   = (List<BigInteger>) actual;

        if (expectedNums.size() != actualNums.size())
            {
            return false;
            }
        for (int i = 0; i < expectedNums.size(); i++)
            {
            Object expectedElement = expectedNums.get(i);
            Object actualElement   = actualNums.get(i);

            System.out.println("expectedElementClass=" + expectedElement.getClass().getName() +
                               "  actual ElementClass=" + actualElement.getClass().getName());

            BigInteger a = actualNums.get(i);
            if (!expectedNums.get(i).equals(a))
                {
                return false;
                }
            }
        return true;
        };
        List<BigInteger> list = new ArrayList<>(Arrays.asList(BigInteger.valueOf(1L),
                                                              BigInteger.valueOf(2L),
                                                              new BigInteger(Long.toString(Long.MAX_VALUE)),
                                                              new BigInteger("987654321987654321987654321")));
        assertRoundTrip(list, compare);

        assertRoundTrip(new BigIntegerArrayHolder(list.toArray(new BigInteger[0])));
        }

    @Test
    void testDuration()
            throws IOException
        {
        Map map = new LinkedHashMap();
        map.put("durationHours", Duration.ofHours(8));
        map.put("durationSeconds", Duration.ofMillis(332342343));
        assertRoundTrip(new PutAll(map));

        BiFunction<Object, Object, Boolean> compare = (expected, actual) ->
        {
        Duration eDuration = ((SimpleHolder<Duration>) expected).get();
        Duration aDuration = ((SimpleHolder<Duration>) actual).get();

        return eDuration.equals(aDuration);
        };

        assertRoundTrip(new SimpleHolder(Duration.ofMillis(23974832174L)), compare);

        assertRoundTrip(new ContainsDuration(Duration.ofHours(8)));
        }

    @Test
    void testNumberHandling()
            throws IOException, ClassNotFoundException
        {
        JavaNumberType num = new JavaNumberType(Integer.MAX_VALUE + 21L);
        assertRoundTrip(num);

        Map map = new LinkedHashMap();
        map.put(1, num);
        assertRoundTrip(new PutAll(map));
        assertJsonProperties(new PutAll(map), "PutAllJavaNumberType");

        JavaNumberType shortNumber = new JavaNumberType((short) 4);
        assertRoundTrip(shortNumber);
        assertJsonProperties(shortNumber, "JavaNumberTypeShort");

        JavaNumberType byteNumber = new JavaNumberType((byte) 4);
        assertRoundTrip(byteNumber);
        assertJsonProperties(byteNumber, "JavaNumberTypeShort");
        }

    @Test
    void testBinary()
            throws IOException
        {
        assertRoundTrip(new Binary("HelloWorld".getBytes()));
        assertRoundTrip(new Binary("HelloWorld".getBytes(), 2, 3));
        }

    @Test
    void testLambda()
            throws IOException
        {
        Person p = new Person("Smith", 42, false);
        BiFunction<Object, Object, Boolean> compare = (expected, actual) ->
        {
        Function<Person, Integer> fe = (Function) expected;
        Function<Person, Integer> fa = (Function) actual;
        return Objects.equals(fe.apply(p), fa.apply(p));
        };
        Remote.Function<Person, Integer> f = Person::getAge;
        assertRoundTrip(f, compare);
        }

    @Test
    void shouldSerializeLambdaListUsingExternalizableHelper()
        {
        Remote.Function                  f1 = s -> s + "prefix";
        Remote.Function<Person, Integer> f2 = Person::getAge;

        Holder<List<Remote.Function>> holder = new Holder(new ArrayList(Arrays.asList(f1, f2)));
        Binary                        binary = ExternalizableHelper.toBinary(holder, m_serializer);

        System.out.println("Json Serialization of class " + Holder.class.getSimpleName() + ": " +
                           new String(binary.toByteArray()));

        Holder    result = ExternalizableHelper.fromBinary(binary, m_serializer, Holder.class);
        ArrayList fcts   = (ArrayList) result.getValues();
        Person    p      = new Person("Smith", 45, false);

        assertThat(((Remote.Function) fcts.get(0)).apply("foo"), is(f1.apply("foo")));
        assertThat(((Remote.Function<Person, Integer>) fcts.get(1)).apply(p), is(f2.apply(p)));
        assertThat(result.getValues().size(), is(holder.getValues().size()));
        }

    @Test
    void shouldSerializeLambdaInMap()
        {
        Remote.Function                  f1 = s -> s + "prefix";
        Remote.Function<Person, Integer> f2 = Person::getAge;

        Map map = new LinkedHashMap();
        map.put(1, f1);
        map.put(2, f2);
        PutAll<Integer, Remote.Function> expected = new PutAll<>(map);

        Binary binary = ExternalizableHelper.toBinary(expected, m_serializer);

        System.out.println("Json Serialization of class " + expected.getClass().getSimpleName() + ": " +
                           new String(binary.toByteArray()));

        Person                           p      = new Person("Smith", 45, false);
        PutAll<Integer, Remote.Function> actual = ExternalizableHelper.fromBinary(binary, m_serializer, PutAll.class);

        assertThat(expected.m_map.get(1).apply("foo"), is(actual.m_map.get(1).apply("foo")));
        assertThat(((Remote.Function<Person, Integer>) map.get(2)).apply(p), is(actual.m_map.get(2).apply(p)));
        assertThat(expected.m_map.size(), is(actual.m_map.size()));
        }

    @Test
    void shouldSerializeListUsingExternalizableHelper()
        {
        Holder<List<Integer>> holder = new Holder(new ArrayList(Arrays.asList(1, 2, 3, 4, 5, 10)));

        Binary binary = ExternalizableHelper.toBinary(holder, m_serializer);

        System.out.println("Json Serialization of class " + Holder.class.getSimpleName() + ": " +
                           new String(binary.toByteArray()));

        Holder result = ExternalizableHelper.fromBinary(binary, m_serializer, Holder.class);

        assertThat(result.getValues(), is(holder.getValues()));
        }

    @Test
    void shouldSerializeJsonMapListUsingExternalizableHelper()
            throws IOException, ClassNotFoundException
        {
        JsonObject map1 = new JsonObject();
        map1.put("name", "smith");
        map1.put("age", 22);
        map1.put("hobbies", new ArrayList(Arrays.asList("bike", "hike", "code")));
        JsonObject map2 = new JsonObject();
        map2.put("name", "jones");
        map2.put("age", 44);
        map2.put("hobbies", new ArrayList());

        Holder<List<JsonObject>> holder = new Holder<>(new ArrayList(Arrays.asList(map1, map2)));

        Binary binary = ExternalizableHelper.toBinary(holder, m_serializer);

        System.out.println("Json Serialization of class " + Holder.class.getSimpleName() + ": " +
                           new String(binary.toByteArray()));

        Holder result = ExternalizableHelper.fromBinary(binary, m_serializer, Holder.class);

        assertThat(result.getValues(), is(holder.getValues()));

        assertJsonProperties(holder, "HolderJsonMapList");
        Holder tmp = loadJsonObject("HolderJsonMapList", Holder.class);
        assertThat(tmp, is(holder));
        assertRoundTrip(holder);
        }

    @Test
    void shouldSerializeJsonObjectUsingExternalizableHelper()
            throws IOException, ClassNotFoundException
        {
        JsonObject o = new JsonObject();
        o.put("name", "smith");
        o.put("age", 22);
        Holder<JsonObject> holder = new Holder<>(o);

        Binary binary = ExternalizableHelper.toBinary(holder, m_serializer);

        System.out.println("Json Serialization of class " + Holder.class.getSimpleName() + ": " +
                           new String(binary.toByteArray()));

        Holder result = ExternalizableHelper.fromBinary(binary, m_serializer, Holder.class);

        assertThat(result.getValue(), is(holder.getValue()));

        Holder tmp = loadJsonObject("HolderJsonMap", Holder.class);
        assertThat(tmp.getValue(), is(holder.getValue()));
        assertJsonProperties(holder, "HolderJsonMap");
        }

    @Test
    void shouldSerializePersonUsingExternalizableHelper()
        {
        Holder<Person> holder = new Holder<>(new Person("smith", 42, false));

        Binary binary = ExternalizableHelper.toBinary(holder, m_serializer);

        System.out.println("Json Serialization of class " + Holder.class.getSimpleName() + ": " +
                           new String(binary.toByteArray()));

        Holder result = ExternalizableHelper.fromBinary(binary, m_serializer, Holder.class);

        assertThat(result.getValue(), is(holder.getValue()));
        }

    @Test
    void shouldSerialize() throws Exception
        {
        List<String>         list   = new ArrayList<>();
        Holder               holder = new Holder(list);
        ByteArrayWriteBuffer buf    = new ByteArrayWriteBuffer(512);

        list.add("foo");
        list.add("bar");

        m_serializer.serialize(buf.getBufferOutput(), holder);

        System.out.println("Json Serialization of class " + Holder.class.getSimpleName() + ": " +
                           new String(buf.toByteArray()));

        Holder result = m_serializer.deserialize(buf.getReadBuffer().getBufferInput(), Holder.class);
        assertThat(result.getValues(), is(holder.getValues()));
        }

    @Test
    void shouldNotSerializeWithoutAlias()
        {
        JsonSerializer       serializer = new JsonSerializer();
        List<String>         list       = new ArrayList<>();
        Holder               holder     = new Holder(list);
        ByteArrayWriteBuffer buf        = new ByteArrayWriteBuffer(512);

        list.add("foo");
        list.add("bar");

        assertThrows(JsonBindingException.class, () ->
                serializer.serialize(buf.getBufferOutput(), holder));
        }

    @Test
    void shouldNotDeserializeWithoutAlias()
        {
        JsonSerializer       serializer = new JsonSerializer();
        ByteArrayWriteBuffer buf        = new ByteArrayWriteBuffer(512);
        byte[]               bytes      = ("{\"@class\": \"" +  Holder.class.getName() +
                                           "\"}").getBytes(StandardCharsets.UTF_8);
        buf.write(0, bytes);

        assertThrows(JsonBindingException.class, () ->
                serializer.deserialize(buf.getReadBuffer().getBufferInput()));
        }

    @Test
    void shouldSupportBackwardsCompatAliases() throws Exception
        {
        JsonSerializer       serializer   = new JsonSerializer();
        ByteArrayWriteBuffer buf          = new ByteArrayWriteBuffer(512);
        byte[]               bytesAliased = "{\"@class\": \"filter.AlwaysFilter\"}".getBytes(StandardCharsets.UTF_8);
        byte[]               bytesFQC     = "{\"@class\": \"util.filter.AlwaysFilter\"}".getBytes(StandardCharsets.UTF_8);

        buf.write(0, bytesAliased);

        Object result = serializer.deserialize(buf.getReadBuffer().getBufferInput());
        assertThat(result, is(AlwaysFilter.INSTANCE()));

        buf.clear();
        buf.write(0, bytesFQC);

        result = serializer.deserialize(buf.getReadBuffer().getBufferInput());
        assertThat(result, is(AlwaysFilter.INSTANCE()));
        }

    @Test
    public void shouldBeAbleToDisableTypeEnforcementSysProp()
        {
        System.setProperty("coherence.json.type.enforcement", "false");
        JsonSerializer       serializer = new JsonSerializer();
        List<String>         list       = new ArrayList<>();
        Holder               holder     = new Holder(list);
        ByteArrayWriteBuffer buf        = new ByteArrayWriteBuffer(512);

        list.add("foo");
        list.add("bar");

        try
            {
            assertDoesNotThrow(() -> serializer.serialize(buf.getBufferOutput(), holder));
            }
        finally
            {
            System.setProperty("coherence.json.type.enforcement", "true");
            }
        }

    @Test
    void testAliasing() throws Exception
        {
        JsonSerializer serializer = new JsonSerializer();
        Genson         genson = serializer.underlying();

        Class<?> clz = String.class;
        String sName = clz.getName();
        assertThat(genson.aliasFor(clz), is(sName));
        assertThat(genson.classFor(sName), notNullValue());
        assertThat(genson.classFor(sName).getName(), is(sName));

        clz = Name.class;
        sName = clz.getName();
        assertThat(genson.aliasFor(clz), is(sName));
        assertThat(genson.classFor(sName), notNullValue());
        assertThat(genson.classFor(sName).getName(), is(sName));

        clz = Object[].class;
        sName = clz.getName();
        assertThat(genson.aliasFor(clz), is(sName));
        assertThat(genson.classFor(sName), notNullValue());
        assertThat(genson.classFor(sName).getName(), is(sName));

        assertThrows(JsonBindingException.class, () -> genson.aliasFor(SafeJsonSerializer.class));
        assertThrows(JsonBindingException.class, () -> genson.aliasFor(SafeJsonSerializer[].class));
        assertThrows(JsonBindingException.class, () -> genson.classFor(SafeJsonSerializer.class.getName()));
        assertThrows(JsonBindingException.class, () -> genson.classFor(SafeJsonSerializer[].class.getName()));
        }

    @Test
    void testRemoteFunctionLambda()
            throws IOException
        {
        Remote.Function<Integer, Optional<Integer>> lambda = e -> Optional.empty();

        BiFunction<Object, Object, Boolean> compare = (expected, actual) ->
        {
        Function eFn = (Function) expected;
        Function aFn = (Function) actual;

        Optional e = (Optional) eFn.apply(1);
        Optional a = (Optional) aFn.apply(1);

        return e.equals(a);
        };

        assertRoundTrip(lambda, Remote.Function.class, compare);
        }

    @Test
    void testFunctionLambda()
            throws IOException
        {
        Function<String, String> lambda = (Remote.Function) s -> s + "Updated";

        BiFunction<Object, Object, Boolean> compare = (expected, actual) ->
        {
        Function<String, String> eFn = (Function) expected;
        Function<String, String> aFn = (Function) actual;

        String e = eFn.apply("foo");
        String a = aFn.apply("foo");

        return e.equals(a);
        };

        assertRoundTrip(lambda, Function.class, compare);
        }

    @Test
    void testSupplierLambda()
            throws IOException
        {
        Supplier<String> lambda = (Remote.Supplier) () -> "Updated";

        BiFunction<Object, Object, Boolean> compare = (expected, actual) ->
        {
        Supplier<String> eFn = (Supplier) expected;
        Supplier<String> aFn = (Supplier) actual;

        String e = eFn.get();
        String a = aFn.get();

        return e.equals(a);
        };

        assertRoundTrip(lambda, Supplier.class, compare);
        }

    @Test
    void testOptional()
            throws IOException
        {
        assertRoundTrip(Optional.empty());
        assertRoundTrip(Optional.empty());
        assertRoundTrip(Optional.of("String"));
        assertRoundTrip(Optional.of(42));
        assertRoundTrip(Optional.of(42.35));
        assertRoundTrip(Optional.of(true));
        assertRoundTrip(Optional.of(false));
        assertRoundTrip(Optional.of(new Person("smith", 33, false)));
        }

    @SuppressWarnings("EqualsWithItself")
    @Test
    void testSafeComparator()
            throws IOException
        {
        BiFunction<Object, Object, Boolean> compare = (expected, actual) ->
        {
        SafeComparator c1 = (SafeComparator) expected;
        SafeComparator c2 = (SafeComparator) actual;

        assertEquals(c1.compare(3, 4), c2.compare(3, 4));
        assertEquals(c1.compare("foo", "foo"), c2.compare("foo", "foo"));

        return c2.isNullFirst() == c2.isNullFirst();
        };

        assertRoundTrip(new SafeComparator());

        Remote.Comparator comparator = (o1, o2) -> o1.hashCode() - o2.hashCode();
        assertRoundTrip(new SafeComparator(comparator), compare);
        assertRoundTrip(new SafeComparator(comparator, false), compare);
        }

    @Test
    public void shouldSerializeBackslash()
        {
        JsonSerializer serializer = new JsonSerializer(Base.getContextClassLoader(),
                                                       builder -> builder.setEnforceTypeAliases(false),
                                                       false);

        Genson              genson = serializer.underlying();
        Map<String, Object> mapIn  = new LinkedHashMap<>();

        mapIn.put("Foo", "\\");
        mapIn.put("Bar", "Bar\\");

        String                        sJson  = genson.serialize(mapIn);
        LinkedHashMap<String, Object> mapOut = genson.deserialize(sJson, LinkedHashMap.class);

        assertThat(mapOut, is(mapIn));
        }

    @Test
    public void shouldRoundTripBigDecimalAggregatorsNoMathContext()
            throws Exception
        {
        assertRoundTrip(new BigDecimalAverage<>("age"));
        assertRoundTrip(new BigDecimalMax<>("age"));
        assertRoundTrip(new BigDecimalMin<>("age"));
        assertRoundTrip(new BigDecimalSum<>("age"));
        }

    @SuppressWarnings("rawtypes")
    @Test
    public void shouldRoundTripBigDecimalAggregatorsWithMathContext()
            throws Exception
        {
        MathContext       mCtx = new MathContext(10);
        BigDecimalAverage avg  = new BigDecimalAverage("age");
        BigDecimalMax     max  = new BigDecimalMax("age");
        BigDecimalMin     min  = new BigDecimalMin("age");
        BigDecimalSum     sum  = new BigDecimalSum("age");

        avg.setMathContext(mCtx);
        max.setMathContext(mCtx);
        min.setMathContext(mCtx);
        sum.setMathContext(mCtx);

        assertRoundTrip(avg);
        assertRoundTrip(max);
        assertRoundTrip(min);
        assertRoundTrip(sum);
        }

    @Test
    public void testFromJson()
        {
        JsonObject result = (JsonObject) JsonSerializer.fromJson("{\"id\": 11, \"name\": \"Name-11\", \"age\": 21, \"insertTime\": 1692663834644975000}");
        assertNotNull(result);
        assertEquals(11, result.get("id"));
        assertEquals("Name-11", result.get("name"));
        assertEquals(21, result.get("age"));
        assertEquals(1692663834644975000L, result.get("insertTime"));
        }

    @Test
    public void testUnicodeEscape()
        {
        StringWriter sw = new StringWriter();
        JsonWriter   w  = new JsonWriter(sw);

        String sNonUnicode = "\\usr\\foo";

        w.beginObject().writeName("key").writeValue(sNonUnicode);
        w.endObject();
        w.flush();
        w.close();

        JsonReader reader = new JsonReader(sw.toString());
        reader.beginObject();
        reader.next();
        assertEquals("key", reader.name());
        assertEquals(sNonUnicode, reader.valueAsString());

        sw = new StringWriter();
        w  = new JsonWriter(sw);

        sNonUnicode = "\\u00G4";

        w.beginObject().writeName("key").writeValue(sNonUnicode);
        w.endObject();
        w.flush();

        reader = new JsonReader(sw.toString());
        reader.beginObject();
        reader.next();
        assertEquals("key", reader.name());
        assertEquals(sNonUnicode, reader.valueAsString());

        sw = new StringWriter();
        w  = new JsonWriter(sw);

        sNonUnicode = "\\u00\\u005A";

        w.beginObject().writeName("key").writeValue(sNonUnicode);
        w.endObject();
        w.flush();

        reader = new JsonReader(sw.toString());
        reader.beginObject();
        reader.next();
        assertEquals("key", reader.name());
        assertEquals("\\u00Z", reader.valueAsString());
        }


    // ----- inner class: Holder --------------------------------------------

    public static class Holder<T>
        {
        // ----- constructors -----------------------------------------------
        @SuppressWarnings("unused")
        public Holder()
            {
            }

        public Holder(T value)
            {
            if (value instanceof Collection)
                {
                m_values = (Collection) value;
                }
            else
                {
                m_value = value;
                }
            }

        // ----- public methods ---------------------------------------------

        public T getValue()
            {
            return m_value;
            }

        public Collection getValues()
            {
            return m_values;
            }

        // ----- Object methods ---------------------------------------------

        public boolean equals(Object o)
            {
            if (o instanceof Holder)
                {
                Holder<T> that = (Holder) o;
                if (m_value != null && that.m_value != null)
                    {
                    return m_value.equals(that.m_value);
                    }
                if (m_values != null && that.m_values != null)
                    {
                    return m_values.containsAll(that.m_values);
                    }

                // handle null
                return m_value == that.m_value && m_values == that.m_values;
                }

            return false;
            }

        // ----- data members -----------------------------------------------

        @JsonProperty("value")
        protected T m_value;

        @JsonProperty("values")
        protected Collection<T> m_values;
        }

    // ----- inner class: ContainsDuration ----------------------------------

    static public class ContainsDuration
        {
        // ----- constructors -----------------------------------------------
        @JsonCreator
        public ContainsDuration(@JsonProperty("duration") Duration d)
            {
            f_duration = d;
            }

        // ---- object methods ----------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            return o instanceof ContainsDuration && f_duration.equals(((ContainsDuration) o).f_duration);
            }

        // ----- data members -----------------------------------------------

        protected final Duration f_duration;
        }

    // ----- inner class: BigIntegerArrayHolder -----------------------------

    public static class BigIntegerArrayHolder
        {
        // ----- constructors -----------------------------------------------

        @SuppressWarnings("unused")
        public BigIntegerArrayHolder()
            {
            }

        public BigIntegerArrayHolder(BigInteger[] array)
            {
            this.m_anArray = array;
            }

        // ----- Object methods ---------------------------------------------

        public boolean equals(Object o)
            {
            if (o instanceof BigIntegerArrayHolder)
                {
                return Arrays.equals(m_anArray, ((BigIntegerArrayHolder) o).m_anArray);
                }
            return false;
            }

        // ----- data members -----------------------------------------------

        @JsonProperty("array")
        public BigInteger[] m_anArray;
        }
    }
