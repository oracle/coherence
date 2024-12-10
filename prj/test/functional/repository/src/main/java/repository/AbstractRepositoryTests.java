/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package repository;

import com.oracle.coherence.repository.AbstractRepository;
import com.oracle.coherence.repository.AbstractRepositoryBase;
import com.tangosol.net.NamedMap;

import com.tangosol.util.Filter;
import com.tangosol.util.Fragment;
import com.tangosol.util.function.Remote;
import com.tangosol.util.stream.RemoteCollectors;

import data.pof.Address;

import data.repository.Gender;
import data.repository.Person;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.tangosol.util.Extractors.fragment;
import static com.tangosol.util.Filters.always;
import static com.tangosol.util.Filters.equal;
import static com.tangosol.util.Filters.greater;
import static com.tangosol.util.Filters.isFalse;
import static com.tangosol.util.Filters.isTrue;
import static com.tangosol.util.Filters.less;

import static data.repository.Gender.FEMALE;
import static data.repository.Gender.MALE;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.isOneOf;

/**
 * Abstract base class containing all the tests for Repository API.
 *
 * @author Aleks Seovic  2021.02.11
 */
public abstract class AbstractRepositoryTests
    {
    protected abstract NamedMap<String, Person> getMap();
    protected abstract AbstractRepository<String, Person> people();

    @Before
    public void populateRepository()
        {
        getMap().clear();
        people().saveAll(Stream.of(
                new Person("aleks").name("Aleks")
                        .dateOfBirth(LocalDate.of(1974, 8, 24))
                        .age(46).gender(MALE).height(79).weight(260.0).salary(BigDecimal.valueOf(5000)),
                new Person("marija").name("Marija")
                        .dateOfBirth(LocalDate.of(1978, 2, 20))
                        .age(43).gender(FEMALE).height(66).weight(130.0).salary(BigDecimal.valueOf(10_000)),
                new Person("ana").name("Ana Maria")
                        .dateOfBirth(LocalDate.of(2004, 8, 14))
                        .age(16).gender(FEMALE).height(68).weight(120.0).salary(BigDecimal.valueOf(1000)),
                new Person("nole").name("Novak")
                        .dateOfBirth(LocalDate.of(2007, 12, 28))
                        .age(13).gender(MALE).height(65).weight(125.0).salary(BigDecimal.valueOf(600)),
                new Person("kiki").name("Kristina")
                        .dateOfBirth(LocalDate.of(2013, 2, 13))
                        .age(8).gender(FEMALE).height(50).weight(60.0).salary(BigDecimal.valueOf(400))
                ));
        }

    @Test
    public void testSave()
        {
        getMap().clear();
        people().save(new Person("aleks").name("Aleks").age(46));

        assertThat(getMap().get("aleks").getName(), is("Aleks"));
        assertThat(getMap().size(), is(1));
        }

    @Test
    public void testSaveArrayOfEntities()
        {
        assertThat(getMap().size(), is(5));
        assertThat(getMap().get("aleks").getName(), is("Aleks"));
        assertThat(getMap().get("marija").getName(), is("Marija"));
        assertThat(getMap().get("ana").getName(), is("Ana Maria"));
        assertThat(getMap().get("nole").getName(), is("Novak"));
        assertThat(getMap().get("kiki").getName(), is("Kristina"));
        }

    @Test
    public void testSaveCollectionOfEntities()
        {
        getMap().clear();
        Set<Person> setPeople = new HashSet<>();
        setPeople.add(new Person("aleks").name("Aleks").age(46));
        setPeople.add(new Person("marija").name("Marija").age(43));
        setPeople.add(new Person("ana").name("Ana Maria").age(16));
        setPeople.add(new Person("nole").name("Novak").age(13));
        setPeople.add(new Person("kiki").name("Kristina").age(8));

        people().saveAll(setPeople);

        assertThat(getMap().size(), is(5));
        assertThat(getMap().get("aleks").getName(), is("Aleks"));
        assertThat(getMap().get("marija").getName(), is("Marija"));
        assertThat(getMap().get("ana").getName(), is("Ana Maria"));
        assertThat(getMap().get("nole").getName(), is("Novak"));
        assertThat(getMap().get("kiki").getName(), is("Kristina"));
        }

    @Test
    public void testGetById()
        {
        assertThat(people().get("kiki").getName(), is("Kristina"));
        }

    @Test
    public void testExists()
        {
        getMap().clear();
        assertThat(people().exists("aleks"), is(false));
        people().save(new Person("aleks").name("Aleks").age(46));
        assertThat(people().exists("aleks"), is(true));
        }

    @Test
    public void testGetAll()
        {
        Collection<? extends Person> results = people().getAll();
        assertThat(results.size(), is(5));
        assertThat(results.stream().map(Person::getName).collect(Collectors.toList()),
                           containsInAnyOrder("Kristina",  "Novak", "Ana Maria", "Marija", "Aleks"));
        }

    @Test
    public void testGetAllOrdered()
        {
        Collection<? extends Person> results = people().getAllOrderedBy(Person::getAge);
        assertThat(results.size(), is(5));
        assertThat(results.stream().map(Person::getName).collect(Collectors.toList()),
                           contains("Kristina",  "Novak", "Ana Maria", "Marija", "Aleks"));
        }

    @Test
    public void testGetAllOrderedByComparator()
        {
        Collection<? extends Person> results = people().getAllOrderedBy(Remote.comparator(Person::getAge).reversed());
        assertThat(results.size(), is(5));
        assertThat(results.stream().map(Person::getName).collect(Collectors.toList()),
                   contains("Aleks", "Marija", "Ana Maria", "Novak", "Kristina"));
        }

    @Test
    public void testGetAllById()
        {
        Collection<? extends Person> results = people().getAll(setOf("kiki", "ana"));
        assertThat(results.size(), is(2));
        assertThat(results.stream().map(Person::getName).collect(Collectors.toList()),
                           containsInAnyOrder("Kristina",  "Ana Maria"));
        }

    @Test
    public void testGetAllFiltered()
        {
        Collection<? extends Person> results = people().getAll(less(Person::getAge, 10));
        assertThat(results.size(), is(1));
        assertThat(results.iterator().next().getName(), is("Kristina"));
        }

    @Test
    public void testGetAllFilteredOrdered()
        {
        Collection<? extends Person> results = people()
                .getAllOrderedBy(greater(Person::getAge, 10), Person::getName);
        assertThat(results.size(), is(4));
        assertThat(results.stream().map(Person::getName).collect(Collectors.toList()),
                   contains("Aleks", "Ana Maria", "Marija", "Novak"));
        }

    @Test
    public void testGetSingleAttribute()
        {
        assertThat(people().get("nole", Person::getAge), is(13));
        }

    @Test
    public void testGetFragment()
        {
        Fragment<Person> fragment = people().get("nole", fragment(Person::getName, Person::getAge));
        assertThat(fragment.get(Person::getName), is("Novak"));
        assertThat(fragment.get(Person::getAge), is(13));
        }

    @Test
    public void testGetAllSingleAttribute()
        {
        Map<String, Integer> ages = people().getAll(Person::getAge);
        assertThat(ages.get("aleks"), is(46));
        assertThat(ages.get("marija"), is(43));
        assertThat(ages.get("ana"), is(16));
        assertThat(ages.get("nole"), is(13));
        assertThat(ages.get("kiki"), is(8));
        }

    @Test
    public void testGetAllSingleAttributeByIds()
        {
        Map<String, Integer> ages = people().getAll(Collections.singleton("kiki"), Person::getAge);
        assertThat(ages.size(), is(1));
        assertThat(ages.get("kiki"), is(8));
        }

    @Test
    public void testGetAllFragment()
        {
        Map<String, Fragment<Person>> map = people().getAll(fragment(Person::getName, Person::getAge));
        assertThat(map.get("aleks").get(Person::getAge), is(46));
        assertThat(map.get("marija").get(Person::getAge), is(43));
        assertThat(map.get("ana").get(Person::getAge), is(16));
        assertThat(map.get("nole").get(Person::getAge), is(13));
        assertThat(map.get("kiki").get(Person::getAge), is(8));
        }

    @Test
    public void testGetAllFragmentById()
        {
        Map<String, Fragment<Person>> map = people().getAll(Collections.singleton("ana"),
                                                            fragment(Person::getName, Person::getAge));
        assertThat(map.size(), is(1));
        assertThat(map.get("ana").get(Person::getAge), is(16));
        }

    @Test
    public void testUpdate()
        {
        people().update("kiki", Person::setWeight, 65.0);
        assertThat(getMap().get("kiki").getWeight(), is(65.0));
        }

    @Test
    public void testUpdateWithBiFunction()
        {
        Person p = people().update("kiki", Person::weight, 65.0);
        assertThat(getMap().get("kiki"), is(p));
        assertThat(p.getWeight(), is(65.0));
        }

    @Test
    public void testUpdateWithFunction()
        {
        Person kiki = people().update("kiki", p ->
            {
            p.setName(p.getName().toUpperCase());
            p.setGender(FEMALE);
            p.setWeight(65.0);
            return p;
            });
        assertThat(getMap().get("kiki"), is(kiki));
        assertThat(kiki.getWeight(), is(65.0));
        assertThat(kiki.getGender(), is(FEMALE));
        assertThat(kiki.getName(), is("KRISTINA"));
        }

    @Test
    public void testUpdateWithFactory()
        {
        people().update("mike", Person::setName, "Michael", Person::new);
        assertThat(getMap().get("mike").getName(), is("Michael"));
        }

    @Test
    public void testUpdateWithBiFunctionAndFactory()
        {
        Person p = people().update("mike", Person::name, "Michael", Person::new);
        assertThat(getMap().get("mike"), is(p));
        assertThat(p.getName(), is("Michael"));
        }

    @Test
    public void testUpdateWithFunctionAndFactory()
        {
        Person mike = people().update("mike", p ->
            {
            p.setName("Michael");
            p.setGender(MALE);
            p.setWeight(220.0);
            return p;
            }, Person::new);

        assertThat(getMap().get("mike"), is(mike));
        assertThat(mike.getWeight(), is(220.0));
        assertThat(mike.getGender(), is(MALE));
        assertThat(mike.getName(), is("Michael"));
        }

    @Test
    public void testUpdateAll()
        {
        people().updateAll(always(), Person::setAddress,
                           new Address("123 Main St", "Tampa", "FL", "33555"));
        assertThat(getMap().get("aleks").getAddress().getCity(), is("Tampa"));
        assertThat(getMap().get("marija").getAddress().getCity(), is("Tampa"));
        assertThat(getMap().get("ana").getAddress().getCity(), is("Tampa"));
        assertThat(getMap().get("nole").getAddress().getCity(), is("Tampa"));
        assertThat(getMap().get("kiki").getAddress().getCity(), is("Tampa"));
        }

    @Test
    public void testUpdateAllWithBiFunction()
        {
        Map<String, Person> adults = people().updateAll(isTrue(Person::isAdult), Person::salary, BigDecimal.valueOf(1000.0));
        assertThat(adults.size(), is(2));
        assertThat(getMap().get("aleks").getSalary(), is(BigDecimal.valueOf(1000.0)));
        assertThat(adults.get("aleks").getSalary(), is(BigDecimal.valueOf(1000.0)));
        assertThat(getMap().get("marija").getSalary(), is(BigDecimal.valueOf(1000.0)));
        assertThat(adults.get("marija").getSalary(), is(BigDecimal.valueOf(1000.0)));
        }

    @Test
    public void testUpdateAllWithFunction()
        {
        Map<String, Person> kids = people().updateAll(isFalse(Person::isAdult), p ->
            {
            p.setName(p.getName().toUpperCase());
            p.setSalary(BigDecimal.ZERO);
            return p;
            });

        assertThat(kids.size(), is(3));
        assertThat(getMap().get("ana"), is(kids.get("ana")));
        assertThat(getMap().get("nole"), is(kids.get("nole")));
        assertThat(getMap().get("kiki"), is(kids.get("kiki")));
        assertThat(kids.get("ana").getName(), is("ANA MARIA"));
        assertThat(kids.get("nole").getName(), is("NOVAK"));
        assertThat(kids.get("kiki").getName(), is("KRISTINA"));
        }

    @Test
    public void testRemoveById()
        {
        assertThat(people().removeById("aleks"), is(true));
        assertThat(people().removeById("aleks"), is(false));
        assertThat(people().count(), is(4L));

        assertThat(people().removeById("marija", true).getName(), is("Marija"));
        assertThat(people().removeById("ana", false), nullValue());
        assertThat(people().count(), is(2L));

        assertThat(people().removeById("ana", true), nullValue());
        assertThat(people().count(), is(2L));
        }

    @Test
    public void testRemove()
        {
        Person aleks  = getMap().get("aleks");
        Person marija = getMap().get("marija");
        Person ana    = getMap().get("ana");

        assertThat(people().remove(aleks), is(true));
        assertThat(people().remove(aleks), is(false));
        assertThat(people().count(), is(4L));

        assertThat(people().remove(marija, true).getName(), is("Marija"));
        assertThat(people().remove(ana, false), nullValue());
        assertThat(people().count(), is(2L));

        assertThat(people().remove(ana, true), nullValue());
        assertThat(people().count(), is(2L));
        }

    @Test
    public void testRemoveAll()
        {
        Person aleks  = getMap().get("aleks");
        Person marija = getMap().get("marija");
        Person ana    = getMap().get("ana");
        Person nole   = getMap().get("nole");

        assertThat(people().removeAll(Stream.of(aleks, marija)), is(true));
        assertThat(people().removeAll(Stream.of(aleks, marija)), is(false));
        assertThat(people().count(), is(3L));

        Map<String, Person> map = people().removeAll(setOf(aleks, marija, ana, nole), true);
        assertThat(map.get("aleks"), nullValue());
        assertThat(map.get("marija"), nullValue());
        assertThat(map.get("ana"), is(ana));
        assertThat(map.get("nole"), is(nole));
        assertThat(people().count(), is(1L));
        }

    @Test
    public void testRemoveAllFilter()
        {
        Person ana    = getMap().get("ana");
        Person nole   = getMap().get("nole");

        assertThat(people().removeAll(isTrue(Person::isAdult)), is(true));
        assertThat(people().removeAll(isTrue(Person::isAdult)), is(false));
        assertThat(people().count(), is(3L));

        Map<String, Person> map = people().removeAll(greater(Person::getAge, 10), true);
        assertThat(map.size(), is(2));
        assertThat(map.get("ana"), is(ana));
        assertThat(map.get("nole"), is(nole));
        assertThat(people().count(), is(1L));
        }

    @Test
    public void testCount()
        {
        assertThat(people().count(), is(5L));
        assertThat(people().count(isTrue(Person::isAdult)), is(2L));
        assertThat(people().count(isFalse(Person::isAdult)), is(3L));
        }
    
    @Test
    public void testMax()
        {
        assertThat(people().max(Person::getAge), is(46));
        assertThat(people().max(Person::getHeight), is(79L));
        assertThat(people().max(Person::getWeight), is(260.0));
        assertThat(people().max(Person::getSalary), is(BigDecimal.valueOf(10_000)));
        assertThat(people().max(Person::getDateOfBirth), is(LocalDate.of(2013, 2, 13)));
        }

    @Test
    public void testMaxFilter()
        {
        Filter<Person> filter = equal(Person::getGender, FEMALE);

        assertThat(people().max(filter, Person::getAge), is(43));
        assertThat(people().max(filter, Person::getHeight), is(68L));
        assertThat(people().max(filter, Person::getWeight), is(130.0));
        assertThat(people().max(filter, Person::getSalary), is(BigDecimal.valueOf(10_000)));
        assertThat(people().max(filter, Person::getDateOfBirth), is(LocalDate.of(2013, 2, 13)));
        }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void testMaxBy()
        {
        assertThat(people().maxBy(Person::getAge).get(), is(getMap().get("aleks")));
        assertThat(people().maxBy(Person::getSalary).get(), is(getMap().get("marija")));
        }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void testMaxByFilter()
        {
        Filter<Person> filter = isFalse(Person::isAdult);

        assertThat(people().maxBy(filter, Person::getAge).get(), is(getMap().get("ana")));
        assertThat(people().maxBy(filter, Person::getWeight).get(), is(getMap().get("nole")));
        }

    @Test
    public void testMin()
        {
        assertThat(people().min(Person::getAge), is(8));
        assertThat(people().min(Person::getHeight), is(50L));
        assertThat(people().min(Person::getWeight), is(60.0));
        assertThat(people().min(Person::getSalary), is(BigDecimal.valueOf(400)));
        assertThat(people().min(Person::getDateOfBirth), is(LocalDate.of(1974, 8, 24)));
        }

    @Test
    public void testMinFilter()
        {
        Filter<Person> filter = equal(Person::getGender, MALE);

        assertThat(people().min(filter, Person::getAge), is(13));
        assertThat(people().min(filter, Person::getHeight), is(65L));
        assertThat(people().min(filter, Person::getWeight), is(125.0));
        assertThat(people().min(filter, Person::getSalary), is(BigDecimal.valueOf(600)));
        assertThat(people().min(filter, Person::getDateOfBirth), is(LocalDate.of(1974, 8, 24)));
        }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void testMinBy()
        {
        assertThat(people().minBy(Person::getAge).get(), is(getMap().get("kiki")));
        assertThat(people().minBy(Person::getDateOfBirth).get(), is(getMap().get("aleks")));
        }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void testMinByFilter()
        {
        Filter<Person> filter = isTrue(Person::isAdult);

        assertThat(people().minBy(filter, Person::getAge).get(), is(getMap().get("marija")));
        assertThat(people().minBy(filter, Person::getWeight).get(), is(getMap().get("marija")));
        }

    @Test
    public void testSum()
        {
        assertThat(people().sum(Person::getAge), is(126L));
        assertThat(people().sum(Person::getHeight), is(328L));
        assertThat(people().sum(Person::getWeight), is(695.0));
        assertThat(people().sum(Person::getSalary), is(BigDecimal.valueOf(17_000)));
        }

    @Test
    public void testSumFilter()
        {
        Filter<Person> filter = isTrue(Person::isAdult);

        assertThat(people().sum(filter, Person::getAge), is(89L));
        assertThat(people().sum(filter, Person::getHeight), is(145L));
        assertThat(people().sum(filter, Person::getWeight), is(390.0));
        assertThat(people().sum(filter, Person::getSalary), is(BigDecimal.valueOf(15_000)));
        }

    @Test
    public void testAverage()
        {
        assertThat(people().average(Person::getAge), is(25.2));
        assertThat(people().average(Person::getHeight), is(65.6));
        assertThat(people().average(Person::getWeight), is(139.0));
        assertThat(people().average(Person::getSalary), is(new BigDecimal("3400.00000000")));
        }

    @Test
    public void testAverageFilter()
        {
        Filter<Person> filter = isTrue(Person::isAdult);

        assertThat(people().average(filter, Person::getAge), is(44.5));
        assertThat(people().average(filter, Person::getHeight), is(72.5));
        assertThat(people().average(filter, Person::getWeight), is(195.0));
        assertThat(people().average(filter, Person::getSalary), is(new BigDecimal("7500.00000000")));
        }

    @Test
    public void testDistinct()
        {
        assertThat(people().distinct(Person::getName),
                   containsInAnyOrder("Aleks", "Marija", "Ana Maria", "Novak", "Kristina"));
        assertThat(people().distinct(isTrue(Person::isAdult), Person::getName),
                   containsInAnyOrder("Aleks", "Marija"));
        }

    @Test
    public void testGroupBy()
        {
        Map<Gender, Set<Person>> map = people().groupBy(Person::getGender);
        assertThat(map.size(), is(2));
        assertThat(map.get(MALE),
                   Matchers.containsInAnyOrder(getMap().get("aleks"), getMap().get("nole")));
        assertThat(map.get(FEMALE),
                   Matchers.containsInAnyOrder(getMap().get("marija"), getMap().get("ana"), getMap().get("kiki")));
        }

    @Test
    public void testGroupByFiltered()
        {
        Map<Gender, Set<Person>> map = people().groupBy(isTrue(Person::isAdult), Person::getGender);
        assertThat(map.size(), is(2));
        assertThat(map.get(MALE),
                   containsInAnyOrder(getMap().get("aleks")));
        assertThat(map.get(FEMALE),
                   containsInAnyOrder(getMap().get("marija")));
        }

    @Test
    public void testGroupByOrdered()
        {
        Map<Gender, SortedSet<Person>> map = people().groupBy(Person::getGender, Remote.comparator(Person::getAge));
        assertThat(map.size(), is(2));
        assertThat(map.get(MALE),
                   Matchers.contains(getMap().get("nole"), getMap().get("aleks")));
        assertThat(map.get(FEMALE),
                   Matchers.contains(getMap().get("kiki"), getMap().get("ana"), getMap().get("marija")));
        }

    @Test
    public void testGroupByOrderedFiltered()
        {
        Map<Gender, SortedSet<Person>> map = people().groupBy(isFalse(Person::isAdult),
                                                              Person::getGender,
                                                              Remote.comparator(Person::getAge).reversed());
        assertThat(map.size(), is(2));
        assertThat(map.get(MALE),
                   contains(getMap().get("nole")));
        assertThat(map.get(FEMALE),
                   Matchers.contains(getMap().get("ana"), getMap().get("kiki")));
        }

    @Test
    public void testGroupByCollector()
        {
        Map<Gender, Long> map = people().groupBy(Person::getGender, RemoteCollectors.summingLong(Person::getAge));
        assertThat(map.size(), is(2));
        assertThat(map.get(MALE), is(59L));
        assertThat(map.get(FEMALE), is(67L));
        }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void testGroupByCollectorFiltered() throws Throwable
        {
        Map<Boolean, Optional<Person>> map = people().groupBy(less(Person::getWeight, 200.0),
                               Person::isAdult,
                               RemoteCollectors.maxBy(Person::getAge));

        assertThat(map.size(), is(2));
        assertThat(map.get(true).get(), is(getMap().get("marija")));
        assertThat(map.get(false).get(), is(getMap().get("ana")));
        }

    @Test
    public void testGroupByCollectorWithSuppliedMap()
        {
        Map<Integer, Long> map = people().groupBy(Person::getAge, TreeMap::new, RemoteCollectors.counting());
        assertThat(map.size(), is(5));
        assertThat(map.keySet(), contains(8, 13, 16, 43, 46));
        assertThat(map.values(), contains(1L, 1L, 1L, 1L, 1L));
        }

    @Test
    public void testGroupByCollectorWithSuppliedMapFiltered()
        {
        Map<Integer, Long> map = people().groupBy(isFalse(Person::isAdult), Person::getAge, TreeMap::new, RemoteCollectors.counting());
        assertThat(map.size(), is(3));
        assertThat(map.keySet(), contains(8, 13, 16));
        assertThat(map.values(), contains(1L, 1L, 1L));
        }

    @Test
    public void testTop()
        {
        assertThat(people().top(Person::getAge, 2), contains(46, 43));
        assertThat(people().top(Person::getAge, Remote.Comparator.reverseOrder(), 2), contains(8, 13));
        }

    @Test
    public void testTopFilter()
        {
        assertThat(people().top(isFalse(Person::isAdult), Person::getAge, 2), contains(16, 13));
        assertThat(people().top(isTrue(Person::isAdult), Person::getAge, Remote.Comparator.reverseOrder(), 2), contains(43, 46));
        }

    @Test
    public void testTopBy()
        {
        assertThat(people().topBy(Person::getAge, 2),
                   Matchers.contains(getMap().get("aleks"), getMap().get("marija")));
        assertThat(people().topBy(Remote.Comparator.comparingInt(Person::getAge).reversed(), 2),
                   Matchers.contains(getMap().get("kiki"), getMap().get("nole")));
        }

    @Test
    public void testTopByFilter()
        {
        assertThat(people().topBy(isFalse(Person::isAdult), Person::getAge, 2),
                   Matchers.contains(getMap().get("ana"), getMap().get("nole")));
        assertThat(people().topBy(isTrue(Person::isAdult), Remote.Comparator.comparingInt(Person::getAge).reversed(), 2),
                   Matchers.contains(getMap().get("marija"), getMap().get("aleks")));
        }

    @Test
    public void testKeyListener() throws InterruptedException
        {
        getMap().clear();

        AtomicInteger cInsert = new AtomicInteger(0);
        AtomicInteger cUpdate = new AtomicInteger(0);
        AtomicInteger cRemove = new AtomicInteger(0);
        CountDownLatch insert = new CountDownLatch(1);
        CountDownLatch update = new CountDownLatch(1);
        CountDownLatch remove = new CountDownLatch(1);

        AbstractRepositoryBase.Listener<Person> listener = people().listener()
                .onInsert(person ->
                          {
                          insert.countDown();
                          cInsert.incrementAndGet();
                          assertThat(person.getName(), is("Aleks"));
                          })
                .onUpdate(person ->
                          {
                          update.countDown();
                          cUpdate.incrementAndGet();
                          assertThat(person.getName(), is("ALEKS"));
                          })
                .onRemove(person ->
                          {
                          remove.countDown();
                          cRemove.incrementAndGet();
                          assertThat(person.getName(), is("ALEKS"));
                          })
                .build();

        people().addListener("aleks", listener);

        populateRepository();

        if (!insert.await(1, TimeUnit.SECONDS))
            {
            Assert.fail("Didn't receive insert event");
            }

        people().updateAll(isTrue(Person::isAdult), p ->
            {
            p.setName(p.getName().toUpperCase());
            return null;
            });

        if (!update.await(1, TimeUnit.SECONDS))
            {
            Assert.fail("Didn't receive update event");
            }

        people().removeById("aleks");

        if (!remove.await(1, TimeUnit.SECONDS))
            {
            Assert.fail("Didn't receive remove event");
            }

        people().removeListener("aleks", listener);
        people().update("aleks", Person::setName, "Aleks", Person::new);
        Thread.sleep(500L);

        assertThat(cInsert.get(), is(1));
        assertThat(cUpdate.get(), is(1));
        assertThat(cRemove.get(), is(1));
        }

    @Test
    public void testFilterListener() throws InterruptedException
        {
        getMap().clear();

        AtomicInteger cInsert = new AtomicInteger(0);
        AtomicInteger cUpdate = new AtomicInteger(0);
        AtomicInteger cRemove = new AtomicInteger(0);
        CountDownLatch insert = new CountDownLatch(2);
        CountDownLatch update = new CountDownLatch(2);
        CountDownLatch remove = new CountDownLatch(1);

        AbstractRepositoryBase.Listener<Person> listener = people().listener()
                .onInsert(person ->
                          {
                          insert.countDown();
                          cInsert.incrementAndGet();
                          assertThat(person.getName(), isOneOf("Aleks", "Marija"));
                          })
                .onUpdate((personOld, personNew) ->
                          {
                          update.countDown();
                          cUpdate.incrementAndGet();
                          assertThat(personOld.getName(), isOneOf("Aleks", "Marija"));
                          assertThat(personNew.getName(), isOneOf("ALEKS", "MARIJA"));
                          })
                .onRemove(person ->
                          {
                          remove.countDown();
                          cRemove.incrementAndGet();
                          assertThat(person.getName(), isOneOf("ALEKS", "MARIJA"));
                          })
                .build();

        people().addListener(isTrue(Person::isAdult), listener);
        populateRepository();

        if (!insert.await(1, TimeUnit.SECONDS))
            {
            Assert.fail("Didn't receive insert events");
            }

        people().updateAll(always(), p ->
            {
            p.setName(p.getName().toUpperCase());
            return null;
            });

        if (!update.await(1, TimeUnit.SECONDS))
            {
            Assert.fail("Didn't receive update events");
            }

        people().removeById("aleks");
        people().removeById("ana");

        if (!remove.await(1, TimeUnit.SECONDS))
            {
            Assert.fail("Didn't receive remove event");
            }

        people().removeListener(isTrue(Person::isAdult), listener);
        people().update("aleks", Person::setName, "Aleks", Person::new);
        Thread.sleep(500L);

        assertThat(cInsert.get(), is(2));
        assertThat(cUpdate.get(), is(2));
        assertThat(cRemove.get(), is(1));
        }

    @Test
    public void testGlobalListener() throws InterruptedException
        {
        getMap().clear();

        AtomicInteger cInsert = new AtomicInteger(0);
        AtomicInteger cUpdate = new AtomicInteger(0);
        AtomicInteger cRemove = new AtomicInteger(0);
        CountDownLatch insert = new CountDownLatch(5);
        CountDownLatch update = new CountDownLatch(5);
        CountDownLatch remove = new CountDownLatch(5);

        AbstractRepositoryBase.Listener<Person> listener = people().listener()
                .onInsert(person ->
                          {
                          insert.countDown();
                          cInsert.incrementAndGet();
                          assertThat(person.getName(), isOneOf("Aleks", "Marija", "Ana Maria", "Novak", "Kristina"));
                          })
                .onUpdate((personOld, personNew) ->
                          {
                          update.countDown();
                          cUpdate.incrementAndGet();

                          assertThat(personOld.getName(), isOneOf("Aleks", "Marija", "Ana Maria", "Novak", "Kristina"));
                          assertThat(personNew.getName(), isOneOf("ALEKS", "MARIJA", "ANA MARIA", "NOVAK", "KRISTINA"));
                          })
                .onRemove(person ->
                          {
                          remove.countDown();
                          cRemove.incrementAndGet();
                          assertThat(person.getName(), isOneOf("ALEKS", "MARIJA", "ANA MARIA", "NOVAK", "KRISTINA"));
                          })
                .build();

        people().addListener(listener);
        populateRepository();

        if (!insert.await(1, TimeUnit.SECONDS))
            {
            Assert.fail("Didn't receive insert events");
            }

        people().updateAll(always(), p ->
            {
            p.setName(p.getName().toUpperCase());
            return null;
            });

        if (!update.await(1, TimeUnit.SECONDS))
            {
            Assert.fail("Didn't receive update events");
            }

        people().removeAll(always());

        if (!remove.await(1, TimeUnit.SECONDS))
            {
            Assert.fail("Didn't receive remove event");
            }

        people().removeListener(listener);
        people().update("aleks", Person::setName, "Aleks", Person::new);
        Thread.sleep(500L);

        assertThat(cInsert.get(), is(5));
        assertThat(cUpdate.get(), is(5));
        assertThat(cRemove.get(), is(5));
        }

    // ---- helpers ---------------------------------------------------------

    @SafeVarargs
    private static <T> Set<T> setOf(T... values)
        {
        return Stream.of(values).collect(Collectors.toSet());
        }

    /**
     * Not used directly, but references from the documentation
     * so we want to make sure it compiles.
     */
    // tag::listener[]
    public static class PeopleListener
            implements AbstractRepositoryBase.Listener<Person>
        {
        public void onInserted(Person personNew)
            {
            // handle INSERT event
            }

        public void onUpdated(Person personOld, Person personNew)
            {
            // handle UPDATE event
            }

        public void onRemoved(Person personOld)
            {
            // handle REMOVE event
            }
        }
    // end::listener[]
    }
