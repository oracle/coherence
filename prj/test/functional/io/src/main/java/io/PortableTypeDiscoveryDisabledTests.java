/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package io;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.schema.annotation.PortableType;
import com.tangosol.net.NamedCache;
        
import com.oracle.coherence.testing.AbstractFunctionalTest;

import data.portabletype.Address;
import data.portabletype.Person;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link ConfigurablePofContext} auto-generation POF Config for {@link PortableType}
 * where we have deliberately disabled auto-discovery via a custom pof-config.
 *
 * @author tam  2020.08.21
 */
public class PortableTypeDiscoveryDisabledTests
    extends AbstractPortableTypeTests
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public PortableTypeDiscoveryDisabledTests()
        {
        super(getCacheConfig());
        }

    // ----- test lifecycle -------------------------------------------------

    @Before
    public void setup()
        {
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.pof.enabled", "true");
        System.setProperty("coherence.pof.config", "portable-type-pof-config.xml");
        System.setProperty("coherence.log.level", "9");
        AbstractFunctionalTest._startup();
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
        try
            {
            ncPerson.put(person.getId(), person);
            Assert.fail("Error type discovery was enabled when it should be disabled");
            }
        catch (Exception e)
            {
            // expected exception due to type discovery being disabled
            }
        }
   }
