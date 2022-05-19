/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package io;


import com.tangosol.net.NamedCache;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.portabletype.Address;

import data.portabletype.Country;
import data.portabletype.Person;
import org.junit.After;

import java.util.Random;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Common functionality for PortableType tests.
 *
 * @author tam  2020.08.21
 */
public abstract class AbstractPortableTypeTests
      extends AbstractFunctionalTest {

    //----- constructors ----------------------------------------------------

    public AbstractPortableTypeTests(String sPath)
        {
        super(sPath);
        }

    //----- test lifecycle --------------------------------------------------

    @After
    public void teardown()
        {
        AbstractFunctionalTest._shutdown();
        }


    //----- helpers ---------------------------------------------------------

    protected void runTest()
        {
        NamedCache<Integer, Person> ncPerson = getNamedCache("person");
        Address                     work     = getRandomAddress("work");
        Address                     home     = getRandomAddress("home");

        Person person = new Person(1,"Tim", home, work);
        ncPerson.clear();
        ncPerson.put(person.getId(), person);

        assertThat(ncPerson.size(), is(1));
        Person person2 = ncPerson.get(person.getId());
        assertThat(person2, is(person));

        NamedCache<String, Country> ncCountry = getNamedCache("countries");
        Country australia = new Country("AU", "Australia");
        Country USA = new Country("USA", "United Stated of America");
        ncCountry.clear();
        ncCountry.put(australia.getCode(), australia);
        ncCountry.put(USA.getCode(), USA);
        assertThat(ncCountry.size(), is(2));
        assertThat(ncCountry.get("AU"), is(australia));
        assertThat(ncCountry.get("USA"), is(USA));
        }

    protected Address getRandomAddress(String sType)
        {
        return new Address(
                (RANDOM.nextInt(100) + 1) + " James Street",
                "",
                "Perth",
                "Western Australia",
                Integer.valueOf(RANDOM.nextInt(999) + 6000).toString(),
                "Australia");
        }

    //----- constants -------------------------------------------------------
    
   protected static String getProjectName()
        {
        return "portabletype";
        }

   protected static String getCacheConfig()
       {
       return  "server-cache-config.xml";
       }

   private static final Random RANDOM = new Random();
}
