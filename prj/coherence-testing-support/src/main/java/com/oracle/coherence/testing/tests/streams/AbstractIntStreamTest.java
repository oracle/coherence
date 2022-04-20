/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.tests.streams;


import com.tangosol.internal.util.collection.PortableSortedSet;
import com.tangosol.internal.util.stream.StreamSupport;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.stream.RemoteIntStream;
import com.tangosol.util.stream.RemoteStream;
import data.pof.Person;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author as  2014.10.02
 */
@SuppressWarnings({"Convert2MethodRef"})
public abstract class AbstractIntStreamTest
        extends AbstractStreamTest
    {
    protected AbstractIntStreamTest(boolean fParallel)
        {
        super(fParallel);
        }

    protected RemoteIntStream getIntStream(ValueExtractor<? super Person, ? extends Number> extractor)
        {
        return RemoteStream.toIntStream(getStream(extractor));
        }

    @Test
    public void testFilter()
        {
        RemoteIntStream stream = getIntStream(Person::getAge);
        assertEquals(2, stream.filter(n -> n > 18).count());
        }

    @Test
    public void testMap()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        int[] ages = stream.map(n -> n * -1)
                .sorted()
                .toArray();
        assertArrayEquals(new int[] {-40, -36, -10, -6, -1}, ages);
        }

    @Test
    public void testMapToLong()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        long[] ages = stream.mapToLong(n -> n * -1L)
                .sorted()
                .toArray();
        assertArrayEquals(new long[] {-40, -36, -10, -6, -1}, ages);
        }

    @Test
    public void testMapToDouble()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        double[] ages = stream.mapToDouble(n -> n * -1.0)
                .sorted()
                .toArray();
        assertArrayEquals(new double[] {-40, -36, -10, -6, -1}, ages, 0.01);
        }

    @Test
    public void testMapToObj()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        String[] ages = stream
                .mapToObj(Integer::toString)
                .sorted()
                .toArray(String[]::new);
        assertArrayEquals(new String[] {"1", "10", "36", "40", "6"}, ages);
        }

    @Test
    public void testFlatMap()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        int[] ages = stream
                .flatMap(n -> Arrays.stream(new int[] {n * -1, n}))
                .sorted()
                .toArray();
        int[] expected = {-1, 1, -6, 6, -10, 10, -36, 36, -40, 40};
        Arrays.sort(expected);
        assertArrayEquals(expected, ages);
        }

    @Test
    public void testDistinct()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        int[] ages = stream
                .flatMap(n -> Arrays.stream(new int[] {n, n, n}))
                .distinct()
                .sorted()
                .toArray();
        assertArrayEquals(new int[] {1, 6, 10, 36, 40}, ages);
        }

    @Test
    public void testPeek()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        int[] ages = stream
                .peek((p) -> System.out.println(p))
                .sorted()
                .toArray();
        assertArrayEquals(new int[] {1, 6, 10, 36, 40}, ages);
        }

    @Test
    public void testLimit()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        int[] ages = stream
                .limit(1)
                .toArray();
        assertEquals(1, ages.length);
        assertTrue(new HashSet<>(Arrays.asList(1, 6, 10, 36, 40)).contains(ages[0]));
        }

    @Test
    public void testSkip()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        int[] ages = stream
                .skip(4)
                .toArray();
        assertEquals(1, ages.length);
        assertTrue(new HashSet<>(Arrays.asList(1, 6, 10, 36, 40)).contains(ages[0]));
        }

    @Test
    public void testForEach()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        HashSet<Integer> ages = new HashSet<>();
        stream.forEach(n -> ages.add(n));
        assertEquals(new HashSet<>(Arrays.asList(1, 6, 10, 36, 40)), ages);
        }

    @Test
    public void testForEachOrdered()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        HashSet<Integer> ages = new HashSet<>();
        stream.forEachOrdered(n -> ages.add(n));
        assertEquals(new HashSet<>(Arrays.asList(1, 6, 10, 36, 40)), ages);
        }

    @Test
    public void testToArray()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        int[] ages = stream.toArray();
        Arrays.sort(ages);
        assertArrayEquals(new int[] {1, 6, 10, 36, 40}, ages);
        }

    @Test
    public void testReduceWithIdentity()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertEquals(93, stream.reduce(0, Integer::sum));
        }

    @Test
    public void testReduce()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertEquals(93, stream.reduce(Integer::sum).getAsInt());
        }

    @Test
    public void testReduceWithEmptyStream()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertFalse(stream.filter(n -> n > 50).reduce(Integer::sum).isPresent());
        }

    @Test
    public void testCollect()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        SortedSet<Integer> ages = stream.collect(PortableSortedSet::new,
                                                 (set, n) -> set.add(n),
                                                 PortableSortedSet::addAll);
        assertEquals(new TreeSet<>(Arrays.asList(1, 6, 10, 36, 40)), ages);
        }

    @Test
    public void testMin()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertEquals(1, stream.min().getAsInt());
        }

    @Test
    public void testMax()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertEquals(40, stream.max().getAsInt());
        }

    @Test
    public void testCount()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertEquals(5, stream.count());
        }

    @Test
    public void testAverage()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertEquals(93.0 / 5, stream.average().getAsDouble(), 0.01);
        }

    @Test
    public void testSummaryStatistics()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));

        IntSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(5L, stats.getCount());
        assertEquals(93, stats.getSum());
        assertEquals(40, stats.getMax());
        assertEquals(1, stats.getMin());
        assertEquals(93.0 / 5, stats.getAverage(), 0.001);
        }

    @Test
    public void testAnyMatch()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertTrue (stream.anyMatch(n -> n > 30));
        assertFalse(stream.anyMatch(n -> n > 50));
        }

    @Test
    public void testAllMatch()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertTrue (stream.allMatch(n -> n < 50));
        assertFalse(stream.allMatch(n -> n < 30));
        }

    @Test
    public void testNoneMatch()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertTrue (stream.noneMatch(n -> n > 50));
        assertFalse(stream.noneMatch(n -> n < 10));
        }

    @Test
    public void testAnyMatchEmptyStream()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertFalse(stream.filter(n -> n > 50).anyMatch(n -> n < 50));
        }

    @Test
    public void testAllMatchEmptyStream()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertTrue(stream.filter(n -> n > 50).allMatch(n -> n < 50));
        }

    @Test
    public void testNoneMatchEmptyStream()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertTrue(stream.filter(n -> n > 50).noneMatch(n -> n < 50));
        }

    @Test
    public void testFindFirst()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertTrue(stream.findFirst().isPresent());
        }

    @Test
    public void testFindAny()
        {
        RemoteIntStream stream = getStream().mapToInt(e -> e.extract(Person::getAge));
        assertTrue(stream.findAny().isPresent());
        }

    @Test
    public void testPipelineBuilder()
        {
        RemoteStream<InvocableMap.Entry<String, Person>> stream = StreamSupport.pipelineBuilder();
        assertTrue(stream.map(InvocableMap.Entry::getValue)
                           .mapToInt(Person::getAge)
                           .pipeline() instanceof RemoteIntStream);
        }

    @Test(expected = IllegalStateException.class)
    public void testPipelineBuilderTerminal()
        {
        RemoteStream<InvocableMap.Entry<String, Person>> stream = StreamSupport.pipelineBuilder();
        stream.map(InvocableMap.Entry::getValue)
                       .mapToInt(Person::getAge)
                       .max();
        }
    }
