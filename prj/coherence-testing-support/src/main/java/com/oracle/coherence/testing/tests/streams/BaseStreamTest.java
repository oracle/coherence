/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.tests.streams;


import com.tangosol.internal.util.DoubleSummaryStatistics;
import com.tangosol.internal.util.IntSummaryStatistics;
import com.tangosol.internal.util.LongSummaryStatistics;
import com.tangosol.internal.util.collection.PortableMap;
import com.tangosol.internal.util.collection.PortableSortedMap;
import com.tangosol.internal.util.stream.StreamSupport;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.stream.RemoteStream;
import data.pof.Person;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.tangosol.util.Filters.greater;
import static com.tangosol.util.Filters.less;
import static com.tangosol.util.stream.RemoteCollectors.averagingDouble;
import static com.tangosol.util.stream.RemoteCollectors.averagingInt;
import static com.tangosol.util.stream.RemoteCollectors.averagingLong;
import static com.tangosol.util.stream.RemoteCollectors.collectingAndThen;
import static com.tangosol.util.stream.RemoteCollectors.counting;
import static com.tangosol.util.stream.RemoteCollectors.groupingBy;
import static com.tangosol.util.stream.RemoteCollectors.mapping;
import static com.tangosol.util.stream.RemoteCollectors.summarizingDouble;
import static com.tangosol.util.stream.RemoteCollectors.summarizingInt;
import static com.tangosol.util.stream.RemoteCollectors.summarizingLong;
import static com.tangosol.util.stream.RemoteCollectors.summingDouble;
import static com.tangosol.util.stream.RemoteCollectors.summingInt;
import static com.tangosol.util.stream.RemoteCollectors.summingLong;
import static com.tangosol.util.stream.RemoteCollectors.toList;
import static com.tangosol.util.stream.RemoteCollectors.toMap;
import static com.tangosol.util.stream.RemoteCollectors.toSet;
import static com.tangosol.util.stream.RemoteCollectors.toSortedSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * @author as  2014.10.02
 */
@SuppressWarnings({"unchecked", "Convert2MethodRef"})
public abstract class BaseStreamTest
        extends AbstractStreamTest
    {
    protected BaseStreamTest(boolean fParallel)
        {
        super(fParallel);
        }

    @Test
    public void testOuterFilter()
        {
        assertEquals(3, getStream(less(Person::getAge, 18)).count());
        }

    @Test
    public void testInnerFilter()
        {
        assertEquals(2, getStream().filter(e -> e.getValue().getAge() > 18).count());
        }

    @Test
    public void testPeekAndCollect()
        {
        SortedSet<String> names = getStream(Person::getName)
                        .peek((p) -> System.out.println(p))
                        .map(String::toUpperCase)
                        .collect(toSortedSet());
        assertEquals(sortedSet("ALEKS", "ANA MARIA", "KRISTINA", "MARIJA", "NOVAK"), names);
        }

    @Test
    public void testFlatMap()
        {
        SortedSet<String> names = getStream(Person::getChildren)
                .flatMap(Arrays::stream)
                .peek((p) -> System.out.println(p))
                .map(Person::getName)
                .collect(toSortedSet());

        assertEquals(sortedSet("Ana Maria", "Kristina", "Novak"), names);
        }

    @Test
    public void testLimit()
        {
        List<String> names = getStream(Person::getName)
                .sorted()
                .limit(3)
                .collect(toList());

        assertEquals(Arrays.asList("Aleks", "Ana Maria", "Kristina"), names);
        }

    @Test
    public void testSkip()
        {
        List<String> names = getStream(Person::getName)
                .sorted()
                .skip(3)
                .collect(toList());

        assertEquals(Arrays.asList("Marija", "Novak"), names);
        }

    @Test
    public void testForEach()
        {
        SortedSet<String> names = new TreeSet<>();
        getStream(greater(Person::getAge, 18), Person::getName)
                .forEach((name) -> names.add(name));
        assertEquals(sortedSet("Aleks", "Marija"), names);
        }

    @Test
    public void testToArray()
        {
        Object[] names = getStream(greater(Person::getAge, 18), Person::getName)
                .toArray();

        assertEquals(sortedSet("Aleks", "Marija"), sortedSet(names));
        }

    @Test
    public void testToArrayWithGenerator()
        {
        String[] names = getStream(greater(Person::getAge, 18), Person::getName)
                .sorted()
                .toArray(String[]::new);

        assertEquals(sortedSet("Aleks", "Marija"), sortedSet(names));
        }

    @Test
    public void testReduce()
        {
        assertEquals(93, (int) getStream(Person::getAge).reduce((sum, age) -> sum += age).get());
        }

    @Test
    public void testReduceWithIdentity()
        {
        assertEquals(93, (int) getStream(Person::getAge).reduce(0, (sum, age) -> sum += age));
        }

    @Test
    public void testReduceWithAccumulator()
        {
        assertEquals(93, (long) getStream(Person::getAge).reduce(0L, (sum, age) -> sum += age, Long::sum));
        }

    @Test
    public void testCollect()
        {
        SortedSet<String> names = getStream(greater(Person::getAge, 18), Person::getName)
                .collect(toSortedSet());
        assertEquals(sortedSet("Aleks", "Marija"), names);
        }

    @Test
    public void testSummingIntCollector()
        {
        assertEquals(93, (int) getStream()
                        .collect(summingInt(Person::getAge)));
        }

    @Test
    public void testSummingLongCollector()
        {
        assertEquals(93L, (long) getStream()
                .collect(summingLong(Person::getAge)));
        }

    @Test
    public void testSummingDoubleCollector()
        {
        assertEquals(93.0, getStream()
                .collect(summingDouble(Person::getAge)), 0.001);
        }

    @Test
    public void testAveragingIntCollector()
        {
        assertEquals(93.0 / 5, getStream()
                .collect(averagingInt(Person::getAge)), 0.001);
        }

    @Test
    public void testAveragingLongCollector()
        {
        assertEquals(93.0 / 5, getStream()
                .collect(averagingLong(Person::getAge)), 0.001);
        }

    @Test
    public void testAveragingDoubleCollector()
        {
        assertEquals(93.0 / 5, getStream()
                .collect(averagingDouble(Person::getAge)), 0.001);
        }

    @Test
    public void testCollectingAndThenCollector()
        {
        String names = getStream(greater(Person::getAge, 18))
                .map(e -> e.extract(Person::getName))
                .collect(collectingAndThen(toSortedSet(), Set::toString));
        assertTrue("[Aleks, Marija]".equals(names) ||
                   "[Marija, Aleks]".equals(names));
        }

    @Test
    public void testCountingCollector()
        {
        assertEquals(5L, (long) getStream().collect(counting()));
        }

    @Test
    public void testGroupingByCollector()
        {
        Map<Character, List<InvocableMap.Entry<String, Person>>> people = getStream()
                .collect(groupingBy(charAt(Person::getName, 0)));
        assertEquals(2, people.get('A').size());
        }

    @Test
    public void testGroupingByCollectorWithDownstream()
        {
        Map<Character, Integer> people = getStream()
                .collect(groupingBy(charAt(Person::getName, 0),
                                    summingInt(Person::getAge)));
        assertEquals(50, (int) people.get('A'));
        }

    @Test
    public void testGroupingByCollectorWithMapFactory()
        {
        Map<Character, Set<String>> people = getStream()
                .collect(groupingBy(charAt(Person::getName, 0),
                                    PortableSortedMap::new,
                                    mapping(e -> e.extract(Person::getName), toSet())));
        Set<String> expected = new HashSet<>(Arrays.asList("Aleks", "Ana Maria"));
        assertEquals(expected, people.get('A'));
        }

    @Test
    public void testToMapCollector()
        {
        Map<String, Integer> nameAge = getStream()
                .collect(toMap(Person::getName,
                               Person::getAge));
        assertEquals(40, (int) nameAge.get("Aleks"));
        assertEquals(1, (int) nameAge.get("Kristina"));
        }

    @Test
    public void testToMapCollectorWithMerge()
        {
        Map<Character, Integer> nameAge = getStream()
                .collect(toMap(charAt(Person::getName, 0),
                               Person::getAge,
                               (s, a) -> s + a));
        assertEquals(50, (int) nameAge.get('A'));
        assertEquals(1, (int) nameAge.get('K'));
        }

    @Test
    public void testToMapCollectorWithMapFactory()
        {
        Map<Character, Integer> nameAge = getStream()
                .collect(toMap(charAt(Person::getName, 0),
                               Person::getAge,
                               (s, a) -> s + a,
                               PortableMap::new));
        assertEquals(50, (int) nameAge.get('A'));
        assertEquals(1, (int) nameAge.get('K'));
        }

    @Test
    public void testSummarizingIntCollector()
        {
        IntSummaryStatistics stats =
                getStream().collect(summarizingInt(Person::getAge));
        assertEquals(5L, stats.getCount());
        assertEquals(93, stats.getSum());
        assertEquals(40, stats.getMax());
        assertEquals( 1, stats.getMin());
        assertEquals(93.0 / 5, stats.getAverage(), 0.001);
        }

    @Test
    public void testSummarizingLongCollector()
        {
        LongSummaryStatistics stats =
                getStream().collect(summarizingLong(Person::getAge));
        assertEquals( 5L, stats.getCount());
        assertEquals(93L, stats.getSum());
        assertEquals(40L, stats.getMax());
        assertEquals( 1L, stats.getMin());
        assertEquals(93.0 / 5, stats.getAverage(), 0.001);
        }

    @Test
    public void testSummarizingDoubleCollector()
        {
        DoubleSummaryStatistics stats =
                getStream().collect(summarizingDouble(Person::getAge));
        assertEquals(  5L, stats.getCount());
        assertEquals(93.0, stats.getSum(), 0.001);
        assertEquals(40.0, stats.getMax(), 0.001);
        assertEquals( 1.0, stats.getMin(), 0.001);
        assertEquals(93.0 / 5, stats.getAverage(), 0.001);
        }

    @Test
    public void testMin()
        {
        assertEquals("Kristina", getStream()
                .min(Person::getAge).get().getValue().getName());
        }

    @Test
    public void testMax()
        {
        InvocableMap.Entry<String, Person> entry = getStream()
                .max(Person::getAge).get();
        assertEquals("Aleks", entry.getValue().getName());
        }

    @Test
    public void testCount()
        {
        assertEquals(5, getStream().count());
        }

    @Test
    public void testAnyMatch()
        {
        assertTrue (getStream().anyMatch(e -> e.extract(Person::getAge) > 30));
        assertFalse(getStream().anyMatch(e -> e.extract(Person::getAge) > 50));
        }

    @Test
    public void testAllMatch()
        {
        assertTrue (getStream().allMatch(e -> e.extract(Person::getAge) < 50));
        assertFalse(getStream().allMatch(e -> e.extract(Person::getAge) < 30));
        }

    @Test
    public void testNoneMatch()
        {
        assertTrue (getStream().noneMatch(e -> e.extract(Person::getAge) > 50));
        assertFalse(getStream().noneMatch(e -> e.extract(Person::getAge) < 10));
        }

    @Test
    public void testAnyMatchEmptyStream()
        {
        assertFalse(getStream().filter(e -> e.extract(Person::getAge) > 50).anyMatch(e -> e.extract(Person::getAge) < 50));
        }

    @Test
    public void testAllMatchEmptyStream()
        {
        assertTrue(getStream().filter(e -> e.extract(Person::getAge) > 50).allMatch(e -> e.extract(Person::getAge) < 50));
        }

    @Test
    public void testNoneMatchEmptyStream()
        {
        assertTrue(getStream().filter(e -> e.extract(Person::getAge) > 50).noneMatch(e -> e.extract(Person::getAge) < 50));
        }

    @Test
    public void testFindFirst()
        {
        assertTrue(getStream().findFirst().isPresent());
        }

    @Test
    public void testFindAny()
        {
        assertTrue(getStream().findAny().isPresent());
        }

    @Test
    public void testPipelineBuilder()
        {
        RemoteStream<InvocableMap.Entry<String, Person>> stream = StreamSupport.pipelineBuilder();
        assertTrue(stream.map(e -> e.extract(Person::getAge)).pipeline() instanceof RemoteStream);
        }

    @Test(expected = IllegalStateException.class)
    public void testPipelineBuilderTerminal()
        {
        RemoteStream<InvocableMap.Entry<String, Person>> stream = StreamSupport.pipelineBuilder();
        stream.mapToInt(e -> e.extract(Person::getAge)).max();
        }

    // ----- static helpers -------------------------------------------------

    static <T> ValueExtractor<T, Character> charAt(ValueExtractor<? super T, ? extends String> extractor, int nIndex)
        {
        return target ->
            {
            String s = extractor.extract(target);
            return s.length() > nIndex ? s.charAt(nIndex) : null;
            };
        }

    protected static <E> SortedSet<E> sortedSet(E... elements)
        {
        SortedSet<E> set = new TreeSet<>();
        set.addAll(Arrays.asList(elements));
        return set;
        }
    }
