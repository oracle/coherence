/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.repository;

import com.tangosol.net.NamedMap;

import data.repository.Person;

/**
 * Simple test repository implementation.
 *
 * @author Aleks Seovic  2021.02.12
 */
@Accelerated
// tag::doc[]
public class PeopleRepository
        extends AbstractRepository<String, Person>
    {
    private NamedMap<String, Person> people;

    public PeopleRepository(NamedMap<String, Person> people)
        {
        this.people = people;
        }

    protected NamedMap<String, Person> getMap()            // <1>
        {
        return people;
        }

    protected String getId(Person person)                  // <2>
        {
        return person.getSsn();
        }

    protected Class<? extends Person> getEntityType()      // <3>
        {
        return Person.class;
        }
    }
// end::doc[]
