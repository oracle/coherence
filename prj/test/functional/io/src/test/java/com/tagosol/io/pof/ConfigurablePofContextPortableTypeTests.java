/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tagosol.io.pof;


import com.tangosol.io.pof.ConfigurablePofContext;

import com.tangosol.net.NamedCache;

import common.AbstractFunctionalTest;

import data.portabletype.Address;
import data.portabletype.Person;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

/**
 * Tests for {@link ConfigurablePofContext} auto-generation POF Config for {@link PortableType}.
 *
 * @author tam  2020.08.21
 */
public class ConfigurablePofContextPortableTypeTests
    extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public ConfigurablePofContextPortableTypeTests()
        {
        super(getCacheConfig());
        }

    // ----- test lifecycle -------------------------------------------------

    @Before
    public void setup()
        {
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.pof.enabled", "true");
        System.setProperty("coherence.log.level", "9");
        AbstractFunctionalTest._startup();
        }

    @After
    public void teardown()
        {
        AbstractFunctionalTest._shutdown();
        }

    /**
     * This test validates that classes annotated with PortableType can be
     * used with POF without creating a pof config file.
     */
    @Test
    public void testGenericPortableTypes()
        {
        NamedCache<Integer, Person> ncPerson = getNamedCache("person");
        Address work = getRandomAddress("work");
        Address home = getRandomAddress("home");

        Person person = new Person(1,"Tim", home, work);
        ncPerson.clear();
        ncPerson.put(person.getId(), person);

        assertThat(ncPerson.size(), is(1));
        Person person2 = ncPerson.get(person.getId());
        assertThat(person2, is(person));
        }

    //----- helpers ---------------------------------------------------------

    protected Address getRandomAddress(String sType)
        {
        String sAddress1 = "work".equalsIgnoreCase(sType)
                    ? ((RANDOM.nextInt(100) + 1) + " James Street")
                    : ((RANDOM.nextInt(100) + 1) + " William Street");
        return new Address(
                (RANDOM.nextInt(100) + 1) + " James Street",
                "",
                "Perth",
                "Western Australia",
                Integer.valueOf(RANDOM.nextInt(999) + 6000).toString(),
                "Australia");
        }

    //----- constants -------------------------------------------------------

   /**
     * Return the project name.
     */
   public static String getProjectName()
        {
        return "portabletype";
        }

   public static String getCacheConfig()
       {
       return  "server-cache-config.xml";
       }

   private static final Random RANDOM = new Random();
   }
