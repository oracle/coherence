/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.internal.util.PartitionedIndexMap.PartitionedIndex;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.MapIndex;
import com.tangosol.util.SimpleMapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.IndexAwareFilter;
import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.LessFilter;

import data.repository.Gender;
import data.repository.Person;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.BeforeClass;
import org.junit.Test;

import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SuppressWarnings("unchecked")
public class PartitionedIndexMapTest
    {
    @BeforeClass
    public static void populateIndices()
        {
        Person aleks  = new Person().name("Aleksandar").age(48).gender(Gender.MALE);
        Person marija = new Person().name("Marija"    ).age(45).gender(Gender.FEMALE);
        Person ana    = new Person().name("Ana Maria" ).age(18).gender(Gender.FEMALE);
        Person tal    = new Person().name("Tal"       ).age(18).gender(Gender.MALE);
        Person nole   = new Person().name("Novak"     ).age(15).gender(Gender.MALE);
        Person kiki   = new Person().name("Kristina"  ).age(10).gender(Gender.FEMALE);

        DistributedCacheService service = Mockito.mock(DistributedCacheService.class);
        Mockito.when(service.getPartitionCount()).thenReturn(3);

        BackingMapManagerContext bmmc = Mockito.mock(BackingMapManagerContext.class);
        Mockito.when(bmmc.getCacheService()).thenReturn(service);
        Mockito.when(bmmc.getKeyPartition("aleks" )).thenReturn(0);
        Mockito.when(bmmc.getKeyPartition("ana"   )).thenReturn(0);
        Mockito.when(bmmc.getKeyPartition("tal"   )).thenReturn(1);
        Mockito.when(bmmc.getKeyPartition("marija")).thenReturn(1);
        Mockito.when(bmmc.getKeyPartition("nole"  )).thenReturn(1);
        Mockito.when(bmmc.getKeyPartition("kiki"  )).thenReturn(2);
        Mockito.when(bmmc.getKeyPartition("kiki"  )).thenReturn(2);

        BackingMapContext bmc = s_bmc = Mockito.mock(BackingMapContext.class);
        Mockito.when(bmc.getManagerContext()).thenReturn(bmmc);

        s_mapIndex = new TreeMap<>();
        s_mapIndex.computeIfAbsent(0, nPart -> new HashMap<>()).put(NAME, createIndex(bmc, NAME, false, Map.of("aleks", aleks, "ana", ana)));
        s_mapIndex.computeIfAbsent(0, nPart -> new HashMap<>()).put(AGE, createIndex(bmc, AGE, true, Map.of("aleks", aleks, "ana", ana)));
        s_mapIndex.computeIfAbsent(0, nPart -> new HashMap<>()).put(GENDER, createIndex(bmc, GENDER, false, Map.of("aleks", aleks, "ana", ana)));
        s_mapIndex.computeIfAbsent(0, nPart -> new HashMap<>()).put(ADULT, createIndex(bmc, ADULT, false, Map.of("aleks", aleks, "ana", ana)));
        s_mapIndex.computeIfAbsent(1, nPart -> new HashMap<>()).put(NAME, createIndex(bmc, NAME, false, Map.of("marija", marija, "nole", nole, "tal", tal)));
        s_mapIndex.computeIfAbsent(1, nPart -> new HashMap<>()).put(AGE, createIndex(bmc, AGE, true, Map.of("marija", marija, "nole", nole, "tal", tal)));
        s_mapIndex.computeIfAbsent(1, nPart -> new HashMap<>()).put(GENDER, createIndex(bmc, GENDER, false, Map.of("marija", marija, "nole", nole, "tal", tal)));
        s_mapIndex.computeIfAbsent(1, nPart -> new HashMap<>()).put(ADULT, createIndex(bmc, ADULT, false, Map.of("marija", marija, "nole", nole, "tal", tal)));
        s_mapIndex.computeIfAbsent(2, nPart -> new HashMap<>()).put(NAME, createIndex(bmc, NAME, false, Map.of("kiki", kiki)));
        s_mapIndex.computeIfAbsent(2, nPart -> new HashMap<>()).put(AGE, createIndex(bmc, AGE, true, Map.of("kiki", kiki)));
        s_mapIndex.computeIfAbsent(2, nPart -> new HashMap<>()).put(GENDER, createIndex(bmc, GENDER, false, Map.of("kiki", kiki)));
        s_mapIndex.computeIfAbsent(2, nPart -> new HashMap<>()).put(ADULT, createIndex(bmc, ADULT, false, Map.of("kiki", kiki)));

        dumpIndices();
        }

    @Test
    public void testIndexCreation()
        {
        PartitionedIndexMap<String, Person> pim = new PartitionedIndexMap<>(s_bmc, s_mapIndex, partitions(0, 1, 2));

        // assert that all indices have been created
        assertThat(pim.size(), is(4));
        assertThat(pim.keySet(), containsInAnyOrder(NAME, AGE, GENDER, ADULT));
        assertThat(pim.values().iterator().next(), is(notNullValue()));
        assertThat(pim.entrySet().iterator().next(), is(notNullValue()));

        // assert attributes of created indices
        assertThat(pim.get(NAME).getValueExtractor(), is(NAME));
        assertThat(pim.get(NAME).isOrdered(), is(false));
        assertThat(pim.get(NAME).isPartial(), is(false));
        assertThat(pim.get(NAME).getComparator(), is(nullValue()));
        assertThat(pim.get(AGE).getValueExtractor(), is(AGE));
        assertThat(pim.get(AGE).isOrdered(), is(true));
        assertThat(pim.get(AGE).isPartial(), is(false));
        assertThat(pim.get(AGE).getComparator(), is(SafeComparator.INSTANCE()));
        assertThat(pim.get(GENDER).getValueExtractor(), is(GENDER));
        assertThat(pim.get(GENDER).isOrdered(), is(false));
        assertThat(pim.get(GENDER).isPartial(), is(false));
        assertThat(pim.get(GENDER).getComparator(), is(nullValue()));
        assertThat(pim.get(ADULT).getValueExtractor(), is(ADULT));
        assertThat(pim.get(ADULT).isOrdered(), is(false));
        assertThat(pim.get(ADULT).isPartial(), is(false));
        assertThat(pim.get(ADULT).getComparator(), is(nullValue()));
        }

    @Test
    public void testSinglePartition()
        {
        PartitionedIndexMap<String, Person> pim = new PartitionedIndexMap<>(s_bmc, s_mapIndex, partitions(1));

        // assert that we use partition index directly for a single partition
        assertThat(pim.get(NAME), isA(SimpleMapIndex.class));
        assertThat(pim.get(AGE), isA(SimpleMapIndex.class));
        assertThat(pim.get(GENDER), isA(SimpleMapIndex.class));
        assertThat(pim.get(ADULT), isA(SimpleMapIndex.class));
        assertThat(pim.get(Person::getSalary), is(nullValue()));

        // assert index contents
        assertThat(pim.get(NAME).getIndexContents().keySet(), containsInAnyOrder("Marija", "Novak", "Tal"));
        assertThat(pim.get(NAME).getIndexContents().isEmpty(), is(false));
        assertThat(pim.get(NAME).getIndexContents().size(), is(3));
        assertThat(pim.get(NAME).getIndexContents().containsKey("Novak"), is(true));
        assertThat(pim.get(NAME).getIndexContents().containsKey("Kristina"), is(false));
        assertThat(pim.get(AGE).getIndexContents().keySet(), contains(15, 18, 45));
        assertThat(pim.get(AGE).getIndexContents().size(), is(3));
        assertThat(pim.get(GENDER).getIndexContents().keySet(), containsInAnyOrder(Gender.MALE, Gender.FEMALE));
        assertThat(pim.get(GENDER).getIndexContents().size(), is(2));
        assertThat(pim.get(GENDER).getIndexContents().containsValue(Set.of("marija")), is(true));
        assertThat(pim.get(GENDER).getIndexContents().containsValue(Set.of("nole", "tal")), is(true));
        assertThat(pim.get(GENDER).getIndexContents().containsValue(Set.of("unknown")), is(false));
        assertThat(pim.get(ADULT).getIndexContents().keySet(), containsInAnyOrder(true, false));
        assertThat(pim.get(ADULT).getIndexContents().size(), is(2));
        assertThat(pim.get(ADULT).getIndexContents().containsValue(Set.of("marija", "tal")), is(true));
        assertThat(pim.get(ADULT).getIndexContents().containsValue(Set.of("nole")), is(true));

        // assert inverse indices
        assertThat(applyIndex(adults(), pim), containsInAnyOrder("marija", "tal"));
        assertThat(applyIndex(children(), pim), containsInAnyOrder("nole"));

        assertThat(applyIndex(girls(), pim), containsInAnyOrder("marija"));
        assertThat(applyIndex(boys(), pim), containsInAnyOrder("nole", "tal"));

        assertThat(applyIndex(elementarySchool(), pim), empty());
        assertThat(applyIndex(highSchool(), pim), containsInAnyOrder("nole"));
        assertThat(applyIndex(college(), pim), containsInAnyOrder("tal"));

        // assert forward indices
        assertThat(pim.get(NAME).get("aleks"), is(MapIndex.NO_VALUE));
        assertThat(pim.get(AGE ).get("aleks"), is(MapIndex.NO_VALUE));
        assertThat(pim.get(NAME).get("marija"), is("Marija"));
        assertThat(pim.get(AGE ).get("marija"), is(45));
        assertThat(pim.get(NAME).get("ana"), is(MapIndex.NO_VALUE));
        assertThat(pim.get(AGE ).get("ana"), is(MapIndex.NO_VALUE));
        assertThat(pim.get(NAME).get("tal"), is("Tal"));
        assertThat(pim.get(AGE ).get("tal"), is(18));
        assertThat(pim.get(NAME).get("nole"), is("Novak"));
        assertThat(pim.get(AGE ).get("nole"), is(15));
        assertThat(pim.get(NAME).get("kiki"), is(MapIndex.NO_VALUE));
        assertThat(pim.get(AGE ).get("kiki"), is(MapIndex.NO_VALUE));
        }

    @Test
    public void testMultiplePartitions()
        {
        PartitionedIndexMap<String, Person> pim = new PartitionedIndexMap<>(s_bmc, s_mapIndex, partitions(0, 2));

        // assert that we use partitioned index wrapper for multiple partitions
        assertThat(pim.get(NAME), isA(PartitionedIndex.class));
        assertThat(pim.get(AGE), isA(PartitionedIndex.class));
        assertThat(pim.get(GENDER), isA(PartitionedIndex.class));
        assertThat(pim.get(ADULT), isA(PartitionedIndex.class));
        assertThat(pim.get(Person::getSalary), is(nullValue()));

        // assert index contents
        assertThat(pim.get(NAME).getIndexContents().keySet(), containsInAnyOrder("Aleksandar", "Ana Maria", "Kristina"));
        assertThat(pim.get(NAME).getIndexContents().isEmpty(), is(false));
        assertThat(pim.get(NAME).getIndexContents().size(), is(3));
        assertThat(pim.get(NAME).getIndexContents().containsKey("Novak"), is(false));
        assertThat(pim.get(NAME).getIndexContents().containsKey("Kristina"), is(true));
        assertThat(pim.get(AGE).getIndexContents().keySet(), contains(10, 18, 48));
        assertThat(pim.get(AGE).getIndexContents().size(), is(3));
        assertThat(pim.get(GENDER).getIndexContents().keySet(), containsInAnyOrder(Gender.MALE, Gender.FEMALE));
        assertThat(pim.get(GENDER).getIndexContents().size(), is(2));
        assertThat(pim.get(GENDER).getIndexContents().containsValue(Set.of("aleks")), is(true));
        assertThat(pim.get(GENDER).getIndexContents().containsValue(Set.of("kiki", "ana")), is(true));
        assertThat(pim.get(GENDER).getIndexContents().containsValue(Set.of("unknown")), is(false));
        assertThat(pim.get(ADULT).getIndexContents().keySet(), containsInAnyOrder(true, false));
        assertThat(pim.get(ADULT).getIndexContents().size(), is(2));
        assertThat(pim.get(ADULT).getIndexContents().containsValue(Set.of("aleks", "ana")), is(true));
        assertThat(pim.get(ADULT).getIndexContents().containsValue(Set.of("kiki")), is(true));

        // assert inverse indices
        assertThat(applyIndex(adults(), pim), containsInAnyOrder("aleks", "ana"));
        assertThat(applyIndex(children(), pim), containsInAnyOrder("kiki"));

        assertThat(applyIndex(girls(), pim), containsInAnyOrder("ana", "kiki"));
        assertThat(applyIndex(boys(), pim), containsInAnyOrder("aleks"));

        assertThat(applyIndex(elementarySchool(), pim), containsInAnyOrder("kiki"));
        assertThat(applyIndex(highSchool(), pim), empty());
        assertThat(applyIndex(college(), pim), containsInAnyOrder("ana"));

        // assert forward indices
        assertThat(pim.get(NAME).get("aleks"), is("Aleksandar"));
        assertThat(pim.get(AGE ).get("aleks"), is(48));
        assertThat(pim.get(NAME).get("marija"), is(MapIndex.NO_VALUE));
        assertThat(pim.get(AGE ).get("marija"), is(MapIndex.NO_VALUE));
        assertThat(pim.get(NAME).get("ana"), is("Ana Maria"));
        assertThat(pim.get(AGE ).get("ana"), is(18));
        assertThat(pim.get(NAME).get("tal"), is(MapIndex.NO_VALUE));
        assertThat(pim.get(AGE ).get("tal"), is(MapIndex.NO_VALUE));
        assertThat(pim.get(NAME).get("nole"), is(MapIndex.NO_VALUE));
        assertThat(pim.get(AGE ).get("nole"), is(MapIndex.NO_VALUE));
        assertThat(pim.get(NAME).get("kiki"), is("Kristina"));
        assertThat(pim.get(AGE ).get("kiki"), is(10));
        }

    @Test
    public void testAllPartitions()
        {
        PartitionedIndexMap<String, Person> pim = new PartitionedIndexMap<>(s_bmc, s_mapIndex, null);

        // assert that we use partitioned index wrapper for all partitions
        assertThat(pim.get(NAME), isA(PartitionedIndex.class));
        assertThat(pim.get(AGE), isA(PartitionedIndex.class));
        assertThat(pim.get(GENDER), isA(PartitionedIndex.class));
        assertThat(pim.get(ADULT), isA(PartitionedIndex.class));
        assertThat(pim.get(Person::getSalary), is(nullValue()));

        // assert index contents
        assertThat(pim.get(NAME).getIndexContents().keySet(), containsInAnyOrder("Aleksandar", "Marija", "Ana Maria", "Tal", "Novak", "Kristina"));
        assertThat(pim.get(NAME).getIndexContents().isEmpty(), is(false));
        assertThat(pim.get(NAME).getIndexContents().size(), is(6));
        assertThat(pim.get(NAME).getIndexContents().containsKey("Novak"), is(true));
        assertThat(pim.get(NAME).getIndexContents().containsKey("Kristina"), is(true));
        assertThat(pim.get(AGE).getIndexContents().keySet(), contains(10, 15, 18, 45, 48));
        assertThat(pim.get(AGE).getIndexContents().size(), is(5));
        assertThat(pim.get(GENDER).getIndexContents().keySet(), containsInAnyOrder(Gender.MALE, Gender.FEMALE));
        assertThat(pim.get(GENDER).getIndexContents().size(), is(2));
        assertThat(pim.get(GENDER).getIndexContents().containsValue(Set.of("aleks", "nole", "tal")), is(true));
        assertThat(pim.get(GENDER).getIndexContents().containsValue(Set.of("kiki", "ana", "marija")), is(true));
        assertThat(pim.get(GENDER).getIndexContents().containsValue(Set.of("unknown")), is(false));
        assertThat(pim.get(ADULT).getIndexContents().keySet(), containsInAnyOrder(true, false));
        assertThat(pim.get(ADULT).getIndexContents().size(), is(2));
        assertThat(pim.get(ADULT).getIndexContents().containsValue(Set.of("aleks", "ana", "marija", "tal")), is(true));
        assertThat(pim.get(ADULT).getIndexContents().containsValue(Set.of("nole", "kiki")), is(true));

        // assert inverse indices
        assertThat(applyIndex(adults(), pim), containsInAnyOrder("aleks", "marija", "ana", "tal"));
        assertThat(applyIndex(children(), pim), containsInAnyOrder("nole", "kiki"));

        assertThat(applyIndex(girls(), pim), containsInAnyOrder("marija", "ana", "kiki"));
        assertThat(applyIndex(boys(), pim), containsInAnyOrder("aleks", "nole", "tal"));

        assertThat(applyIndex(elementarySchool(), pim), containsInAnyOrder("kiki"));
        assertThat(applyIndex(highSchool(), pim), containsInAnyOrder("nole"));
        assertThat(applyIndex(college(), pim), containsInAnyOrder("ana", "tal"));

        // assert forward indices
        assertThat(pim.get(NAME).get("aleks"), is("Aleksandar"));
        assertThat(pim.get(AGE ).get("aleks"), is(48));
        assertThat(pim.get(NAME).get("marija"), is("Marija"));
        assertThat(pim.get(AGE ).get("marija"), is(45));
        assertThat(pim.get(NAME).get("ana"), is("Ana Maria"));
        assertThat(pim.get(AGE ).get("ana"), is(18));
        assertThat(pim.get(NAME).get("tal"), is("Tal"));
        assertThat(pim.get(AGE ).get("tal"), is(18));
        assertThat(pim.get(NAME).get("nole"), is("Novak"));
        assertThat(pim.get(AGE ).get("nole"), is(15));
        assertThat(pim.get(NAME).get("kiki"), is("Kristina"));
        assertThat(pim.get(AGE ).get("kiki"), is(10));
        }

    // ---- helpers ---------------------------------------------------------
    
    private Set<String> applyIndex(IndexAwareFilter<Object, Person> filter, PartitionedIndexMap<String, Person> pim)
        {
        Map<Integer, Set<String>> mapKeysByPartition = Map.of(
                0, Set.of("aleks", "ana"),
                1, Set.of("marija", "nole", "tal"),
                2, Set.of("kiki")
        );

        // only add candidate keys for partitions covered by the PartitionedIndexMap view
        Set<String>  setCandidateKeys = new HashSet<>();
        for (int nPart : pim.getPartitions())
            {
            setCandidateKeys.addAll(mapKeysByPartition.get(nPart));
            }

        assertThat(filter.applyIndex(pim, setCandidateKeys), nullValue());

        return setCandidateKeys;
        }

    private static PartitionSet partitions(int... aParts)
        {
        PartitionSet partitionSet = new PartitionSet(3);
        for (int nPart : aParts)
            {
            partitionSet.add(nPart);
            }
        return partitionSet;
        }

    private static MapIndex<String, Person, Object> createIndex(BackingMapContext ctx, ValueExtractor<Person, ?> extractor, boolean fOrdered, Map<String, Person> map)
        {
        SimpleMapIndex index = new SimpleMapIndex(ValueExtractor.of(extractor), fOrdered, null, ctx);
        map.entrySet().forEach(index::insert);
        return index;
        }

    private static void dumpIndices()
        {
        for (Map.Entry<Integer, Map<ValueExtractor<Person, ?>, MapIndex<String, Person, ?>>> mapPart : s_mapIndex.entrySet())
            {
            System.out.printf("\nPartition #%d:", mapPart.getKey());
            for (Map.Entry<ValueExtractor<Person, ?>, MapIndex<String, Person, ?>> mapIndex : mapPart.getValue().entrySet())
                {
                System.out.printf("\n%16s:  %s", mapIndex.getKey(), ((SimpleMapIndex) mapIndex.getValue()).toString(true));
                }
            }
        }

    // ---- filters ---------------------------------------------------------

    private static IndexAwareFilter<Object, Person> adults()
        {
        return new EqualsFilter<>(Person::isAdult, true);
        }

    private static IndexAwareFilter<Object, Person> children()
        {
        return new EqualsFilter<>(Person::isAdult, false);
        }

    private static IndexAwareFilter<Object, Person> boys()
        {
        return new EqualsFilter<>(Person::getGender, Gender.MALE);
        }

    private static IndexAwareFilter<Object, Person> girls()
        {
        return new EqualsFilter<>(Person::getGender, Gender.FEMALE);
        }

    private static IndexAwareFilter<Object, Person> elementarySchool()
        {
        return new LessFilter<>(Person::getAge, 14);
        }

    private static IndexAwareFilter<Object, Person> highSchool()
        {
        return new BetweenFilter<>(Person::getAge, 14, 17);
        }

    private static IndexAwareFilter<Object, Person> college()
        {
        return (IndexAwareFilter<Object, Person>)
                new GreaterEqualsFilter<>(Person::getAge, 18)
                .and(new LessEqualsFilter<>(Person::getAge, 22));
        }

    // ---- constants -------------------------------------------------------

    public static final ValueExtractor<Person, String>  NAME   = ValueExtractor.of(Person::getName);
    public static final ValueExtractor<Person, Integer> AGE    = ValueExtractor.of(Person::getAge);
    public static final ValueExtractor<Person, Gender>  GENDER = ValueExtractor.of(Person::getGender);
    public static final ValueExtractor<Person, Boolean> ADULT  = ValueExtractor.of(Person::isAdult);

    // ---- static data members ---------------------------------------------

    private static Map<Integer, Map<ValueExtractor<Person, ?>, MapIndex<String, Person, ?>>> s_mapIndex;

    private static BackingMapContext s_bmc;
    }
