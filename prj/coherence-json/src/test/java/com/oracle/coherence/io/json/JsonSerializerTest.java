/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.oracle.coherence.common.base.SimpleHolder;

import com.tangosol.io.ByteArrayWriteBuffer;

import com.tangosol.util.Binary;

import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.function.Remote;

import common.data.JavaNumberType;
import common.data.Person;
import common.data.PutAll;

import java.io.IOException;

import java.math.BigInteger;

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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JSON serialization tests.
 *
 * @author Aleks Seovic  2017.10.02
* @author jf  2018.03.09
 * @since 20.06
 */
@SuppressWarnings("unchecked")
class JsonSerializerTest
        extends AbstractSerializerTest
    {
    // ----- test cases -----------------------------------------------------
    @Test
    void shouldFallBackToJsonObjectWhenClassIsMissing()
        {
        String json = "{\"@class\":\"com.missing.Point\",\"x\":22,\"y\":42}";

        JsonSerializer serializer = new JsonSerializer();
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

//    @Test
//    void shouldNotSetLoaderTwice() {
//        ClassLoader loader = Thread.currentThread().getContextClassLoader();
//        JsonSerializer serializer = new JsonSerializer();
//
//        serializer.setContextClassLoader(loader);
//
//        assertThrows(AssertionError.class, () -> serializer.setContextClassLoader(mock(ClassLoader.class)));
//    }

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
            throws IOException, ClassNotFoundException
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
        List<String> actualNums   = (List<String>) actual;

        if (expectedNums.size() != actualNums.size())
            {
            return false;
            }
        for (int i = 0; i < expectedNums.size(); i++)
            {
            Object expectedElement = expectedNums.get(i);
            Object actualElement   = actualNums.get(i);

            System.out.println("expectedElementClass= " + expectedElement.getClass().getName() +
                               "  actual ElementClass=" + actualElement.getClass().getName());

            BigInteger a = new BigInteger(actualNums.get(i));
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
        assertRoundTrip(Optional.ofNullable(null));
        assertRoundTrip(Optional.ofNullable("String"));
        assertRoundTrip(Optional.ofNullable(42));
        assertRoundTrip(Optional.ofNullable(42.35));
        assertRoundTrip(Optional.ofNullable(true));
        assertRoundTrip(Optional.ofNullable(false));
        assertRoundTrip(Optional.ofNullable(new Person("smith", 33, false)));
        }

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
                if (m_values instanceof Collection &&
                    that.m_values instanceof Collection)
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
