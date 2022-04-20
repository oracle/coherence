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
import com.tangosol.util.stream.RemoteLongStream;
import com.tangosol.util.stream.RemoteStream;
import data.pof.Person;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LongSummaryStatistics;
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
public abstract class AbstractLongStreamTest
        extends AbstractStreamTest
    {
    public AbstractLongStreamTest(boolean fParallel)
        {
        super(fParallel);
        }

    protected RemoteLongStream getLongStream(ValueExtractor<? super Person, ? extends Number> extractor)
        {
        return RemoteStream.toLongStream(getStream(extractor));
        }

    @Test
    public void testFilter()
        {
        RemoteLongStream stream = getLongStream(Person::getAge);
        assertEquals(2, stream.filter(n -> n > 18).count());
        }

    @Test
    public void testMap()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        long[] ages = stream.map(n -> n * -1)
                .sorted()
                .toArray();
        assertArrayEquals(new long[] {-40, -36, -10, -6, -1}, ages);
        }

    @Test
    public void testMapToInt()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        int[] ages = stream.mapToInt(n -> (int) n * -1)
                .sorted()
                .toArray();
        assertArrayEquals(new int[] {-40, -36, -10, -6, -1}, ages);
        }

    @Test
    public void testMapToDouble()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        double[] ages = stream.mapToDouble(n -> n * -1.0)
                .sorted()
                .toArray();
        assertArrayEquals(new double[] {-40, -36, -10, -6, -1}, ages, 0.01);
        }

    @Test
    public void testMapToObj()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        String[] ages = stream
                .mapToObj(Long::toString)
                .sorted()
                .toArray(String[]::new);
        assertArrayEquals(new String[] {"1", "10", "36", "40", "6"}, ages);
        }

    @Test
    public void testFlatMap()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        long[] ages = stream
                .flatMap(n -> Arrays.stream(new long[] {n * -1, n}))
                .sorted()
                .toArray();
        long[] expected = {-1, 1, -6, 6, -10, 10, -36, 36, -40, 40};
        Arrays.sort(expected);
        assertArrayEquals(expected, ages);
        }

    @Test
    public void testDistinct()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        long[] ages = stream
                .flatMap(n -> Arrays.stream(new long[] {n, n, n}))
                .distinct()
                .sorted()
                .toArray();
        assertArrayEquals(new long[] {1, 6, 10, 36, 40}, ages);
        }

    @Test
    public void testPeek()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        long[] ages = stream
                .peek((p) -> System.out.println(p))
                .sorted()
                .toArray();
        assertArrayEquals(new long[] {1, 6, 10, 36, 40}, ages);
        }

    @Test
    public void testLimit()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        long[] ages = stream
                .limit(1)
                .toArray();
        assertEquals(1, ages.length);
        assertTrue(new HashSet<>(Arrays.asList(1L, 6L, 10L, 36L, 40L)).contains(ages[0]));
        }

    @Test
    public void testSkip()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        long[] ages = stream
                .skip(4)
                .toArray();
        assertEquals(1, ages.length);
        assertTrue(new HashSet<>(Arrays.asList(1L, 6L, 10L, 36L, 40L)).contains(ages[0]));
        }

    @Test
    public void testForEach()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        HashSet<Long> ages = new HashSet<>();
        stream.forEach(n -> ages.add(n));
        assertEquals(new HashSet<>(Arrays.asList(1L, 6L, 10L, 36L, 40L)), ages);
        }

    @Test
    public void testForEachOrdered()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        HashSet<Long> ages = new HashSet<>();
        stream.forEachOrdered(n -> ages.add(n));
        assertEquals(new HashSet<>(Arrays.asList(1L, 6L, 10L, 36L, 40L)), ages);
        }

    @Test
    public void testToArray()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        long[] ages = stream.toArray();
        Arrays.sort(ages);
        assertArrayEquals(new long[] {1, 6, 10, 36, 40}, ages);
        }

    @Test
    public void testReduceWithIdentity()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertEquals(93, stream.reduce(0, Long::sum));
        }

    @Test
    public void testReduce()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertEquals(93, stream.reduce(Long::sum).getAsLong());
        }

    @Test
    public void testReduceWithEmptyStream()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertFalse(stream.filter(n -> n > 50).reduce(Long::sum).isPresent());
        }

    @Test
    public void testCollect()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        SortedSet<Long> ages = stream.collect(PortableSortedSet::new,
                                              (set, n) -> set.add(n),
                                              PortableSortedSet::addAll);
        assertEquals(new TreeSet<>(Arrays.asList(1L, 6L, 10L, 36L, 40L)), ages);
        }

    @Test
    public void testMin()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertEquals(1, stream.min().getAsLong());
        }

    @Test
    public void testMax()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertEquals(40, stream.max().getAsLong());
        }

    @Test
    public void testCount()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertEquals(5, stream.count());
        }

    @Test
    public void testAverage()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertEquals(93.0 / 5, stream.average().getAsDouble(), 0.01);
        }

    @Test
    public void testSummaryStatistics()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));

        LongSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(5L, stats.getCount());
        assertEquals(93, stats.getSum());
        assertEquals(40, stats.getMax());
        assertEquals( 1, stats.getMin());
        assertEquals(93.0 / 5, stats.getAverage(), 0.001);
        }

    @Test
    public void testAnyMatch()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertTrue (stream.anyMatch(n -> n > 30));
        assertFalse(stream.anyMatch(n -> n > 50));
        }

    @Test
    public void testAllMatch()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertTrue (stream.allMatch(n -> n < 50));
        assertFalse(stream.allMatch(n -> n < 30));
        }

    @Test
    public void testNoneMatch()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertTrue (stream.noneMatch(n -> n > 50));
        assertFalse(stream.noneMatch(n -> n < 10));
        }

    @Test
    public void testAnyMatchEmptyStream()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertFalse(stream.filter(n -> n > 50).anyMatch(n -> n < 50));
        }

    @Test
    public void testAllMatchEmptyStream()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertTrue(stream.filter(n -> n > 50).allMatch(n -> n < 50));
        }

    @Test
    public void testNoneMatchEmptyStream()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertTrue(stream.filter(n -> n > 50).noneMatch(n -> n < 50));
        }

    @Test
    public void testFindFirst()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertTrue(stream.findFirst().isPresent());
        }

    @Test
    public void testFindAny()
        {
        RemoteLongStream stream = getStream().mapToLong(e -> e.extract(Person::getAge));
        assertTrue(stream.findAny().isPresent());
        }

    @Test
    public void testPipelineBuilder()
        {
        RemoteStream<InvocableMap.Entry<String, Person>> stream = StreamSupport.pipelineBuilder();
        assertTrue(stream.map(InvocableMap.Entry::getValue)
                           .mapToLong(Person::getAge)
                           .pipeline() instanceof RemoteLongStream);
        }

    @Test(expected = IllegalStateException.class)
    public void testPipelineBuilderTerminal()
        {
        RemoteStream<InvocableMap.Entry<String, Person>> stream = StreamSupport.pipelineBuilder();
        stream.map(InvocableMap.Entry::getValue)
                       .mapToLong(Person::getAge)
                       .max();
        }
    }
