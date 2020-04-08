/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.WrapperNamedCache;

import com.tangosol.util.extractor.KeyExtractor;
import data.pof.Person;
import data.pof.PortablePerson;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static com.tangosol.util.Filters.equal;
import static com.tangosol.util.Filters.greaterEqual;
import static com.tangosol.util.Filters.less;

import static org.junit.Assert.assertEquals;


/**
 * Unit tests for Filter DSL.
 *
 * @author as  2014.07.31
 */
@SuppressWarnings({"unchecked", "deprecation"})
public class FilterDslTest
    {
    protected NamedCache<String, Person> getNamedCache()
        {
        return new WrapperNamedCache<>(new HashMap<>(), "test");
        }

    @Test
    public void testFilterDSL()
        {
        InvocableMap<String, Person> people = getNamedCache();
        people.put("Aleks", new PortablePerson("Aleks", DOB_ALEKS, 40));
        people.put("Marija",   new PortablePerson("Marija", DOB_MARIJA, 36));
        people.put("Ana",      new PortablePerson("Ana", DOB_ANA, 10));
        people.put("Novak",    new PortablePerson("Novak", DOB_NOVAK, 6));
        people.put("Kristina", new PortablePerson("Kristina", DOB_KRISTINA, 1));

        Map<String, Person> expected = new HashMap<>();
        expected.put("Aleks",  new PortablePerson("Aleks", DOB_ALEKS, 40));
        expected.put("Ana",    new PortablePerson("Ana", DOB_ANA, 10));

        assertEquals(expected,
                     people.invokeAll(less(KeyExtractor.of(String::length), 6)
                                          .and(equal(Person::getName, "Ana")
                                               .or(greaterEqual(PortablePerson::getAge, 18))),
                                      InvocableMap.Entry::getValue));

        }

    private static final Date DOB_ALEKS    = new Date( 74,  7, 24);
    private static final Date DOB_MARIJA   = new Date( 78,  1, 20);
    private static final Date DOB_ANA      = new Date(104,  7, 14);
    private static final Date DOB_NOVAK    = new Date(107, 11, 28);
    private static final Date DOB_KRISTINA = new Date(113,  1, 13);
    }
