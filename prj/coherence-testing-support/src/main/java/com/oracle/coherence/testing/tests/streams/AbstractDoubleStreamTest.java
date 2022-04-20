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
import com.tangosol.util.stream.RemoteDoubleStream;
import com.tangosol.util.stream.RemoteStream;
import data.pof.Person;
import org.junit.Test;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
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
public abstract class AbstractDoubleStreamTest
        extends AbstractStreamTest
    {
    protected AbstractDoubleStreamTest(boolean fParallel)
        {
        super(fParallel);
        }

    @Test
    public void testFilter()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertEquals(2, stream.filter(n -> n > 18).count());
        }

    @Test
    public void testMap()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        double[] ages = stream.map(n -> n * -1)
                .sorted()
                .toArray();
        assertArrayEquals(new double[] {-40, -36, -10, -6, -1}, ages, 0.01);
        }

    @Test
    public void testMapToInt()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        int[] ages = stream.mapToInt(n -> (int) n * -1)
                .sorted()
                .toArray();
        assertArrayEquals(new int[] {-40, -36, -10, -6, -1}, ages);
        }

    @Test
    public void testMapToLong()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        long[] ages = stream.mapToLong(n -> (long) n * -1L)
                .sorted()
                .toArray();
        assertArrayEquals(new long[] {-40, -36, -10, -6, -1}, ages);
        }

    @Test
    public void testMapToObj()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        String[] ages = stream
                .mapToObj(Double::toString)
                .sorted()
                .toArray(String[]::new);
        assertArrayEquals(new String[] {"1.0", "10.0", "36.0", "40.0", "6.0"}, ages);
        }

    @Test
    public void testFlatMap()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        double[] ages = stream
                .flatMap(n -> Arrays.stream(new double[] {n * -1, n}))
                .sorted()
                .toArray();
        double[] expected = {-1, 1, -6, 6, -10, 10, -36, 36, -40, 40};
        Arrays.sort(expected);
        assertArrayEquals(expected, ages, 0.01);
        }

    @Test
    public void testDistinct()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        double[] ages = stream
                .flatMap(n -> Arrays.stream(new double[] {n, n, n}))
                .distinct()
                .sorted()
                .toArray();
        assertArrayEquals(new double[] {1, 6, 10, 36, 40}, ages, 0.01);
        }

    @Test
    public void testPeek()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        double[] ages = stream
                .peek((p) -> System.out.println(p))
                .sorted()
                .toArray();
        assertArrayEquals(new double[] {1, 6, 10, 36, 40}, ages, 0.01);
        }

    @Test
    public void testLimit()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        double[] ages = stream
                .limit(1)
                .toArray();
        assertEquals(1, ages.length);
        assertTrue(new HashSet<>(Arrays.asList(1.0, 6.0, 10.0, 36.0, 40.0)).contains(ages[0]));
        }

    @Test
    public void testSkip()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        double[] ages = stream
                .skip(4)
                .toArray();
        assertEquals(1, ages.length);
        assertTrue(new HashSet<>(Arrays.asList(1.0, 6.0, 10.0, 36.0, 40.0)).contains(ages[0]));
        }

    @Test
    public void testForEach()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        HashSet<Double> ages = new HashSet<>();
        stream.forEach(n -> ages.add(n));
        assertEquals(new HashSet<>(Arrays.asList(1.0, 6.0, 10.0, 36.0, 40.0)), ages);
        }

    @Test
    public void testForEachOrdered()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        HashSet<Double> ages = new HashSet<>();
        stream.forEachOrdered(n -> ages.add(n));
        assertEquals(new HashSet<>(Arrays.asList(1.0, 6.0, 10.0, 36.0, 40.0)), ages);
        }

    @Test
    public void testToArray()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        double[] ages = stream.toArray();
        Arrays.sort(ages);
        assertArrayEquals(new double[] {1, 6, 10, 36, 40}, ages, 0.01);
        }

    @Test
    public void testReduceWithIdentity()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertEquals(93, stream.reduce(0, Double::sum), 0.01);
        }

    @Test
    public void testReduce()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertEquals(93, stream.reduce(Double::sum).getAsDouble(), 0.01);
        }

    @Test
    public void testReduceWithEmptyStream()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertFalse(stream.filter(n -> n > 50).reduce(Double::sum).isPresent());
        }

    @Test
    public void testCollect()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        SortedSet<Double> ages = stream.collect(PortableSortedSet::new,
                                                (set, n) -> set.add(n),
                                                PortableSortedSet::addAll);
        assertEquals(new TreeSet<>(Arrays.asList(1.0, 6.0, 10.0, 36.0, 40.0)), ages);
        }

    @Test
    public void testMin()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertEquals(1, stream.min().getAsDouble(), 0.01);
        }

    @Test
    public void testMax()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertEquals(40, stream.max().getAsDouble(), 0.01);
        }

    @Test
    public void testCount()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertEquals(5, stream.count());
        }

    @Test
    public void testAverage()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertEquals(93.0 / 5, stream.average().getAsDouble(), 0.01);
        }

    @Test
    public void testSummaryStatistics()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));

        DoubleSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(5L, stats.getCount());
        assertEquals(93, stats.getSum(), 0.01);
        assertEquals(40, stats.getMax(), 0.01);
        assertEquals( 1, stats.getMin(), 0.01);
        assertEquals(93.0 / 5, stats.getAverage(), 0.001);
        }

    @Test
    public void testAnyMatch()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertTrue (stream.anyMatch(n -> n > 30));
        assertFalse(stream.anyMatch(n -> n > 50));
        }

    @Test
    public void testAllMatch()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertTrue (stream.allMatch(n -> n < 50));
        assertFalse(stream.allMatch(n -> n < 30));
        }

    @Test
    public void testNoneMatch()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertTrue (stream.noneMatch(n -> n > 50));
        assertFalse(stream.noneMatch(n -> n < 10));
        }

    @Test
    public void testAnyMatchEmptyStream()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertFalse(stream.filter(n -> n > 50).anyMatch(n -> n < 50));
        }

    @Test
    public void testAllMatchEmptyStream()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertTrue(stream.filter(n -> n > 50).allMatch(n -> n < 50));
        }

    @Test
    public void testNoneMatchEmptyStream()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertTrue(stream.filter(n -> n > 50).noneMatch(n -> n < 50));
        }

    @Test
    public void testFindFirst()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertTrue(stream.findFirst().isPresent());
        }

    @Test
    public void testFindAny()
        {
        RemoteDoubleStream stream = getStream().mapToDouble(e -> e.extract(Person::getAge));
        assertTrue(stream.findAny().isPresent());
        }

    @Test
    public void testPipelineBuilder()
        {
        RemoteStream<InvocableMap.Entry<String, Person>> stream = StreamSupport.pipelineBuilder();
        assertTrue(stream.map(InvocableMap.Entry::getValue)
                         .mapToDouble(Person::getAge)
                         .pipeline() instanceof RemoteDoubleStream);
        }

    @Test(expected = IllegalStateException.class)
    public void testPipelineBuilderTerminal()
        {
        RemoteStream<InvocableMap.Entry<String, Person>> stream = StreamSupport.pipelineBuilder();
        stream.map(InvocableMap.Entry::getValue)
              .mapToDouble(Person::getAge)
              .max();
        }
    }
