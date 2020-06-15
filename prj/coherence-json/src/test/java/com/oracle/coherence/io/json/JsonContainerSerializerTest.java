/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import common.data.Person;

import java.io.IOException;

import java.math.BigInteger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

/**
 * Tests to validate container type serialization.
 *
 * @since 20.06
 */
class JsonContainerSerializerTest
        extends AbstractSerializerTest
    {

    @Test
    void testObjectArrayHolder()
            throws IOException
        {
        assertRoundTrip(ResultHolder.createResultHolder(new String[] {"one", "two", "three"}));
        assertRoundTrip(ResultHolder.createResultHolder(new Object[] {10, 9, 8, 7, 6, 5, 4}));
        assertRoundTrip(ResultHolder.createResultHolder(
                new Person[] { new Person("array_smith", 12, true),  new Person("arr_jones", 22, false)}
        ));
        }

    @Test
    void testCollectionResultHolder()
            throws IOException
        {
        assertRoundTrip(ResultHolder.createResultHolder(Arrays.asList("one", "two", "three")));
        assertRoundTrip(ResultHolder.createResultHolder(Arrays.asList(10, 9, 8, 7, 6, 5, 4)));

        ResultHolder collectionResult =
                ResultHolder.createResultHolder(
                        Arrays.asList(new Person("col_smith", 12, true),
                                      new Person("col_jones", 22, false)));
        assertRoundTrip(collectionResult);
        }

    @Test
    void testResultHolder()
            throws IOException
        {
        assertRoundTrip(ResultHolder.createResultHolder(true));
        assertRoundTrip(ResultHolder.createResultHolder(42));
        assertRoundTrip(ResultHolder.createResultHolder("foo"));
        assertRoundTrip(ResultHolder.createResultHolder(Long.MAX_VALUE - 25));
        assertRoundTrip(ResultHolder.createResultHolder(Double.MAX_VALUE - 25.0));

        ResultHolder result = ResultHolder.createResultHolder(new Person("brown", 74, false));
        assertRoundTrip(result);
        }

    @Test
    void testBigIntegerObjectArrayHolder()
            throws IOException
        {
        // number comparison, don't fail compare due to comparing a BigInteger.ONE with Integer value of 1.
        BiFunction<Object, Object, Boolean> compare = (expected, actual) ->
        {
        Object[] arrayE = ((ObjectArrayResultHolder) expected).getValue();
        Object[] arrayA = ((ObjectArrayResultHolder) expected).getValue();

        if (arrayE.length != arrayA.length)
            {
            return false;
            }

        if (arrayE[0] instanceof BigInteger)
            {
            for (int i = 0; i < arrayE.length; i++)
                {
                if (!arrayE[i].equals(new BigInteger(arrayA[i].toString())))
                    {
                    return false;
                    }
                }
            return true;
            }
        else
            {
            for (int i = 0; i < arrayA.length; i++)
                {
                if (!arrayA[i].equals(new BigInteger(arrayE[i].toString())))
                    {
                    return false;
                    }
                }
            return true;
            }
        };
        assertRoundTrip(
                ResultHolder.createResultHolder(
                    new BigInteger[] { BigInteger.ONE, BigInteger.TEN, new BigInteger("987654321987654321987654321")}),
                        compare);
        }

    @Test
    void testReferencedCollectionsInResult()
            throws IOException
        {
        assertRoundTrip(new Result());
        }

    // ----- inner class: Result --------------------------------------------

    public static class Result
        {
        // ----- Object methods ---------------------------------------------

        public boolean equals(Object o)
            {
            if (o instanceof Result)
                {
                Result that = (Result) o;
                if (!f_colPerson.equals(that.f_colPerson))
                    {
                    System.out.println("compare failed on collectionObjectPerson property" + " that elementType=" +
                                       ((List) that.f_colPerson).get(1).getClass().getName());

                    return false;
                    }
                return true;
                }
            return false;
            }

        // ----- data members -----------------------------------------------

        private final Object f_colPerson =
                new ArrayList<>(Arrays.asList(new Person("col_smith", 12, true),
                                              new Person("col_jones", 22, false)));
        }

    // ----- inner class: ResultHolder --------------------------------------

    public static class ResultHolder<T>
        {
        // ----- constructors -----------------------------------------------

        public ResultHolder()
            {
            }

        public ResultHolder(T value)
            {
            m_value = value;
            }

        // ----- public methods ---------------------------------------------

        public T getValue()
            {
            return m_value;
            }

        @SuppressWarnings("unchecked")
        static public ResultHolder createResultHolder(Object result)
            {
            if (result instanceof Collection)
                {
                return new CollectionResultHolder((Collection) result);
                }
            else if (result.getClass().isArray())
                {
                return new ObjectArrayResultHolder((Object[]) result);
                }
            else
                {
                return new ResultHolder(result);
                }
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (o instanceof ResultHolder)
                {
                return m_value.equals(((ResultHolder) o).m_value);
                }
            return false;
            }

        // ----- data members -----------------------------------------------

        @JsonProperty("value")
        protected T m_value;
        }

    // ----- inner class: CollectionResultHolder ----------------------------

    public static class CollectionResultHolder
            extends ResultHolder<Collection>
        {
        // ----- constructors -----------------------------------------------

        @JsonCreator
        public CollectionResultHolder(@JsonProperty(value = "value", required = true) Collection value)
            {
            m_value = value;
            }

        // ----- Object methods ---------------------------------------------

        @SuppressWarnings("unchecked")
        @Override
        public boolean equals(Object o)
            {
            if (o instanceof CollectionResultHolder)
                {
                return m_value.containsAll(((CollectionResultHolder) o).m_value);
                }
            return false;
            }
        }

    // ----- inner class: ObjectArrayResultHolder ---------------------------

    public static class ObjectArrayResultHolder
            extends ResultHolder<Object[]>
        {
        // ----- constructors -----------------------------------------------

        @JsonCreator
        public ObjectArrayResultHolder(@JsonProperty(value = "value", required = true) Object[] value)
            {
            m_value = value;
            }

        // ----- object methods ---------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (o instanceof ObjectArrayResultHolder)
                {
                return Arrays.equals(m_value, ((ObjectArrayResultHolder) o).m_value);
                }
            return false;
            }
        }
    }
