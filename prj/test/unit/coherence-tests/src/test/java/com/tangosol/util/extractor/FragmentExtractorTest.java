/*
 * Copyright (c) 2001, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.extractor;

import com.tangosol.util.Fragment;
import com.tangosol.util.ValueExtractor;

import data.pof.Address;
import data.repository.Person;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static com.tangosol.util.Extractors.fragment;

import static data.repository.Gender.MALE;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link FragmentExtractor} class.
 *
 * @author Aleks Seovic  2021.02.22
 */
public class FragmentExtractorTest
    {
    private Person aleks;

    @Before
    public void setUp()
        {
        aleks = new Person("aleks")
                .name("Aleks")
                .age(46)
                .dateOfBirth(LocalDate.of(1974, 8, 24))
                .gender(MALE)
                .weight(259.2)
                .salary(BigDecimal.valueOf(1000.00))
                .address(new Address("555 Main St", "Lutz", "FL", "33559"));
        }

    @Test
    public void shouldExtractDirectAttributes()
        {
        Fragment<Person> fragment = fragment(Person::getName,
                                             Person::getAge,
                                             Person::getAddress).extract(aleks);

        System.out.println(fragment);

        assertThat(fragment.get(Person::getName), is(aleks.getName()));
        assertThat(fragment.get("name"), is(aleks.getName()));
        assertThat(fragment.get(Person::getAge), is(aleks.getAge()));
        assertThat(fragment.get("age"), is(aleks.getAge()));
        assertThat(fragment.get(Person::getAddress), is(aleks.getAddress()));
        assertThat(fragment.get("address"), is(aleks.getAddress()));
        }

    @Test
    public void shouldExtractNestedAttributes()
        {
        Fragment<Person> fragment = fragment(Person::getName,
                                             Person::getAge,
                                             fragment(Person::getAddress,
                                                      Address::getCity,
                                                      fragment(Address::getStreet,
                                                               String::length
                                                      )
                                             )).extract(aleks);

        System.out.println(fragment);
        System.out.println(fragment.get("address").toString());

        assertThat(fragment.get(Person::getName), is(aleks.getName()));
        assertThat(fragment.get("name"), is(aleks.getName()));
        assertThat(fragment.get(Person::getAge), is(aleks.getAge()));
        assertThat(fragment.get("age"), is(aleks.getAge()));
        assertThat(fragment.getFragment(Person::getAddress).get(Address::getCity), is(aleks.getAddress().getCity()));
        assertThat(fragment.getFragment("address").get("city"), is(aleks.getAddress().getCity()));
        assertThat(fragment.getFragment(Person::getAddress).getFragment(Address::getStreet).get(String::length), is(aleks.getAddress().getStreet().length()));
        assertThat(fragment.getFragment("address").getFragment("street").get("length()"), is(aleks.getAddress().getStreet().length()));
        }

    @Test
    @SuppressWarnings("Convert2MethodRef")
    public void shouldCreatePositionalAttributesForMissingCanonicalName()
        {
        ValueExtractor<Person, String>  exName   = p -> p.getName();
        ValueExtractor<Person, Integer> exAge    = p -> p.getAge();
        ValueExtractor<Address, String> exCity   = a -> a.getCity();
        ValueExtractor<String, Integer> exLength = s -> s.length();

        Fragment<Person> fragment = fragment(exName,
                                             exAge,
                                             fragment(Person::getAddress, exCity,
                                                      fragment(Address::getStreet, exLength
                                                      )
                                             )).extract(aleks);

        System.out.println(fragment);

        assertThat(fragment.get("$0"), is(aleks.getName()));
        assertThat(fragment.get("$1"), is(aleks.getAge()));
        assertThat(fragment.getFragment(Person::getAddress).get("$0"), is(aleks.getAddress().getCity()));
        assertThat(fragment.getFragment(Person::getAddress).getFragment(Address::getStreet).get("$0"), is(aleks.getAddress().getStreet().length()));
        }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldRecursivelyConvertFragmentToMap()
        {
        Fragment<Person> fragment = fragment(Person::getName,
                                             Person::getAge,
                                             fragment(Person::getAddress,
                                                      Address::getCity,
                                                      fragment(Address::getStreet,
                                                               String::length
                                                      )
                                             )).extract(aleks);

        Map<String, Object> mapPerson  = fragment.toMap();
        Map<String, Object> mapAddress = (Map<String, Object>) mapPerson.get("address");
        Map<String, Object> mapStreet = (Map<String, Object>) mapAddress.get("street");

        System.out.println(mapPerson);
        System.out.println(mapAddress.toString());

        assertThat(mapPerson.get("name"), is(aleks.getName()));
        assertThat(mapPerson.get("age"), is(aleks.getAge()));
        assertThat(mapAddress.get("city"), is(aleks.getAddress().getCity()));
        assertThat(mapStreet.get("length()"), is(aleks.getAddress().getStreet().length()));
        }

    }
