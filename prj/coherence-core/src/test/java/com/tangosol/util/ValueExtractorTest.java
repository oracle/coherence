/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.io.pof.reflect.SimplePofPath;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import data.pof.Person;
import data.pof.PersonLite;

import org.junit.Test;

import static com.tangosol.util.extractor.AbstractExtractor.KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author as  2015.08.19
 */
public class ValueExtractorTest
    {
    @Test
    public void testCompatibility()
        {
        ValueExtractor<Person, String> lambdaName     = ValueExtractor.of(Person::getName);
        ValueExtractor<Person, String> reflectionName = new ReflectionExtractor<>("getName");
        ValueExtractor<Person, String> pofName        = new PofExtractor<>(String.class, PersonLite.NAME, "name");

        assertEquals(lambdaName, reflectionName);
        assertEquals(reflectionName, lambdaName);
        assertEquals(lambdaName.hashCode(), reflectionName.hashCode());

        assertEquals(lambdaName, pofName);
        assertEquals(pofName, lambdaName);
        assertEquals(lambdaName.hashCode(), pofName.hashCode());

        assertEquals(reflectionName, pofName);
        assertEquals(pofName, reflectionName);
        assertEquals(reflectionName.hashCode(), pofName.hashCode());

        // validate that a ValueExtractor of target type VALUE is
        // not equals to a ValueExtractor of target type KEY
        ValueExtractor<Person, String> reflectionTypeKeyName =
                new ReflectionExtractor<>("getName", null, KEY);

        assertNotEquals(reflectionTypeKeyName, lambdaName);
        assertNotEquals(pofName, reflectionTypeKeyName);
        assertNotEquals(reflectionTypeKeyName, pofName);
        }

    /**
     * Test dynamic lambdas using method references where the method is declared
     * on a derived / super class.
     *
     * \@see COH-14627
     */
    @Test
    public void testDerivation()
        {
        ValueExtractor<Person, String>     vePerson     = ValueExtractor.of(Person::getName);
        ValueExtractor<PersonLite, String> vePersonLite = ValueExtractor.of(PersonLite::getName);

        String     sName      = "Sigmund Freud";
        Person     person     = new Person(sName, null);
        PersonLite personLite = new PersonLite(sName, null);

        assertEquals(sName, vePerson.extract(person));
        assertEquals(sName, vePersonLite.extract(person));
        assertEquals(sName, vePersonLite.extract(personLite));
        }
    }
